package com.dujie.v1;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;


public class MyMainV1Class {

	/***
	 * @Description: 已经执行过可以debug
	 * @Param [args]
	 * @return
	 */
	public static void main(String[] args) {
		//获取 ApplicationContext 容器
		ApplicationContext context = new ClassPathXmlApplicationContext("classpath:beans.xml");
		//获取代理对象
		UserDao userDao = context.getBean("userDaoProxy", UserDao.class);
		//调用 UserDao 中的各个方法
		userDao.add();
		userDao.delete();
		userDao.get();
		userDao.modify();
	}
}
