package com.dujie;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.scheduling.concurrent.CustomizableThreadFactory;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Configuration
@EnableAspectJAutoProxy(exposeProxy = true)
public class MainConfig {

	@Bean
	public Calculate calculate() {
		return new MyCalculate();
	}

	@Bean
	public MyLogAspect myLogAspect() {
		return new MyLogAspect();
	}

	@Bean(value = "commonThreadPool", destroyMethod = "shutdown")
	public ExecutorService commonExecutorService() {
		ThreadFactory namedThreadFactory = new CustomizableThreadFactory("common-thread-");
		return new ThreadPoolExecutor(5, 8, 0L, TimeUnit.MILLISECONDS, new ArrayBlockingQueue<>(10000),
				namedThreadFactory, new ThreadPoolExecutor.CallerRunsPolicy());
	}
}
