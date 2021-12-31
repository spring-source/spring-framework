package com.mybatis.v3.compent;

import com.mybatis.v3.anno.EnableMapperScanner;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.type.AnnotationMetadata;

/**
 * [来个全套]
 *
 * @slogan: 高于生活，源于生活
 * @Description: TODO
 * @author: dujie
 * @date 2020/5/5 14:29
 */
public class MyImportBeanDefinitionRegister implements ImportBeanDefinitionRegistrar {


	@Override
	public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {

		AnnotationAttributes attributes = (AnnotationAttributes) importingClassMetadata.getAnnotationAttributes(EnableMapperScanner.class.getName());

		//配置了EnableMapperScanner 注解
		if (attributes == null) {
			return;
		}

		BeanDefinitionBuilder beanDefinitionBuilder = BeanDefinitionBuilder.genericBeanDefinition(MyMapperRegister.class);

		beanDefinitionBuilder.addPropertyValue("basePackage", attributes.getString("basePackage"));

		registry.registerBeanDefinition(MyMapperRegister.class.getName(), beanDefinitionBuilder.getBeanDefinition());

	}
}
