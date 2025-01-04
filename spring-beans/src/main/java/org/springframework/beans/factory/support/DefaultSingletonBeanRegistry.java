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

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;

import org.jspecify.annotations.Nullable;

import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.BeanCreationNotAllowedException;
import org.springframework.beans.factory.BeanCurrentlyInCreationException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.config.SingletonBeanRegistry;
import org.springframework.core.SimpleAliasRegistry;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Generic registry for shared bean instances, implementing the
 * {@link org.springframework.beans.factory.config.SingletonBeanRegistry}.
 * Allows for registering singleton instances that should be shared
 * for all callers of the registry, to be obtained via bean name.
 *
 * <p>Also supports registration of
 * {@link org.springframework.beans.factory.DisposableBean} instances,
 * (which might or might not correspond to registered singletons),
 * to be destroyed on shutdown of the registry. Dependencies between
 * beans can be registered to enforce an appropriate shutdown order.
 *
 * <p>This class mainly serves as base class for
 * {@link org.springframework.beans.factory.BeanFactory} implementations,
 * factoring out the common management of singleton bean instances. Note that
 * the {@link org.springframework.beans.factory.config.ConfigurableBeanFactory}
 * interface extends the {@link SingletonBeanRegistry} interface.
 *
 * <p>Note that this class assumes neither a bean definition concept
 * nor a specific creation process for bean instances, in contrast to
 * {@link AbstractBeanFactory} and {@link DefaultListableBeanFactory}
 * (which inherit from it). Can alternatively also be used as a nested
 * helper to delegate to.
 *
 * @author Juergen Hoeller
 * @since 2.0
 * @see #registerSingleton
 * @see #registerDisposableBean
 * @see org.springframework.beans.factory.DisposableBean
 * @see org.springframework.beans.factory.config.ConfigurableBeanFactory
 */
public class DefaultSingletonBeanRegistry extends SimpleAliasRegistry implements SingletonBeanRegistry {

	/** Maximum number of suppressed exceptions to preserve. */
	private static final int SUPPRESSED_EXCEPTIONS_LIMIT = 100;


	/** Common lock for singleton creation. */
	final Lock singletonLock = new ReentrantLock();

	/** Cache of singleton objects: bean name to bean instance. */
	/**
	 * singletonObjects，单例缓存，存储已经实例化完成的单例。
	 * singletonFactories，生产单例的工厂的缓存，存储工厂。
	 * earlySingletonObjects，提前暴露的单例缓存，这时候的单例刚刚创建完，但还会注入依赖。
	 */
	/** 一级缓存 这个就是我们大名鼎鼎的单例缓存池 用于保存我们所有的单实例bean key:BeanName value:bean实例 */
	private final Map<String, Object> singletonObjects = new ConcurrentHashMap<>(256);

	/** Creation-time registry of singleton factories: bean name to ObjectFactory. */
	/** 三级缓存 该map用户缓存 key为 beanName  value 为ObjectFactory(包装为早期对象) key：BeanName value：创建的bean工厂*/
	private final Map<String, ObjectFactory<?>> singletonFactories = new ConcurrentHashMap<>(16);

	/** Custom callbacks for singleton creation/registration. */
	private final Map<String, Consumer<Object>> singletonCallbacks = new ConcurrentHashMap<>(16);

	/** Cache of early singleton objects: bean name to bean instance. */
	/** 二级缓存 ，用户缓存我们的key为beanName value是我们的早期对象(对象属性还没有来得及进行赋值) */
	private final Map<String, Object> earlySingletonObjects = new ConcurrentHashMap<>(16);

	/** Set of registered singletons, containing the bean names in registration order. */
	// 用来保存当前所有已经注册的bean
	private final Set<String> registeredSingletons = Collections.synchronizedSet(new LinkedHashSet<>(256));

	/** Names of beans that are currently in creation. */
	/** 该集合用户缓存当前正在创建bean的名称 */
	private final Set<String> singletonsCurrentlyInCreation = ConcurrentHashMap.newKeySet(16);

	/** Names of beans currently excluded from in creation checks. */
	/** 排除当前创建检查的*/
	private final Set<String> inCreationCheckExclusions = ConcurrentHashMap.newKeySet(16);

	/** Flag that indicates whether we're currently within destroySingletons. */
	private volatile boolean singletonsCurrentlyInDestruction = false;

	/** Collection of suppressed Exceptions, available for associating related causes. */
	private @Nullable Set<Exception> suppressedExceptions;

	/** Disposable bean instances: bean name to disposable instance. */
	/** 用户缓存记录实现了DisposableBean 接口的实例 */
	private final Map<String, DisposableBean> disposableBeans = new LinkedHashMap<>();

	/** Map between containing bean names: bean name to Set of bean names that the bean contains. */
	/** 缓存bean的属性关系的映射<dujieServce,<dujieDao,dujieDao2> --> Set of bean names that the bean contains */
	private final Map<String, Set<String>> containedBeanMap = new ConcurrentHashMap<>(16);

	/** Map between dependent bean names: bean name to Set of dependent bean names.
	 * 保存的是依赖 beanName 之间的映射关系：beanName - > 依赖 beanName 的集合
	 **/
	private final Map<String, Set<String>> dependentBeanMap = new ConcurrentHashMap<>(64);

	/** Map between depending bean names: bean name to Set of bean names for the bean's dependencies.
	 * 保存的是依赖 beanName 之间的映射关系：依赖 beanName - > beanName 的集合
	 **/
	private final Map<String, Set<String>> dependenciesForBeanMap = new ConcurrentHashMap<>(64);


	@Override
	public void registerSingleton(String beanName, Object singletonObject) throws IllegalStateException {
		Assert.notNull(beanName, "Bean name must not be null");
		Assert.notNull(singletonObject, "Singleton object must not be null");
		this.singletonLock.lock();
		try {
			addSingleton(beanName, singletonObject);
		}
		finally {
			this.singletonLock.unlock();
		}
	}

	/**
	 * Add the given singleton object to the singleton registry.
	 * <p>To be called for exposure of freshly registered/created singletons.
	 * @param beanName the name of the bean
	 * @param singletonObject the singleton object
	 * 把对象加入到单例缓存池中(所谓的一级缓存 并且考虑循环依赖和正常情况下,移除二三级缓存)
	 * @param beanName bean的名称
	 * @param singletonObject 创建出来的单实例bean
	 */
	protected void addSingleton(String beanName, Object singletonObject) {
		//加入到单例缓存池中
		Object oldObject = this.singletonObjects.putIfAbsent(beanName, singletonObject);
		if (oldObject != null) {
			throw new IllegalStateException("Could not register object [" + singletonObject +
					"] under bean name '" + beanName + "': there is already object [" + oldObject + "] bound");
		}
		//从三级缓存中移除(针对的不是处理循环依赖的)
		this.singletonFactories.remove(beanName);
		//从二级缓存中移除(循环依赖的时候 早期对象存在于二级缓存)
		this.earlySingletonObjects.remove(beanName);
		//用来记录保存已经处理的bean
		this.registeredSingletons.add(beanName);

		Consumer<Object> callback = this.singletonCallbacks.get(beanName);
		if (callback != null) {
			callback.accept(singletonObject);
		}
	}

	/**
	 * 该方法用于把早期对象包装成一个ObjectFactory 暴露到三级缓存中 用于将解决循环依赖...
	 * Add the given singleton factory for building the specified singleton
	 * if necessary.
	 * <p>To be called for early exposure purposes, for example, to be able to
	 * resolve circular references.
	 * @param beanName the name of the bean
	 * @param singletonFactory the factory for the singleton object
	 */
	protected void addSingletonFactory(String beanName, ObjectFactory<?> singletonFactory) {
		Assert.notNull(singletonFactory, "Singleton factory must not be null");
		//加入到三级缓存中，，，，，暴露早期对象用于解决循环依赖
		this.singletonFactories.put(beanName, singletonFactory);
		this.earlySingletonObjects.remove(beanName);
		this.registeredSingletons.add(beanName);
	}

	/**
	 * 该方法是一个空壳方法
	 * @param beanName bean的名称
	 * @return 缓存中的对象(有可能是一个单例完整对象,也有可能是一个早期对象(用于解决循环依赖))
	 */
	@Override
	public void addSingletonCallback(String beanName, Consumer<Object> singletonConsumer) {
		this.singletonCallbacks.put(beanName, singletonConsumer);
	}

	@Override
	public @Nullable Object getSingleton(String beanName) {
		//在这里 系统一般是允许早期对象引用的 allowEarlyReference通过这个参数可以控制解决循环依赖
		return getSingleton(beanName, true);
	}

	/**
	 * 在网上很多很多写源码的大佬或者是<spring源码深度解析>一书上,也没有说清楚为啥要使用三级缓存(二级缓存可不可以能够
	 * 解决) 答案是：可以, 但是没有很好的扩展性为啥这么说.......
	 * 原因: 获取三级缓存-----getEarlyBeanReference()经过一系列的后置处理来给我们早期对象进行特殊化处理
	 * //从三级缓存中获取包装对象的时候 ，他会经过一次后置处理器的处理对我们早期对象的bean进行
	 * 特殊化处理，但是spring的原生后置处理器没有经过处理，而是留给了我们程序员进行扩展
	 * singletonObject = singletonFactory.getObject();
	 * 把三级缓存移植到二级缓存中
	 * this.earlySingletonObjects.put(beanName, singletonObject);
	 * //删除三级缓存中的之
	 * this.singletonFactories.remove(beanName);
	 * @param beanName bean的名称
	 * @param allowEarlyReference 是否允许暴露早期对象  通过该参数可以控制是否能够解决循环依赖的.
	 * @return
	 * 	    这里可能返回一个null（IOC容器加载单实例bean的时候,第一次进来是返回null）
	 * 	    也有可能返回一个单例对象(IOC容器加载了单实例了,第二次来获取当前的Bean)
	 * 	    也可能返回一个早期对象(用于解决循环依赖问题)
	 */
	protected @Nullable Object getSingleton(String beanName, boolean allowEarlyReference) {
		// Quick check for existing instance without full singleton lock.
		/**
		 * 第一步:我们尝试去一级缓存(单例缓存池中去获取对象,一般情况从该map中获取的对象是直接可以使用的)
		 * IOC容器初始化加载单实例bean的时候第一次进来的时候 该map中一般返回空
		 */
		Object singletonObject = this.singletonObjects.get(beanName);
		/**
		 * 若在第一级缓存中没有获取到对象,并且singletonsCurrentlyInCreation这个list包含该beanName
		 * IOC容器初始化加载单实例bean的时候第一次进来的时候 该list中一般返回空,但是循环依赖的时候可以满足该条件
		 */
		if (singletonObject == null && isSingletonCurrentlyInCreation(beanName)) {
			/**
			 * 尝试去二级缓存中获取对象(二级缓存中的对象是一个早期对象)
			 * 何为早期对象:就是bean刚刚调用了构造方法，还来不及给bean的属性进行赋值的对象
			 * 就是早期对象
			 */
			singletonObject = this.earlySingletonObjects.get(beanName);
			if (singletonObject == null && allowEarlyReference) {
				if (!this.singletonLock.tryLock()) {
					// Avoid early singleton inference outside of original creation thread.
					return null;
				}
				try {
					/**
					 * 直接从三级缓存中获取 ObjectFactory对象 这个对接就是用来解决循环依赖的关键所在
					 * 在ioc后期的过程中,当bean调用了构造方法的时候,把早期对象包裹成一个ObjectFactory
					 * 暴露到三级缓存中
					 */
					// Consistent creation of early reference within full singleton lock.
					singletonObject = this.singletonObjects.get(beanName);
					if (singletonObject == null) {
						//从三级缓存中获取到对象不为空
						singletonObject = this.earlySingletonObjects.get(beanName);
						if (singletonObject == null) {
							/**
							 * 在这里通过暴露的ObjectFactory 包装对象中,通过调用他的getObject()来获取我们的早期对象
							 * 在这个环节中会调用到 getEarlyBeanReference()来进行后置处理
							 */
							ObjectFactory<?> singletonFactory = this.singletonFactories.get(beanName);
							if (singletonFactory != null) {
								singletonObject = singletonFactory.getObject();
								// Singleton could have been added or removed in the meantime.
								if (this.singletonFactories.remove(beanName) != null) {
									//把早期对象放置在二级缓存,
									this.earlySingletonObjects.put(beanName, singletonObject);
								}
								else {
									//ObjectFactory 包装对象从三级缓存中删除掉
									singletonObject = this.singletonObjects.get(beanName);
								}
							}
						}
					}
				}
				finally {
					this.singletonLock.unlock();
				}
			}
		}
		return singletonObject;
	}

	/**
	 * 获取单例对象(该流程用于触发构建bean)
	 * Return the (raw) singleton object registered under the given name,
	 * creating and registering a new one if none registered yet.
	 * @param beanName the name of the bean
	 * @param singletonFactory the ObjectFactory to lazily create the singleton
	 * with, if necessary
	 * @return the registered singleton object
	 */
	@SuppressWarnings("NullAway") // Dataflow analysis limitation
	public Object getSingleton(String beanName, ObjectFactory<?> singletonFactory) {
		Assert.notNull(beanName, "Bean name must not be null");

		Boolean lockFlag = isCurrentThreadAllowedToHoldSingletonLock();
		boolean acquireLock = !Boolean.FALSE.equals(lockFlag);
		boolean locked = (acquireLock && this.singletonLock.tryLock());
		try {
			//尝试从单例缓存池中获取对象
			Object singletonObject = this.singletonObjects.get(beanName);
			if (singletonObject == null) {
				if (acquireLock && !locked) {
					if (Boolean.TRUE.equals(lockFlag)) {
						// Another thread is busy in a singleton factory callback, potentially blocked.
						// Fallback as of 6.2: process given singleton bean outside of singleton lock.
						// Thread-safe exposure is still guaranteed, there is just a risk of collisions
						// when triggering creation of other beans as dependencies of the current bean.
						if (logger.isInfoEnabled()) {
							logger.info("Creating singleton bean '" + beanName + "' in thread \"" +
									Thread.currentThread().getName() + "\" while other thread holds " +
									"singleton lock for other beans " + this.singletonsCurrentlyInCreation);
						}
					}
					else {
						// No specific locking indication (outside a coordinated bootstrap) and
						// singleton lock currently held by some other creation method -> wait.
						this.singletonLock.lock();
						locked = true;
						// Singleton object might have possibly appeared in the meantime.
						singletonObject = this.singletonObjects.get(beanName);
						if (singletonObject != null) {
							return singletonObject;
						}
					}
				}

				if (this.singletonsCurrentlyInDestruction) {
					throw new BeanCreationNotAllowedException(beanName,
							"Singleton bean creation not allowed while singletons of this factory are in destruction " +
							"(Do not request a bean from a BeanFactory in a destroy method implementation!)");
				}
				if (logger.isDebugEnabled()) {
					logger.debug("Creating shared instance of singleton bean '" + beanName + "'");
				}
				/**
				 * 标记当前的bean马上就要被创建了
				 * singletonsCurrentlyInCreation 在这里会把beanName加入进来，若第二次循环依赖（构造器注入会抛出异常）
				 */
				beforeSingletonCreation(beanName);
				boolean newSingleton = false;
				boolean recordSuppressedExceptions = (locked && this.suppressedExceptions == null);
				if (recordSuppressedExceptions) {
					this.suppressedExceptions = new LinkedHashSet<>();
				}
				try {
					//<3> 初始化 bean
					// 这个过程其实是调用 createBean() 方法
					singletonObject = singletonFactory.getObject();
					newSingleton = true;
				}
				catch (IllegalStateException ex) {
					// Has the singleton object implicitly appeared in the meantime ->
					// if yes, proceed with it since the exception indicates that state.
					//回调我们singletonObjects的get方法,进行正在的创建bean的逻辑
					singletonObject = this.singletonObjects.get(beanName);
					if (singletonObject == null) {
						throw ex;
					}
				}
				catch (BeanCreationException ex) {
					if (recordSuppressedExceptions) {
						for (Exception suppressedException : this.suppressedExceptions) {
							ex.addRelatedCause(suppressedException);
						}
					}
					throw ex;
				}
				finally {
					if (recordSuppressedExceptions) {
						this.suppressedExceptions = null;
					}
					// <4> 后置处理
					//主要做的事情就是把singletonsCurrentlyInCreation标记正在创建的bean从集合中移除
					afterSingletonCreation(beanName);
				}
				if (newSingleton) {
					// <5> 加入缓存中
					addSingleton(beanName, singletonObject);
				}
			}
			return singletonObject;
		}
		finally {
			if (locked) {
				this.singletonLock.unlock();
			}
		}
	}

	/**
	 * Determine whether the current thread is allowed to hold the singleton lock.
	 * <p>By default, any thread may acquire and hold the singleton lock, except
	 * background threads from {@link DefaultListableBeanFactory#setBootstrapExecutor}.
	 * @return {@code false} if the current thread is explicitly not allowed to hold
	 * the lock, {@code true} if it is explicitly allowed to hold the lock but also
	 * accepts lenient fallback behavior, or {@code null} if there is no specific
	 * indication (traditional behavior: always holding a full lock)
	 * @since 6.2
	 */
	protected @Nullable Boolean isCurrentThreadAllowedToHoldSingletonLock() {
		return null;
	}

	/**
	 * Register an exception that happened to get suppressed during the creation of a
	 * singleton bean instance, for example, a temporary circular reference resolution problem.
	 * <p>The default implementation preserves any given exception in this registry's
	 * collection of suppressed exceptions, up to a limit of 100 exceptions, adding
	 * them as related causes to an eventual top-level {@link BeanCreationException}.
	 * @param ex the Exception to register
	 * @see BeanCreationException#getRelatedCauses()
	 */
	protected void onSuppressedException(Exception ex) {
		if (this.suppressedExceptions != null && this.suppressedExceptions.size() < SUPPRESSED_EXCEPTIONS_LIMIT) {
			this.suppressedExceptions.add(ex);
		}
	}

	/**
	 * Remove the bean with the given name from the singleton registry, either on
	 * regular destruction or on cleanup after early exposure when creation failed.
	 * @param beanName the name of the bean
	 */
	protected void removeSingleton(String beanName) {
		this.singletonObjects.remove(beanName);
		this.singletonFactories.remove(beanName);
		this.earlySingletonObjects.remove(beanName);
		this.registeredSingletons.remove(beanName);
	}

	@Override
	public boolean containsSingleton(String beanName) {
		return this.singletonObjects.containsKey(beanName);
	}

	@Override
	public String[] getSingletonNames() {
		return StringUtils.toStringArray(this.registeredSingletons);
	}

	@Override
	public int getSingletonCount() {
		return this.registeredSingletons.size();
	}


	public void setCurrentlyInCreation(String beanName, boolean inCreation) {
		Assert.notNull(beanName, "Bean name must not be null");
		if (!inCreation) {
			this.inCreationCheckExclusions.add(beanName);
		}
		else {
			this.inCreationCheckExclusions.remove(beanName);
		}
	}

	public boolean isCurrentlyInCreation(String beanName) {
		Assert.notNull(beanName, "Bean name must not be null");
		return (!this.inCreationCheckExclusions.contains(beanName) && isActuallyInCreation(beanName));
	}

	protected boolean isActuallyInCreation(String beanName) {
		return isSingletonCurrentlyInCreation(beanName);
	}

	/**
	 * Return whether the specified singleton bean is currently in creation
	 * (within the entire factory).
	 * @param beanName the name of the bean
	 */
	public boolean isSingletonCurrentlyInCreation(@Nullable String beanName) {
		return this.singletonsCurrentlyInCreation.contains(beanName);
	}

	/**
	 * Callback before singleton creation.
	 * <p>The default implementation register the singleton as currently in creation.
	 * @param beanName the name of the singleton about to be created
	 * @see #isSingletonCurrentlyInCreation
	 */
	protected void beforeSingletonCreation(String beanName) {
		if (!this.inCreationCheckExclusions.contains(beanName) && !this.singletonsCurrentlyInCreation.add(beanName)) {
			throw new BeanCurrentlyInCreationException(beanName);
		}
	}

	/**
	 * Callback after singleton creation.
	 * <p>The default implementation marks the singleton as not in creation anymore.
	 * @param beanName the name of the singleton that has been created
	 * @see #isSingletonCurrentlyInCreation
	 */
	protected void afterSingletonCreation(String beanName) {
		if (!this.inCreationCheckExclusions.contains(beanName) && !this.singletonsCurrentlyInCreation.remove(beanName)) {
			throw new IllegalStateException("Singleton '" + beanName + "' isn't currently in creation");
		}
	}


	/**
	 * Add the given bean to the list of disposable beans in this registry.
	 * <p>Disposable beans usually correspond to registered singletons,
	 * matching the bean name but potentially being a different instance
	 * (for example, a DisposableBean adapter for a singleton that does not
	 * naturally implement Spring's DisposableBean interface).
	 * @param beanName the name of the bean
	 * @param bean the bean instance
	 */
	public void registerDisposableBean(String beanName, DisposableBean bean) {
		synchronized (this.disposableBeans) {
			this.disposableBeans.put(beanName, bean);
		}
	}

	/**
	 * Register a containment relationship between two beans,
	 * for example, between an inner bean and its containing outer bean.
	 * <p>Also registers the containing bean as dependent on the contained bean
	 * in terms of destruction order.
	 * @param containedBeanName the name of the contained (inner) bean
	 * @param containingBeanName the name of the containing (outer) bean
	 * @see #registerDependentBean
	 */
	public void registerContainedBean(String containedBeanName, String containingBeanName) {
		synchronized (this.containedBeanMap) {
			Set<String> containedBeans =
					this.containedBeanMap.computeIfAbsent(containingBeanName, k -> new LinkedHashSet<>(8));
			if (!containedBeans.add(containedBeanName)) {
				return;
			}
		}
		registerDependentBean(containedBeanName, containingBeanName);
	}

	/**
	 * Register a dependent bean for the given bean,
	 * to be destroyed before the given bean is destroyed.
	 * @param beanName the name of the bean
	 * @param dependentBeanName the name of the dependent bean
	 */
	public void registerDependentBean(String beanName, String dependentBeanName) {
		String canonicalName = canonicalName(beanName);

		synchronized (this.dependentBeanMap) {
			Set<String> dependentBeans =
					this.dependentBeanMap.computeIfAbsent(canonicalName, k -> new LinkedHashSet<>(8));
			if (!dependentBeans.add(dependentBeanName)) {
				return;
			}
		}

		synchronized (this.dependenciesForBeanMap) {
			Set<String> dependenciesForBean =
					this.dependenciesForBeanMap.computeIfAbsent(dependentBeanName, k -> new LinkedHashSet<>(8));
			dependenciesForBean.add(canonicalName);
		}
	}

	/**
	 * Determine whether the specified dependent bean has been registered as
	 * dependent on the given bean or on any of its transitive dependencies.
	 * @param beanName the name of the bean to check
	 * @param dependentBeanName the name of the dependent bean
	 * @since 4.0
	 */
	protected boolean isDependent(String beanName, String dependentBeanName) {
		synchronized (this.dependentBeanMap) {
			return isDependent(beanName, dependentBeanName, null);
		}
	}

	private boolean isDependent(String beanName, String dependentBeanName, @Nullable Set<String> alreadySeen) {
		if (alreadySeen != null && alreadySeen.contains(beanName)) {
			return false;
		}
		String canonicalName = canonicalName(beanName);
		Set<String> dependentBeans = this.dependentBeanMap.get(canonicalName);
		if (dependentBeans == null || dependentBeans.isEmpty()) {
			return false;
		}
		if (dependentBeans.contains(dependentBeanName)) {
			return true;
		}
		if (alreadySeen == null) {
			alreadySeen = new HashSet<>();
		}
		alreadySeen.add(beanName);
		for (String transitiveDependency : dependentBeans) {
			if (isDependent(transitiveDependency, dependentBeanName, alreadySeen)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Determine whether a dependent bean has been registered for the given name.
	 * @param beanName the name of the bean to check
	 */
	protected boolean hasDependentBean(String beanName) {
		return this.dependentBeanMap.containsKey(beanName);
	}

	/**
	 * Return the names of all beans which depend on the specified bean, if any.
	 * @param beanName the name of the bean
	 * @return the array of dependent bean names, or an empty array if none
	 */
	public String[] getDependentBeans(String beanName) {
		Set<String> dependentBeans = this.dependentBeanMap.get(beanName);
		if (dependentBeans == null) {
			return new String[0];
		}
		synchronized (this.dependentBeanMap) {
			return StringUtils.toStringArray(dependentBeans);
		}
	}

	/**
	 * Return the names of all beans that the specified bean depends on, if any.
	 * @param beanName the name of the bean
	 * @return the array of names of beans which the bean depends on,
	 * or an empty array if none
	 */
	public String[] getDependenciesForBean(String beanName) {
		Set<String> dependenciesForBean = this.dependenciesForBeanMap.get(beanName);
		if (dependenciesForBean == null) {
			return new String[0];
		}
		synchronized (this.dependenciesForBeanMap) {
			return StringUtils.toStringArray(dependenciesForBean);
		}
	}

	public void destroySingletons() {
		if (logger.isTraceEnabled()) {
			logger.trace("Destroying singletons in " + this);
		}
		this.singletonsCurrentlyInDestruction = true;

		String[] disposableBeanNames;
		synchronized (this.disposableBeans) {
			disposableBeanNames = StringUtils.toStringArray(this.disposableBeans.keySet());
		}
		for (int i = disposableBeanNames.length - 1; i >= 0; i--) {
			destroySingleton(disposableBeanNames[i]);
		}

		this.containedBeanMap.clear();
		this.dependentBeanMap.clear();
		this.dependenciesForBeanMap.clear();

		this.singletonLock.lock();
		try {
			clearSingletonCache();
		}
		finally {
			this.singletonLock.unlock();
		}
	}

	/**
	 * Clear all cached singleton instances in this registry.
	 * @since 4.3.15
	 */
	protected void clearSingletonCache() {
		this.singletonObjects.clear();
		this.singletonFactories.clear();
		this.earlySingletonObjects.clear();
		this.registeredSingletons.clear();
		this.singletonsCurrentlyInDestruction = false;
	}

	/**
	 *发生移除，销毁当前bean的所有信息
	 * @param beanName bean的名称
	 * @see #destroyBean
	 */
	public void destroySingleton(String beanName) {
		// Destroy the corresponding DisposableBean instance.
		// This also triggers the destruction of dependent beans.
		DisposableBean disposableBean;
		//从缓存中移除当前bean的相关信息,由于不知道在哪里发生异常，所以我们把跟当前bean的所有缓存记录都清除
		synchronized (this.disposableBeans) {
			disposableBean = this.disposableBeans.remove(beanName);
		}
		//创建一个变量用于接受 实现了DisposableBean接口的对象变量
		//进行bean的销毁
		destroyBean(beanName, disposableBean);

		// destroySingletons() removes all singleton instances at the end,
		// leniently tolerating late retrieval during the shutdown phase.
		if (!this.singletonsCurrentlyInDestruction) {
			// For an individual destruction, remove the registered instance now.
			// As of 6.2, this happens after the current bean's destruction step,
			// allowing for late bean retrieval by on-demand suppliers etc.
			this.singletonLock.lock();
			try {
				removeSingleton(beanName);
			}
			finally {
				this.singletonLock.unlock();
			}
		}
	}

	/**
	 * 	销毁bean的依赖关系
	 * Destroy the given bean. Must destroy beans that depend on the given
	 * bean before the bean itself. Should not throw any exceptions.
	 * @param beanName the name of the bean
	 * @param bean the bean instance to destroy
	 */
	protected void destroyBean(String beanName, @Nullable DisposableBean bean) {
		// Trigger destruction of dependent beans first...
		//销毁dependentBeanMap中保存的是当前bean和依赖bean之间的映射
		Set<String> dependentBeanNames;
		synchronized (this.dependentBeanMap) {
			// Within full synchronization in order to guarantee a disconnected Set
			//把当前创建dependon 依赖的bean从缓存中移除并且返回处理
			dependentBeanNames = this.dependentBeanMap.remove(beanName);
		}
		if (dependentBeanNames != null) {
			if (logger.isTraceEnabled()) {
				logger.trace("Retrieved dependent beans for bean '" + beanName + "': " + dependentBeanNames);
			}
			//递归销毁bean
			for (String dependentBeanName : dependentBeanNames) {
				destroySingleton(dependentBeanName);
			}
		}

		// Actually destroy the bean now...
		// 真正的调用bean的destory()方法
		if (bean != null) {
			try {
				bean.destroy();
			}
			catch (Throwable ex) {
				if (logger.isWarnEnabled()) {
					logger.warn("Destruction of bean with name '" + beanName + "' threw an exception", ex);
				}
			}
		}

		// Trigger destruction of contained beans...
		Set<String> containedBeans;
		synchronized (this.containedBeanMap) {
			// Within full synchronization in order to guarantee a disconnected Set
			containedBeans = this.containedBeanMap.remove(beanName);
		}
		if (containedBeans != null) {
			for (String containedBeanName : containedBeans) {
				destroySingleton(containedBeanName);
			}
		}

		// Remove destroyed bean from other beans' dependencies.
		synchronized (this.dependentBeanMap) {
			for (Iterator<Map.Entry<String, Set<String>>> it = this.dependentBeanMap.entrySet().iterator(); it.hasNext();) {
				Map.Entry<String, Set<String>> entry = it.next();
				Set<String> dependenciesToClean = entry.getValue();
				dependenciesToClean.remove(beanName);
				if (dependenciesToClean.isEmpty()) {
					it.remove();
				}
			}
		}

		// Remove destroyed bean's prepared dependency information.
		this.dependenciesForBeanMap.remove(beanName);
	}

	@Deprecated(since = "6.2")
	@Override
	public final Object getSingletonMutex() {
		return new Object();
	}

}
