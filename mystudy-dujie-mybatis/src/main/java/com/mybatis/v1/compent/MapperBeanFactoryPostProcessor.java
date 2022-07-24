package com.mybatis.v1.compent;

import com.mybatis.v1.factorybean.AccountMapperFactoryBean;
import com.mybatis.v1.factorybean.ProductMapperFactoryBean;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.stereotype.Component;

/**
 * [来个全套]
 *
 * @slogan: 高于生活，源于生活
 * @Description: TODO
 * @author: dujie
 * @date 2020/5/5 13:31
 */
@Component
public class MapperBeanFactoryPostProcessor implements BeanFactoryPostProcessor {
	@Override
	public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) {

		GenericBeanDefinition accountMapperBd = (GenericBeanDefinition) beanFactory.getBeanDefinition("accountMapper");
		accountMapperBd.setBeanClass(AccountMapperFactoryBean.class);
		System.out.println("accountMapperBd:" + accountMapperBd.toString());

		GenericBeanDefinition productMapperBd = (GenericBeanDefinition) beanFactory.getBeanDefinition("productMapper");
		productMapperBd.setBeanClass(ProductMapperFactoryBean.class);
		System.out.println("productMapperBd:" + productMapperBd.toString());

	}
}
