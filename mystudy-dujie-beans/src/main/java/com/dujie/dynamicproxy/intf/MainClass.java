package com.dujie.dynamicproxy.intf;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;


public class MainClass {
	/***
	 * @Description: 已经执行过可以debug
	 * @Param [args]
	 * @return
	 */
	public static void main(String[] args) {
		AnnotationConfigApplicationContext context =
				new AnnotationConfigApplicationContext(MainCofig.class);
		UserInfoMapper userInfoMapper = (UserInfoMapper) context.getBean("userInfoMapper");
		System.out.println("UserInfoMapper的类型:"+userInfoMapper.getClass());
		userInfoMapper.UserInfo(1);

	}
}
