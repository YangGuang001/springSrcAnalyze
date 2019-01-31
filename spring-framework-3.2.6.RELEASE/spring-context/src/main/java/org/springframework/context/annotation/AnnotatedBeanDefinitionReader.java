/*
 * Copyright 2002-2012 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.context.annotation;

import java.lang.annotation.Annotation;

import org.springframework.beans.factory.annotation.AnnotatedGenericBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.support.AutowireCandidateQualifier;
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanNameGenerator;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.env.Environment;
import org.springframework.core.env.EnvironmentCapable;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.util.Assert;

/**
 * Convenient adapter for programmatic registration of annotated bean classes.
 * This is an alternative to {@link ClassPathBeanDefinitionScanner}, applying
 * the same resolution of annotations but for explicitly registered classes only.
 *
 * @author Juergen Hoeller
 * @author Chris Beams
 * @author Sam Brannen
 * @since 3.0
 * @see AnnotationConfigApplicationContext#register
 */
public class AnnotatedBeanDefinitionReader {

	private final BeanDefinitionRegistry registry;

	private Environment environment;

	private BeanNameGenerator beanNameGenerator = new AnnotationBeanNameGenerator();

	private ScopeMetadataResolver scopeMetadataResolver = new AnnotationScopeMetadataResolver();


	/**
	 * Create a new {@code AnnotatedBeanDefinitionReader} for the given registry.
	 * If the registry is {@link EnvironmentCapable}, e.g. is an {@code ApplicationContext},
	 * the {@link Environment} will be inherited, otherwise a new
	 * {@link StandardEnvironment} will be created and used.
	 * @param registry the {@code BeanFactory} to load bean definitions into,
	 * in the form of a {@code BeanDefinitionRegistry}
	 * @see #AnnotatedBeanDefinitionReader(BeanDefinitionRegistry, Environment)
	 * @see #setEnvironment(Environment)
	 */
	public AnnotatedBeanDefinitionReader(BeanDefinitionRegistry registry) {
		this(registry, getOrCreateEnvironment(registry));
	}

	/**
	 * Create a new {@code AnnotatedBeanDefinitionReader} for the given registry and using
	 * the given {@link Environment}.
	 * @param registry the {@code BeanFactory} to load bean definitions into,
	 * in the form of a {@code BeanDefinitionRegistry}
	 * @param environment the {@code Environment} to use when evaluating bean definition
	 * profiles.
	 * @since 3.1
	 */
	public AnnotatedBeanDefinitionReader(BeanDefinitionRegistry registry, Environment environment) {
		Assert.notNull(registry, "BeanDefinitionRegistry must not be null");
		Assert.notNull(environment, "Environment must not be null");
		this.registry = registry;
		this.environment = environment;
		//注册各种解析器，主要用于解析@Configuration的Bean
		AnnotationConfigUtils.registerAnnotationConfigProcessors(this.registry);
	}

	/**
	 * Return the BeanDefinitionRegistry that this scanner operates on.
	 */
	public final BeanDefinitionRegistry getRegistry() {
		return this.registry;
	}

	/**
	 * Set the Environment to use when evaluating whether
	 * {@link Profile @Profile}-annotated component classes should be registered.
	 * <p>The default is a {@link StandardEnvironment}.
	 * @see #registerBean(Class, String, Class...)
	 */
	public void setEnvironment(Environment environment) {
		this.environment = environment;
	}

	/**
	 * Set the BeanNameGenerator to use for detected bean classes.
	 * <p>The default is a {@link AnnotationBeanNameGenerator}.
	 */
	public void setBeanNameGenerator(BeanNameGenerator beanNameGenerator) {
		this.beanNameGenerator = (beanNameGenerator != null ? beanNameGenerator : new AnnotationBeanNameGenerator());
	}

	/**
	 * Set the ScopeMetadataResolver to use for detected bean classes.
	 * <p>The default is an {@link AnnotationScopeMetadataResolver}.
	 */
	public void setScopeMetadataResolver(ScopeMetadataResolver scopeMetadataResolver) {
		this.scopeMetadataResolver =
				(scopeMetadataResolver != null ? scopeMetadataResolver : new AnnotationScopeMetadataResolver());
	}
	//注册多个注解Bean定义类
	public void register(Class<?>... annotatedClasses) {
		for (Class<?> annotatedClass : annotatedClasses) {
			registerBean(annotatedClass);
		}
	}
	//注册一个注解Bean定义类
	public void registerBean(Class<?> annotatedClass) {
		registerBean(annotatedClass, null, (Class<? extends Annotation>[]) null);
	}
	//Bean定义读取器注册注解Bean定义的入口方法
	public void registerBean(Class<?> annotatedClass, Class<? extends Annotation>... qualifiers) {
		registerBean(annotatedClass, null, qualifiers);
	}
	//Bean定义读取器向容器注册注解Bean定义类
	public void registerBean(Class<?> annotatedClass, String name, Class<? extends Annotation>... qualifiers) {
		//根据指定的注解Bean定义类，创建Spring容器中对注解Bean的封装的数据结构
		AnnotatedGenericBeanDefinition abd = new AnnotatedGenericBeanDefinition(annotatedClass);
		AnnotationMetadata metadata = abd.getMetadata();
		if (metadata.isAnnotated(Profile.class.getName())) {
			AnnotationAttributes profile = MetadataUtils.attributesFor(metadata, Profile.class);
			if (!this.environment.acceptsProfiles(profile.getStringArray("value"))) {
				return;
			}
		}
		//解析注解Bean定义的作用域，若@Scope("prototype")，则Bean为原型类型；
		//若@Scope("singleton")，则Bean为单态类型
		ScopeMetadata scopeMetadata = this.scopeMetadataResolver.resolveScopeMetadata(abd);
		abd.setScope(scopeMetadata.getScopeName());
		String beanName = (name != null ? name : this.beanNameGenerator.generateBeanName(abd, this.registry));
		//处理注解Bean定义中的通用注解
		AnnotationConfigUtils.processCommonDefinitionAnnotations(abd);
		//如果在向容器注册注解Bean定义时，使用了额外的限定符注解，则解析限定符注解。
		//主要是配置的关于autowiring自动依赖注入装配的限定条件，即@Qualifier
		//注解，Spring自动依赖注入装配默认是按类型装配，如果使用@Qualifier则按名称
		if (qualifiers != null) {
			for (Class<? extends Annotation> qualifier : qualifiers) {
				//如果配置了@Primary注解，设置该Bean为autowiring自动依赖注入装
				// 配时的首选
				if (Primary.class.equals(qualifier)) {
					abd.setPrimary(true);
				}
				//如果配置了@Lazy注解，则设置该Bean为非延迟初始化，如果没有配置，
				//则该Bean为预实例化
				else if (Lazy.class.equals(qualifier)) {
					abd.setLazyInit(true);
				}
				else {
					//如果使用了除@Primary和@Lazy以外的其他注解，则为该Bean添加一
					//个autowiring自动依赖注入装配限定符，该Bean在进autowiring
					//自动依赖注入装配时，根据名称装配限定符指定的Bean
					abd.addQualifier(new AutowireCandidateQualifier(qualifier));
				}
			}
		}
		//创建一个指定Bean名称的Bean定义对象，封装注解Bean定义类数据
		BeanDefinitionHolder definitionHolder = new BeanDefinitionHolder(abd, beanName);
		//根据注解Bean定义类中配置的作用域，创建相应的代理对象
		definitionHolder = AnnotationConfigUtils.applyScopedProxyMode(scopeMetadata, definitionHolder, this.registry);
		//向IoC容器注册注解Bean类定义对象
		BeanDefinitionReaderUtils.registerBeanDefinition(definitionHolder, this.registry);
	}


	/**
	 * Get the Environment from the given registry if possible, otherwise return a new
	 * StandardEnvironment.
	 */
	private static Environment getOrCreateEnvironment(BeanDefinitionRegistry registry) {
		Assert.notNull(registry, "BeanDefinitionRegistry must not be null");
		if (registry instanceof EnvironmentCapable) {
			return ((EnvironmentCapable) registry).getEnvironment();
		}
		return new StandardEnvironment();
	}

}
