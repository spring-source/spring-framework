package com.dujie;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.util.ArrayList;
import java.util.List;
import java.util.WeakHashMap;
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

		Thread thread = new Thread();
		WeakHashMap<String,String> weakHashMap = new WeakHashMap<>();
		weakHashMap.put("","");
//		List<String> test = new ArrayList<>();
//		test.add("");
//		ExecutorService commonExecutorService = (ExecutorService) ctx.getBean("commonThreadPool");
		calculate.div(6, 2);
		System.out.println(calculate);
//		final Future<String> submit = commonExecutorService.submit(() -> {
//			return "SUCCESS";
//		});
	}
}
