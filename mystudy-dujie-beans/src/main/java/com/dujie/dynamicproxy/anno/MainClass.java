package com.dujie.dynamicproxy.anno;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;

/**
 * Created by dujie on 2020/3/31.
 */
public class MainClass {

	public static void main(String[] args) {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(MyConfig.class);
		IUserInfoService userInfoService = context.getBean(IUserInfoService.class);
		userInfoService.queryUserInfo(1);

	}
}
