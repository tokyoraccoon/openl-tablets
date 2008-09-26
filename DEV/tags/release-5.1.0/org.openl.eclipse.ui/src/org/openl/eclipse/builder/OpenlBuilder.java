package org.openl.eclipse.builder;

import java.net.URLClassLoader;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.openl.OpenConfigurationException;
import org.openl.OpenL;
import org.openl.conf.ClassLoaderFactory;
import org.openl.conf.IUserContext;
import org.openl.conf.OpenLConfiguration;
import org.openl.conf.UserContext;
import org.openl.eclipse.base.OpenlBasePlugin;
import org.openl.eclipse.util.Debug;
import org.openl.eclipse.util.JDTUtil;
import org.openl.eclipse.util.UrlUtil;
import org.openl.main.OpenlMain;
import org.openl.syntax.SyntaxErrorException;
import org.openl.types.java.JavaOpenClass;
import org.openl.util.RuntimeExceptionWrapper;

public class OpenlBuilder extends IncrementalProjectBuilder {

	public OpenlBuilder() {
		Debug.debug("In OpenlBuilder");
	}

	class OpenlDeltaVisitor implements IResourceDeltaVisitor {
		/*
		 * (non-Javadoc)
		 * 
		 * @see org.eclipse.core.resources.IResourceDeltaVisitor#visit(org.eclipse.core.resources.IResourceDelta)
		 */
		public boolean visit(IResourceDelta delta) throws CoreException {
			IResource resource = delta.getResource();
			switch (delta.getKind()) {
			case IResourceDelta.ADDED:
				// handle added resource
				checkOpenl(resource);
				break;
			case IResourceDelta.REMOVED:
				// handle removed resource
				break;
			case IResourceDelta.CHANGED:
				// handle changed resource
				checkOpenl(resource);
				break;
			}
			// return true to continue visiting children.
			return true;
		}
	}

	class OpenlResourceVisitor implements IResourceVisitor {
		public boolean visit(IResource resource) {
			checkOpenl(resource);
			// return true to continue visiting children.
			return true;
		}
	}

	private void addMarker(IResource resource, SyntaxErrorException se, String openl) {
		OpenlMarkers.addMarkers(resource, se, openl);
	}

	private static final String MARKER_TYPE = OpenlBasePlugin.PLUGIN_ID
			+ ".openl.problem";

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.core.internal.events.InternalBuilder#build(int,
	 *      java.util.Map, org.eclipse.core.runtime.IProgressMonitor)
	 */
	protected IProject[] build(int kind, Map args, IProgressMonitor monitor)
			throws CoreException {
		if (kind == FULL_BUILD) {
			fullBuild(monitor);
		} else {
			IResourceDelta delta = getDelta(getProject());
			if (delta == null) {
				fullBuild(monitor);
			} else {
				incrementalBuild(delta, monitor);
			}
		}
		return null;
	}

	boolean isOpenlFile(IFile file) {
		return isOpenlExtension(file) && isOnSourcePath(file);
	}

	public static boolean isOnSourcePath(IFile file) {
		IJavaProject jp = asJavaProject(file.getProject());
		if (jp == null)
			return true;
		return jp.isOnClasspath(file) && isNotInOutputFolder(file, jp);
	}

	public static boolean isNotInOutputFolder(IFile file, IJavaProject jp) {
		try {
			IPath out = jp.getOutputLocation();
			return !out.isPrefixOf(file.getFullPath());
		} catch (JavaModelException e) {
			return true;
		}
	}

	static public IJavaProject asJavaProject(IProject project) {

		try {
			return (IJavaProject) project.getNature(JavaCore.NATURE_ID);
		} catch (CoreException e) {
			return null;
		}
	}

	static final String[] extensions = { ".openl", ".j", ".xls", ".j.science",
			".dom.xml" };

	boolean isOpenlExtension(IFile file) {
		for (int i = 0; i < extensions.length; i++) {

			if (file.getName().endsWith(extensions[i]))
				return true;
		}
		return false;
	}

	void checkOpenl(IResource resource) {
		IFile file = null;
		if (resource instanceof IFile)         
			file = (IFile) resource;
		else return;
		
		if (isOpenlExtension(file))
			deleteMarkers(file);
			
		if (isOpenlFile(file)) {
			OpenL openl = null;
			try {
				openl = getOpenlConfiguration(file);
				if (openl != null)
				   openl.compile(new EclipseFileSourceCodeModule(file));
			} catch (SyntaxErrorException se) {
				addMarker(file, se, openl.getName());
			} catch (Throwable t) {

				int severity = IMarker.SEVERITY_ERROR;

				OpenlMarkers.addMarker(file, file.getFullPath().toString(),
						"Unhandled Exception: " + t.getMessage(), severity,
						null, null, null);
				throw RuntimeExceptionWrapper.wrap(t);
			}

		}
	}

	IUserContext userContext;

	protected synchronized IUserContext getUserContext() {
		if (userContext == null) {
			userContext = new UserContext(getProjectClassLoader(),
					getProjectHome());
		}
		return userContext;
	}

	protected String getProjectHome() {

		return getProject().getLocation().toString();
	}
	
	
	public void resetOpenL()
	{
		userContext = null;
		OpenL.reset();
		OpenLConfiguration.reset();
		HashMap old = ClassLoaderFactory.reset();
		JavaOpenClass.resetAllClassloaders(old);
	}


	protected ClassLoader getProjectClassLoader() {

		String[] cp;
		try {
			cp = JDTUtil.getJavaProjectClasspath(getProject());
//			saveClasspath(cp, getProject());
		} catch (Exception e) {
			throw RuntimeExceptionWrapper.wrap(e);
		}
		
		boolean userHasOpenL = false;
		
		for (int i = 0; i < cp.length; i++) {
			if (cp[i].indexOf("org.openl.core") >= 0)
			{
				userHasOpenL = true;
				break;
			}	
		}
		
		return userHasOpenL ? new URLClassLoader(UrlUtil.toUrl(cp), OpenL.class.getClassLoader()) : new URLClassLoader(UrlUtil.toUrl(cp));
	}
	
	static final String openlPropertiesFname = "openl.project.classpath.properties";
	static final String openlClasspathProperty = "openl.project.classpath";
	

	OpenL getOpenlConfiguration(IFile file) {
		try
		{
			String openlName = getOpenlName(file.getName());
			return OpenL.getInstance(openlName, getUserContext());
		}
		catch(OpenConfigurationException oce)
		{
			int severity = IMarker.SEVERITY_ERROR;

			OpenlMarkers.addMarker(file, file.getFullPath().toString(),
					"Openl Configuration Exception: " + oce.getMessage(), severity,
					null, null, null);
			return null;
		}
	}

	private void deleteMarkers(IFile file) {
		try {
			file.deleteMarkers(MARKER_TYPE, false, IResource.DEPTH_ZERO);
		} catch (CoreException ce) {
		}
	}

	protected void fullBuild(final IProgressMonitor monitor)
			throws CoreException {
		try {
			resetOpenL();
			getProject().accept(new OpenlResourceVisitor());
		} catch (CoreException e) {
		}
	}

	protected void incrementalBuild(IResourceDelta delta,
			IProgressMonitor monitor) throws CoreException {
		// the visitor does the work.
		delta.accept(new OpenlDeltaVisitor());
	}

	static public int getEndOfLineIndex(String s) {
		int len = s.length();

		int lf = s.indexOf('\n');
		int cr = s.indexOf('\r');

		return Math.min(cr < 0 ? len : cr, lf < 0 ? len : lf);
	}

	static final int MAX_LINE_SIZE = 30;

	// Content description is located in the first line.
	// Similar <code>#!"shell"</code> in unix scripts.
	static public boolean isOpenlMethod(String code) {
		int indexOfOpenl = code.indexOf("openl");
		return 0 <= indexOfOpenl && indexOfOpenl < MAX_LINE_SIZE
				&& indexOfOpenl < getEndOfLineIndex(code);
	}

	static public String getOpenlName(String fileURL) {
		//TODO provide custom mapping
		return getDefaultOpenlName(fileURL);
	}

	static public String getDefaultOpenlName(String fileURL) {
		return OpenlMain.getOpenlName(fileURL);
	}

}
