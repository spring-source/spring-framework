package com.dujie;

import com.dujie.config.MainConfig;
import com.dujie.service.PayService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Created by dujie on 2019/6/17.
 */
@Slf4j
public class MainClass {


	public static void main(String[] args) {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(MainConfig.class);

		PayService payService = (PayService) context.getBean("payServiceImpl");
		DataSource dataSource = (DataSource) context.getBean("dataSource");

		try {
			transferAccount(dataSource);
			System.out.println("事务测试步骤");
			payService.pay("123456789", 10);

		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("查询失败");
		}

	}

	/**
	 * @Description: 手动提交事务demo
	 * @Param [dataSource]
	 * @Author: 渡劫 dujie
	 * @Date: 2022/7/12 10:57 AM
	 * @return
	 */
	public static void transferAccount(DataSource dataSource) throws SQLException {
		Connection conn = null;
		Statement stmt = null;
		try {
			conn = dataSource.getConnection();
			// 将自动提交设置为 false，若设置为 true 则数据库将会把每一次数据更新认定为一个事务并自动提交
			conn.setAutoCommit(false);
			stmt = conn.createStatement();
			// 将 A 账户中的金额减少 500
			stmt.execute("update account_info set blance = blance - 500 where ID = 3");
			// 将 B 账户中的金额增加 500
			stmt.execute("update account_info set blance = blance + 500 where ID = 4");
			// 提交事务
			conn.commit();
			// 事务提交：转账的两步操作同时成功
		} catch (SQLException sqle) {
			// 发生异常，回滚在本事务中的操做
			assert conn != null;
			conn.rollback();
			// 事务回滚：转账的两步操作完全撤销
			assert stmt != null;
			stmt.close();
			conn.close();
		}
	}
}
