package com.mybatis.v1.anno;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * [来个全套]
 *
 * @Description: TODO
 * @date 2020/5/5 13:53
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface MySelect {

	String value();
}
