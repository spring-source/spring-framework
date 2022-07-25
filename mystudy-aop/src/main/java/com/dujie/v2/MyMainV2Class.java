package com.dujie.v2;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;


public class MyMainV2Class {

	/***
	 * @Description: 已经执行过可以debug
	 * @Param [args]
	 * @return
	 */
	public static void main(String[] args) {
		//获取 ApplicationContext 容器
		ApplicationContext context = new ClassPathXmlApplicationContext("classpath:beans.xml");
		//获取代理对象
		OrderDao orderDao = context.getBean("orderDaoProxy", OrderDao.class);
		//调用 OrderDao 中的各个方法
		orderDao.add();
		orderDao.adds();
		orderDao.delete();
		orderDao.get();
		orderDao.modify();
	}
}
