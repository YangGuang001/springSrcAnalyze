package com.yang.myannotation;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.util.ClassUtils;

import javax.xml.ws.spi.http.HttpHandler;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Set;

@Slf4j
public class HTTPRequestRegistrar implements ImportBeanDefinitionRegistrar,
        ResourceLoaderAware, BeanClassLoaderAware, EnvironmentAware, BeanFactoryAware {
    private BeanFactory beanFactory;
    private ClassLoader classLoader;
    private Environment environment;
    private ResourceLoader resourceLoader;


    @Override
    public void setBeanClassLoader(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        this.beanFactory = beanFactory;
    }

    @Override
    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void setResourceLoader(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    @Override
    public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {
        registerHttpRequest(importingClassMetadata, registry);
    }

    private void registerHttpRequest(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {
        ClassPathScanningCandidateComponentProvider classScanner = getClassScanner();
        classScanner.setResourceLoader(this.resourceLoader);
        //只关注@HTTPUtil的接口
        AnnotationTypeFilter annotationTypeFilter = new AnnotationTypeFilter(HTTPUtil.class);
        classScanner.addIncludeFilter(annotationTypeFilter);
        AnnotationAttributes attributes = AnnotationAttributes.fromMap(importingClassMetadata.getAnnotationAttributes(EnableHttpUtil.class.getName()));
        String[] basePackage = attributes.getStringArray("basePackage");
        Set<BeanDefinition> beanDefinitionSet = classScanner.findCandidateComponents(basePackage[0]);
        for (BeanDefinition beanDefinition : beanDefinitionSet) {
            if (beanDefinition instanceof AnnotatedBeanDefinition) {
                registerBeans((AnnotatedBeanDefinition)beanDefinition);
            }
        }
    }

    /**
     * 创建动态代理，注册到BeanFactory中
     * @param beanDefinition
     */
    private void registerBeans(AnnotatedBeanDefinition beanDefinition) {
        String className = beanDefinition.getBeanClassName();
        ((DefaultListableBeanFactory)this.beanFactory).registerSingleton(className, createProxy(beanDefinition));
    }

    /**
     * 创建动态代理
     * @param beanDefinition
     * @return
     */
    private Object createProxy(AnnotatedBeanDefinition beanDefinition) {

        try {
            AnnotationMetadata annotationMetadata = beanDefinition.getMetadata();
            Class<?> target = Class.forName(annotationMetadata.getClassName());
            InvocationHandler invocationHandler = createInvocationHandler();
            Object proxy = Proxy.newProxyInstance(HTTPRequest.class.getClassLoader(), new Class[]{target}, invocationHandler);
            return proxy;
        } catch (ClassNotFoundException e) {
            log.error(e.getMessage());
        }
        return null;
    }

    private InvocationHandler createInvocationHandler() {
        return new InvocationHandler() {
            private HTTPHandler httpHandler = new DemoHttpHandler();

            @Override
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                return httpHandler.handle(method);
            }
        };
    }

    /**
     *  构造Class扫描器，设置只扫描顶级接口，不扫描具体类
     * @return
     */
    private ClassPathScanningCandidateComponentProvider getClassScanner() {
        return new ClassPathScanningCandidateComponentProvider(false, this.environment) {
            @Override
            protected boolean isCandidateComponent(AnnotatedBeanDefinition beanDefinition) {
                if (beanDefinition.getMetadata().isInterface()) {
                    try {
                        Class<?> target = ClassUtils.forName(beanDefinition.getMetadata().getClassName(), classLoader);
                        return !target.isAnnotation();
                    } catch (ClassNotFoundException e) {
                        log.error("Exception :" + e);
                    }
                }
                return false;
            }
        };
    }
}
