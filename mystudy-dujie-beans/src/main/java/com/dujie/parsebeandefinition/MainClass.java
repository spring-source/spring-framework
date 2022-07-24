package com.dujie.parsebeandefinition;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;

public class MainClass {
	/***
	 * @Description: 已经执行过可以debug
	 * @Param [args]
	 * @return
	 */
	public static void main(String[] args) {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(MainConfig.class);
	}
}
