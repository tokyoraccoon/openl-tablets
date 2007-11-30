package org.openl.rules.ui.repository;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.openl.rules.ui.repository.tree.AbstractTreeNode;
import org.openl.rules.ui.repository.tree.TreeDProject;
import org.openl.rules.ui.repository.tree.TreeFile;
import org.openl.rules.ui.repository.tree.TreeFolder;
import org.openl.rules.ui.repository.tree.TreeProject;
import org.openl.rules.ui.repository.tree.TreeRepository;
import org.openl.rules.workspace.abstracts.DeploymentDescriptorProject;
import org.openl.rules.workspace.abstracts.Project;
import org.openl.rules.workspace.abstracts.ProjectArtefact;
import org.openl.rules.workspace.abstracts.ProjectFolder;
import org.openl.rules.workspace.dtr.RepositoryException;
import org.openl.rules.workspace.uw.UserWorkspace;
import org.openl.rules.workspace.uw.UserWorkspaceDeploymentProject;
import org.openl.rules.workspace.uw.UserWorkspaceProject;
import org.richfaces.component.UITree;
import org.richfaces.event.NodeSelectedEvent;

import java.util.Collection;
import java.util.List;


/**
 * Used for holding information about rulesRepository tree.
 *
 * @author Andrey Naumenko
 */
public class RepositoryTreeState {
    private final static Log log = LogFactory.getLog(RepositoryTreeState.class);

    /** Root node for RichFaces's tree.  It is not displayed. */
    private TreeRepository root;
    private AbstractTreeNode selectedNode;
    private TreeRepository rulesRepository;
    private TreeRepository deploymentRepository;
    private UserWorkspace userWorkspace;

    private void traverseFolder(TreeFolder folder,
        Collection<?extends ProjectArtefact> artefacts) {
        for (ProjectArtefact artefact : artefacts) {
            String id = artefact.getName();
            if (artefact instanceof ProjectFolder) {
                TreeFolder treeFolder = new TreeFolder(id, artefact.getName());
                treeFolder.setDataBean(artefact);
                folder.add(treeFolder);
                traverseFolder(treeFolder, ((ProjectFolder) artefact).getArtefacts());
            } else {
                TreeFile treeFile = new TreeFile(id, artefact.getName());
                treeFile.setDataBean(artefact);
                folder.add(treeFile);
            }
        }
    }

    private void buildTree() {
        if (root != null) {
            return;
        }
        root = new TreeRepository("", "", "root");

        String rpName = "Rules Projects";
        rulesRepository = new TreeRepository(rpName, rpName, UiConst.TYPE_REPOSITORY);
        rulesRepository.setDataBean(null);

        if (selectedNode == null) {
            selectedNode = rulesRepository;
        }

        String dpName = "Deployment Projects";
        deploymentRepository = new TreeRepository(dpName, dpName,
                UiConst.TYPE_DEPLOYMENT_REPOSITORY);
        deploymentRepository.setDataBean(null);

        root.add(rulesRepository);
        root.add(deploymentRepository);

        Collection<UserWorkspaceProject> rulesProjects = userWorkspace.getProjects();

        for (Project project : rulesProjects) {
            TreeProject prj = new TreeProject(project.getName(), project.getName());
            prj.setDataBean(project);
            rulesRepository.add(prj);
            traverseFolder(prj, project.getArtefacts());
        }

        List<UserWorkspaceDeploymentProject> deploymentsProjects = null;

        try {
            deploymentsProjects = userWorkspace.getDDProjects();
        } catch (RepositoryException e) {
            log.error("Cannot get deployment projects", e);
        }

        for (UserWorkspaceDeploymentProject project : deploymentsProjects) {
            TreeDProject prj = new TreeDProject(dpName + "/" + project.getName(),
                    project.getName());
            prj.setDataBean(project);
            deploymentRepository.add(prj);
        }
    }

    public void processSelection(NodeSelectedEvent event) {
        UITree tree = (UITree) event.getComponent();
        selectedNode = (AbstractTreeNode) tree.getRowData();
    }

    public Boolean adviseNodeSelected(UITree uiTree) {
        AbstractTreeNode node = (AbstractTreeNode) uiTree.getRowData();

        ProjectArtefact projectArtefact = node.getDataBean();
        ProjectArtefact selected = selectedNode.getDataBean();

        if ((selected == null) || (projectArtefact == null)) {
            return selectedNode.getId().equals(node.getId());
        }

        if (selected.getArtefactPath().equals(projectArtefact.getArtefactPath())) {
            if (projectArtefact instanceof DeploymentDescriptorProject) {
                return selected instanceof DeploymentDescriptorProject;
            }
            return true;
        }
        return false;
    }

    public TreeRepository getRoot() {
        buildTree();
        return root;
    }

    public void setRoot(TreeRepository root) {
        this.root = root;
    }

    public AbstractTreeNode getSelectedNode() {
        buildTree();
        return selectedNode;
    }

    public void setSelectedNode(AbstractTreeNode selectedNode) {
        this.selectedNode = selectedNode;
    }

    public TreeRepository getRulesRepository() {
        buildTree();
        return rulesRepository;
    }

    public TreeRepository getDeploymentRepository() {
        buildTree();
        return deploymentRepository;
    }

    public void setUserWorkspace(UserWorkspace userWorkspace) {
        this.userWorkspace = userWorkspace;
    }
}
