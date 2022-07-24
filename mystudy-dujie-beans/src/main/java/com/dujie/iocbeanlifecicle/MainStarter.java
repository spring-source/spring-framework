package com.dujie.iocbeanlifecicle;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;

public class MainStarter {

	/***
	 * @Description: 已经执行过可以debug
	 * @Param [args]
	 * @return
	 */
	public static void main(String[] args) {

		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(MainConfig.class);

		Person person = (Person) context.getBean("person");

		System.out.println("person:"+person);
	}
}
