package com.dujie;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;


public class MyMainClass {

	/***
	 * @Description: 已经执行过可以debug
	 * @Param [args]
	 * @return
	 */
    public static void main(String[] args) {

    	AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(MainConfig.class);

        Calculate calculate = (Calculate) ctx.getBean("calculate");

		calculate.div(6,2);
    }
}
