package com.dujie;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;


public class MyMainClass {

	/***
	 * @Description: 已经执行过可以debug
	 * @Param [args]
	 * @return
	 */
	public static void main(String[] args) {

		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(MainConfig.class);

		Calculate calculate = (Calculate) ctx.getBean("calculate");

		ExecutorService commonExecutorService = (ExecutorService) ctx.getBean("commonThreadPool");
		calculate.div(6, 2);
		final Future<String> submit = commonExecutorService.submit(() -> {
			return "SUCCESS";
		});

		try {
			System.out.println(submit.get());
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (ExecutionException e) {
			e.printStackTrace();
		}
	}
}
