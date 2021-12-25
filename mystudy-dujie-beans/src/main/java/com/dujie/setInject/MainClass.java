package com.dujie.setInject;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.ClassPathBeanDefinitionScanner;


public class MainClass {
	/***
	 * @Description: 已经执行过可以debug
	 * @Param [args]
	 * @return
	 */
	public static void main(String[] args) {

		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(MainConfig.class);


		ClassPathBeanDefinitionScanner scanner = new ClassPathBeanDefinitionScanner(ctx);


		InstA instA = (InstA) ctx.getBean("instA");

		System.out.println(instA);

	}
}
