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

package org.springframework.test.context.bean.override.mockito;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.mockito.Answers;
import org.mockito.MockSettings;

import org.springframework.core.annotation.AliasFor;
import org.springframework.test.context.bean.override.BeanOverride;

/**
 * {@code @MockitoBean} is an annotation that can be applied to a non-static field
 * in a test class to override a bean in the test's
 * {@link org.springframework.context.ApplicationContext ApplicationContext}
 * using a Mockito mock.
 *
 * <p>By default, the bean to mock is inferred from the type of the annotated
 * field. If multiple candidates exist, a {@code @Qualifier} annotation can be
 * used to help disambiguate. In the absence of a {@code @Qualifier} annotation,
 * the name of the annotated field will be used as a fallback qualifier.
 * Alternatively, you can explicitly specify a bean name to mock by setting the
 * {@link #value() value} or {@link #name() name} attribute.
 *
 * <p>A bean will be created if a corresponding bean does not exist. However, if
 * you would like for the test to fail when a corresponding bean does not exist,
 * you can set the {@link #enforceOverride() enforceOverride} attribute to {@code true}
 * &mdash; for example,  {@code @MockitoBean(enforceOverride = true)}.
 *
 * <p>Dependencies that are known to the application context but are not beans
 * (such as those
 * {@linkplain org.springframework.beans.factory.config.ConfigurableListableBeanFactory#registerResolvableDependency(Class, Object)
 * registered directly}) will not be found, and a mocked bean will be added to
 * the context alongside the existing dependency.
 *
 * <p><strong>NOTE</strong>: Only <em>singleton</em> beans can be mocked.
 * Any attempt to mock a non-singleton bean will result in an exception. When
 * mocking a bean created by a {@link org.springframework.beans.factory.FactoryBean
 * FactoryBean}, the {@code FactoryBean} will be replaced with a singleton mock
 * of the type of object created by the {@code FactoryBean}.
 *
 * <p>There are no restrictions on the visibility of a {@code @MockitoBean} field.
 * Such fields can therefore be {@code public}, {@code protected}, package-private
 * (default visibility), or {@code private} depending on the needs or coding
 * practices of the project.
 *
 * @author Simon Baslé
 * @author Sam Brannen
 * @since 6.2
 * @see org.springframework.test.context.bean.override.mockito.MockitoSpyBean @MockitoSpyBean
 * @see org.springframework.test.context.bean.override.convention.TestBean @TestBean
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@BeanOverride(MockitoBeanOverrideProcessor.class)
public @interface MockitoBean {

	/**
	 * Alias for {@link #name()}.
	 * <p>Intended to be used when no other attributes are needed &mdash; for
	 * example, {@code @MockitoBean("customBeanName")}.
	 * @see #name()
	 */
	@AliasFor("name")
	String value() default "";

	/**
	 * Name of the bean to mock.
	 * <p>If left unspecified, the bean to mock is selected according to the
	 * annotated field's type, taking qualifiers into account if necessary. See
	 * the {@linkplain MockitoBean class-level documentation} for details.
	 * @see #value()
	 */
	@AliasFor("value")
	String name() default "";

	/**
	 * Extra interfaces that should also be declared by the mock.
	 * <p>Defaults to none.
	 * @return any extra interfaces
	 * @see MockSettings#extraInterfaces(Class...)
	 */
	Class<?>[] extraInterfaces() default {};

	/**
	 * The {@link Answers} type to use in the mock.
	 * <p>Defaults to {@link Answers#RETURNS_DEFAULTS}.
	 * @return the answer type
	 */
	Answers answers() default Answers.RETURNS_DEFAULTS;

	/**
	 * Whether the generated mock is serializable.
	 * <p>Defaults to {@code false}.
	 * @return {@code true} if the mock is serializable
	 * @see MockSettings#serializable()
	 */
	boolean serializable() default false;

	/**
	 * The reset mode to apply to the mock.
	 * <p>The default is {@link MockReset#AFTER} meaning that mocks are
	 * automatically reset after each test method is invoked.
	 * @return the reset mode
	 */
	MockReset reset() default MockReset.AFTER;

	/**
	 * Whether to require the existence of the bean being mocked.
	 * <p>Defaults to {@code false} which means that a mock will be created if a
	 * corresponding bean does not exist.
	 * <p>Set to {@code true} to cause an exception to be thrown if a corresponding
	 * bean does not exist.
	 * @see org.springframework.test.context.bean.override.BeanOverrideStrategy#REPLACE_OR_CREATE
	 * @see org.springframework.test.context.bean.override.BeanOverrideStrategy#REPLACE
	 */
	boolean enforceOverride() default false;

}
