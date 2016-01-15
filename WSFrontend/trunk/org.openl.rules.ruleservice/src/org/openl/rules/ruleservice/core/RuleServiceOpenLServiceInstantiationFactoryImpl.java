package org.openl.rules.ruleservice.core;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openl.dependency.IDependencyManager;
import org.openl.dependency.loader.IDependencyLoader;
import org.openl.rules.project.dependencies.CompositeRulesProjectDependencyManager;
import org.openl.rules.project.dependencies.RulesModuleDependencyLoader;
import org.openl.rules.project.instantiation.RulesInstantiationException;
import org.openl.rules.project.instantiation.RulesInstantiationStrategy;
import org.openl.rules.project.instantiation.RuntimeContextInstantiationStrategyEnhancer;
import org.openl.rules.project.instantiation.variation.VariationInstantiationStrategyEnhancer;
import org.openl.rules.project.model.Module;
import org.openl.rules.ruleservice.core.instantiation.RuleServiceRuntimeContextInstantiationStrategyEnhancer;
import org.openl.rules.ruleservice.core.interceptors.DynamicInterfaceAnnotationEnchancerHelper;
import org.openl.rules.ruleservice.core.interceptors.ServiceInvocationAdvice;
import org.openl.rules.ruleservice.loader.RuleServiceLoader;
import org.openl.rules.ruleservice.management.ServiceDescriptionHolder;
import org.openl.rules.ruleservice.publish.RuleServiceInstantiationStrategyFactory;
import org.openl.rules.ruleservice.publish.RuleServiceInstantiationStrategyFactoryImpl;
import org.openl.runtime.IEngineWrapper;
import org.springframework.aop.framework.ProxyFactory;

/**
 * Default implementation of RuleServiceOpenLServiceInstantiationFactory. Depend
 * on RuleLoader.
 * 
 * @author Marat Kamalov
 * 
 */
public class RuleServiceOpenLServiceInstantiationFactoryImpl implements RuleServiceInstantiationFactory {
    private final Log log = LogFactory.getLog(RuleServiceOpenLServiceInstantiationFactoryImpl.class);

    private RuleServiceLoader ruleServiceLoader;

    private RuleServiceInstantiationStrategyFactory instantiationStrategyFactory = new RuleServiceInstantiationStrategyFactoryImpl();

    private IDependencyManager externalDependencyManager;

    private Map<String, Object> externalParameters;

    private void initService(IDependencyManager dependencyManager, OpenLService service)
            throws RulesInstantiationException, ClassNotFoundException {
        if (service == null) {
            throw new IllegalArgumentException("service argument can't be null");
        }

        RulesInstantiationStrategy instantiationStrategy = instantiationStrategyFactory.getStrategy(
                service.getModules(), dependencyManager);
        instantiationStrategy.setExternalParameters(externalParameters);

        if (service.isProvideVariations()) {
            instantiationStrategy = new VariationInstantiationStrategyEnhancer(instantiationStrategy);
        }
        if (service.isProvideRuntimeContext()) {
            instantiationStrategy = new RuntimeContextInstantiationStrategyEnhancer(instantiationStrategy);
            if (service.isUseRuleServiceRuntimeContext()) {
                instantiationStrategy = new RuleServiceRuntimeContextInstantiationStrategyEnhancer(
                        (RuntimeContextInstantiationStrategyEnhancer) instantiationStrategy);
            }
        }
        resolveInterface(service, instantiationStrategy);
        instantiateServiceBean(service, instantiationStrategy);
    }

    private void instantiateServiceBean(OpenLService service, RulesInstantiationStrategy instantiationStrategy)
            throws RulesInstantiationException, ClassNotFoundException {
        Object serviceBean = null;
        Class<?> serviceClass = service.getServiceClass();
        serviceBean = instantiationStrategy.instantiate();
        ProxyFactory factory = new ProxyFactory();
        factory.addAdvice(new ServiceInvocationAdvice(serviceBean, serviceClass));
        if (serviceClass.isInterface()) {
            factory.addInterface(serviceClass);
            if (!service.isProvideRuntimeContext()) {
                factory.addInterface(IEngineWrapper.class);
            }
        } else {
            // deprecated approach with wrapper: service class is not interface
            factory.setTarget(serviceBean);
            if (!Proxy.isProxyClass(serviceBean.getClass())) {
                factory.setProxyTargetClass(true);
            } else {
                factory.setProxyTargetClass(false);
            }
        }
        ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(instantiationStrategy.getClassLoader());
        Object proxyServiceBean = null;
        try {
            proxyServiceBean = factory.getProxy();
            service.setServiceBean(proxyServiceBean);
        } catch (Throwable t) {
            throw new RuleServiceRuntimeException("Can't create a proxy of service bean object", t);
        } finally {
            Thread.currentThread().setContextClassLoader(oldClassLoader);
        }
    }

    private void resolveInterface(OpenLService service, RulesInstantiationStrategy instantiationStrategy)
            throws RulesInstantiationException, ClassNotFoundException {
        String serviceClassName = service.getServiceClassName();
        Class<?> serviceClass = null;
        ClassLoader serviceClassLoader = instantiationStrategy.getClassLoader();
        if (serviceClassName != null) {
            try {
                serviceClass = serviceClassLoader.loadClass(serviceClassName);
                instantiationStrategy.setServiceClass(RuleServiceInstantiationFactoryHelper
                        .getInterfaceForInstantiationStrategy(instantiationStrategy, serviceClass));
            } catch (ClassNotFoundException e) {
                if (log.isErrorEnabled()) {
                    log.error(String.format("Failed to load service class with name \"%s\"", serviceClassName), e);
                }
                if (log.isInfoEnabled()) {
                    log.info(String.format(
                            "Service class is undefined of service '%s'. Generated interface will be used.",
                            service.getName()));
                }
                serviceClass = processGeneratedServiceClass(instantiationStrategy.getInstanceClass(),
                        serviceClassLoader);
            }
        } else {
            if (log.isInfoEnabled()) {
                log.info(String.format("Service class is undefined of service '%s'. Generated interface will be used.",
                        service.getName()));
            }
            serviceClass = processGeneratedServiceClass(instantiationStrategy.getInstanceClass(), serviceClassLoader);
        }
        service.setServiceClass(serviceClass);
    }

    private Class<?> processGeneratedServiceClass(Class<?> serviceClass, ClassLoader classLoader) {
        if (serviceClass == null) {
            return null; // It shouldn't happen
        }
        ServiceDescription serviceDescription = ServiceDescriptionHolder.getInstance().getServiceDescription();
        if (serviceDescription == null) {
            if (log.isWarnEnabled()) {
                log.warn("Service description didn't find! Something wrong!");
            }
            return serviceClass;
        } else {
            String clazzName = serviceDescription.getInterceptorTemplateClassName();
            if (clazzName != null) {
                try {
                    Class<?> interceptingTemplateClass = classLoader.loadClass(clazzName);
                    Class<?> decoratedClass = DynamicInterfaceAnnotationEnchancerHelper.decorate(serviceClass,
                            interceptingTemplateClass, classLoader);
                    if (log.isInfoEnabled()) {
                        log.info("Interceptor template class \"" + clazzName + "\" was used for service: "
                                + serviceDescription.getName());
                    }
                    return decoratedClass;
                } catch (Exception e) {
                    if (log.isErrorEnabled()) {
                        log.error(String.format(
                                "Failed to load or appling intercepting template class with name \"%s\"", clazzName), e);
                        log.error("Intercepting template class wasn't used!");
                    }
                }
            }
        }
        return serviceClass;
    }

    /** {@inheritDoc} */
    public OpenLService createService(ServiceDescription serviceDescription) throws RuleServiceInstantiationException {

        DeploymentRelatedInfo info = getDeploymentRelatedInfo(serviceDescription);
        DeploymentRelatedInfo.setCurrent(info);

        OpenLService.OpenLServiceBuilder builder = new OpenLService.OpenLServiceBuilder();
        builder.setName(serviceDescription.getName()).setUrl(serviceDescription.getUrl())
                .setServiceClassName(serviceDescription.getServiceClassName())
                .setProvideRuntimeContext(serviceDescription.isProvideRuntimeContext())
                .setProvideVariations(serviceDescription.isProvideVariations())
                .setUseRuleServiceRuntimeContext(serviceDescription.isUseRuleServiceRuntimeContext())
                .addModules(getModulesInService(serviceDescription, info.getModulesInDeployment()));

        OpenLService openLService = builder.build();

        try {
            initService(info.getDependencyManager(), openLService);
        } catch (Exception e) {
            if (log.isWarnEnabled()) {
                log.warn("Failed to initialiaze service " + openLService.getName(), e);
            }
            throw new RuleServiceInstantiationException(String.format("Failed to initialiaze OpenL service \"%s\"",
                    openLService.getName()), e);
        } finally {
            DeploymentRelatedInfo.removeCurrent();
        }

        if (log.isInfoEnabled()) {
            String.format("Deploying service with name=\"%s\"...", openLService.getName());
        }
        return openLService;
    }

    /* for internal tests */public OpenLService createOpenLService(String serviceName, String url,
            String serviceClassName, boolean isProvideRuntimeContext, Collection<Module> modules)
            throws RuleServiceInstantiationException {
        return createOpenLService(serviceName, url, serviceClassName, isProvideRuntimeContext, false, modules);
    }

    /* for internal tests */public OpenLService createOpenLService(String serviceName, String url,
            String serviceClassName, boolean isProvideRuntimeContext, boolean isProvideVariations,
            Collection<Module> modules) throws RuleServiceInstantiationException {
        OpenLService openLService = new OpenLService(serviceName, url, serviceClassName, isProvideRuntimeContext, false,
                isProvideVariations, modules);
        try {
            initService(externalDependencyManager, openLService);
        } catch (Exception e) {
            if (log.isWarnEnabled()) {
                log.warn("Failed to initialiaze service " + openLService.getName(), e);
            }
            throw new RuleServiceInstantiationException(String.format("Failed to initialiaze OpenL service \"%s\"",
                    openLService.getName()), e);
        }

        if (log.isInfoEnabled()) {
            String.format("Deploying service with name=\"%s\"...", openLService.getName());
        }
        return openLService;
    }

    public RuleServiceLoader getRuleServiceLoader() {
        return ruleServiceLoader;
    }

    public void setRuleServiceLoader(RuleServiceLoader ruleServiceLoader) {
        if (ruleServiceLoader == null) {
            throw new IllegalArgumentException("rulesLoader arg can't be null");
        }
        this.ruleServiceLoader = ruleServiceLoader;
    }

    public IDependencyManager getDependencyManager() {
        return externalDependencyManager;
    }

    public void setDependencyManager(IDependencyManager dependencyManager) {
        this.externalDependencyManager = dependencyManager;
    }

    public RuleServiceInstantiationStrategyFactory getInstantiationStrategyFactory() {
        return instantiationStrategyFactory;
    }

    public void setInstantiationStrategyFactory(RuleServiceInstantiationStrategyFactory instantiationStrategyFactory) {
        if (instantiationStrategyFactory == null) {
            throw new IllegalArgumentException("instantiationStrategyFactory arg can't be null");
        }
        this.instantiationStrategyFactory = instantiationStrategyFactory;
    }

    public Map<String, Object> getExternalParameters() {
        return externalParameters;
    }

    public void setExternalParameters(Map<String, Object> externalParameters) {
        this.externalParameters = externalParameters;
    }

    public DeploymentRelatedInfo getDeploymentRelatedInfo(ServiceDescription serviceDescription) {
        DeploymentDescription deployment = serviceDescription.getDeployment();

        DeploymentRelatedInfoCache cache = DeploymentRelatedInfoCache.getInstance();
        DeploymentRelatedInfo info = cache.getDeploymentRelatedInfo(deployment);
        if (info == null) {
            Collection<Module> modulesInDeployment = ruleServiceLoader
                    .getModulesByServiceDescription(serviceDescription);
            IDependencyManager dependencyManager = getDependencyManager(serviceDescription, modulesInDeployment);
            info = new DeploymentRelatedInfo(deployment, dependencyManager, modulesInDeployment);
            cache.putDeploymentRelatedInfo(deployment, info);
        }
        return info;
    }

    private IDependencyManager getDependencyManager(ServiceDescription serviceDescription,
            Collection<Module> modulesInDeployment) {
        if (!isMultiModule(serviceDescription)) {
            return externalDependencyManager;
        }

        CompositeRulesProjectDependencyManager multiModuleDependencyManager = new CompositeRulesProjectDependencyManager();
        // multimodule is only available for execution(execution mode == true)
        multiModuleDependencyManager.setExecutionMode(true);

        if (externalDependencyManager != null) {
            multiModuleDependencyManager.addDependencyManager(externalDependencyManager);
        }

        IDependencyLoader loader = new RulesModuleDependencyLoader(modulesInDeployment);
        multiModuleDependencyManager.setDependencyLoaders(Arrays.asList(loader));

        multiModuleDependencyManager.setExternalParameters(externalParameters);

        return multiModuleDependencyManager;
    }

    private boolean isMultiModule(ServiceDescription serviceDescription) {
        return serviceDescription.getModules().size() > 1;
    }

    private Collection<Module> getModulesInService(ServiceDescription serviceDescription,
            Collection<Module> modulesInDeployment) {
        List<Module> modulesInService = new ArrayList<Module>();
        for (ModuleDescription moduleDescription : serviceDescription.getModulesInService()) {
            for (Module module : modulesInDeployment) {
                if (module.getName().equals(moduleDescription.getModuleName())) {
                    modulesInService.add(module);
                }
            }
        }

        return modulesInService;
    }
}