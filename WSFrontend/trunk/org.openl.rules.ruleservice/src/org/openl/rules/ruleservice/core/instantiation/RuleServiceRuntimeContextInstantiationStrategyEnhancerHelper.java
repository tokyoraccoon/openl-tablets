package org.openl.rules.ruleservice.core.instantiation;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import net.sf.cglib.core.ReflectUtils;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.objectweb.asm.ClassAdapter;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.openl.rules.ruleservice.context.IRulesRuntimeContext;
import org.openl.rules.runtime.RuleInfo;
import org.openl.rules.runtime.InterfaceGenerator;
import org.openl.util.generation.InterfaceTransformer;
/**
 * 
 * @author Marat Kamalov
 *
 */
final class RuleServiceRuntimeContextInstantiationStrategyEnhancerHelper {
    private RuleServiceRuntimeContextInstantiationStrategyEnhancerHelper() {
    }

    /**
     * Suffix of undecorated class name.
     */
    private static final String UNDECORATED_CLASS_NAME_SUFFIX = "$RSRCUndecorated";

    /**
     * Suffix of decorated class name.
     */
    private static final String DECORATED_CLASS_NAME_SUFFIX = "$RSRCDecorated";

    public static boolean isDecoratedClass(Class<?> clazz) {
        for (Method method : clazz.getMethods()) {
            if (method.getParameterTypes().length == 0 || method.getParameterTypes()[0] != IRulesRuntimeContext.class) {
                return false;
            }
        }
        return true;
    }

    /**
     * Undecorates methods signatures of given clazz.
     * 
     * @param clazz class to undecorate
     * @param classLoader The classloader where generated class should be
     *            placed.
     * @return new class with undecorated methods signatures: removed
     *         {@link IRulesRuntimeContext} as the first parameter for each
     *         method.
     * @throws Exception
     */
    public static Class<?> undecorateClass(Class<?> clazz, ClassLoader classLoader) throws Exception {
        if (!clazz.isInterface()) {
            throw new IllegalArgumentException("Supports only interface classes!!!");
        }

        final Log log = LogFactory.getLog(RuleServiceRuntimeContextInstantiationStrategyEnhancerHelper.class);

        String className = clazz.getName() + UNDECORATED_CLASS_NAME_SUFFIX;

        if (log.isDebugEnabled()) {
            log.debug(String.format("Generating interface with replace runtime context for '%s' class", clazz.getName()));
        }

        return innerUndecorateInterface(className, clazz, classLoader);
    }

    private static Class<?> innerUndecorateInterface(String className, Class<?> original, ClassLoader classLoader)
            throws Exception {

        ClassWriter classWriter = new ClassWriter(0);
        ClassVisitor classVisitor = new UndecoratingClassWriter(classWriter, className);
        InterfaceTransformer transformer = new InterfaceTransformer(original, className);
        transformer.accept(classVisitor);
        classWriter.visitEnd();

        // Create class object.
        //
        ReflectUtils.defineClass(className, classWriter.toByteArray(), classLoader);

        // Return loaded to classpath class object.
        //
        return Class.forName(className, true, classLoader);
    }

    /**
     * TODO: replace with a configurable implementation
     * 
     * 
     * Check that method should be ignored by enhancer.
     * 
     * @param method method to check
     * @return <code>true</code> if method should be ignored; <code>false</code>
     *         - otherwise
     */
    private static boolean isIgnored(Method method) {
        // Ignore methods what are inherited from Object.class
        // Note that ignored inherited methods only.
        return ArrayUtils.contains(Object.class.getMethods(), method);
    }

    /**
     * Decorates methods signatures of given clazz.
     * 
     * @param clazz class to decorate
     * @param classLoader The classloader where generated class should be
     *            placed.
     * @return new class with decorated methods signatures: added
     *         {@link IRulesRuntimeContext} as the first parameter for each
     *         method.
     * @throws Exception
     */
    public static Class<?> decorateClass(Class<?> clazz, ClassLoader classLoader) throws Exception {
        final Log log = LogFactory.getLog(RuleServiceRuntimeContextInstantiationStrategyEnhancer.class);

        Method[] methods = clazz.getMethods();
        List<RuleInfo> rules = getRulesDecorated(methods);

        String className = clazz.getName() + DECORATED_CLASS_NAME_SUFFIX;
        RuleInfo[] rulesArray = rules.toArray(new RuleInfo[rules.size()]);

        if (log.isDebugEnabled()) {
            log.debug(String.format("Generating interface for '%s' class", clazz.getName()));
        }

        return InterfaceGenerator.generateInterface(className, rulesArray, classLoader);
    }

    /**
     * Gets list of rules.
     * 
     * @param methods array of methods what represents rule methods
     * @return list of rules meta-info
     */
    private static List<RuleInfo> getRulesDecorated(Method[] methods) {

        List<RuleInfo> rules = new ArrayList<RuleInfo>(methods.length);

        for (Method method : methods) {

            // Check that method should be ignored or not.
            if (isIgnored(method)) {
                continue;
            }

            String methodName = method.getName();

            Class<?>[] paramTypes = method.getParameterTypes();
            Class<?> returnType = method.getReturnType();
            paramTypes[0] = IRulesRuntimeContext.class;

            RuleInfo ruleInfo = InterfaceGenerator.createRuleInfo(methodName, paramTypes, returnType);

            rules.add(ruleInfo);
        }

        return rules;
    }

    /**
     * {@link ClassWriter} for creation undecorated class: for replacing
     * {@link IRulesRuntimeContext} from signature of each method.
     * 
     * @author Marat Kamalov
     */
    private static class UndecoratingClassWriter extends ClassAdapter {

        private static final String RUNTIME_CONTEXT = "Lorg/openl/rules/context/IRulesRuntimeContext;";
        private static final String RULESERVICE_RUNTIME_CONTEXT = "Lorg/openl/rules/ruleservice/context/IRulesRuntimeContext;";
        private String className;

        public UndecoratingClassWriter(ClassVisitor delegatedClassVisitor, String className) {
            super(delegatedClassVisitor);
            this.className = className;
        }

        @Override
        public void visit(int arg0, int arg1, String arg2, String arg3, String arg4, String[] arg5) {
            super.visit(arg0, arg1, className.replace('.', '/'), arg3, arg4, arg5);
        }

        @Override
        public MethodVisitor visitMethod(int arg0, String arg1, String arg2, String arg3, String[] arg4) {
            return super.visitMethod(arg0, arg1, replaceRuntimeContextFromSignature(arg2), arg3, arg4);
        }

        private String replaceRuntimeContextFromSignature(String signature) {
            if (signature.startsWith("(" + RULESERVICE_RUNTIME_CONTEXT)) {
                return "(" + RUNTIME_CONTEXT + signature.substring(RULESERVICE_RUNTIME_CONTEXT.length() + 1);
            } else {
                throw new RuntimeException("Wrong signature!");
            }
        }
    }
}