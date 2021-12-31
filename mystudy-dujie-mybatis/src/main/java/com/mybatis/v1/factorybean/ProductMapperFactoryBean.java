package com.mybatis.v1.factorybean;

import com.mybatis.v1.anno.MySelect;
import com.mybatis.v1.dao.ProductMapper;
import com.mybatis.v1.entity.ProductInfo;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;

/**
 * [来个全套]
 *
 * @date 2020/5/5 14:04
 */
@Component
public class ProductMapperFactoryBean implements FactoryBean {

	@Nullable
	@Override
	public ProductMapper getObject() {
		return (ProductMapper) Proxy.newProxyInstance(ProductMapper.class.getClassLoader(), new Class[]{ProductMapper.class}, new ProductMapperProxy());
	}

	@Nullable
	@Override
	public Class<?> getObjectType() {
		return ProductMapper.class;
	}
}

class ProductMapperProxy implements InvocationHandler {

	@Override
	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
		//处理Object原生的方法
		if (method.getDeclaringClass().equals(Object.class)) {
			return method.invoke(this, args);
		}

		MySelect tulingSelect = method.getAnnotation(MySelect.class);
		String parseSql = tulingSelect.value();
		System.out.println("解析业务sql:" + parseSql + "入参:" + Arrays.asList(args));

		//模拟查询数据库返回
		Class returnType = method.getReturnType();
		ProductInfo productInfo = (ProductInfo) returnType.newInstance();
		productInfo.setProductName("苹果11");
		productInfo.setStore(99);
		productInfo.setProductId(1);


		return productInfo;
	}
}