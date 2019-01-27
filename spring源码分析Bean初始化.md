## 主流程
* Bean 实例化
* 属性注入以及Bean注册
* 生成代理Bean
### BeanFactory流程分析
在[上一节](https://note.youdao.com/ynoteshare1/index.html?id=fc2357a86398168bfaf6962c433e34ce&type=note#/)中主要分析了BeanDefinition注册到DefaultListableBeanFactory的beanDefinitionMap中，以及入口，下面接着入口进行分析Bean实例化。beanFactory = obtainFreshBeanFactory(); beanFactory已经获取到了所有的BeanDefinition，包括aop，property-placeholder等信息的BeanDefinition，
prepareBeanFactory为容器配置特性，postProcessBeanFactory方法调用实现BeanDefinitionRegistryPostProcessor接口，可以实现继续添加BeanDefinition到BeanFactory中，调用BeanFactoryPostProcessor接口类，可以实现从BeanFactory获取Bean，
```java
public void refresh() throws BeansException, IllegalStateException {
    synchronized (this.startupShutdownMonitor) {
    // Prepare this context for refreshing.
    //调用容器准备刷新的方法，获取容器的当时时间，同时给容器设置同步标识
    prepareRefresh();
    
    // Tell the subclass to refresh the internal bean factory.
    //告诉子类启动refreshBeanFactory()方法，Bean定义资源文件的载入从子类的refreshBeanFactory()方法启动  
    ConfigurableListableBeanFactory beanFactory = obtainFreshBeanFactory();
    
    // Prepare the bean factory for use in this context.
    //为BeanFactory配置容器特性，例如类加载器、事件处理器等
    prepareBeanFactory(beanFactory);
    
    try {
    	// Allows post-processing of the bean factory in context subclasses.
    	//为容器的某些子类指定特殊的BeanPost事件处理器
    	postProcessBeanFactory(beanFactory);
    
    	// Invoke factory processors registered as beans in the context.
    	//调用所有注册的BeanFactoryPostProcessor的Bean
    	invokeBeanFactoryPostProcessors(beanFactory);
    
    	// Register bean processors that intercept bean creation.
    	//为BeanFactory注册BeanPost事件处理器.  
        //BeanPostProcessor是Bean后置处理器，用于监听容器触发的事件 
    	registerBeanPostProcessors(beanFactory);
    
    	// Initialize message source for this context.
    	//初始化信息源，和国际化相关.
    	initMessageSource();
    
    	// Initialize event multicaster for this context.
    	//初始化容器事件传播器
    	initApplicationEventMulticaster();
    
    	// Initialize other special beans in specific context subclasses.
    	//调用子类的某些特殊Bean初始化方法
    	onRefresh();
    
    	// Check for listener beans and register them.
    	//为事件传播器注册事件监听器.
    	registerListeners();
    
    	// Instantiate all remaining (non-lazy-init) singletons.
    	//初始化所有剩余的单例Bean.
    	finishBeanFactoryInitialization(beanFactory);
    
    	// Last step: publish corresponding event.
    	//初始化容器的生命周期事件处理器，并发布容器的生命周期事件
    	finishRefresh();
    }
```
postProcessBeanFactory方法调用实现BeanDefinitionRegistryPostProcessor接口，可以实现续添加BeanDefinition到BeanFactory中，例如ConfigurationClassPostProcessor注入ImportAwareBeanPostProcessor，调用BeanFactoryPostProcessor接口类，可以实现从BeanFactory获取Bean,例如PropertyResourceConfigurer，实现对BeanDefinition的属性值进行替换，例如Outh2等
```java
protected void invokeBeanFactoryPostProcessors(ConfigurableListableBeanFactory beanFactory) {
		// Invoke BeanDefinitionRegistryPostProcessors first, if any.
		Set<String> processedBeans = new HashSet<String>();
		//DefaultListableBeanFactory 实现了BeanDefinitionRegistry接口，具备添加/删除BeanDefinition
		if (beanFactory instanceof BeanDefinitionRegistry) {
			BeanDefinitionRegistry registry = (BeanDefinitionRegistry) beanFactory;
			List<BeanFactoryPostProcessor> regularPostProcessors = new LinkedList<BeanFactoryPostProcessor>();
			List<BeanDefinitionRegistryPostProcessor> registryPostProcessors =
					new LinkedList<BeanDefinitionRegistryPostProcessor>();
			//getBeanFactoryPostProcessors() 拓展接口
			for (BeanFactoryPostProcessor postProcessor : getBeanFactoryPostProcessors()) {
				if (postProcessor instanceof BeanDefinitionRegistryPostProcessor) {
					BeanDefinitionRegistryPostProcessor registryPostProcessor =
							(BeanDefinitionRegistryPostProcessor) postProcessor;
					registryPostProcessor.postProcessBeanDefinitionRegistry(registry);
					registryPostProcessors.add(registryPostProcessor);
				}
				else {
					regularPostProcessors.add(postProcessor);
				}
			}
			//获取BeanDefinitionRegistryPostProcessor接口类
			Map<String, BeanDefinitionRegistryPostProcessor> beanMap =
					beanFactory.getBeansOfType(BeanDefinitionRegistryPostProcessor.class, true, false);
			List<BeanDefinitionRegistryPostProcessor> registryPostProcessorBeans =
					new ArrayList<BeanDefinitionRegistryPostProcessor>(beanMap.values());
			OrderComparator.sort(registryPostProcessorBeans);
			//实现BeanDefinitionRegistryPostProcessor接口调用，可以向BeanFactory中添加/删除BeanDefinition
			for (BeanDefinitionRegistryPostProcessor postProcessor : registryPostProcessorBeans) {
				postProcessor.postProcessBeanDefinitionRegistry(registry);
			}
			//调用BeanFactoryPostProcessor的postProcessBeanFactory后置处理器，可以对BeanFactory进行操作，例如获取Bean
			invokeBeanFactoryPostProcessors(registryPostProcessors, beanFactory);
			invokeBeanFactoryPostProcessors(registryPostProcessorBeans, beanFactory);
			invokeBeanFactoryPostProcessors(regularPostProcessors, beanFactory);
			processedBeans.addAll(beanMap.keySet());
		}
		else {
			// Invoke factory processors registered with the context instance.
			invokeBeanFactoryPostProcessors(getBeanFactoryPostProcessors(), beanFactory);
		}

		// Do not initialize FactoryBeans here: We need to leave all regular beans
		// uninitialized to let the bean factory post-processors apply to them!
		//调用BeanFactoryPostProcessor的postProcessBeanFactory后置处理器，可以对BeanFactory进行操作，例如获取Bean
		String[] postProcessorNames =
				beanFactory.getBeanNamesForType(BeanFactoryPostProcessor.class, true, false);

		// Separate between BeanFactoryPostProcessors that implement PriorityOrdered,
		// Ordered, and the rest.
		List<BeanFactoryPostProcessor> priorityOrderedPostProcessors = new ArrayList<BeanFactoryPostProcessor>();
		List<String> orderedPostProcessorNames = new ArrayList<String>();
		List<String> nonOrderedPostProcessorNames = new ArrayList<String>();
		for (String ppName : postProcessorNames) {
			if (processedBeans.contains(ppName)) {
				// skip - already processed in first phase above
			}
			else if (isTypeMatch(ppName, PriorityOrdered.class)) {
				priorityOrderedPostProcessors.add(beanFactory.getBean(ppName, BeanFactoryPostProcessor.class));
			}
			else if (isTypeMatch(ppName, Ordered.class)) {
				orderedPostProcessorNames.add(ppName);
			}
			else {
				nonOrderedPostProcessorNames.add(ppName);
			}
		}
		//按照PriorityOrdered、Ordered、none等先后顺序调用后置处理器
		// First, invoke the BeanFactoryPostProcessors that implement PriorityOrdered.
		OrderComparator.sort(priorityOrderedPostProcessors);
		invokeBeanFactoryPostProcessors(priorityOrderedPostProcessors, beanFactory);

		// Next, invoke the BeanFactoryPostProcessors that implement Ordered.
		List<BeanFactoryPostProcessor> orderedPostProcessors = new ArrayList<BeanFactoryPostProcessor>();
		for (String postProcessorName : orderedPostProcessorNames) {
			orderedPostProcessors.add(getBean(postProcessorName, BeanFactoryPostProcessor.class));
		}
		OrderComparator.sort(orderedPostProcessors);
		invokeBeanFactoryPostProcessors(orderedPostProcessors, beanFactory);

		// Finally, invoke all other BeanFactoryPostProcessors.
		List<BeanFactoryPostProcessor> nonOrderedPostProcessors = new ArrayList<BeanFactoryPostProcessor>();
		for (String postProcessorName : nonOrderedPostProcessorNames) {
			nonOrderedPostProcessors.add(getBean(postProcessorName, BeanFactoryPostProcessor.class));
		}
		invokeBeanFactoryPostProcessors(nonOrderedPostProcessors, beanFactory);
	}
```
跟上述类似BeanPostProcessor接口为Bean实例化前后添加拦截器, 国际化资源、事件处理化等都使用到了getBean或者getBeanNamesForType来实例化Bean
```java
protected void registerBeanPostProcessors(ConfigurableListableBeanFactory beanFactory) {
	//获取BeanPostProcessor子类，此时所有子类都已经实例化了，并保存在BeanFactory中
	String[] postProcessorNames = beanFactory.getBeanNamesForType(BeanPostProcessor.class, true, false);

	// Register BeanPostProcessorChecker that logs an info message when
	// a bean is created during BeanPostProcessor instantiation, i.e. when
	// a bean is not eligible for getting processed by all BeanPostProcessors.
	
	int beanProcessorTargetCount = beanFactory.getBeanPostProcessorCount() + 1 + postProcessorNames.length;
	beanFactory.addBeanPostProcessor(new BeanPostProcessorChecker(beanFactory, beanProcessorTargetCount));

	// Separate between BeanPostProcessors that implement PriorityOrdered,
	// Ordered, and the rest.
	List<BeanPostProcessor> priorityOrderedPostProcessors = new ArrayList<BeanPostProcessor>();
	List<BeanPostProcessor> internalPostProcessors = new ArrayList<BeanPostProcessor>();
	List<String> orderedPostProcessorNames = new ArrayList<String>();
	List<String> nonOrderedPostProcessorNames = new ArrayList<String>();
	for (String ppName : postProcessorNames) {
		if (isTypeMatch(ppName, PriorityOrdered.class)) {
			BeanPostProcessor pp = beanFactory.getBean(ppName, BeanPostProcessor.class);
			priorityOrderedPostProcessors.add(pp);
			if (pp instanceof MergedBeanDefinitionPostProcessor) {
				internalPostProcessors.add(pp);
			}
		}
		else if (isTypeMatch(ppName, Ordered.class)) {
			orderedPostProcessorNames.add(ppName);
		}
		else {
			nonOrderedPostProcessorNames.add(ppName);
		}
	}

	// First, register the BeanPostProcessors that implement PriorityOrdered.
	OrderComparator.sort(priorityOrderedPostProcessors);
	registerBeanPostProcessors(beanFactory, priorityOrderedPostProcessors);
```
### Bean实例化
Bean类型有单例、原型、scope、FactoryBean等类型，阐述单例类型，其他类型原理类似。
```java
//真正实现向IOC容器获取Bean的功能，也是触发依赖注入功能的地方
	@SuppressWarnings("unchecked")
	protected <T> T doGetBean(
			final String name, final Class<T> requiredType, final Object[] args, boolean typeCheckOnly)
			throws BeansException {

		//根据指定的名称获取被管理Bean的名称，剥离指定名称中对容器的相关依赖  
	    //如果指定的是别名，将别名转换为规范的Bean名称
		final String beanName = transformedBeanName(name);
		Object bean;

		// Eagerly check singleton cache for manually registered singletons.
		//先从缓存中取是否已经有被创建过的单态类型的Bean
	    //对于单例模式的Bean整个IOC容器中只创建一次，不需要重复创建
		Object sharedInstance = getSingleton(beanName);
		 //IOC容器创建单例模式Bean实例对象
		if (sharedInstance != null && args == null) {
			if (logger.isDebugEnabled()) {
				//如果指定名称的Bean在容器中已有单例模式的Bean被创建
	            //直接返回已经创建的Bean
				if (isSingletonCurrentlyInCreation(beanName)) {
					logger.debug("Returning eagerly cached instance of singleton bean '" + beanName +
							"' that is not fully initialized yet - a consequence of a circular reference");
				}
				else {
					logger.debug("Returning cached instance of singleton bean '" + beanName + "'");
				}
			}
		    //获取给定Bean的实例对象，主要是完成FactoryBean的相关处理  
            //注意：BeanFactory是管理容器中Bean的工厂，而FactoryBean是  
            //创建创建对象的工厂Bean，两者之间有区别
			bean = getObjectForBeanInstance(sharedInstance, name, beanName, null);
		}

		else {
			// Fail if we're already creating this bean instance:
			// We're assumably within a circular reference.
			//缓存没有正在创建的单例模式Bean  
	        //缓存中已经有已经创建的原型模式Bean
	        //但是由于循环引用的问题导致实 例化对象失败
			if (isPrototypeCurrentlyInCreation(beanName)) {
				throw new BeanCurrentlyInCreationException(beanName);
			}

			// Check if bean definition exists in this factory.
			//对IOC容器中是否存在指定名称的BeanDefinition进行检查，首先检查是否  
	        //能在当前的BeanFactory中获取的所需要的Bean，如果不能则委托当前容器  
	        //的父级容器去查找，如果还是找不到则沿着容器的继承体系向父级容器查找
			BeanFactory parentBeanFactory = getParentBeanFactory();
			//当前容器的父级容器存在，且当前容器中不存在指定名称的Bean
			if (parentBeanFactory != null && !containsBeanDefinition(beanName)) {
				// Not found -> check parent.
				//解析指定Bean名称的原始名称
				String nameToLookup = originalBeanName(name);
				if (args != null) {
					// Delegation to parent with explicit args.
					//委派父级容器根据指定名称和显式的参数查找
					return (T) parentBeanFactory.getBean(nameToLookup, args);
				}
				else {
					// No args -> delegate to standard getBean method.
					//委派父级容器根据指定名称和类型查找
					return parentBeanFactory.getBean(nameToLookup, requiredType);
				}
			}

			//创建的Bean是否需要进行类型验证，一般不需要
			if (!typeCheckOnly) {
				//向容器标记指定的Bean已经被创建
				markBeanAsCreated(beanName);
			}

			try {
				//根据指定Bean名称获取其父级的Bean定义
		        //主要解决Bean继承时子类合并父类公共属性问题 
				final RootBeanDefinition mbd = getMergedLocalBeanDefinition(beanName);
				checkMergedBeanDefinition(mbd, beanName, args);

				// Guarantee initialization of beans that the current bean depends on.
				//获取当前Bean所有依赖Bean的名称
				String[] dependsOn = mbd.getDependsOn();
				//如果当前Bean有依赖Bean
				if (dependsOn != null) {
					for (String dependsOnBean : dependsOn) {
						//递归调用getBean方法，获取当前Bean的依赖Bean
						getBean(dependsOnBean);
						//把被依赖Bean注册给当前依赖的Bean
						registerDependentBean(dependsOnBean, beanName);
					}
				}

				// Create bean instance.
				//创建单例模式Bean的实例对象
				if (mbd.isSingleton()) {
					//这里使用了一个匿名内部类，创建Bean实例对象，并且注册给所依赖的对象
					sharedInstance = getSingleton(beanName, new ObjectFactory<Object>() {
						public Object getObject() throws BeansException {
							try {
								//创建一个指定Bean实例对象，如果有父级继承，则合并子类和父类的定义
								return createBean(beanName, mbd, args);
							}
							catch (BeansException ex) {
								// Explicitly remove instance from singleton cache: It might have been put there
								// eagerly by the creation process, to allow for circular reference resolution.
								// Also remove any beans that received a temporary reference to the bean.
								//显式地从容器单例模式Bean缓存中清除实例对象
								destroySingleton(beanName);
								throw ex;
							}
						}
					});
					//获取给定Bean的实例对象
					bean = getObjectForBeanInstance(sharedInstance, name, beanName, mbd);
				}

				//IOC容器创建原型模式Bean实例对象
				else if (mbd.isPrototype()) {
					// It's a prototype -> create a new instance.
					//原型模式(Prototype)是每次都会创建一个新的对象
					Object prototypeInstance = null;
					try {
						//回调beforePrototypeCreation方法，默认的功能是注册当前创建的原型对象
						beforePrototypeCreation(beanName);
						//创建指定Bean对象实例
						prototypeInstance = createBean(beanName, mbd, args);
					}
					finally {
						//回调afterPrototypeCreation方法，默认的功能告诉IOC容器指定Bean的原型对象不再创建了
						afterPrototypeCreation(beanName);
					}
					//获取给定Bean的实例对象
					bean = getObjectForBeanInstance(prototypeInstance, name, beanName, mbd);
				}

				//要创建的Bean既不是单例模式，也不是原型模式，则根据Bean定义资源中  
		        //配置的生命周期范围，选择实例化Bean的合适方法，这种在Web应用程序中  
		        //比较常用，如：request、session、application等生命周期 
				else {
					String scopeName = mbd.getScope();
					final Scope scope = this.scopes.get(scopeName);
					//Bean定义资源中没有配置生命周期范围，则Bean定义不合法
					if (scope == null) {
						throw new IllegalStateException("No Scope registered for scope '" + scopeName + "'");
					}
					try {
						//这里又使用了一个匿名内部类，获取一个指定生命周期范围的实例
						Object scopedInstance = scope.get(beanName, new ObjectFactory<Object>() {
							public Object getObject() throws BeansException {
								beforePrototypeCreation(beanName);
								try {
									return createBean(beanName, mbd, args);
								}
								finally {
									afterPrototypeCreation(beanName);
								}
							}
						});
						//获取给定Bean的实例对象
						bean = getObjectForBeanInstance(scopedInstance, name, beanName, mbd);
					}
					catch (IllegalStateException ex) {
						throw new BeanCreationException(beanName,
								"Scope '" + scopeName + "' is not active for the current thread; " +
								"consider defining a scoped proxy for this bean if you intend to refer to it from a singleton",
								ex);
					}
				}
			}
			catch (BeansException ex) {
				cleanupAfterBeanCreationFailure(beanName);
				throw ex;
			}
		}

		// Check if required type matches the type of the actual bean instance.
		//对创建的Bean实例对象进行类型检查
		if (requiredType != null && bean != null && !requiredType.isAssignableFrom(bean.getClass())) {
			try {
				return getTypeConverter().convertIfNecessary(bean, requiredType);
			}
			catch (TypeMismatchException ex) {
				if (logger.isDebugEnabled()) {
					logger.debug("Failed to convert bean '" + name + "' to required type [" +
							ClassUtils.getQualifiedName(requiredType) + "]", ex);
				}
				throw new BeanNotOfRequiredTypeException(name, requiredType, bean.getClass());
			}
		}
		return (T) bean;
	}
```
实例化Bean之前，根据BeanName和BeanDefinition信息以及上述过程实例化的BeanPostProcessor判断是否需要进行生成代理，创建代理的流程放入到下一节再详细阐述。
```java
//创建Bean实例对象
	@Override
	protected Object createBean(final String beanName, final RootBeanDefinition mbd, final Object[] args)
			throws BeanCreationException {

	if (logger.isDebugEnabled()) {
		logger.debug("Creating instance of bean '" + beanName + "'");
	}
	// Make sure bean class is actually resolved at this point.
	//判断需要创建的Bean是否可以实例化，即是否可以通过当前的类加载器加载
	resolveBeanClass(mbd, beanName);

	// Prepare method overrides.
	//校验和准备Bean中的方法覆盖
	try {
		mbd.prepareMethodOverrides();
	}
	catch (BeanDefinitionValidationException ex) {
		throw new BeanDefinitionStoreException(mbd.getResourceDescription(),
				beanName, "Validation of method overrides failed", ex);
	}

	try {
		// Give BeanPostProcessors a chance to return a proxy instead of the target bean instance.
		//如果Bean配置了初始化前和初始化后的处理器，则试图返回一个需要创建Bean的代理对象
		Object bean = resolveBeforeInstantiation(beanName, mbd);
		if (bean != null) {
			return bean;
		}
	}
	catch (Throwable ex) {
		throw new BeanCreationException(mbd.getResourceDescription(), beanName,
				"BeanPostProcessor before instantiation of bean failed", ex);
	}

	//创建Bean的入口
	Object beanInstance = doCreateBean(beanName, mbd, args);
	if (logger.isDebugEnabled()) {
		logger.debug("Finished creating instance of bean '" + beanName + "'");
	}
	return beanInstance;
}
```
doCreateBean方法创建JavaBean，创建包装Bean,调用MergedBeanDefinitionPostProcessor后置处理,populateBean配置Bean属性，返回包装Bean
```java
//真正创建Bean的方法 
protected Object doCreateBean(final String beanName, final RootBeanDefinition mbd, final Object[] args) {
	// Instantiate the bean.
	//封装被创建的Bean对象
	BeanWrapper instanceWrapper = null;
	if (mbd.isSingleton()) {//单例模式的Bean，先从容器中缓存中获取同名Bean
		instanceWrapper = this.factoryBeanInstanceCache.remove(beanName);
	}
	if (instanceWrapper == null) {
		//创建实例对象
		instanceWrapper = createBeanInstance(beanName, mbd, args);
	}
	final Object bean = (instanceWrapper != null ? instanceWrapper.getWrappedInstance() : null);
	//获取实例化对象的类型
	Class<?> beanType = (instanceWrapper != null ? instanceWrapper.getWrappedClass() : null);

	// Allow post-processors to modify the merged bean definition.
	//调用PostProcessor后置处理器
	synchronized (mbd.postProcessingLock) {
		if (!mbd.postProcessed) {
			applyMergedBeanDefinitionPostProcessors(mbd, beanType, beanName);
			mbd.postProcessed = true;
		}
	}

	// Eagerly cache singletons to be able to resolve circular references
	// even when triggered by lifecycle interfaces like BeanFactoryAware.
	//向容器中缓存单例模式的Bean对象，以防循环引用
	boolean earlySingletonExposure = (mbd.isSingleton() && this.allowCircularReferences &&
			isSingletonCurrentlyInCreation(beanName));
	if (earlySingletonExposure) {
		if (logger.isDebugEnabled()) {
			logger.debug("Eagerly caching bean '" + beanName +
					"' to allow for resolving potential circular references");
		}
		//这里是一个匿名内部类，为了防止循环引用，尽早持有对象的引用
		addSingletonFactory(beanName, new ObjectFactory<Object>() {
			public Object getObject() throws BeansException {
				return getEarlyBeanReference(beanName, mbd, bean);
			}
		});
	}

	// Initialize the bean instance.
	//Bean对象的初始化，依赖注入在此触发  
    //这个exposedObject在初始化完成之后返回作为依赖注入完成后的Bean
	Object exposedObject = bean;
	try {
		//将Bean实例对象封装，并且Bean定义中配置的属性值赋值给实例对象
		populateBean(beanName, mbd, instanceWrapper);
		if (exposedObject != null) {
			//初始化Bean对象 
			//在对Bean实例对象生成和依赖注入完成以后，开始对Bean实例对象  
            //进行初始化 ，为Bean实例对象应用BeanPostProcessor后置处理器
			exposedObject = initializeBean(beanName, exposedObject, mbd);
		}
	}
	catch (Throwable ex) {
		if (ex instanceof BeanCreationException && beanName.equals(((BeanCreationException) ex).getBeanName())) {
			throw (BeanCreationException) ex;
		}
		else {
			throw new BeanCreationException(mbd.getResourceDescription(), beanName, "Initialization of bean failed", ex);
		}
	}

	if (earlySingletonExposure) {
		//获取指定名称的已注册的单例模式Bean对象
		Object earlySingletonReference = getSingleton(beanName, false);
		if (earlySingletonReference != null) {
			//根据名称获取的已注册的Bean和正在实例化的Bean是同一个
			if (exposedObject == bean) {
				//当前实例化的Bean初始化完成
				exposedObject = earlySingletonReference;
			}
			//当前Bean依赖其他Bean，并且当发生循环引用时不允许新创建实例对象
			else if (!this.allowRawInjectionDespiteWrapping && hasDependentBean(beanName)) {
				String[] dependentBeans = getDependentBeans(beanName);
				Set<String> actualDependentBeans = new LinkedHashSet<String>(dependentBeans.length);
				//获取当前Bean所依赖的其他Bean 
				for (String dependentBean : dependentBeans) {
					//对依赖Bean进行类型检查
					if (!removeSingletonIfCreatedForTypeCheckOnly(dependentBean)) {
						actualDependentBeans.add(dependentBean);
					}
				}
				if (!actualDependentBeans.isEmpty()) {
					throw new BeanCurrentlyInCreationException(beanName,
							"Bean with name '" + beanName + "' has been injected into other beans [" +
							StringUtils.collectionToCommaDelimitedString(actualDependentBeans) +
							"] in its raw version as part of a circular reference, but has eventually been " +
							"wrapped. This means that said other beans do not use the final version of the " +
							"bean. This is often the result of over-eager type matching - consider using " +
							"'getBeanNamesOfType' with the 'allowEagerInit' flag turned off, for example.");
				}
			}
		}
	}

	// Register bean as disposable.
	//注册完成依赖注入的Bean
	try {
		registerDisposableBeanIfNecessary(beanName, bean, mbd);
	}
	catch (BeanDefinitionValidationException ex) {
		throw new BeanCreationException(mbd.getResourceDescription(), beanName, "Invalid destruction signature", ex);
	}

	//为应用返回所需要的实例对象
	return exposedObject;
}
```
根据需要实例化Bean类型，初始化FactoryBean类型Bean、无参数Bean、有参数Bean，以及处理化后调用SmartInstantiationAwareBeanPostProcessor接口，进行回调通知，下面以简单的无参数Bean为例
```java
//创建Bean的实例对象
protected BeanWrapper createBeanInstance(String beanName, RootBeanDefinition mbd, Object[] args) {
	// Make sure bean class is actually resolved at this point.
	//检查确认Bean是可实例化的
	Class<?> beanClass = resolveBeanClass(mbd, beanName);

	//使用工厂方法对Bean进行实例化
	if (beanClass != null && !Modifier.isPublic(beanClass.getModifiers()) && !mbd.isNonPublicAccessAllowed()) {
		throw new BeanCreationException(mbd.getResourceDescription(), beanName,
				"Bean class isn't public, and non-public access not allowed: " + beanClass.getName());
	}

	if (mbd.getFactoryMethodName() != null)  {
		//调用工厂方法实例化
		return instantiateUsingFactoryMethod(beanName, mbd, args);
	}

	// Shortcut when re-creating the same bean...
	//使用容器的自动装配方法进行实例化
	boolean resolved = false;
	boolean autowireNecessary = false;
	if (args == null) {
		synchronized (mbd.constructorArgumentLock) {
			if (mbd.resolvedConstructorOrFactoryMethod != null) {
				resolved = true;
				autowireNecessary = mbd.constructorArgumentsResolved;
			}
		}
	}
	if (resolved) {
		if (autowireNecessary) {
			//配置了自动装配属性，使用容器的自动装配实例化  
            //容器的自动装配是根据参数类型匹配Bean的构造方法
			return autowireConstructor(beanName, mbd, null, null);
		}
		else {
			//使用默认的无参构造方法实例化
			return instantiateBean(beanName, mbd);
		}
	}

	// Need to determine the constructor...
	//使用Bean的构造方法进行实例化
	Constructor<?>[] ctors = determineConstructorsFromBeanPostProcessors(beanClass, beanName);
	if (ctors != null ||
			mbd.getResolvedAutowireMode() == RootBeanDefinition.AUTOWIRE_CONSTRUCTOR ||
			mbd.hasConstructorArgumentValues() || !ObjectUtils.isEmpty(args))  {
		//使用容器的自动装配特性，调用匹配的构造方法实例化 
		return autowireConstructor(beanName, mbd, ctors, args);
	}

	// No special handling: simply use no-arg constructor.
	//使用默认的无参构造方法实例化
	return instantiateBean(beanName, mbd);
}
```
无参数Bean实例化，采用策略模式，创建实例对象
```java
//使用默认的无参构造方法实例化Bean对象
protected BeanWrapper instantiateBean(final String beanName, final RootBeanDefinition mbd) {
	try {
		Object beanInstance;
		final BeanFactory parent = this;
		//获取系统的安全管理接口，JDK标准的安全管理AP
		if (System.getSecurityManager() != null) {
			//这里是一个匿名内置类，根据实例化策略创建实例对象
			beanInstance = AccessController.doPrivileged(new PrivilegedAction<Object>() {
				public Object run() {
					return getInstantiationStrategy().instantiate(mbd, beanName, parent);
				}
			}, getAccessControlContext());
		}
		else {
			//将实例化的对象封装起来
			beanInstance = getInstantiationStrategy().instantiate(mbd, beanName, parent);
		}
		BeanWrapper bw = new BeanWrapperImpl(beanInstance);
		initBeanWrapper(bw);
		return bw;
	}
	catch (Throwable ex) {
		throw new BeanCreationException(mbd.getResourceDescription(), beanName, "Instantiation of bean failed", ex);
	}
}
```
BeanDefinition如果有lookup-method或者replaced-method等属性，采用cglib实现代理， 没有，直接使用反射实例化，返回包装类。
```java
//使用初始化策略实例化Bean对象
public Object instantiate(RootBeanDefinition beanDefinition, String beanName, BeanFactory owner) {
	// Don't override the class with CGLIB if no overrides.
	//如果Bean定义中没有方法覆盖lookup-method或者replaced-method，则就不需要CGLIB父类类的方法
	if (beanDefinition.getMethodOverrides().isEmpty()) {
		Constructor<?> constructorToUse;
		synchronized (beanDefinition.constructorArgumentLock) {
			//获取对象的构造方法或工厂方法
			constructorToUse = (Constructor<?>) beanDefinition.resolvedConstructorOrFactoryMethod;
			//如果没有构造方法且没有工厂方法 
			if (constructorToUse == null) {
				//使用JDK的反射机制，判断要实例化的Bean是否是接口
				final Class clazz = beanDefinition.getBeanClass();
				if (clazz.isInterface()) {
					throw new BeanInstantiationException(clazz, "Specified class is an interface");
				}
				try {
					if (System.getSecurityManager() != null) {
						//这里是一个匿名内置类，使用反射机制获取Bean的构造方法
						constructorToUse = AccessController.doPrivileged(new PrivilegedExceptionAction<Constructor>() {
							public Constructor run() throws Exception {
								return clazz.getDeclaredConstructor((Class[]) null);
							}
						});
					}
					else {
						constructorToUse =	clazz.getDeclaredConstructor((Class[]) null);
					}
					beanDefinition.resolvedConstructorOrFactoryMethod = constructorToUse;
				}
				catch (Exception ex) {
					throw new BeanInstantiationException(clazz, "No default constructor found", ex);
				}
			}
		}
		//使用BeanUtils实例化，通过反射机制调用”构造方法.newInstance(arg)”来进行实例化
		return BeanUtils.instantiateClass(constructorToUse);
	}
	else {
		// Must generate CGLIB subclass.
		//使用CGLIB来实例化对象
		return instantiateWithMethodInjection(beanDefinition, beanName, owner);
	}
}
```
### populateBean 属性注入
```java
//将Bean属性设置到生成的实例对象上
	protected void populateBean(String beanName, RootBeanDefinition mbd, BeanWrapper bw) {
		//获取容器在解析Bean定义资源时为BeanDefiniton中设置的属性值
		PropertyValues pvs = mbd.getPropertyValues();

		//实例对象为null
		if (bw == null) {
			if (!pvs.isEmpty()) {
				throw new BeanCreationException(
						mbd.getResourceDescription(), beanName, "Cannot apply property values to null instance");
			}
			else {
				// Skip property population phase for null instance.
				//实例对象为null，属性值也为空，不需要设置属性值，直接返回 
				return;
			}
		}

		// Give any InstantiationAwareBeanPostProcessors the opportunity to modify the
		// state of the bean before properties are set. This can be used, for example,
		// to support styles of field injection.
		//在设置属性之前调用Bean的PostProcessor后置处理器
		boolean continueWithPropertyPopulation = true;

		if (!mbd.isSynthetic() && hasInstantiationAwareBeanPostProcessors()) {
			for (BeanPostProcessor bp : getBeanPostProcessors()) {
				if (bp instanceof InstantiationAwareBeanPostProcessor) {
					InstantiationAwareBeanPostProcessor ibp = (InstantiationAwareBeanPostProcessor) bp;
					if (!ibp.postProcessAfterInstantiation(bw.getWrappedInstance(), beanName)) {
						continueWithPropertyPopulation = false;
						break;
					}
				}
			}
		}

		if (!continueWithPropertyPopulation) {
			return;
		}

		//依赖注入开始，首先处理autowire自动装配的注入
		if (mbd.getResolvedAutowireMode() == RootBeanDefinition.AUTOWIRE_BY_NAME ||
				mbd.getResolvedAutowireMode() == RootBeanDefinition.AUTOWIRE_BY_TYPE) {
			MutablePropertyValues newPvs = new MutablePropertyValues(pvs);

			// Add property values based on autowire by name if applicable.
			//对autowire自动装配的处理，根据Bean名称自动装配注入
			if (mbd.getResolvedAutowireMode() == RootBeanDefinition.AUTOWIRE_BY_NAME) {
				autowireByName(beanName, mbd, bw, newPvs);
			}

			// Add property values based on autowire by type if applicable.
			//根据Bean类型自动装配注入
			if (mbd.getResolvedAutowireMode() == RootBeanDefinition.AUTOWIRE_BY_TYPE) {
				autowireByType(beanName, mbd, bw, newPvs);
			}

			pvs = newPvs;
		}

		//检查容器是否持有用于处理单例模式Bean关闭时的后置处理器
		boolean hasInstAwareBpps = hasInstantiationAwareBeanPostProcessors();
		//Bean实例对象没有依赖，即没有继承基类
		boolean needsDepCheck = (mbd.getDependencyCheck() != RootBeanDefinition.DEPENDENCY_CHECK_NONE);

		if (hasInstAwareBpps || needsDepCheck) {
			//从实例对象中提取属性描述符
			PropertyDescriptor[] filteredPds = filterPropertyDescriptorsForDependencyCheck(bw, mbd.allowCaching);
			if (hasInstAwareBpps) {
				for (BeanPostProcessor bp : getBeanPostProcessors()) {
					if (bp instanceof InstantiationAwareBeanPostProcessor) {
						InstantiationAwareBeanPostProcessor ibp = (InstantiationAwareBeanPostProcessor) bp;
						//使用BeanPostProcessor处理器处理属性值
						pvs = ibp.postProcessPropertyValues(pvs, filteredPds, bw.getWrappedInstance(), beanName);
						if (pvs == null) {
							return;
						}
					}
				}
			}
			if (needsDepCheck) {
				//为要设置的属性进行依赖检查
				checkDependencies(beanName, mbd, filteredPds, pvs);
			}
		}
		//对属性进行注入
		applyPropertyValues(beanName, mbd, bw, pvs);
	}
```

### 生成代理Bean
从上面Bean实例化可以了解Bean实例化之前，调用BeanPostProcessor接口生成代理Bean, 实现类有AbstractAutoProxyCreator、AbstractAdvisorAutoProxyCreator
```java
public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
	if (bean != null) {
		//从缓存中查找
		Object cacheKey = getCacheKey(bean.getClass(), beanName);
		if (!this.earlyProxyReferences.containsKey(cacheKey)) {
			return wrapIfNecessary(bean, beanName, cacheKey);
		}
	}
	return bean;
}
```
getAdvicesAndAdvisorsForBean根据切面判断是否需要进行代理
```java
protected Object wrapIfNecessary(Object bean, String beanName, Object cacheKey) {
	//targetSourcedBeans缓存中是否存在
	if (beanName != null && this.targetSourcedBeans.containsKey(beanName)) {
		return bean;
	}
	//advisedBean缓存中是否存在
	if (Boolean.FALSE.equals(this.advisedBeans.get(cacheKey))) {
		return bean;
	}
	if (isInfrastructureClass(bean.getClass()) || shouldSkip(bean.getClass(), beanName)) {
		this.advisedBeans.put(cacheKey, Boolean.FALSE);
		return bean;
	}
	//根据advisor和advice等配置判断是否需要进行代理
	// Create proxy if we have advice.
	Object[] specificInterceptors = getAdvicesAndAdvisorsForBean(bean.getClass(), beanName, null);
	if (specificInterceptors != DO_NOT_PROXY) {
		this.advisedBeans.put(cacheKey, Boolean.TRUE);
		Object proxy = createProxy(bean.getClass(), beanName, specificInterceptors, new SingletonTargetSource(bean));
		this.proxyTypes.put(cacheKey, proxy.getClass());
		return proxy;
	}

	this.advisedBeans.put(cacheKey, Boolean.FALSE);
	return bean;
}
```
根据切面或者切点返回advisor
```java
//根据切面或者切点返回advisor
@Override
protected Object[] getAdvicesAndAdvisorsForBean(Class beanClass, String beanName, TargetSource targetSource) {
	List advisors = findEligibleAdvisors(beanClass, beanName);
	if (advisors.isEmpty()) {
		return DO_NOT_PROXY;
	}
	return advisors.toArray();
}
```
## 总结
1、BeanFactoryPostProcessor接口或者子接口或者抽象类，完成对BeanFactory中BeanDefinition修改或者添加、删除，以及可以获取BeanDefinition对应的Bean   
2、BeanPostProcessor接口或者子接口，完成对Bean初始化之前或者之后的相关操作，例如生成代理类、事件通知等。   
3、。。。。