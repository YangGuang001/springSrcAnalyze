## 主要流程
* 资源定位
* 加载资源
* 注册
### 主要接口
* Resource: 定位、访问资源的接口
* Beandefinition: IOC容器中Bean的定义，通过Beandefinition数据结构，可以很方便了解需要实例化Bean的属性信息
* BeanFactory: 定义IOC容器接口，为IOC容器定义了一些基础方法， 例如getBean， 其子类有列表化ListableBeanFactory、父子HierarchicalBeanFactory
* ApplicationContext: 具体应用的高级接口，继承了BeanFactory、MessageSource、ApplicationEventPublisher、HierarchicalBeanFactory、ListableBeanFactory等，主要是应用初始化过程中一些行为规范的定义，实现ListableBeanFactory可以getBean获取Bean， 实现HierarchicalBeanFactory可以存在父子BeanFactory。
### 简单使用方法
```java
ClassPathResource classPathResource = new ClassPathResource("properties.xml");
DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
XmlBeanDefinitionReader beanDefinitionReader = new XmlBeanDefinitionReader(beanFactory);
beanDefinitionReader.loadBeanDefinitions(classPathResource);
beanFactory.getBean("user");
//ApplicationContext applicationContext = new ClassPathXmlApplicationContext("properties.xml");
//applicationContext.getBean("user");
```
### 资源定位以及入口ClassPathXmlApplicationContext
入口: ClassPathXmlApplicationContext启动加载
```java
public ClassPathXmlApplicationContext(String[] configLocations, boolean refresh, ApplicationContext parent)
		throws BeansException {
	//configLocations是new ClassPathXmlApplicationContext("classpath:bean.xml");中传入的资源信息
	super(parent);
	setConfigLocations(configLocations);
	if (refresh) {
	    //ClassPathXmlApplicationContext实现了ApplicationContext接口，在refresh中会有MessageSource、ApplicationEventPublisher等相关信息处理
		refresh();
	}
}
```
应用初始化流程
```java
//容器初始化的过程，读入Bean定义资源，并解析注册
	public void refresh() throws BeansException, IllegalStateException {
		synchronized (this.startupShutdownMonitor) {
			//调用容器准备刷新的方法，获取容器的当时时间，同时给容器设置同步标识
			prepareRefresh();

			//告诉子类启动refreshBeanFactory()方法，Bean定义资源文件的载入从子类的refreshBeanFactory()方法启动  
			ConfigurableListableBeanFactory beanFactory = obtainFreshBeanFactory();

			//为BeanFactory配置容器特性，例如类加载器、事件处理器等
			prepareBeanFactory(beanFactory);

			try {
				//为容器的某些子类指定特殊的BeanPost事件处理器
				postProcessBeanFactory(beanFactory);

				//调用所有注册的BeanFactoryPostProcessor的Bean
				invokeBeanFactoryPostProcessors(beanFactory);

				//为BeanFactory注册BeanPost事件处理器.  
		        //BeanPostProcessor是Bean后置处理器，用于监听容器触发的事件 
				registerBeanPostProcessors(beanFactory);

				//初始化信息源，和国际化相关.
				initMessageSource();

				//初始化容器事件传播器
				initApplicationEventMulticaster();

				//调用子类的某些特殊Bean初始化方法
				onRefresh();

				//为事件传播器注册事件监听器.
				registerListeners();

				//初始化所有剩余的单例Bean.
				finishBeanFactoryInitialization(beanFactory);

				// Last step: publish corresponding event.
				//初始化容器的生命周期事件处理器，并发布容器的生命周期事件
				finishRefresh();
			} catch (BeansException ex) {
				// Destroy already created singletons to avoid dangling resources.
				//销毁以创建的单态Bean
				destroyBeans();
				//取消refresh操作，重置容器的同步标识.
				cancelRefresh(ex);
				throw ex;
			}
		}
	}
```
obtainFreshBeanFactory()中进行资源加载，返回BeanFactory，也就是本节中需要讲解的整个流程
```java
protected ConfigurableListableBeanFactory obtainFreshBeanFactory() {
		//使用了委派设计模式，父类定义了抽象的refreshBeanFactory()方法，具体实现调用子类容器的refreshBeanFactory()方法
		refreshBeanFactory();
		ConfigurableListableBeanFactory beanFactory = getBeanFactory();
		if (logger.isDebugEnabled()) {
			logger.debug("Bean factory for " + getDisplayName() + ": " + beanFactory);
		}
		return beanFactory;
	}
```
**refreshBeanFactory方法，委派给AbstractApplicationContext的子类实现，主要实现创建BeanFactory，定位、加载、注册BeanDefinition的主要流程**
```java
protected final void refreshBeanFactory() throws BeansException {
	if (hasBeanFactory()) {//如果已经有容器，销毁容器中的bean，关闭容器
		destroyBeans();
		closeBeanFactory();
	}
	try {
		//创建IOC容器
		DefaultListableBeanFactory beanFactory = createBeanFactory();
		beanFactory.setSerializationId(getId());
		//对IOC容器进行定制化，如设置启动参数，开启注解的自动装配等
		customizeBeanFactory(beanFactory);
		//调用载入Bean定义的方法，主要这里又使用了一个委派模式，在当前类中只定义了抽象的loadBeanDefinitions方法，具体的实现调用子类容器
		loadBeanDefinitions(beanFactory);
		synchronized (this.beanFactoryMonitor) {
			this.beanFactory = beanFactory;
		}
	}
```
**loadBeanDefinitions方法，委派给AbstractRefreshableConfigApplicationContext的子类实现，这个主要讲解XML的定位、加载、注册的流程，查看AbstractXmlApplicationContext**
```java
protected void loadBeanDefinitions(DefaultListableBeanFactory beanFactory) throws BeansException, IOException {
		// Create a new XmlBeanDefinitionReader for the given BeanFactory.
		//创建XmlBeanDefinitionReader，即创建Bean读取器，并通过回调设置到容器中去，容  器使用该读取器读取Bean定义资源
		XmlBeanDefinitionReader beanDefinitionReader = new XmlBeanDefinitionReader(beanFactory);

		// Configure the bean definition reader with this context's
		// resource loading environment.
		beanDefinitionReader.setEnvironment(this.getEnvironment());
		//为Bean读取器设置Spring资源加载器，AbstractXmlApplicationContext的  
        //祖先父类AbstractApplicationContext继承DefaultResourceLoader，因此，容器本身也是一个资源加载器
		beanDefinitionReader.setResourceLoader(this);
		//为Bean读取器设置SAX xml解析器
		beanDefinitionReader.setEntityResolver(new ResourceEntityResolver(this));

		// Allow a subclass to provide custom initialization of the reader,
		// then proceed with actually loading the bean definitions.
		//当Bean读取器读取Bean定义的Xml资源文件时，启用Xml的校验机制
		initBeanDefinitionReader(beanDefinitionReader);
		//Bean读取器真正实现加载的方法
		loadBeanDefinitions(beanDefinitionReader);
	}
```
beanDefinitionReader跟开始demo中的使用方法一致，可以得出ClassPathXmlApplicationContext内部也是使用XmlBeanDefinitionReader读取并解析xml，注册BeanDefinition信息到BeanFactory中。
```java
protected void loadBeanDefinitions(XmlBeanDefinitionReader reader) throws BeansException, IOException {
	//获取Bean定义资源的定位
	//这里使用了一个委托模式，调用子类的获取Bean定义资源定位的方法  
    //该方法在ClassPathXmlApplicationContext中进行实现，对于我们  
	Resource[] configResources = getConfigResources();
	if (configResources != null) {
		//Xml Bean读取器调用其父类AbstractBeanDefinitionReader读取定位的Bean定义资源
		reader.loadBeanDefinitions(configResources);
	}
	//如果子类中获取的Bean定义资源定位为空，则获取FileSystemXmlApplicationContext构造方法中setConfigLocations方法设置的资源
	String[] configLocations = getConfigLocations();
	if (configLocations != null) {
		//Xml Bean读取器调用其父类AbstractBeanDefinitionReader读取定位的Bean定义资源
		reader.loadBeanDefinitions(configLocations);
	}
}
```
**getConfigResources() 委托给AbstractXmlApplicationContext子类进行获取xml文件位置，AbstractXmlApplicationContext子类ClassPathXmlApplicationContext、FileSystemXmlApplicationContext获取资源** ClassPathXmlApplicationContext中定位资源getConfigResources
### XmlBeanDefinitionReader 加载资源
```java
//XmlBeanDefinitionReader加载资源的入口方法
public int loadBeanDefinitions(Resource resource) throws BeanDefinitionStoreException {
	//将读入的XML资源进行特殊编码处理
	return loadBeanDefinitions(new EncodedResource(resource));
}
//这里是载入XML形式Bean定义资源文件方法
public int loadBeanDefinitions(EncodedResource encodedResource) throws BeanDefinitionStoreException {
	Assert.notNull(encodedResource, "EncodedResource must not be null");
	if (logger.isInfoEnabled()) {
		logger.info("Loading XML bean definitions from " + encodedResource.getResource());
	}
	Set<EncodedResource> currentResources = this.resourcesCurrentlyBeingLoaded.get();
	if (currentResources == null) {
		currentResources = new HashSet<EncodedResource>(4);
		this.resourcesCurrentlyBeingLoaded.set(currentResources);
	}
	if (!currentResources.add(encodedResource)) {
		throw new BeanDefinitionStoreException(
				"Detected cyclic loading of " + encodedResource + " - check your import definitions!");
	}
	try {
		//将资源文件转为InputStream的IO流
		InputStream inputStream = encodedResource.getResource().getInputStream();
		try {
			//从InputStream中得到XML的解析源
			InputSource inputSource = new InputSource(inputStream);
			if (encodedResource.getEncoding() != null) {
				inputSource.setEncoding(encodedResource.getEncoding());
			}
			//这里是具体的读取过程
			return doLoadBeanDefinitions(inputSource, encodedResource.getResource());
		}
		finally {
			//关闭从Resource中得到的IO流
			inputStream.close();
		}
	}
```
统一文件编码格式，进行xml加载，使用DOM4j解析xml文件
```java
//从特定XML文件中实际载入Bean定义资源的方法
	protected int doLoadBeanDefinitions(InputSource inputSource, Resource resource)
			throws BeanDefinitionStoreException {
		try {
			int validationMode = getValidationModeForResource(resource);
			//将XML文件转换为DOM对象，解析过程由documentLoader实现
			Document doc = this.documentLoader.loadDocument(
					inputSource, getEntityResolver(), this.errorHandler, validationMode, isNamespaceAware());
			//这里是启动对Bean定义解析的详细过程，该解析过程会用到Spring的Bean配置规则
			return registerBeanDefinitions(doc, resource);
		}
```
在registerBeanDefinitions中进行xml加载，解析为BeanDefinition，注册到BeanFactory中
```java
//按照Spring的Bean语义要求将Bean定义资源解析并转换为容器内部数据结构
public int registerBeanDefinitions(Document doc, Resource resource) throws BeanDefinitionStoreException {
	//得到BeanDefinitionDocumentReader来对xml格式的BeanDefinition解析
	BeanDefinitionDocumentReader documentReader = createBeanDefinitionDocumentReader();
	documentReader.setEnvironment(this.getEnvironment());
	//获得容器中注册的Bean数量
	int countBefore = getRegistry().getBeanDefinitionCount();
	//解析过程入口，这里使用了委派模式，BeanDefinitionDocumentReader只是个接口，
	//具体的解析实现过程有实现类DefaultBeanDefinitionDocumentReader完成
	documentReader.registerBeanDefinitions(doc, createReaderContext(resource));
	//统计解析的Bean数量
	return getRegistry().getBeanDefinitionCount() - countBefore;
}
```
BeanDefinitionDocumentReader的子类DefaultBeanDefinitionDocumentReader完成xml解析成BeanDefinition
```java
//根据Spring DTD对Bean的定义规则解析Bean定义Document对象
public void registerBeanDefinitions(Document doc, XmlReaderContext readerContext) {
	//获得XML描述符
	this.readerContext = readerContext;
	logger.debug("Loading bean definitions");
	//获得Document的根元素
	Element root = doc.getDocumentElement();
	doRegisterBeanDefinitions(root);
}
protected void doRegisterBeanDefinitions(Element root) {
	String profileSpec = root.getAttribute(PROFILE_ATTRIBUTE);
	//具体的解析过程由BeanDefinitionParserDelegate实现，  
    //BeanDefinitionParserDelegate中定义了Spring Bean定义XML文件的各种元素 
	BeanDefinitionParserDelegate parent = this.delegate;
	this.delegate = createDelegate(this.readerContext, root, parent);
	
	//在解析Bean定义之前，进行自定义的解析，增强解析过程的可扩展性
	preProcessXml(root);
	//从Document的根元素开始进行Bean定义的Document对象
	parseBeanDefinitions(root, this.delegate);
	//在解析Bean定义之后，进行自定义的解析，增加解析过程的可扩展性
	postProcessXml(root);

	this.delegate = parent;
}
```
DefaultBeanDefinitionDocumentReader提供了两个拓展接口preProcessXml、postProcessXml，具体的解析交给BeanDefinitionParserDelegate,同时root Element可以根据xml中命名空间交给header进行解析，这里拓展出可以自定义xml命名空间标签。
```java
//使用Spring的Bean规则从Document的根元素开始进行Bean定义的Document对象
protected void parseBeanDefinitions(Element root, BeanDefinitionParserDelegate delegate) {
	//Bean定义的Document对象使用了Spring默认的XML命名空间
	if (delegate.isDefaultNamespace(root)) {
		//获取Bean定义的Document对象根元素的所有子节点
		NodeList nl = root.getChildNodes();
		for (int i = 0; i < nl.getLength(); i++) {
			Node node = nl.item(i);
			//获得Document节点是XML元素节点
			if (node instanceof Element) {
				Element ele = (Element) node;
				//Bean定义的Document的元素节点使用的是Spring默认的XML命名空间 
				if (delegate.isDefaultNamespace(ele)) {
					//使用Spring的Bean规则解析元素节点
					parseDefaultElement(ele, delegate);
				}
				else {
					//没有使用Spring默认的XML命名空间，则使用用户自定义的解
					//析规则解析元素节点
					delegate.parseCustomElement(ele);
				}
			}
		}
	}
	else {
		//Document的根节点没有使用Spring默认的命名空间，则使用用户自定义的  
	    //解析规则解析Document根节点
		delegate.parseCustomElement(root);
	}
}
```
自定义命名空间
```java
public BeanDefinition parseCustomElement(Element ele, BeanDefinition containingBd) {
	//获取命名空间uri
	String namespaceUri = getNamespaceURI(ele);
	//根据命名空间uri查找对应hander
	NamespaceHandler handler = this.readerContext.getNamespaceHandlerResolver().resolve(namespaceUri);
	if (handler == null) {
		error("Unable to locate Spring NamespaceHandler for XML schema namespace [" + namespaceUri + "]", ele);
		return null;
	}
	//header解析xml，生成BeanDefinition
	return handler.parse(ele, new ParserContext(this.readerContext, this, containingBd));
}
```
默认命名空间
```java
//使用Spring的Bean规则解析Document元素节点
private void parseDefaultElement(Element ele, BeanDefinitionParserDelegate delegate) {
	//如果元素节点是<Import>导入元素，进行导入解析
	if (delegate.nodeNameEquals(ele, IMPORT_ELEMENT)) {
		importBeanDefinitionResource(ele);
	}
	//如果元素节点是<Alias>别名元素，进行别名解析
	else if (delegate.nodeNameEquals(ele, ALIAS_ELEMENT)) {
		processAliasRegistration(ele);
	}
	//元素节点既不是导入元素，也不是别名元素，即普通的<Bean>元素，  
	//按照Spring的Bean规则解析元素
	else if (delegate.nodeNameEquals(ele, BEAN_ELEMENT)) {
		processBeanDefinition(ele, delegate);
	}
	else if (delegate.nodeNameEquals(ele, NESTED_BEANS_ELEMENT)) {
		// recurse
		doRegisterBeanDefinitions(ele);
	}
}
```
<Bean>元素进行解析，并注册到BeanDefinitionRegistry中
```java
//解析Bean定义资源Document对象的普通元素
protected void processBeanDefinition(Element ele, BeanDefinitionParserDelegate delegate) {
	BeanDefinitionHolder bdHolder = delegate.parseBeanDefinitionElement(ele);
	// BeanDefinitionHolder是对BeanDefinition的封装，即Bean定义的封装类  
	// 对Document对象中<Bean>元素的解析由BeanDefinitionParserDelegate实现
	// BeanDefinitionHolder bdHolder = delegate.parseBeanDefinitionElement(ele);
	if (bdHolder != null) {
		bdHolder = delegate.decorateBeanDefinitionIfRequired(ele, bdHolder);
		try {
			// Register the final decorated instance.
			//向Spring IOC容器注册解析得到的Bean定义，这是Bean定义向IOC容器注册的入口
			BeanDefinitionReaderUtils.registerBeanDefinition(bdHolder, getReaderContext().getRegistry());
		}
		catch (BeanDefinitionStoreException ex) {
			getReaderContext().error("Failed to register bean definition with name '" +
					bdHolder.getBeanName() + "'", ele, ex);
		}
		// Send registration event.
		//在完成向Spring IOC容器注册解析得到的Bean定义之后，发送注册事件
		getReaderContext().fireComponentRegistered(new BeanComponentDefinition(bdHolder));
	}
}
```
BeanDefinitionHolder是对BeanDefinition的封装,如果默认标签中使用到了自定义标签，再使用decorateBeanDefinitionIfRequired进行解析。parseBeanDefinitionElement解析<Bean>中属性。
```java
//详细对<Bean>元素中配置的Bean定义其他属性进行解析，由于上面的方法中已经对
//Bean的id、name和别名等属性进行了处理，该方法中主要处理除这三个以外的其他属性数据  
public AbstractBeanDefinition parseBeanDefinitionElement(
		Element ele, String beanName, BeanDefinition containingBean) {

	//记录解析的<Bean>
	this.parseState.push(new BeanEntry(beanName));

	//这里只读取<Bean>元素中配置的class名字，然后载入到BeanDefinition中去  
    //只是记录配置的class名字，不做实例化，对象的实例化在依赖注入时完成
	String className = null;
	if (ele.hasAttribute(CLASS_ATTRIBUTE)) {
		className = ele.getAttribute(CLASS_ATTRIBUTE).trim();
	}

	try {
		String parent = null;
		//如果<Bean>元素中配置了parent属性，则获取parent属性的值
		if (ele.hasAttribute(PARENT_ATTRIBUTE)) {
			parent = ele.getAttribute(PARENT_ATTRIBUTE);
		}
		
		//根据<Bean>元素配置的class名称和parent属性值创建BeanDefinition  
        //为载入Bean定义信息做准备
		AbstractBeanDefinition bd = createBeanDefinition(className, parent);

		//对当前的<Bean>元素中配置的一些属性进行解析和设置，如配置的单态(singleton)属性等
		parseBeanDefinitionAttributes(ele, beanName, containingBean, bd);
		//为<Bean>元素解析的Bean设置description信息
		bd.setDescription(DomUtils.getChildElementValueByTagName(ele, DESCRIPTION_ELEMENT));

		//对<Bean>元素的meta(元信息)属性解析
		parseMetaElements(ele, bd);
		//对<Bean>元素的lookup-method属性解析
		parseLookupOverrideSubElements(ele, bd.getMethodOverrides());
		//对<Bean>元素的replaced-method属性解析
		parseReplacedMethodSubElements(ele, bd.getMethodOverrides());

		//解析<Bean>元素的构造方法设置
		parseConstructorArgElements(ele, bd);
		//解析<Bean>元素的<property>设置
		parsePropertyElements(ele, bd);
		//解析<Bean>元素的qualifier属性
		parseQualifierElements(ele, bd);

		//为当前解析的Bean设置所需的资源和依赖对象
		bd.setResource(this.readerContext.getResource());
		bd.setSource(extractSource(ele));

		return bd;
	}
```
### BeanDefinition注册
在BeanDefinitionReaderUtils.registerBeanDefinition中完成对BeanDefinition的注册。
```java
//将解析的BeanDefinitionHold注册到容器中
public static void registerBeanDefinition(
		BeanDefinitionHolder definitionHolder, BeanDefinitionRegistry registry)
		throws BeanDefinitionStoreException {

	// Register bean definition under primary name.
	//获取解析的BeanDefinition的名称
	String beanName = definitionHolder.getBeanName();
	//向IOC容器注册BeanDefinition
	registry.registerBeanDefinition(beanName, definitionHolder.getBeanDefinition());

	// Register aliases for bean name, if any.
	//如果解析的BeanDefinition有别名，向容器为其注册别名
	String[] aliases = definitionHolder.getAliases();
	if (aliases != null) {
		for (String aliase : aliases) {
			registry.registerAlias(beanName, aliase);
		}
	}
}
```
把包装类BeanDefinitionHolder中BeanDefinition注册到DefaultListableBeanFactory的beanDefinitionMap中。
```java
//向IoC容器注册解析的BeanDefinito
public void registerBeanDefinition(String beanName, BeanDefinition beanDefinition)
		throws BeanDefinitionStoreException {

	Assert.hasText(beanName, "Bean name must not be empty");
	Assert.notNull(beanDefinition, "BeanDefinition must not be null");

	//校验解析的BeanDefiniton
	if (beanDefinition instanceof AbstractBeanDefinition) {
		try {
			((AbstractBeanDefinition) beanDefinition).validate();
		}
		catch (BeanDefinitionValidationException ex) {
			throw new BeanDefinitionStoreException(beanDefinition.getResourceDescription(), beanName,
					"Validation of bean definition failed", ex);
		}
	}

	//注册的过程中需要线程同步，以保证数据的一致性
	synchronized (this.beanDefinitionMap) {
		Object oldBeanDefinition = this.beanDefinitionMap.get(beanName);
		
		//检查是否有同名的BeanDefinition已经在IOC容器中注册，如果已经注册，  
        //并且不允许覆盖已注册的Bean，则抛出注册失败异常
		if (oldBeanDefinition != null) {
			if (!this.allowBeanDefinitionOverriding) {
				throw new BeanDefinitionStoreException(beanDefinition.getResourceDescription(), beanName,
						"Cannot register bean definition [" + beanDefinition + "] for bean '" + beanName +
						"': There is already [" + oldBeanDefinition + "] bound.");
			}
			else {//如果允许覆盖，则同名的Bean，后注册的覆盖先注册的
				if (this.logger.isInfoEnabled()) {
					this.logger.info("Overriding bean definition for bean '" + beanName +
							"': replacing [" + oldBeanDefinition + "] with [" + beanDefinition + "]");
				}
			}
		}
		else {//IOC容器中没有已经注册同名的Bean，按正常注册流程注册
			this.beanDefinitionNames.add(beanName);
			this.frozenBeanDefinitionNames = null;
		}
		this.beanDefinitionMap.put(beanName, beanDefinition);
	}
	//重置所有已经注册过的BeanDefinition的缓存
	resetBeanDefinition(beanName);
}
```

## 总结
1、自定义xml命名空间和标签可以实现NamespaceHandlerSupport和BeanDefinitionParser接口，注册自定义标签的BeanDefinition。
* spring内部aop标签也是实现这两接口，对aop标签解析为BeanDefinition进行注册。
* 属性配置注入property-placeholder、component-scan、annotation-driven等    
2、使用spring的PropertyPlaceholderConfigurer查找加载配置文件。