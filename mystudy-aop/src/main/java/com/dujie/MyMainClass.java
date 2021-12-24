package com.dujie;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;


public class MyMainClass {

    public static void main(String[] args) {

    	AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(MainConfig.class);

        Calculate calculate = (Calculate) ctx.getBean("calculate");

		calculate.div(6,2);
    }
}
