package com.mybatis.v2.factorybean;

import com.mybatis.v2.anno.MySelect;
import com.mybatis.v2.entity.AccountInfo;
import com.mybatis.v2.entity.ProductInfo;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.lang.Nullable;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;

/**
 * [来个全套]
 *
 * @slogan: 高于生活，源于生活
 * @Description: TODO
 * @author: dujie
 * @date 2020/5/5 14:44
 */
public class MyMapperFactorybean<T> implements FactoryBean<T> {

	private Class<T> targetClass;

	public MyMapperFactorybean(Class<T> targetClass) {
		this.targetClass = targetClass;
	}

	@Nullable
	@Override
	public T getObject() throws Exception {
		return (T) Proxy.newProxyInstance(targetClass.getClassLoader(), new Class[]{targetClass}, new MyMapperProxy());
	}

	@Nullable
	@Override
	public Class<?> getObjectType() {
		return targetClass;
	}

	@Override
	public boolean isSingleton() {
		return true;
	}
}

class MyMapperProxy implements InvocationHandler {

	@Override
	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
		//处理Object原生的方法
		if (method.getDeclaringClass().equals(Object.class)) {
			return method.invoke(this, args);
		}

		MySelect mySelect = method.getAnnotation(MySelect.class);
		String parseSql = mySelect.value();
		System.out.println("解析业务sql:" + parseSql + "入参:" + Arrays.asList(args));

		Class<?> clazz = method.getReturnType();
		if (clazz.equals(ProductInfo.class)) {
			return new ProductInfo();
		}
		if (clazz.equals(AccountInfo.class)) {
			return new AccountInfo();
		}
		return null;
	}
}
