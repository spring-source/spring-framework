/*
 * Copyright 2002-2024 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.beans.factory.support;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Supplier;

import org.apache.commons.logging.Log;
import org.jspecify.annotations.Nullable;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.beans.BeansException;
import org.springframework.beans.InvalidPropertyException;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.PropertyAccessorUtils;
import org.springframework.beans.PropertyValue;
import org.springframework.beans.PropertyValues;
import org.springframework.beans.TypeConverter;
import org.springframework.beans.factory.Aware;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.BeanCurrentlyInCreationException;
import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.InjectionPoint;
import org.springframework.beans.factory.UnsatisfiedDependencyException;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.beans.factory.config.AutowiredPropertyMarker;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.beans.factory.config.ConstructorArgumentValues;
import org.springframework.beans.factory.config.DependencyDescriptor;
import org.springframework.beans.factory.config.InstantiationAwareBeanPostProcessor;
import org.springframework.beans.factory.config.SmartInstantiationAwareBeanPostProcessor;
import org.springframework.beans.factory.config.TypedStringValue;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.MethodParameter;
import org.springframework.core.NamedThreadLocal;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.core.PriorityOrdered;
import org.springframework.core.ResolvableType;
import org.springframework.util.ClassUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.ReflectionUtils.MethodCallback;
import org.springframework.util.StringUtils;
import org.springframework.util.function.ThrowingSupplier;

/**
 * Abstract bean factory superclass that implements default bean creation,
 * with the full capabilities specified by the {@link RootBeanDefinition} class.
 * Implements the {@link org.springframework.beans.factory.config.AutowireCapableBeanFactory}
 * interface in addition to AbstractBeanFactory's {@link #createBean} method.
 *
 * <p>Provides bean creation (with constructor resolution), property population,
 * wiring (including autowiring), and initialization. Handles runtime bean
 * references, resolves managed collections, calls initialization methods, etc.
 * Supports autowiring constructors, properties by name, and properties by type.
 *
 * <p>The main template method to be implemented by subclasses is
 * {@link #resolveDependency(DependencyDescriptor, String, Set, TypeConverter)}, used for
 * autowiring. In case of a {@link org.springframework.beans.factory.ListableBeanFactory}
 * which is capable of searching its bean definitions, matching beans will typically be
 * implemented through such a search. Otherwise, simplified matching can be implemented.
 *
 * <p>Note that this class does <i>not</i> assume or implement bean definition
 * registry capabilities. See {@link DefaultListableBeanFactory} for an implementation
 * of the {@link org.springframework.beans.factory.ListableBeanFactory} and
 * {@link BeanDefinitionRegistry} interfaces, which represent the API and SPI
 * view of such a factory, respectively.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Rob Harrop
 * @author Mark Fisher
 * @author Costin Leau
 * @author Chris Beams
 * @author Sam Brannen
 * @author Phillip Webb
 * @since 13.02.2004
 * @see RootBeanDefinition
 * @see DefaultListableBeanFactory
 * @see BeanDefinitionRegistry
 */
public abstract class AbstractAutowireCapableBeanFactory extends AbstractBeanFactory
		implements AutowireCapableBeanFactory {

	/** Strategy for creating bean instances. */
	private InstantiationStrategy instantiationStrategy;

	/** Resolver strategy for method parameter names. */
	private @Nullable ParameterNameDiscoverer parameterNameDiscoverer = new DefaultParameterNameDiscoverer();

	/** Whether to automatically try to resolve circular references between beans. */
	private boolean allowCircularReferences = true;

	/**
	 * Whether to resort to injecting a raw bean instance in case of circular reference,
	 * even if the injected bean eventually got wrapped.
	 */
	private boolean allowRawInjectionDespiteWrapping = false;

	/**
	 * Dependency types to ignore on dependency check and autowire, as Set of
	 * Class objects: for example, String. Default is none.
	 */
	private final Set<Class<?>> ignoredDependencyTypes = new HashSet<>();

	/**
	 * Dependency interfaces to ignore on dependency check and autowire, as Set of
	 * Class objects. By default, only the BeanFactory interface is ignored.
	 */
	private final Set<Class<?>> ignoredDependencyInterfaces = new HashSet<>();

	/**
	 * The name of the currently created bean, for implicit dependency registration
	 * on getBean etc invocations triggered from a user-specified Supplier callback.
	 */
	private final NamedThreadLocal<String> currentlyCreatedBean = new NamedThreadLocal<>("Currently created bean");

	/** Cache of unfinished FactoryBean instances: FactoryBean name to BeanWrapper. */
	private final ConcurrentMap<String, BeanWrapper> factoryBeanInstanceCache = new ConcurrentHashMap<>();

	/** Cache of candidate factory methods per factory class. */
	private final ConcurrentMap<Class<?>, Method[]> factoryMethodCandidateCache = new ConcurrentHashMap<>();

	/** Cache of filtered PropertyDescriptors: bean Class to PropertyDescriptor array. */
	private final ConcurrentMap<Class<?>, PropertyDescriptor[]> filteredPropertyDescriptorsCache =
			new ConcurrentHashMap<>();


	/**
	 * Create a new AbstractAutowireCapableBeanFactory.
	 */
	public AbstractAutowireCapableBeanFactory() {
		super();
		ignoreDependencyInterface(BeanNameAware.class);
		ignoreDependencyInterface(BeanFactoryAware.class);
		ignoreDependencyInterface(BeanClassLoaderAware.class);
		this.instantiationStrategy = new CglibSubclassingInstantiationStrategy();
	}

	/**
	 * Create a new AbstractAutowireCapableBeanFactory with the given parent.
	 * @param parentBeanFactory parent bean factory, or {@code null} if none
	 */
	public AbstractAutowireCapableBeanFactory(@Nullable BeanFactory parentBeanFactory) {
		this();
		setParentBeanFactory(parentBeanFactory);
	}


	/**
	 * Set the instantiation strategy to use for creating bean instances.
	 * Default is CglibSubclassingInstantiationStrategy.
	 * @see CglibSubclassingInstantiationStrategy
	 */
	public void setInstantiationStrategy(InstantiationStrategy instantiationStrategy) {
		this.instantiationStrategy = instantiationStrategy;
	}

	/**
	 * Return the instantiation strategy to use for creating bean instances.
	 */
	public InstantiationStrategy getInstantiationStrategy() {
		return this.instantiationStrategy;
	}

	/**
	 * Set the ParameterNameDiscoverer to use for resolving method parameter
	 * names if needed (for example, for constructor names).
	 * <p>Default is a {@link DefaultParameterNameDiscoverer}.
	 */
	public void setParameterNameDiscoverer(@Nullable ParameterNameDiscoverer parameterNameDiscoverer) {
		this.parameterNameDiscoverer = parameterNameDiscoverer;
	}

	/**
	 * Return the ParameterNameDiscoverer to use for resolving method parameter
	 * names if needed.
	 */
	public @Nullable ParameterNameDiscoverer getParameterNameDiscoverer() {
		return this.parameterNameDiscoverer;
	}

	/**
	 * Set whether to allow circular references between beans - and automatically
	 * try to resolve them.
	 * <p>Note that circular reference resolution means that one of the involved beans
	 * will receive a reference to another bean that is not fully initialized yet.
	 * This can lead to subtle and not-so-subtle side effects on initialization;
	 * it does work fine for many scenarios, though.
	 * <p>Default is "true". Turn this off to throw an exception when encountering
	 * a circular reference, disallowing them completely.
	 * <p><b>NOTE:</b> It is generally recommended to not rely on circular references
	 * between your beans. Refactor your application logic to have the two beans
	 * involved delegate to a third bean that encapsulates their common logic.
	 */
	public void setAllowCircularReferences(boolean allowCircularReferences) {
		this.allowCircularReferences = allowCircularReferences;
	}

	/**
	 * Return whether to allow circular references between beans.
	 * @since 5.3.10
	 * @see #setAllowCircularReferences
	 */
	public boolean isAllowCircularReferences() {
		return this.allowCircularReferences;
	}

	/**
	 * Set whether to allow the raw injection of a bean instance into some other
	 * bean's property, despite the injected bean eventually getting wrapped
	 * (for example, through AOP auto-proxying).
	 * <p>This will only be used as a last resort in case of a circular reference
	 * that cannot be resolved otherwise: essentially, preferring a raw instance
	 * getting injected over a failure of the entire bean wiring process.
	 * <p>Default is "false", as of Spring 2.0. Turn this on to allow for non-wrapped
	 * raw beans injected into some of your references, which was Spring 1.2's
	 * (arguably unclean) default behavior.
	 * <p><b>NOTE:</b> It is generally recommended to not rely on circular references
	 * between your beans, in particular with auto-proxying involved.
	 * @see #setAllowCircularReferences
	 */
	public void setAllowRawInjectionDespiteWrapping(boolean allowRawInjectionDespiteWrapping) {
		this.allowRawInjectionDespiteWrapping = allowRawInjectionDespiteWrapping;
	}

	/**
	 * Return whether to allow the raw injection of a bean instance.
	 * @since 5.3.10
	 * @see #setAllowRawInjectionDespiteWrapping
	 */
	public boolean isAllowRawInjectionDespiteWrapping() {
		return this.allowRawInjectionDespiteWrapping;
	}

	/**
	 * Ignore the given dependency type for autowiring:
	 * for example, String. Default is none.
	 */
	public void ignoreDependencyType(Class<?> type) {
		this.ignoredDependencyTypes.add(type);
	}

	/**
	 * Ignore the given dependency interface for autowiring.
	 * <p>This will typically be used by application contexts to register
	 * dependencies that are resolved in other ways, like BeanFactory through
	 * BeanFactoryAware or ApplicationContext through ApplicationContextAware.
	 * <p>By default, only the BeanFactoryAware interface is ignored.
	 * For further types to ignore, invoke this method for each type.
	 * @see org.springframework.beans.factory.BeanFactoryAware
	 * @see org.springframework.context.ApplicationContextAware
	 */
	public void ignoreDependencyInterface(Class<?> ifc) {
		this.ignoredDependencyInterfaces.add(ifc);
	}

	@Override
	public void copyConfigurationFrom(ConfigurableBeanFactory otherFactory) {
		super.copyConfigurationFrom(otherFactory);
		if (otherFactory instanceof AbstractAutowireCapableBeanFactory otherAutowireFactory) {
			this.instantiationStrategy = otherAutowireFactory.instantiationStrategy;
			this.allowCircularReferences = otherAutowireFactory.allowCircularReferences;
			this.ignoredDependencyTypes.addAll(otherAutowireFactory.ignoredDependencyTypes);
			this.ignoredDependencyInterfaces.addAll(otherAutowireFactory.ignoredDependencyInterfaces);
		}
	}


	//-------------------------------------------------------------------------
	// Typical methods for creating and populating external bean instances
	//-------------------------------------------------------------------------

	@Override
	@SuppressWarnings("unchecked")
	public <T> T createBean(Class<T> beanClass) throws BeansException {
		// Use non-singleton bean definition, to avoid registering bean as dependent bean.
		RootBeanDefinition bd = new CreateFromClassBeanDefinition(beanClass);
		bd.setScope(SCOPE_PROTOTYPE);
		bd.allowCaching = ClassUtils.isCacheSafe(beanClass, getBeanClassLoader());
		return (T) createBean(beanClass.getName(), bd, null);
	}

	@Override
	public void autowireBean(Object existingBean) {
		// Use non-singleton bean definition, to avoid registering bean as dependent bean.
		RootBeanDefinition bd = new RootBeanDefinition(ClassUtils.getUserClass(existingBean));
		bd.setScope(SCOPE_PROTOTYPE);
		bd.allowCaching = ClassUtils.isCacheSafe(bd.getBeanClass(), getBeanClassLoader());
		BeanWrapper bw = new BeanWrapperImpl(existingBean);
		initBeanWrapper(bw);
		populateBean(bd.getBeanClass().getName(), bd, bw);
	}

	@Override
	public Object configureBean(Object existingBean, String beanName) throws BeansException {
		markBeanAsCreated(beanName);
		BeanDefinition mbd = getMergedBeanDefinition(beanName);
		RootBeanDefinition bd = null;
		if (mbd instanceof RootBeanDefinition rbd) {
			bd = (rbd.isPrototype() ? rbd : rbd.cloneBeanDefinition());
		}
		if (bd == null) {
			bd = new RootBeanDefinition(mbd);
		}
		if (!bd.isPrototype()) {
			bd.setScope(SCOPE_PROTOTYPE);
			bd.allowCaching = ClassUtils.isCacheSafe(ClassUtils.getUserClass(existingBean), getBeanClassLoader());
		}
		BeanWrapper bw = new BeanWrapperImpl(existingBean);
		initBeanWrapper(bw);
		populateBean(beanName, bd, bw);
		return initializeBean(beanName, existingBean, bd);
	}


	//-------------------------------------------------------------------------
	// Specialized methods for fine-grained control over the bean lifecycle
	//-------------------------------------------------------------------------

	@Deprecated
	@Override
	public Object createBean(Class<?> beanClass, int autowireMode, boolean dependencyCheck) throws BeansException {
		// Use non-singleton bean definition, to avoid registering bean as dependent bean.
		RootBeanDefinition bd = new RootBeanDefinition(beanClass, autowireMode, dependencyCheck);
		bd.setScope(SCOPE_PROTOTYPE);
		return createBean(beanClass.getName(), bd, null);
	}

	@Override
	public Object autowire(Class<?> beanClass, int autowireMode, boolean dependencyCheck) throws BeansException {
		// Use non-singleton bean definition, to avoid registering bean as dependent bean.
		RootBeanDefinition bd = new RootBeanDefinition(beanClass, autowireMode, dependencyCheck);
		bd.setScope(SCOPE_PROTOTYPE);
		if (bd.getResolvedAutowireMode() == AUTOWIRE_CONSTRUCTOR) {
			return autowireConstructor(beanClass.getName(), bd, null, null).getWrappedInstance();
		}
		else {
			Object bean = getInstantiationStrategy().instantiate(bd, null, this);
			populateBean(beanClass.getName(), bd, new BeanWrapperImpl(bean));
			return bean;
		}
	}

	@Override
	public void autowireBeanProperties(Object existingBean, int autowireMode, boolean dependencyCheck)
			throws BeansException {

		if (autowireMode == AUTOWIRE_CONSTRUCTOR) {
			throw new IllegalArgumentException("AUTOWIRE_CONSTRUCTOR not supported for existing bean instance");
		}
		// Use non-singleton bean definition, to avoid registering bean as dependent bean.
		RootBeanDefinition bd =
				new RootBeanDefinition(ClassUtils.getUserClass(existingBean), autowireMode, dependencyCheck);
		bd.setScope(SCOPE_PROTOTYPE);
		BeanWrapper bw = new BeanWrapperImpl(existingBean);
		initBeanWrapper(bw);
		populateBean(bd.getBeanClass().getName(), bd, bw);
	}

	@Override
	public void applyBeanPropertyValues(Object existingBean, String beanName) throws BeansException {
		markBeanAsCreated(beanName);
		BeanDefinition bd = getMergedBeanDefinition(beanName);
		BeanWrapper bw = new BeanWrapperImpl(existingBean);
		initBeanWrapper(bw);
		applyPropertyValues(beanName, bd, bw, bd.getPropertyValues());
	}

	@Override
	public Object initializeBean(Object existingBean, String beanName) {
		return initializeBean(beanName, existingBean, null);
	}

	@Deprecated(since = "6.1")
	@Override
	public Object applyBeanPostProcessorsBeforeInitialization(Object existingBean, String beanName)
			throws BeansException {

		Object result = existingBean;
		//获取我们容器中的所有的bean的后置处理器
		for (BeanPostProcessor processor : getBeanPostProcessors()) {
			//挨个调用我们的bean的后置处理器的postProcessBeforeInitialization
			Object current = processor.postProcessBeforeInitialization(result, beanName);
			//若只有有一个返回null 那么直接返回原始的
			if (current == null) {
				return result;
			}
			result = current;
		}
		return result;
	}

	@Deprecated(since = "6.1")
	@Override
	public Object applyBeanPostProcessorsAfterInitialization(Object existingBean, String beanName)
			throws BeansException {

		Object result = existingBean;
		//获取我们容器中的所有的bean的后置处理器
		for (BeanPostProcessor processor : getBeanPostProcessors()) {
			/**
			 * 在这里是后置处理器的【第九次调用】 aop和事务都会在这里生存代理对象
			 *
			 * 【很重要】
			 * 我们AOP @EnableAspectJAutoProxy 为我们容器中导入了 AnnotationAwareAspectJAutoProxyCreator
			 * 我们事务注解@EnableTransactionManagement 为我们的容器导入了 InfrastructureAdvisorAutoProxyCreator
			 * 都是实现了我们的 BeanPostProcessor接口,InstantiationAwareBeanPostProcessor,
			 * 在这里实现的是BeanPostProcessor接口的postProcessAfterInitialization来生成我们的代理对象
			 */
			Object current = processor.postProcessAfterInitialization(result, beanName);
			//若只有有一个返回null 那么直接返回原始的
			if (current == null) {
				return result;
			}
			result = current;
		}
		return result;
	}

	@Override
	public void destroyBean(Object existingBean) {
		new DisposableBeanAdapter(existingBean, getBeanPostProcessorCache().destructionAware).destroy();
	}


	//-------------------------------------------------------------------------
	// Delegate methods for resolving injection points
	//-------------------------------------------------------------------------

	@Override
	public Object resolveBeanByName(String name, DependencyDescriptor descriptor) {
		InjectionPoint previousInjectionPoint = ConstructorResolver.setCurrentInjectionPoint(descriptor);
		try {
			return getBean(name, descriptor.getDependencyType());
		}
		finally {
			ConstructorResolver.setCurrentInjectionPoint(previousInjectionPoint);
		}
	}

	@Override
	public @Nullable Object resolveDependency(DependencyDescriptor descriptor, @Nullable String requestingBeanName) throws BeansException {
		return resolveDependency(descriptor, requestingBeanName, null, null);
	}


	//---------------------------------------------------------------------
	// Implementation of relevant AbstractBeanFactory template methods
	//---------------------------------------------------------------------

	/**
	 * Central method of this class: creates a bean instance,
	 * populates the bean instance, applies post-processors, etc.
	 * @see #doCreateBean
	 */
	@Override
	protected Object createBean(String beanName, RootBeanDefinition mbd, @Nullable Object @Nullable [] args)
			throws BeanCreationException {

		if (logger.isTraceEnabled()) {
			logger.trace("Creating instance of bean '" + beanName + "'");
		}
		RootBeanDefinition mbdToUse = mbd;

		// Make sure bean class is actually resolved at this point, and
		// clone the bean definition in case of a dynamically resolved Class
		// which cannot be stored in the shared merged bean definition.
		// 确保此时的 bean 已经被解析了
		// 如果获取的class 属性不为null，则克隆该 BeanDefinition
		// 主要是因为该动态解析的 class 无法保存到到共享的 BeanDefinition
		Class<?> resolvedClass = resolveBeanClass(mbd, beanName);
		if (resolvedClass != null && !mbd.hasBeanClass() && mbd.getBeanClassName() != null) {
			mbdToUse = new RootBeanDefinition(mbd);
			mbdToUse.setBeanClass(resolvedClass);
			// Prepare method overrides. 验证及准备覆盖的方法
			try {
				mbdToUse.prepareMethodOverrides();
			}
			catch (BeanDefinitionValidationException ex) {
				throw new BeanDefinitionStoreException(mbdToUse.getResourceDescription(),
						beanName, "Validation of method overrides failed", ex);
			}
		}

		try {
			// Give BeanPostProcessors a chance to return a proxy instead of the target bean instance.
			/**
			 * 通过bean的后置处理器来进行后置处理生成代理对象,一般情况下在此处不会生成代理对象
			 * 为什么不能生成代理对象,不管是我们的jdk代理还是cglib代理都不会在此处进行代理，因为我们的
			 * 真实的对象没有生成,所以在这里不会生成代理对象，那么在这一步是我们aop和事务的关键，因为在这里
			 * 解析我们的aop切面信息进行缓存
			 */
			Object bean = resolveBeforeInstantiation(beanName, mbdToUse);
			if (bean != null) {
				return bean;
			}
		}
		catch (Throwable ex) {
			throw new BeanCreationException(mbdToUse.getResourceDescription(), beanName,
					"BeanPostProcessor before instantiation of bean failed", ex);
		}

		try {
			/**
			 * 该步骤是我们真正的创建我们的bean的实例对象的过程
			 */
			Object beanInstance = doCreateBean(beanName, mbdToUse, args);
			if (logger.isTraceEnabled()) {
				logger.trace("Finished creating instance of bean '" + beanName + "'");
			}
			return beanInstance;
		}
		catch (BeanCreationException | ImplicitlyAppearedSingletonException ex) {
			// A previously detected exception with proper bean creation context already,
			// or illegal singleton state to be communicated up to DefaultSingletonBeanRegistry.
			throw ex;
		}
		catch (Throwable ex) {
			throw new BeanCreationException(
					mbdToUse.getResourceDescription(), beanName, "Unexpected exception during bean creation", ex);
		}
	}


	/**
	 * 真的创建bean的逻辑,该方法是最复杂的包含了调用构造函数,给bean的属性赋值
	 * 调用bean的初始化操作以及 生成代理对象 都是在这里
	 * Actually create the specified bean. Pre-creation processing has already happened
	 * at this point, for example, checking {@code postProcessBeforeInstantiation} callbacks.
	 * <p>Differentiates between default bean instantiation, use of a
	 * factory method, and autowiring a constructor.
	 * @param beanName the name of the bean
	 * @param mbd the merged bean definition for the bean
	 * @param args explicit arguments to use for constructor or factory method invocation
	 * @return a new instance of the bean
	 * @throws BeanCreationException if the bean could not be created
	 * @see #instantiateBean
	 * @see #instantiateUsingFactoryMethod
	 * @see #autowireConstructor
	 */
	protected Object doCreateBean(String beanName, RootBeanDefinition mbd, Object @Nullable [] args)
			throws BeanCreationException {

		//BeanWrapper 是对 Bean 的包装，其接口中所定义的功能很简单包括设置获取被包装的对象，获取被包装 bean 的属性描述器
		BeanWrapper instanceWrapper = null;
		if (mbd.isSingleton()) {
			//从没有完成的FactoryBean中移除
			instanceWrapper = this.factoryBeanInstanceCache.remove(beanName);
		}
		if (instanceWrapper == null) {
			//使用合适的实例化策略来创建新的实例：工厂方法、构造函数自动注入、简单初始化 该方法很复杂也很重要
			instanceWrapper = createBeanInstance(beanName, mbd, args);
		}
		//从beanWrapper中获取我们的早期对象
		Object bean = instanceWrapper.getWrappedInstance();
		Class<?> beanType = instanceWrapper.getWrappedClass();
		if (beanType != NullBean.class) {
			mbd.resolvedTargetType = beanType;
		}

		// Allow post-processors to modify the merged bean definition.
		synchronized (mbd.postProcessingLock) {
			if (!mbd.postProcessed) {
				try {
					//进行后置处理 @AutoWired的注解的预解析
					applyMergedBeanDefinitionPostProcessors(mbd, beanType, beanName);
				}
				catch (Throwable ex) {
					throw new BeanCreationException(mbd.getResourceDescription(), beanName,
							"Post-processing of merged bean definition failed", ex);
				}
				mbd.markAsPostProcessed();
			}
		}

		/**
		 * 该对象进行判断是否能够暴露早期对象的条件
		 * 单实例
		 * this.allowCircularReferences 默认为true
		 * isSingletonCurrentlyInCreation(表示当前的bean对象正在创建singletonsCurrentlyInCreation包含当前正在创建的bean)
		 */
		// Eagerly cache singletons to be able to resolve circular references
		// even when triggered by lifecycle interfaces like BeanFactoryAware.
		boolean earlySingletonExposure = (mbd.isSingleton() && this.allowCircularReferences &&
				isSingletonCurrentlyInCreation(beanName));
		//上述条件满足，允许中期暴露对象
		if (earlySingletonExposure) {
			if (logger.isTraceEnabled()) {
				logger.trace("Eagerly caching bean '" + beanName +
						"' to allow for resolving potential circular references");
			}
			//把我们的早期对象包装成一个singletonFactory对象 该对象提供了一个getObject方法,该方法内部调用getEarlyBeanReference方法
			addSingletonFactory(beanName, () -> getEarlyBeanReference(beanName, mbd, bean));
		}

		// Initialize the bean instance.
		Object exposedObject = bean;
		try {
			//给我们的属性进行赋值(调用set方法进行赋值) 可能存在依赖于其他bean的属性，则会递归初始依赖bean
			populateBean(beanName, mbd, instanceWrapper);
			//进行对象初始化操作(在这里可能生成代理对象) 比如init-method
			exposedObject = initializeBean(beanName, exposedObject, mbd);
		}
		catch (Throwable ex) {
			if (ex instanceof BeanCreationException bce && beanName.equals(bce.getBeanName())) {
				throw bce;
			}
			else {
				throw new BeanCreationException(mbd.getResourceDescription(), beanName, ex.getMessage(), ex);
			}
		}
		//允许早期对象的引用
		if (earlySingletonExposure) {
			/**
			 * 去缓存中获取到我们的对象 由于传递的allowEarlyReferce 是false 要求只能在一级二级缓存中去获取
			 * 正常普通的bean(不存在循环依赖的bean) 创建的过程中，压根不会把三级缓存提升到二级缓存中
			 */
			Object earlySingletonReference = getSingleton(beanName, false);
			//能够获取到
			if (earlySingletonReference != null) {
				//经过后置处理的bean和早期的bean引用还相等的话(表示当前的bean没有被代理过)
				if (exposedObject == bean) {
					exposedObject = earlySingletonReference;
				}
				//处理依赖的bean
				else if (!this.allowRawInjectionDespiteWrapping && hasDependentBean(beanName)) {
					String[] dependentBeans = getDependentBeans(beanName);
					Set<String> actualDependentBeans = CollectionUtils.newLinkedHashSet(dependentBeans.length);
					for (String dependentBean : dependentBeans) {
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
								"'getBeanNamesForType' with the 'allowEagerInit' flag turned off, for example.");
					}
				}
			}
		}

		// Register bean as disposable.
		try {
			//注册销毁的bean的销毁接口
			registerDisposableBeanIfNecessary(beanName, bean, mbd);
		}
		catch (BeanDefinitionValidationException ex) {
			throw new BeanCreationException(
					mbd.getResourceDescription(), beanName, "Invalid destruction signature", ex);
		}

		return exposedObject;
	}

	@Override
	protected @Nullable Class<?> predictBeanType(String beanName, RootBeanDefinition mbd, Class<?>... typesToMatch) {
		Class<?> targetType = determineTargetType(beanName, mbd, typesToMatch);
		// Apply SmartInstantiationAwareBeanPostProcessors to predict the
		// eventual type after a before-instantiation shortcut.
		if (targetType != null && !mbd.isSynthetic() && hasInstantiationAwareBeanPostProcessors()) {
			boolean matchingOnlyFactoryBean = (typesToMatch.length == 1 && typesToMatch[0] == FactoryBean.class);
			for (SmartInstantiationAwareBeanPostProcessor bp : getBeanPostProcessorCache().smartInstantiationAware) {
				Class<?> predicted = bp.predictBeanType(targetType, beanName);
				if (predicted != null &&
						(!matchingOnlyFactoryBean || FactoryBean.class.isAssignableFrom(predicted))) {
					return predicted;
				}
			}
		}
		return targetType;
	}

	/**
	 * Determine the target type for the given bean definition.
	 * @param beanName the name of the bean (for error handling purposes)
	 * @param mbd the merged bean definition for the bean
	 * @param typesToMatch the types to match in case of internal type matching purposes
	 * (also signals that the returned {@code Class} will never be exposed to application code)
	 * @return the type for the bean if determinable, or {@code null} otherwise
	 */
	protected @Nullable Class<?> determineTargetType(String beanName, RootBeanDefinition mbd, Class<?>... typesToMatch) {
		Class<?> targetType = mbd.getTargetType();
		if (targetType == null) {
			if (mbd.getFactoryMethodName() != null) {
				targetType = getTypeForFactoryMethod(beanName, mbd, typesToMatch);
			}
			else {
				targetType = resolveBeanClass(mbd, beanName, typesToMatch);
				if (mbd.hasBeanClass()) {
					targetType = getInstantiationStrategy().getActualBeanClass(mbd, beanName, this);
				}
			}
			if (ObjectUtils.isEmpty(typesToMatch) || getTempClassLoader() == null) {
				mbd.resolvedTargetType = targetType;
			}
		}
		return targetType;
	}

	/**
	 * Determine the target type for the given bean definition which is based on
	 * a factory method. Only called if there is no singleton instance registered
	 * for the target bean already.
	 * <p>This implementation determines the type matching {@link #createBean}'s
	 * different creation strategies. As far as possible, we'll perform static
	 * type checking to avoid creation of the target bean.
	 * @param beanName the name of the bean (for error handling purposes)
	 * @param mbd the merged bean definition for the bean
	 * @param typesToMatch the types to match in case of internal type matching purposes
	 * (also signals that the returned {@code Class} will never be exposed to application code)
	 * @return the type for the bean if determinable, or {@code null} otherwise
	 * @see #createBean
	 */
	protected @Nullable Class<?> getTypeForFactoryMethod(String beanName, RootBeanDefinition mbd, Class<?>... typesToMatch) {
		ResolvableType cachedReturnType = mbd.factoryMethodReturnType;
		if (cachedReturnType != null) {
			return cachedReturnType.resolve();
		}

		Class<?> commonType = null;
		Method uniqueCandidate = mbd.factoryMethodToIntrospect;

		if (uniqueCandidate == null) {
			Class<?> factoryClass;
			boolean isStatic = true;

			String factoryBeanName = mbd.getFactoryBeanName();
			if (factoryBeanName != null) {
				if (factoryBeanName.equals(beanName)) {
					throw new BeanDefinitionStoreException(mbd.getResourceDescription(), beanName,
							"factory-bean reference points back to the same bean definition");
				}
				// Check declared factory method return type on factory class.
				factoryClass = getType(factoryBeanName);
				isStatic = false;
			}
			else {
				// Check declared factory method return type on bean class.
				factoryClass = resolveBeanClass(mbd, beanName, typesToMatch);
			}

			if (factoryClass == null) {
				return null;
			}
			factoryClass = ClassUtils.getUserClass(factoryClass);

			// If all factory methods have the same return type, return that type.
			// Can't clearly figure out exact method due to type converting / autowiring!
			int minNrOfArgs =
					(mbd.hasConstructorArgumentValues() ? mbd.getConstructorArgumentValues().getArgumentCount() : 0);
			Method[] candidates = this.factoryMethodCandidateCache.computeIfAbsent(factoryClass,
					clazz -> ReflectionUtils.getUniqueDeclaredMethods(clazz, ReflectionUtils.USER_DECLARED_METHODS));

			for (Method candidate : candidates) {
				if (Modifier.isStatic(candidate.getModifiers()) == isStatic && mbd.isFactoryMethod(candidate) &&
						candidate.getParameterCount() >= minNrOfArgs) {
					// Declared type variables to inspect?
					if (candidate.getTypeParameters().length > 0) {
						try {
							// Fully resolve parameter names and argument values.
							ConstructorArgumentValues cav = mbd.getConstructorArgumentValues();
							Class<?>[] paramTypes = candidate.getParameterTypes();
							String[] paramNames = null;
							if (cav.containsNamedArgument()) {
								ParameterNameDiscoverer pnd = getParameterNameDiscoverer();
								if (pnd != null) {
									paramNames = pnd.getParameterNames(candidate);
								}
							}
							Set<ConstructorArgumentValues.ValueHolder> usedValueHolders = CollectionUtils.newHashSet(paramTypes.length);
							Object[] args = new Object[paramTypes.length];
							for (int i = 0; i < args.length; i++) {
								ConstructorArgumentValues.ValueHolder valueHolder = cav.getArgumentValue(
										i, paramTypes[i], (paramNames != null ? paramNames[i] : null), usedValueHolders);
								if (valueHolder == null) {
									valueHolder = cav.getGenericArgumentValue(null, null, usedValueHolders);
								}
								if (valueHolder != null) {
									args[i] = valueHolder.getValue();
									usedValueHolders.add(valueHolder);
								}
							}
							Class<?> returnType = AutowireUtils.resolveReturnTypeForFactoryMethod(
									candidate, args, getBeanClassLoader());
							uniqueCandidate = (commonType == null && returnType == candidate.getReturnType() ?
									candidate : null);
							commonType = ClassUtils.determineCommonAncestor(returnType, commonType);
							if (commonType == null) {
								// Ambiguous return types found: return null to indicate "not determinable".
								return null;
							}
						}
						catch (Throwable ex) {
							if (logger.isDebugEnabled()) {
								logger.debug("Failed to resolve generic return type for factory method: " + ex);
							}
						}
					}
					else {
						uniqueCandidate = (commonType == null ? candidate : null);
						commonType = ClassUtils.determineCommonAncestor(candidate.getReturnType(), commonType);
						if (commonType == null) {
							// Ambiguous return types found: return null to indicate "not determinable".
							return null;
						}
					}
				}
			}

			mbd.factoryMethodToIntrospect = uniqueCandidate;
			if (commonType == null) {
				return null;
			}
		}

		// Common return type found: all factory methods return same type. For a non-parameterized
		// unique candidate, cache the full type declaration context of the target factory method.
		try {
			cachedReturnType = (uniqueCandidate != null ?
					ResolvableType.forMethodReturnType(uniqueCandidate) : ResolvableType.forClass(commonType));
			mbd.factoryMethodReturnType = cachedReturnType;
			return cachedReturnType.resolve();
		}
		catch (LinkageError err) {
			// For example, a NoClassDefFoundError for a generic method return type
			if (logger.isDebugEnabled()) {
				logger.debug("Failed to resolve type for factory method of bean '" + beanName + "': " +
						(uniqueCandidate != null ? uniqueCandidate : commonType), err);
			}
			return null;
		}
	}

	/**
	 * This implementation attempts to query the FactoryBean's generic parameter metadata
	 * if present to determine the object type. If not present, i.e. the FactoryBean is
	 * declared as a raw type, it checks the FactoryBean's {@code getObjectType} method
	 * on a plain instance of the FactoryBean, without bean properties applied yet.
	 * If this doesn't return a type yet and {@code allowInit} is {@code true}, full
	 * creation of the FactoryBean is attempted as fallback (through delegation to the
	 * superclass implementation).
	 * <p>The shortcut check for a FactoryBean is only applied in case of a singleton
	 * FactoryBean. If the FactoryBean instance itself is not kept as singleton,
	 * it will be fully created to check the type of its exposed object.
	 */
	@Override
	protected ResolvableType getTypeForFactoryBean(String beanName, RootBeanDefinition mbd, boolean allowInit) {
		ResolvableType result;

		// Check if the bean definition itself has defined the type with an attribute
		try {
			result = getTypeForFactoryBeanFromAttributes(mbd);
			if (result != ResolvableType.NONE) {
				return result;
			}
		}
		catch (IllegalArgumentException ex) {
			throw new BeanDefinitionStoreException(mbd.getResourceDescription(), beanName,
					String.valueOf(ex.getMessage()));
		}

		// For instance supplied beans, try the target type and bean class immediately
		if (mbd.getInstanceSupplier() != null) {
			result = getFactoryBeanGeneric(mbd.targetType);
			if (result.resolve() != null) {
				return result;
			}
			result = getFactoryBeanGeneric(mbd.hasBeanClass() ? ResolvableType.forClass(mbd.getBeanClass()) : null);
			if (result.resolve() != null) {
				return result;
			}
		}

		// Consider factory methods
		String factoryBeanName = mbd.getFactoryBeanName();
		String factoryMethodName = mbd.getFactoryMethodName();

		// Scan the factory bean methods
		if (factoryBeanName != null) {
			if (factoryMethodName != null) {
				// Try to obtain the FactoryBean's object type from its factory method
				// declaration without instantiating the containing bean at all.
				BeanDefinition factoryBeanDefinition = getBeanDefinition(factoryBeanName);
				Class<?> factoryBeanClass;
				if (factoryBeanDefinition instanceof AbstractBeanDefinition abstractBeanDefinition &&
						abstractBeanDefinition.hasBeanClass()) {
					factoryBeanClass = abstractBeanDefinition.getBeanClass();
				}
				else {
					RootBeanDefinition fbmbd = getMergedBeanDefinition(factoryBeanName, factoryBeanDefinition);
					factoryBeanClass = determineTargetType(factoryBeanName, fbmbd);
				}
				if (factoryBeanClass != null) {
					result = getTypeForFactoryBeanFromMethod(factoryBeanClass, factoryMethodName);
					if (result.resolve() != null) {
						return result;
					}
				}
			}
			// If not resolvable above and the referenced factory bean doesn't exist yet,
			// exit here - we don't want to force the creation of another bean just to
			// obtain a FactoryBean's object type...
			if (!isBeanEligibleForMetadataCaching(factoryBeanName)) {
				return ResolvableType.NONE;
			}
		}

		// If we're allowed, we can create the factory bean and call getObjectType() early
		if (allowInit) {
			FactoryBean<?> factoryBean = (mbd.isSingleton() ?
					getSingletonFactoryBeanForTypeCheck(beanName, mbd) :
					getNonSingletonFactoryBeanForTypeCheck(beanName, mbd));
			if (factoryBean != null) {
				// Try to obtain the FactoryBean's object type from this early stage of the instance.
				Class<?> type = getTypeForFactoryBean(factoryBean);
				if (type != null) {
					return ResolvableType.forClass(type);
				}
				// No type found for shortcut FactoryBean instance:
				// fall back to full creation of the FactoryBean instance.
				return super.getTypeForFactoryBean(beanName, mbd, true);
			}
		}

		if (factoryBeanName == null && mbd.hasBeanClass() && factoryMethodName != null) {
			// No early bean instantiation possible: determine FactoryBean's type from
			// static factory method signature or from class inheritance hierarchy...
			return getTypeForFactoryBeanFromMethod(mbd.getBeanClass(), factoryMethodName);
		}

		// For regular beans, try the target type and bean class as fallback
		if (mbd.getInstanceSupplier() == null) {
			result = getFactoryBeanGeneric(mbd.targetType);
			if (result.resolve() != null) {
				return result;
			}
			result = getFactoryBeanGeneric(mbd.hasBeanClass() ? ResolvableType.forClass(mbd.getBeanClass()) : null);
			if (result.resolve() != null) {
				return result;
			}
		}

		// FactoryBean type not resolvable
		return ResolvableType.NONE;
	}

	/**
	 * Introspect the factory method signatures on the given bean class,
	 * trying to find a common {@code FactoryBean} object type declared there.
	 * @param beanClass the bean class to find the factory method on
	 * @param factoryMethodName the name of the factory method
	 * @return the common {@code FactoryBean} object type, or {@code null} if none
	 */
	private ResolvableType getTypeForFactoryBeanFromMethod(Class<?> beanClass, String factoryMethodName) {
		// CGLIB subclass methods hide generic parameters; look at the original user class.
		Class<?> factoryBeanClass = ClassUtils.getUserClass(beanClass);
		FactoryBeanMethodTypeFinder finder = new FactoryBeanMethodTypeFinder(factoryMethodName);
		ReflectionUtils.doWithMethods(factoryBeanClass, finder, ReflectionUtils.USER_DECLARED_METHODS);
		return finder.getResult();
	}

	/**
	 * 这个方法很重要 网上大多数人都没有说清楚，在解决循环依赖的过程中把三级对象的getObject()的时候，会触发getEarlyBeanReference
	 * 在该方法中，我们可以通过SmartInstantiationAwareBeanPostProcessor的后置处理器来修改我们早期对象的属性,
	 * 但是spring内部的SmartInstantiationAwareBeanPostProcessor.getEarlyBeanReference没有做任何的处理
	 * 正符合spring开放式接口规范 留个我们扩展
	 * @param beanName the name of the bean (for error handling purposes)
	 * @param mbd the merged bean definition for the bean
	 * @param bean the raw bean instance
	 * @return the object to expose as bean reference
	 */
	protected Object getEarlyBeanReference(String beanName, RootBeanDefinition mbd, Object bean) {
		Object exposedObject = bean;
		//判读我们容器中是否有InstantiationAwareBeanPostProcessors类型的后置处理器
		if (!mbd.isSynthetic() && hasInstantiationAwareBeanPostProcessors()) {
			//获取我们所有的后置处理器
			for (SmartInstantiationAwareBeanPostProcessor bp : getBeanPostProcessorCache().smartInstantiationAware) {
				//进行强制转换
				//挨个调用SmartInstantiationAwareBeanPostProcessor的getEarlyBeanReference
				exposedObject = bp.getEarlyBeanReference(exposedObject, beanName);
			}
		}
		return exposedObject;
	}


	//---------------------------------------------------------------------
	// Implementation methods
	//---------------------------------------------------------------------

	/**
	 * Obtain a "shortcut" singleton FactoryBean instance to use for a
	 * {@code getObjectType()} call, without full initialization of the FactoryBean.
	 * @param beanName the name of the bean
	 * @param mbd the bean definition for the bean
	 * @return the FactoryBean instance, or {@code null} to indicate
	 * that we couldn't obtain a shortcut FactoryBean instance
	 */
	private @Nullable FactoryBean<?> getSingletonFactoryBeanForTypeCheck(String beanName, RootBeanDefinition mbd) {
		this.singletonLock.lock();
		try {
			BeanWrapper bw = this.factoryBeanInstanceCache.get(beanName);
			if (bw != null) {
				return (FactoryBean<?>) bw.getWrappedInstance();
			}
			Object beanInstance = getSingleton(beanName, false);
			if (beanInstance instanceof FactoryBean<?> factoryBean) {
				return factoryBean;
			}
			if (isSingletonCurrentlyInCreation(beanName) ||
					(mbd.getFactoryBeanName() != null && isSingletonCurrentlyInCreation(mbd.getFactoryBeanName()))) {
				return null;
			}

			Object instance;
			try {
				// Mark this bean as currently in creation, even if just partially.
				beforeSingletonCreation(beanName);
				// Give BeanPostProcessors a chance to return a proxy instead of the target bean instance.
				instance = resolveBeforeInstantiation(beanName, mbd);
				if (instance == null) {
					bw = createBeanInstance(beanName, mbd, null);
					instance = bw.getWrappedInstance();
					this.factoryBeanInstanceCache.put(beanName, bw);
				}
			}
			catch (UnsatisfiedDependencyException ex) {
				// Don't swallow, probably misconfiguration...
				throw ex;
			}
			catch (BeanCreationException ex) {
				// Don't swallow a linkage error since it contains a full stacktrace on
				// first occurrence... and just a plain NoClassDefFoundError afterwards.
				if (ex.contains(LinkageError.class)) {
					throw ex;
				}
				// Instantiation failure, maybe too early...
				if (logger.isDebugEnabled()) {
					logger.debug("Bean creation exception on singleton FactoryBean type check: " + ex);
				}
				onSuppressedException(ex);
				return null;
			}
			finally {
				// Finished partial creation of this bean.
				afterSingletonCreation(beanName);
			}

			return getFactoryBean(beanName, instance);
		}
		finally {
			this.singletonLock.unlock();
		}
	}

	/**
	 * Obtain a "shortcut" non-singleton FactoryBean instance to use for a
	 * {@code getObjectType()} call, without full initialization of the FactoryBean.
	 * @param beanName the name of the bean
	 * @param mbd the bean definition for the bean
	 * @return the FactoryBean instance, or {@code null} to indicate
	 * that we couldn't obtain a shortcut FactoryBean instance
	 */
	private @Nullable FactoryBean<?> getNonSingletonFactoryBeanForTypeCheck(String beanName, RootBeanDefinition mbd) {
		if (isPrototypeCurrentlyInCreation(beanName)) {
			return null;
		}

		Object instance;
		try {
			// Mark this bean as currently in creation, even if just partially.
			beforePrototypeCreation(beanName);
			// Give BeanPostProcessors a chance to return a proxy instead of the target bean instance.
			instance = resolveBeforeInstantiation(beanName, mbd);
			if (instance == null) {
				BeanWrapper bw = createBeanInstance(beanName, mbd, null);
				instance = bw.getWrappedInstance();
			}
		}
		catch (UnsatisfiedDependencyException ex) {
			// Don't swallow, probably misconfiguration...
			throw ex;
		}
		catch (BeanCreationException ex) {
			// Instantiation failure, maybe too early...
			if (logger.isDebugEnabled()) {
				logger.debug("Bean creation exception on non-singleton FactoryBean type check: " + ex);
			}
			onSuppressedException(ex);
			return null;
		}
		finally {
			// Finished partial creation of this bean.
			afterPrototypeCreation(beanName);
		}

		return getFactoryBean(beanName, instance);
	}

	/**
	 * Apply MergedBeanDefinitionPostProcessors to the specified bean definition,
	 * invoking their {@code postProcessMergedBeanDefinition} methods.
	 * @param mbd the merged bean definition for the bean
	 * @param beanType the actual type of the managed bean instance
	 * @param beanName the name of the bean
	 * @see MergedBeanDefinitionPostProcessor#postProcessMergedBeanDefinition
	 */
	protected void applyMergedBeanDefinitionPostProcessors(RootBeanDefinition mbd, Class<?> beanType, String beanName) {
		for (MergedBeanDefinitionPostProcessor processor : getBeanPostProcessorCache().mergedDefinition) {
			processor.postProcessMergedBeanDefinition(mbd, beanType, beanName);
		}
	}

	/**
	 * Apply before-instantiation post-processors, resolving whether there is a
	 * before-instantiation shortcut for the specified bean.
	 * @param beanName the name of the bean
	 * @param mbd the bean definition for the bean
	 * @return the shortcut-determined bean instance, or {@code null} if none
	 */
	@SuppressWarnings("deprecation")
	protected @Nullable Object resolveBeforeInstantiation(String beanName, RootBeanDefinition mbd) {
		Object bean = null;
		//获取我们容器中的所有的bean的后置处理器
		if (!Boolean.FALSE.equals(mbd.beforeInstantiationResolved)) {
			// Make sure bean class is actually resolved at this point.

			if (!mbd.isSynthetic() && hasInstantiationAwareBeanPostProcessors()) {
				//获取当前bean 的class对象
				Class<?> targetType = determineTargetType(beanName, mbd);
				if (targetType != null) {
					/**
					 * 后置处理器的【第一次】调用 总共有九处调用 事务在这里不会被调用，aop的才会被调用
					 * 为啥aop在这里调用了，因为在此处需要解析出对应的切面报错到缓存中
					 */
					bean = applyBeanPostProcessorsBeforeInstantiation(targetType, beanName);
					//若InstantiationAwareBeanPostProcessors后置处理器的postProcessBeforeInstantiation返回不为null
					//说明生成了代理对象那么我们就调用
					if (bean != null) {
						/**
						 * 后置处理器的第二处调用，该后置处理器若被调用的话，那么第一处的处理器肯定返回的不是null
						 * InstantiationAwareBeanPostProcessors后置处理器postProcessAfterInitialization
						 */
						bean = applyBeanPostProcessorsAfterInitialization(bean, beanName);
					}
				}
			}
			mbd.beforeInstantiationResolved = (bean != null);
		}
		return bean;
	}

	/**
	 * Apply InstantiationAwareBeanPostProcessors to the specified bean definition
	 * (by class and name), invoking their {@code postProcessBeforeInstantiation} methods.
	 * <p>Any returned object will be used as the bean instead of actually instantiating
	 * the target bean. A {@code null} return value from the post-processor will
	 * result in the target bean being instantiated.
	 * @param beanClass the class of the bean to be instantiated
	 * @param beanName the name of the bean
	 * @return the bean object to use instead of a default instance of the target bean, or {@code null}
	 * @see InstantiationAwareBeanPostProcessor#postProcessBeforeInstantiation
	 */
	protected @Nullable Object applyBeanPostProcessorsBeforeInstantiation(Class<?> beanClass, String beanName) {
		/**
		 * 获取容器中的所有后置处理器
		 */
		for (InstantiationAwareBeanPostProcessor bp : getBeanPostProcessorCache().instantiationAware) {
			/**
			 * 【很重要】
			 * 我们AOP @EnableAspectJAutoProxy 为我们容器中导入了 AnnotationAwareAspectJAutoProxyCreator
			 * 我们事务注解@EnableTransactionManagement 为我们的容器导入了 InfrastructureAdvisorAutoProxyCreator
			 * 都是实现了我们的 BeanPostProcessor接口,InstantiationAwareBeanPostProcessor,
			 * 进行后置处理解析切面
			 */
			Object result = bp.postProcessBeforeInstantiation(beanClass, beanName);
			if (result != null) {
				return result;
			}
		}
		return null;
	}

	/**
	 * Create a new instance for the specified bean, using an appropriate instantiation strategy:
	 * factory method, constructor autowiring, or simple instantiation.
	 * @param beanName the name of the bean
	 * @param mbd the bean definition for the bean
	 * @param args explicit arguments to use for constructor or factory method invocation
	 * @return a BeanWrapper for the new instance
	 * @see #obtainFromSupplier
	 * @see #instantiateUsingFactoryMethod
	 * @see #autowireConstructor
	 * @see #instantiateBean
	 */
	protected BeanWrapper createBeanInstance(String beanName, RootBeanDefinition mbd, @Nullable Object @Nullable [] args) {
		// Make sure bean class is actually resolved at this point.
		//从bean定义中解析出当前bean的class对象
		Class<?> beanClass = resolveBeanClass(mbd, beanName);
		/**
		 * 检测类的访问权限。默认情况下，对于非 public 的类，是允许访问的。
		 * 若禁止访问，这里会抛出异常
		 */
		if (beanClass != null && !Modifier.isPublic(beanClass.getModifiers()) && !mbd.isNonPublicAccessAllowed()) {
			throw new BeanCreationException(mbd.getResourceDescription(), beanName,
					"Bean class isn't public, and non-public access not allowed: " + beanClass.getName());
		}
		/**
		 * 该方法是spring5.0 新增加的  如果存在 Supplier 回调，则使用给定的回调方法初始化策略
		 */
		if (args == null) {
			Supplier<?> instanceSupplier = mbd.getInstanceSupplier();
			if (instanceSupplier != null) {
				return obtainFromSupplier(instanceSupplier, beanName, mbd);
			}
		}
		/**
		 * 工厂方法,我们通过配置类来进行配置的话 采用的就是工厂方法,方法名称就是DujieDao就是我们工厂方法的名称
		 *  Bean
		 	public DujieDao DujieDao() {
		 		return new DujieDao(dujieDataSource());
		 	}
		 */
		if (mbd.getFactoryMethodName() != null) {
			return instantiateUsingFactoryMethod(beanName, mbd, args);
		}

		/**
		 * 当多次构建同一个 bean 时，可以使用此处的快捷路径，即无需再次推断应该使用哪种方式构造实例，
		 * 以提高效率。比如在多次构建同一个 prototype 类型的 bean 时，就可以走此处的捷径。
		 * 这里的 resolved 和 mbd.constructorArgumentsResolved 将会在 bean 第一次实例
		 * 化的过程中被设置，在后面的源码中会分析到，先继续往下看。
		 */
		//判断当前构造函数是否被解析过
		boolean resolved = false;
		//有没有必须进行依赖注入
		boolean autowireNecessary = false;
		/**
		 * 通过getBean传入进来的构造函数是否来指定需要推断构造函数
		 * 若传递进来的args不为空，那么就可以直接选出对应的构造函数
		 */
		if (args == null) {
			synchronized (mbd.constructorArgumentLock) {
				//判断我们的bean定义信息中的resolvedConstructorOrFactoryMethod(用来缓存我们的已经解析的构造函数或者工厂方法)
				if (mbd.resolvedConstructorOrFactoryMethod != null) {
					//修改已经解析过的构造函数的标志
					resolved = true;
					//修改标记为ture 标识构造函数或者工厂方法已经解析过
					autowireNecessary = mbd.constructorArgumentsResolved;
				}
			}
		}
		//若被解析过
		if (resolved) {
			if (autowireNecessary) {
				//通过有参的构造函数进行反射调用
				return autowireConstructor(beanName, mbd, null, null);
			}
			else {
				//调用无参数的构造函数进行创建对象
				return instantiateBean(beanName, mbd);
			}
		}

		/**
		 * 通过bean的后置处理器进行选举出合适的构造函数对象
		 */
		Constructor<?>[] ctors = determineConstructorsFromBeanPostProcessors(beanClass, beanName);
		/**
		 * 通过后置处理器解析出构造器对象不为null
		 * 获取bean定义中的注入模式是构造器注入
		 * bean定义信息ConstructorArgumentValues
		 * 获取通过getBean的方式传入的构造器函数参数类型不为null
		 */
		if (ctors != null || mbd.getResolvedAutowireMode() == AUTOWIRE_CONSTRUCTOR ||
				mbd.hasConstructorArgumentValues() || !ObjectUtils.isEmpty(args)) {
			//通过构造函数创建对象
			return autowireConstructor(beanName, mbd, ctors, args);
		}

		// Preferred constructors for default construction?
		ctors = mbd.getPreferredConstructors();
		if (ctors != null) {
			return autowireConstructor(beanName, mbd, ctors, null);
		}

		//使用无参数的构造函数调用创建对象
		return instantiateBean(beanName, mbd);
	}

	/**
	 * Obtain a bean instance from the given supplier.
	 * @param supplier the configured supplier
	 * @param beanName the corresponding bean name
	 * @return a BeanWrapper for the new instance
	 */
	private BeanWrapper obtainFromSupplier(Supplier<?> supplier, String beanName, RootBeanDefinition mbd) {
		String outerBean = this.currentlyCreatedBean.get();
		this.currentlyCreatedBean.set(beanName);
		Object instance;

		try {
			instance = obtainInstanceFromSupplier(supplier, beanName, mbd);
		}
		catch (Throwable ex) {
			if (ex instanceof BeansException beansException) {
				throw beansException;
			}
			throw new BeanCreationException(beanName, "Instantiation of supplied bean failed", ex);
		}
		finally {
			if (outerBean != null) {
				this.currentlyCreatedBean.set(outerBean);
			}
			else {
				this.currentlyCreatedBean.remove();
			}
		}

		if (instance == null) {
			instance = new NullBean();
		}
		BeanWrapper bw = new BeanWrapperImpl(instance);
		initBeanWrapper(bw);
		return bw;
	}

	/**
	 * Obtain a bean instance from the given supplier.
	 * @param supplier the configured supplier
	 * @param beanName the corresponding bean name
	 * @param mbd the bean definition for the bean
	 * @return the bean instance (possibly {@code null})
	 * @since 6.0.7
	 */
	protected @Nullable Object obtainInstanceFromSupplier(Supplier<?> supplier, String beanName, RootBeanDefinition mbd)
			throws Exception {

		if (supplier instanceof ThrowingSupplier<?> throwingSupplier) {
			return throwingSupplier.getWithException();
		}
		return supplier.get();
	}

	/**
	 * Overridden in order to implicitly register the currently created bean as
	 * dependent on further beans getting programmatically retrieved during a
	 * {@link Supplier} callback.
	 * @since 5.0
	 * @see #obtainFromSupplier
	 */
	@Override
	protected Object getObjectForBeanInstance(
			Object beanInstance, String name, String beanName, @Nullable RootBeanDefinition mbd) {

		String currentlyCreatedBean = this.currentlyCreatedBean.get();
		if (currentlyCreatedBean != null) {
			registerDependentBean(beanName, currentlyCreatedBean);
		}

		return super.getObjectForBeanInstance(beanInstance, name, beanName, mbd);
	}

	/**
	 * Determine candidate constructors to use for the given bean, checking all registered
	 * {@link SmartInstantiationAwareBeanPostProcessor SmartInstantiationAwareBeanPostProcessors}.
	 * @param beanClass the raw class of the bean
	 * @param beanName the name of the bean
	 * @return the candidate constructors, or {@code null} if none specified
	 * @throws org.springframework.beans.BeansException in case of errors
	 * @see org.springframework.beans.factory.config.SmartInstantiationAwareBeanPostProcessor#determineCandidateConstructors
	 */
	protected Constructor<?> @Nullable [] determineConstructorsFromBeanPostProcessors(@Nullable Class<?> beanClass, String beanName)
			throws BeansException {

		if (beanClass != null && hasInstantiationAwareBeanPostProcessors()) {
			for (SmartInstantiationAwareBeanPostProcessor bp : getBeanPostProcessorCache().smartInstantiationAware) {
				Constructor<?>[] ctors = bp.determineCandidateConstructors(beanClass, beanName);
				if (ctors != null) {
					return ctors;
				}
			}
		}
		return null;
	}

	/**
	 * Instantiate the given bean using its default constructor.
	 * @param beanName the name of the bean
	 * @param mbd the bean definition for the bean
	 * @return a BeanWrapper for the new instance
	 */
	protected BeanWrapper instantiateBean(String beanName, RootBeanDefinition mbd) {
		try {
			Object beanInstance = getInstantiationStrategy().instantiate(mbd, beanName, this);
			BeanWrapper bw = new BeanWrapperImpl(beanInstance);
			initBeanWrapper(bw);
			return bw;
		}
		catch (Throwable ex) {
			throw new BeanCreationException(mbd.getResourceDescription(), beanName, ex.getMessage(), ex);
		}
	}

	/**
	 * Instantiate the bean using a named factory method. The method may be static, if the
	 * mbd parameter specifies a class, rather than a factoryBean, or an instance variable
	 * on a factory object itself configured using Dependency Injection.
	 * @param beanName the name of the bean
	 * @param mbd the bean definition for the bean
	 * @param explicitArgs argument values passed in programmatically via the getBean method,
	 * or {@code null} if none (implying the use of constructor argument values from bean definition)
	 * @return a BeanWrapper for the new instance
	 * @see #getBean(String, Object[])
	 */
	protected BeanWrapper instantiateUsingFactoryMethod(
			String beanName, RootBeanDefinition mbd, @Nullable Object @Nullable [] explicitArgs) {

		return new ConstructorResolver(this).instantiateUsingFactoryMethod(beanName, mbd, explicitArgs);
	}

	/**
	 * "autowire constructor" (with constructor arguments by type) behavior.
	 * Also applied if explicit constructor argument values are specified,
	 * matching all remaining arguments with beans from the bean factory.
	 * <p>This corresponds to constructor injection: In this mode, a Spring
	 * bean factory is able to host components that expect constructor-based
	 * dependency resolution.
	 * @param beanName the name of the bean
	 * @param mbd the bean definition for the bean
	 * @param ctors the chosen candidate constructors
	 * @param explicitArgs argument values passed in programmatically via the getBean method,
	 * or {@code null} if none (implying the use of constructor argument values from bean definition)
	 * @return a BeanWrapper for the new instance
	 */
	protected BeanWrapper autowireConstructor(
			String beanName, RootBeanDefinition mbd, Constructor<?> @Nullable [] ctors, @Nullable Object @Nullable [] explicitArgs) {

		return new ConstructorResolver(this).autowireConstructor(beanName, mbd, ctors, explicitArgs);
	}

	/**
	 * 给我们的对象BeanWrapper赋值
	 * @param beanName bean的名称
	 * @param mbd bean的定义
	 * @param bw bean实例包装对象
	 */
	protected void populateBean(String beanName, RootBeanDefinition mbd, @Nullable BeanWrapper bw) {
		//若bw为null的话,则说明对象没有实例化
		if (bw == null) {
			//进入if 说明对象有属性，bw为空,不能为他设置属性，那就在下面就执行抛出异常
			if (mbd.hasPropertyValues()) {
				throw new BeanCreationException(
						mbd.getResourceDescription(), beanName, "Cannot apply property values to null instance");
			}
			else {
				// Skip property population phase for null instance.
				return;
			}
		}

		if (bw.getWrappedClass().isRecord()) {
			if (mbd.hasPropertyValues()) {
				throw new BeanCreationException(
						mbd.getResourceDescription(), beanName, "Cannot apply property values to a record");
			}
			else {
				// Skip property population phase for records since they are immutable.
				return;
			}
		}

		// Give any InstantiationAwareBeanPostProcessors the opportunity to modify the
		// state of the bean before properties are set. This can be used, for example,
		// to support styles of field injection.

		/**
		 * 在属性被填充前，给 InstantiationAwareBeanPostProcessor 类型的后置处理器一个修改
		 * bean 状态的机会。官方的解释是：让用户可以自定义属性注入。比如用户实现一
		 * 个 InstantiationAwareBeanPostProcessor 类型的后置处理器，并通过
		 * postProcessAfterInstantiation 方法向 bean 的成员变量注入自定义的信息。
		 * 当时我们发现系统中的的InstantiationAwareBeanPostProcessor.postProcessAfterInstantiationM没有进行任何处理，
		 * 若我们自己实现了这个接口 可以自定义处理.....spring 留给我们自己扩展接口的
		 * 特殊需求，直接使用配置中的信息注入即可。
		 */
		// 是否持有 InstantiationAwareBeanPostProcessor
		if (!mbd.isSynthetic() && hasInstantiationAwareBeanPostProcessors()) {
			//获取容器中的所有的BeanPostProcessor
			for (InstantiationAwareBeanPostProcessor bp : getBeanPostProcessorCache().instantiationAware) {
				//若存在后置处理器给我们属性赋值了，那么返回false 可以来修改我们的开关变量，就不会走下面的逻辑了
				if (!bp.postProcessAfterInstantiation(bw.getWrappedInstance(), beanName)) {
					// 返回值为是否继续填充 bean
					// postProcessAfterInstantiation：如果应该在 bean上面设置属性则返回 true，否则返回 false
					// 一般情况下，应该是返回true 。
					// 返回 false 的话，将会阻止在此 Bean 实例上调用任何后续的 InstantiationAwareBeanPostProcessor 实
					return;
				}
			}
		}
		//获取bean定义的属性
		PropertyValues pvs = (mbd.hasPropertyValues() ? mbd.getPropertyValues() : null);

		int resolvedAutowireMode = mbd.getResolvedAutowireMode();
		/**
		 * 判断我们的bean的属性注入模型
		 * AUTOWIRE_BY_NAME 根据名称注入
		 * AUTOWIRE_BY_TYPE 根据类型注入
		 */
		if (resolvedAutowireMode == AUTOWIRE_BY_NAME || resolvedAutowireMode == AUTOWIRE_BY_TYPE) {
			// 把PropertyValues封装成为MutablePropertyValues
			MutablePropertyValues newPvs = new MutablePropertyValues(pvs);
			// 根据bean的属性名称注入
			// Add property values based on autowire by name if applicable.
			if (resolvedAutowireMode == AUTOWIRE_BY_NAME) {
				autowireByName(beanName, mbd, bw, newPvs);
			}
			//根据bean的类型进行注入
			// Add property values based on autowire by type if applicable.
			if (resolvedAutowireMode == AUTOWIRE_BY_TYPE) {
				autowireByType(beanName, mbd, bw, newPvs);
			}
			//把处理过的 属性覆盖原来的
			pvs = newPvs;
		}
		/**
		 * 这里又是一种后置处理，用于在 Spring 填充属性到 bean 对象前，对属性的值进行相应的处理，
		 * 比如可以修改某些属性的值。这时注入到 bean 中的值就不是配置文件中的内容了，
		 * 而是经过后置处理器修改后的内容
		 */
		if (hasInstantiationAwareBeanPostProcessors()) {
			if (pvs == null) {
				pvs = mbd.getPropertyValues();
			}
			//获取所有的后置处理器
			for (InstantiationAwareBeanPostProcessor bp : getBeanPostProcessorCache().instantiationAware) {
				PropertyValues pvsToUse = bp.postProcessProperties(pvs, bw.getWrappedInstance(), beanName);
				if (pvsToUse == null) {
					return;
				}
				pvs = pvsToUse;
			}
		}
		// 判断是否检查依赖
		boolean needsDepCheck = (mbd.getDependencyCheck() != AbstractBeanDefinition.DEPENDENCY_CHECK_NONE);
		if (needsDepCheck) {
			PropertyDescriptor[] filteredPds = filterPropertyDescriptorsForDependencyCheck(bw, mbd.allowCaching);
			checkDependencies(beanName, mbd, filteredPds, pvs);
		}
		/**
		 * 其实，上面只是完成了所有注入属性的获取，将获取的属性封装在 PropertyValues 的实例对象 pvs 中，
		 * 并没有应用到已经实例化的 bean 中。而 #applyPropertyValues(String beanName, BeanDefinition mbd, BeanWrapper bw, PropertyValues pvs) 方法，
		 * 则是完成这一步骤的
		 */
		if (pvs != null) {
			applyPropertyValues(beanName, mbd, bw, pvs);
		}
	}

	/**
	 * Fill in any missing property values with references to
	 * other beans in this factory if autowire is set to "byName".
	 * @param beanName the name of the bean we're wiring up.
	 * Useful for debugging messages; not used functionally.
	 * @param mbd bean definition to update through autowiring
	 * @param bw the BeanWrapper from which we can obtain information about the bean
	 * @param pvs the PropertyValues to register wired objects with
	 */
	protected void autowireByName(
			String beanName, AbstractBeanDefinition mbd, BeanWrapper bw, MutablePropertyValues pvs) {
		//从我们的bean定义中解析出我们的所有属性对象
		String[] propertyNames = unsatisfiedNonSimpleProperties(mbd, bw);
		//循环我们当前bean的属性
		for (String propertyName : propertyNames) {
			//判断该属性名称是不是引用对象
			if (containsBean(propertyName)) {
				//显示的调用getBean所有属性的名称bean显示调用BeanFactory
				Object bean = getBean(propertyName);
				//把我们依赖的属性添加到pvs中
				pvs.add(propertyName, bean);
				//注册当前bean和属性依赖bean的依赖关系
				registerDependentBean(propertyName, beanName);
				if (logger.isTraceEnabled()) {
					logger.trace("Added autowiring by name from bean name '" + beanName +
							"' via property '" + propertyName + "' to bean named '" + propertyName + "'");
				}
			}
			else {
				if (logger.isTraceEnabled()) {
					logger.trace("Not autowiring property '" + propertyName + "' of bean '" + beanName +
							"' by name: no matching bean found");
				}
			}
		}
	}

	/**
	 * Abstract method defining "autowire by type" (bean properties by type) behavior.
	 * <p>This is like PicoContainer default, in which there must be exactly one bean
	 * of the property type in the bean factory. This makes bean factories simple to
	 * configure for small namespaces, but doesn't work as well as standard Spring
	 * behavior for bigger applications.
	 * @param beanName the name of the bean to autowire by type
	 * @param mbd the merged bean definition to update through autowiring
	 * @param bw the BeanWrapper from which we can obtain information about the bean
	 * @param pvs the PropertyValues to register wired objects with
	 */
	protected void autowireByType(
			String beanName, AbstractBeanDefinition mbd, BeanWrapper bw, MutablePropertyValues pvs) {
		// 获取 TypeConverter 实例
		// 使用自定义的 TypeConverter，用于取代默认的 PropertyEditor 机制
		TypeConverter converter = getCustomTypeConverter();
		//没有获取到  把bw赋值给转换器(BeanWrapper实现了 TypeConverter接口 )
		if (converter == null) {
			converter = bw;
		}

		String[] propertyNames = unsatisfiedNonSimpleProperties(mbd, bw);
		Set<String> autowiredBeanNames = new LinkedHashSet<>(propertyNames.length * 2);
		/**
		 *   spring认为的简单属性
		 *   1. CharSequence 接口的实现类，比如 String
		 *   2. Enum
		 *   3. Date
		 *   4. URI/URL
		 *   5. Number 的继承类，比如 Integer/Long
		 *   6. byte/short/int... 等基本类型
		 *   7. Locale
		 *   8. 以上所有类型的数组形式，比如 String[]、Date[]、int[] 等等
		 * 不包含在当前bean的配置文件的中属性 !pvs.contains(pd.getName()
		 **/
		//循环属性,循环我们的依赖属性
		for (String propertyName : propertyNames) {
			try {
				//获取 PropertyDescriptor 实例
				PropertyDescriptor pd = bw.getPropertyDescriptor(propertyName);
				// Don't try autowiring by type for type Object: never makes sense,
				// even if it technically is an unsatisfied, non-simple property.
				//判断当前正在创建的bean中所依赖的属性描述的类型不是为object类型的
				if (Object.class != pd.getPropertyType()) {
					//获取当前正常创建备案所依赖的属性的setter方法对象
					MethodParameter methodParam = BeanUtils.getWriteMethodParameter(pd);
					// Do not allow eager init for type matching in case of a prioritized post-processor.
					boolean eager = !(bw.getWrappedInstance() instanceof PriorityOrdered);
					//创建自动注入的依赖描述符创建
					DependencyDescriptor desc = new AutowireByTypeDependencyDescriptor(methodParam, eager);
					// 解析指定 beanName 的属性所匹配的值，并把解析到的属性名称存储在 autowiredBeanNames 中
					// 当属性存在过个封装 bean 时将会找到所有匹配的 bean 并将其注入
					Object autowiredArgument = resolveDependency(desc, beanName, autowiredBeanNames, converter);
					//若解析出来的不为空autowiredArgument  把属性名称和对应的值进绑定
					if (autowiredArgument != null) {
						pvs.add(propertyName, autowiredArgument);
					}
					for (String autowiredBeanName : autowiredBeanNames) {
						//注册依赖
						registerDependentBean(autowiredBeanName, beanName);
						if (logger.isTraceEnabled()) {
							logger.trace("Autowiring by type from bean name '" + beanName + "' via property '" +
									propertyName + "' to bean named '" + autowiredBeanName + "'");
						}
					}
					autowiredBeanNames.clear();
				}
			}
			catch (BeansException ex) {
				throw new UnsatisfiedDependencyException(mbd.getResourceDescription(), beanName, propertyName, ex);
			}
		}
	}


	/**
	 * Return an array of non-simple bean properties that are unsatisfied.
	 * These are probably unsatisfied references to other beans in the
	 * factory. Does not include simple properties like primitives or Strings.
	 * @param mbd the merged bean definition the bean was created with
	 * @param bw the BeanWrapper the bean was created with
	 * @return an array of bean property names
	 * @see org.springframework.beans.BeanUtils#isSimpleProperty
	 */
	/**
	 *   spring认为的简单属性
	 *   1. CharSequence 接口的实现类，比如 String
	 *   2. Enum
	 *   3. Date
	 *   4. URI/URL
	 *   5. Number 的继承类，比如 Integer/Long
	 *   6. byte/short/int... 等基本类型
	 *   7. Locale
	 *   8. 以上所有类型的数组形式，比如 String[]、Date[]、int[] 等等
	 * 不包含在当前bean的配置文件的中属性 !pvs.contains(pd.getName()
	 **/
	protected String[] unsatisfiedNonSimpleProperties(AbstractBeanDefinition mbd, BeanWrapper bw) {
		Set<String> result = new TreeSet<>();
		PropertyValues pvs = mbd.getPropertyValues();
		PropertyDescriptor[] pds = bw.getPropertyDescriptors();
		for (PropertyDescriptor pd : pds) {
			if (pd.getWriteMethod() != null && !isExcludedFromDependencyCheck(pd) && !pvs.contains(pd.getName()) &&
					!BeanUtils.isSimpleProperty(pd.getPropertyType())) {
				result.add(pd.getName());
			}
		}
		return StringUtils.toStringArray(result);
	}

	/**
	 * Extract a filtered set of PropertyDescriptors from the given BeanWrapper,
	 * excluding ignored dependency types or properties defined on ignored dependency interfaces.
	 * @param bw the BeanWrapper the bean was created with
	 * @param cache whether to cache filtered PropertyDescriptors for the given bean Class
	 * @return the filtered PropertyDescriptors
	 * @see #isExcludedFromDependencyCheck
	 * @see #filterPropertyDescriptorsForDependencyCheck(org.springframework.beans.BeanWrapper)
	 */
	protected PropertyDescriptor[] filterPropertyDescriptorsForDependencyCheck(BeanWrapper bw, boolean cache) {
		PropertyDescriptor[] filtered = this.filteredPropertyDescriptorsCache.get(bw.getWrappedClass());
		if (filtered == null) {
			filtered = filterPropertyDescriptorsForDependencyCheck(bw);
			if (cache) {
				PropertyDescriptor[] existing =
						this.filteredPropertyDescriptorsCache.putIfAbsent(bw.getWrappedClass(), filtered);
				if (existing != null) {
					filtered = existing;
				}
			}
		}
		return filtered;
	}

	/**
	 * Extract a filtered set of PropertyDescriptors from the given BeanWrapper,
	 * excluding ignored dependency types or properties defined on ignored dependency interfaces.
	 * @param bw the BeanWrapper the bean was created with
	 * @return the filtered PropertyDescriptors
	 * @see #isExcludedFromDependencyCheck
	 */
	protected PropertyDescriptor[] filterPropertyDescriptorsForDependencyCheck(BeanWrapper bw) {
		List<PropertyDescriptor> pds = new ArrayList<>(Arrays.asList(bw.getPropertyDescriptors()));
		pds.removeIf(this::isExcludedFromDependencyCheck);
		return pds.toArray(new PropertyDescriptor[0]);
	}

	/**
	 * Determine whether the given bean property is excluded from dependency checks.
	 * <p>This implementation excludes properties defined by CGLIB and
	 * properties whose type matches an ignored dependency type or which
	 * are defined by an ignored dependency interface.
	 * @param pd the PropertyDescriptor of the bean property
	 * @return whether the bean property is excluded
	 * @see #ignoreDependencyType(Class)
	 * @see #ignoreDependencyInterface(Class)
	 */
	protected boolean isExcludedFromDependencyCheck(PropertyDescriptor pd) {
		return (AutowireUtils.isExcludedFromDependencyCheck(pd) ||
				this.ignoredDependencyTypes.contains(pd.getPropertyType()) ||
				AutowireUtils.isSetterDefinedInInterface(pd, this.ignoredDependencyInterfaces));
	}

	/**
	 * Perform a dependency check that all properties exposed have been set,
	 * if desired. Dependency checks can be objects (collaborating beans),
	 * simple (primitives and String), or all (both).
	 * @param beanName the name of the bean
	 * @param mbd the merged bean definition the bean was created with
	 * @param pds the relevant property descriptors for the target bean
	 * @param pvs the property values to be applied to the bean
	 * @see #isExcludedFromDependencyCheck(java.beans.PropertyDescriptor)
	 */
	protected void checkDependencies(
			String beanName, AbstractBeanDefinition mbd, PropertyDescriptor[] pds, @Nullable PropertyValues pvs)
			throws UnsatisfiedDependencyException {

		int dependencyCheck = mbd.getDependencyCheck();
		for (PropertyDescriptor pd : pds) {
			if (pd.getWriteMethod() != null && (pvs == null || !pvs.contains(pd.getName()))) {
				boolean isSimple = BeanUtils.isSimpleProperty(pd.getPropertyType());
				boolean unsatisfied = (dependencyCheck == AbstractBeanDefinition.DEPENDENCY_CHECK_ALL) ||
						(isSimple && dependencyCheck == AbstractBeanDefinition.DEPENDENCY_CHECK_SIMPLE) ||
						(!isSimple && dependencyCheck == AbstractBeanDefinition.DEPENDENCY_CHECK_OBJECTS);
				if (unsatisfied) {
					throw new UnsatisfiedDependencyException(mbd.getResourceDescription(), beanName, pd.getName(),
							"Set this property value or disable dependency checking for this bean.");
				}
			}
		}
	}

	/**
	 * Apply the given property values, resolving any runtime references
	 * to other beans in this bean factory. Must use deep copy, so we
	 * don't permanently modify this property.
	 * @param beanName the bean name passed for better exception information
	 * @param mbd the merged bean definition
	 * @param bw the BeanWrapper wrapping the target object
	 * @param pvs the new property values
	 */
	protected void applyPropertyValues(String beanName, BeanDefinition mbd, BeanWrapper bw, PropertyValues pvs) {
		if (pvs.isEmpty()) {
			return;
		}
		// MutablePropertyValues 类型属性
		MutablePropertyValues mpvs = null;
		// 原始类型
		List<PropertyValue> original;
		// 获得 original
		if (pvs instanceof MutablePropertyValues _mpvs) {
			//把我们的pvs强行转为MutablePropertyValues
			mpvs = _mpvs;
			//判断MutablePropertyValues 已经转换过了
			if (mpvs.isConverted()) {
				// Shortcut: use the pre-converted values as-is.
				try {
					//直接设置到beanWrapper中
					bw.setPropertyValues(mpvs);
					return;
				}
				catch (BeansException ex) {
					throw new BeanCreationException(
							mbd.getResourceDescription(), beanName, "Error setting property values", ex);
				}
			}
			// 如果 pvs 不是 MutablePropertyValues 类型，则直接使用原始类型
			original = mpvs.getPropertyValueList();
		}
		else {
			original = Arrays.asList(pvs.getPropertyValues());
		}
		// 获取 TypeConverter = 获取用户自定义的类型转换
		TypeConverter converter = getCustomTypeConverter();
		if (converter == null) {
			converter = bw;
		}
		// 获取对应的解析器
		BeanDefinitionValueResolver valueResolver = new BeanDefinitionValueResolver(this, beanName, mbd, converter);

		// Create a deep copy, resolving any references for values.
		List<PropertyValue> deepCopy = new ArrayList<>(original.size());
		boolean resolveNecessary = false;
		// 遍历属性，将属性转换为对应类的对应属性的类型
		for (PropertyValue pv : original) {
			if (pv.isConverted()) {
				deepCopy.add(pv);
			}
			else {
				//属性的名称
				String propertyName = pv.getName();
				//属性对象的原始值
				Object originalValue = pv.getValue();
				if (originalValue == AutowiredPropertyMarker.INSTANCE) {
					Method writeMethod = bw.getPropertyDescriptor(propertyName).getWriteMethod();
					if (writeMethod == null) {
						throw new IllegalArgumentException("Autowire marker for property without write method: " + pv);
					}
					originalValue = new DependencyDescriptor(new MethodParameter(writeMethod, 0), true);
				}
				/**
				 *   转换属性值，例如将引用转换为IoC容器中实例化对象引用 ！！！！！ 对属性值的解析！
				 */
				Object resolvedValue = valueResolver.resolveValueIfNecessary(pv, originalValue);
				Object convertedValue = resolvedValue;
				boolean convertible = isConvertibleProperty(propertyName, bw);
				// 使用用户自定义的类型转换器转换属性值
				if (convertible) {
					// 对属性值的类型进行转换，比如将 String 类型的属性值 "123" 转为 Integer 类型的 123
					convertedValue = convertForProperty(resolvedValue, propertyName, bw, converter);
				}
				// Possibly store converted value in merged bean definition,
				// in order to avoid re-conversion for every created bean instance.
				// 存储转换后的属性值，避免每次属性注入时的转换工作
				if (resolvedValue == originalValue) {
					if (convertible) {
						pv.setConvertedValue(convertedValue);
					}
					deepCopy.add(pv);
				}
				// 属性是可转换的，且属性原始值是字符串类型，且属性的原始类型值不是
				// 动态生成的字符串，且属性的原始值不是集合或者数组类型
				else if (convertible && originalValue instanceof TypedStringValue typedStringValue &&
						!typedStringValue.isDynamic() &&
						!(convertedValue instanceof Collection || ObjectUtils.isArray(convertedValue))) {
					pv.setConvertedValue(convertedValue);
					deepCopy.add(pv);
				}
				else {
					// 重新封装属性的值
					resolveNecessary = true;
					deepCopy.add(new PropertyValue(pv, convertedValue));
				}
			}
		}
		// 标记属性值已经转换过
		if (mpvs != null && !resolveNecessary) {
			mpvs.setConverted();
		}

		// Set our (possibly massaged) deep copy.
		try {
			// 进行属性依赖注入，依赖注入的真真正正实现依赖的注入方法在此！！！ 这这里就会调用我们的构造方法
			bw.setPropertyValues(new MutablePropertyValues(deepCopy));
		}
		catch (BeansException ex) {
			throw new BeanCreationException(mbd.getResourceDescription(), beanName, ex.getMessage(), ex);
		}
	}

	/**
	 * Determine whether the factory should cache a converted value for the given property.
	 */
	private boolean isConvertibleProperty(String propertyName, BeanWrapper bw) {
		try {
			return !PropertyAccessorUtils.isNestedOrIndexedProperty(propertyName) &&
					BeanUtils.hasUniqueWriteMethod(bw.getPropertyDescriptor(propertyName));
		}
		catch (InvalidPropertyException ex) {
			return false;
		}
	}

	/**
	 * Convert the given value for the specified target property.
	 */
	private @Nullable Object convertForProperty(
			@Nullable Object value, String propertyName, BeanWrapper bw, TypeConverter converter) {

		if (converter instanceof BeanWrapperImpl beanWrapper) {
			return beanWrapper.convertForProperty(value, propertyName);
		}
		else {
			PropertyDescriptor pd = bw.getPropertyDescriptor(propertyName);
			MethodParameter methodParam = BeanUtils.getWriteMethodParameter(pd);
			return converter.convertIfNecessary(value, pd.getPropertyType(), methodParam);
		}
	}


	/**
	 * Initialize the given bean instance, applying factory callbacks
	 * as well as init methods and bean post processors.
	 * <p>Called from {@link #createBean} for traditionally defined beans,
	 * and from {@link #initializeBean} for existing bean instances.
	 * @param beanName the bean name in the factory (for debugging purposes)
	 * @param bean the new bean instance we may need to initialize
	 * @param mbd the bean definition that the bean was created with
	 * (can also be {@code null}, if given an existing bean instance)
	 * @return the initialized bean instance (potentially wrapped)
	 * @see BeanNameAware
	 * @see BeanClassLoaderAware
	 * @see BeanFactoryAware
	 * @see #applyBeanPostProcessorsBeforeInitialization
	 * @see #invokeInitMethods
	 * @see #applyBeanPostProcessorsAfterInitialization
	 */
	@SuppressWarnings("deprecation")
	protected Object initializeBean(String beanName, Object bean, @Nullable RootBeanDefinition mbd) {
		invokeAwareMethods(beanName, bean);

		Object wrappedBean = bean;
		if (mbd == null || !mbd.isSynthetic()) {
			//调用我们的bean的后置处理器的postProcessorsBeforeInitialization方法  @PostCust注解的方法
			wrappedBean = applyBeanPostProcessorsBeforeInitialization(wrappedBean, beanName);
		}

		try {
			//调用初始化方法
			invokeInitMethods(beanName, wrappedBean, mbd);
		}
		catch (Throwable ex) {
			throw new BeanCreationException(
					(mbd != null ? mbd.getResourceDescription() : null), beanName, ex.getMessage(), ex);
		}
		if (mbd == null || !mbd.isSynthetic()) {
			//调用我们bean的后置处理器的PostProcessorsAfterInitialization方法
			wrappedBean = applyBeanPostProcessorsAfterInitialization(wrappedBean, beanName);
		}

		return wrappedBean;
	}

	private void invokeAwareMethods(String beanName, Object bean) {
		if (bean instanceof Aware) {
			//我们的bean实现了BeanNameAware
			if (bean instanceof BeanNameAware beanNameAware) {
				beanNameAware.setBeanName(beanName);
			}
			//实现了BeanClassLoaderAware接口
			if (bean instanceof BeanClassLoaderAware beanClassLoaderAware) {
				ClassLoader bcl = getBeanClassLoader();
				if (bcl != null) {
					beanClassLoaderAware.setBeanClassLoader(bcl);
				}
			}
			//实现了BeanFactoryAware
			if (bean instanceof BeanFactoryAware beanFactoryAware) {
				beanFactoryAware.setBeanFactory(AbstractAutowireCapableBeanFactory.this);
			}
		}
	}

	/**
	 * Give a bean a chance to initialize itself after all its properties are set,
	 * and a chance to know about its owning bean factory (this object).
	 * <p>This means checking whether the bean implements {@link InitializingBean}
	 * or defines any custom init methods, and invoking the necessary callback(s)
	 * if it does.
	 * @param beanName the bean name in the factory (for debugging purposes)
	 * @param bean the new bean instance we may need to initialize
	 * @param mbd the merged bean definition that the bean was created with
	 * (can also be {@code null}, if given an existing bean instance)
	 * @throws Throwable if thrown by init methods or by the invocation process
	 * @see #invokeCustomInitMethod
	 */
	protected void invokeInitMethods(String beanName, Object bean, @Nullable RootBeanDefinition mbd)
			throws Throwable {
		//判断我们的容器中是否实现了InitializingBean接口
		boolean isInitializingBean = (bean instanceof InitializingBean);
		if (isInitializingBean && (mbd == null || !mbd.hasAnyExternallyManagedInitMethod("afterPropertiesSet"))) {
			if (logger.isTraceEnabled()) {
				logger.trace("Invoking afterPropertiesSet() on bean with name '" + beanName + "'");
			}
			((InitializingBean) bean).afterPropertiesSet();
		}

		if (mbd != null && bean.getClass() != NullBean.class) {
			//我们beanclass中看是否有自己定义的init方法
			String[] initMethodNames = mbd.getInitMethodNames();
			if (initMethodNames != null) {
				for (String initMethodName : initMethodNames) {
					//判断自定义的init方法名称不叫afterPropertiesSet
					if (StringUtils.hasLength(initMethodName) &&
							!(isInitializingBean && "afterPropertiesSet".equals(initMethodName)) &&
							!mbd.hasAnyExternallyManagedInitMethod(initMethodName)) {
						//调用我们自己的初始化方法
						invokeCustomInitMethod(beanName, bean, mbd, initMethodName);
					}
				}
			}
		}
	}

	/**
	 * Invoke the specified custom init method on the given bean.
	 * <p>Called by {@link #invokeInitMethods(String, Object, RootBeanDefinition)}.
	 * <p>Can be overridden in subclasses for custom resolution of init methods
	 * with arguments.
	 * @see #invokeInitMethods
	 */
	protected void invokeCustomInitMethod(String beanName, Object bean, RootBeanDefinition mbd, String initMethodName)
			throws Throwable {

		Class<?> beanClass = bean.getClass();
		MethodDescriptor descriptor = MethodDescriptor.create(beanName, beanClass, initMethodName);
		String methodName = descriptor.methodName();

		Method initMethod = (mbd.isNonPublicAccessAllowed() ?
				BeanUtils.findMethod(descriptor.declaringClass(), methodName) :
				ClassUtils.getMethodIfAvailable(beanClass, methodName));

		if (initMethod == null) {
			if (mbd.isEnforceInitMethod()) {
				throw new BeanDefinitionValidationException("Could not find an init method named '" +
						methodName + "' on bean with name '" + beanName + "'");
			}
			else {
				if (logger.isTraceEnabled()) {
					logger.trace("No default init method named '" + methodName +
							"' found on bean with name '" + beanName + "'");
				}
				// Ignore non-existent default lifecycle methods.
				return;
			}
		}

		if (logger.isTraceEnabled()) {
			logger.trace("Invoking init method '" + methodName + "' on bean with name '" + beanName + "'");
		}
		Method methodToInvoke = ClassUtils.getPubliclyAccessibleMethodIfPossible(initMethod, beanClass);

		try {
			ReflectionUtils.makeAccessible(methodToInvoke);
			methodToInvoke.invoke(bean);
		}
		catch (InvocationTargetException ex) {
			throw ex.getTargetException();
		}
	}


	/**
	 * Applies the {@code postProcessAfterInitialization} callback of all
	 * registered BeanPostProcessors, giving them a chance to post-process the
	 * object obtained from FactoryBeans (for example, to auto-proxy them).
	 * @see #applyBeanPostProcessorsAfterInitialization
	 */
	@SuppressWarnings("deprecation")
	@Override
	protected Object postProcessObjectFromFactoryBean(Object object, String beanName) {
		return applyBeanPostProcessorsAfterInitialization(object, beanName);
	}

	/**
	 * Overridden to clear FactoryBean instance cache as well.
	 */
	@Override
	protected void removeSingleton(String beanName) {
		super.removeSingleton(beanName);
		this.factoryBeanInstanceCache.remove(beanName);
	}

	/**
	 * Overridden to clear FactoryBean instance cache as well.
	 */
	@Override
	protected void clearSingletonCache() {
		super.clearSingletonCache();
		this.factoryBeanInstanceCache.clear();
	}

	/**
	 * Expose the logger to collaborating delegates.
	 * @since 5.0.7
	 */
	Log getLogger() {
		return logger;
	}


	/**
	 * {@link RootBeanDefinition} subclass for {@code #createBean} calls with
	 * flexible selection of a Kotlin primary / single public / single non-public
	 * constructor candidate in addition to the default constructor.
	 * @see BeanUtils#getResolvableConstructor(Class)
	 */
	@SuppressWarnings("serial")
	private static class CreateFromClassBeanDefinition extends RootBeanDefinition {

		public CreateFromClassBeanDefinition(Class<?> beanClass) {
			super(beanClass);
		}

		public CreateFromClassBeanDefinition(CreateFromClassBeanDefinition original) {
			super(original);
		}

		@Override
		public Constructor<?> @Nullable [] getPreferredConstructors() {
			Constructor<?>[] fromAttribute = super.getPreferredConstructors();
			if (fromAttribute != null) {
				return fromAttribute;
			}
			return ConstructorResolver.determinePreferredConstructors(getBeanClass());
		}

		@Override
		public RootBeanDefinition cloneBeanDefinition() {
			return new CreateFromClassBeanDefinition(this);
		}
	}


	/**
	 * Special DependencyDescriptor variant for Spring's good old autowire="byType" mode.
	 * Always optional; never considering the parameter name for choosing a primary candidate.
	 */
	@SuppressWarnings("serial")
	private static class AutowireByTypeDependencyDescriptor extends DependencyDescriptor {

		public AutowireByTypeDependencyDescriptor(MethodParameter methodParameter, boolean eager) {
			super(methodParameter, false, eager);
		}

		@Override
		public @Nullable String getDependencyName() {
			return null;
		}
	}


	/**
	 * {@link MethodCallback} used to find {@link FactoryBean} type information.
	 */
	private static class FactoryBeanMethodTypeFinder implements MethodCallback {

		private final String factoryMethodName;

		private ResolvableType result = ResolvableType.NONE;

		FactoryBeanMethodTypeFinder(String factoryMethodName) {
			this.factoryMethodName = factoryMethodName;
		}

		@Override
		public void doWith(Method method) throws IllegalArgumentException {
			if (isFactoryBeanMethod(method)) {
				ResolvableType returnType = ResolvableType.forMethodReturnType(method);
				ResolvableType candidate = returnType.as(FactoryBean.class).getGeneric();
				if (this.result == ResolvableType.NONE) {
					this.result = candidate;
				}
				else {
					Class<?> resolvedResult = this.result.resolve();
					Class<?> commonAncestor = ClassUtils.determineCommonAncestor(candidate.resolve(), resolvedResult);
					if (!ObjectUtils.nullSafeEquals(resolvedResult, commonAncestor)) {
						this.result = ResolvableType.forClass(commonAncestor);
					}
				}
			}
		}

		private boolean isFactoryBeanMethod(Method method) {
			return (method.getName().equals(this.factoryMethodName) &&
					FactoryBean.class.isAssignableFrom(method.getReturnType()));
		}

		ResolvableType getResult() {
			Class<?> resolved = this.result.resolve();
			boolean foundResult = resolved != null && resolved != Object.class;
			return (foundResult ? this.result : ResolvableType.NONE);
		}
	}

}
