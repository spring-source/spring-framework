package com.dujie;


import com.dujie.config.MainConfig;
import com.dujie.dao.AccountInfoDao;
import com.dujie.service.UserServiceImpl;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

public class MainClassStarter {

	/***
	 * @Description: 已经执行过可以debug
	 * @Param [args]
	 * @return
	 */
	public static void main(String[] args) {

		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(MainConfig.class);

		AccountInfoDao accountInfoDao = (AccountInfoDao) context.getBean("accountInfoDao");

		accountInfoDao.saveAccountInfo("9988776644",500);

		UserServiceImpl userService = (UserServiceImpl) context.getBean("userServiceImpl");
		//userService.updateBlance(1);
		userService.findUserName(1);
	}

}
