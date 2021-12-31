package com.mybatis.v3.compent;

import com.mybatis.v2.factorybean.MyMapperFactorybean;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.beans.factory.support.GenericBeanDefinition;

import java.util.Set;

/**
 * [来个全套]
 *
 * @slogan: 高于生活，源于生活
 * @Description: TODO
 * @author: dujie
 * @date 2020/5/5 15:12
 */
public class MyMapperRegister implements BeanDefinitionRegistryPostProcessor {

	private static Class targetClass = MyMapperFactorybean.class;
	private String basePackage;

	@Override
	public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
		//扫描bean定义
		MyClassPathMapperScanner mapperScanner = new MyClassPathMapperScanner(registry);

		mapperScanner.addIncludeFilter((metadataReader, metadataReaderFactory) -> true);


		Set<BeanDefinitionHolder> scannedBds = mapperScanner.doScan(this.basePackage);

		for (BeanDefinitionHolder bdh : scannedBds) {

			GenericBeanDefinition beanDefinition = (GenericBeanDefinition) bdh.getBeanDefinition();
			String sourceClass = beanDefinition.getBeanClassName();
			System.out.println("原生接口的class类型:" + sourceClass);
			beanDefinition.setBeanClass(targetClass);
			beanDefinition.getConstructorArgumentValues().addGenericArgumentValue(sourceClass);
		}

		System.out.println(scannedBds);
	}

	@Override
	public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {

	}
}
