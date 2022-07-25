package com.dujie.v3;

import com.dujie.v1.UserDao;
import com.dujie.v2.OrderDao;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class MainApp {

	public static void main(String[] args) {
		//获取 ApplicationContext 容器
		ApplicationContext context = new ClassPathXmlApplicationContext("beans2.xml");
		//获取代理对象
		UserDao userDao = context.getBean("userDao", UserDao.class);
		//获取代理对象
		OrderDao orderDao = context.getBean("orderDao", OrderDao.class);

		//调用 UserDao 中的各个方法
		userDao.add();
		userDao.delete();
		userDao.get();
		userDao.modify();

		//调用 OrderDao 中的各个方法
		orderDao.add();
		orderDao.adds();
		orderDao.delete();
		orderDao.get();
		orderDao.modify();
	}
}