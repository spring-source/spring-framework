package com.dujie;

import com.dujie.config.MainConfig;
import com.dujie.service.PayService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import javax.sql.DataSource;
//import javax.transaction.HeuristicMixedException;
//import javax.transaction.HeuristicRollbackException;
//import javax.transaction.InvalidTransactionException;
//import javax.transaction.NotSupportedException;
//import javax.transaction.RollbackException;
//import javax.transaction.SystemException;
//import javax.transaction.Transaction;
//import javax.transaction.TransactionManager;
//import javax.transaction.UserTransaction;
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
//			transferAccountCloud(dataSource);
			System.out.println("事务测试步骤");
			payService.pay("123456789", 10);

		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("查询失败");
		}

	}

	/**
	 * @return
	 * @Description: 手动提交事务demo
	 * @Param [dataSource]
	 * @Author: 渡劫 dujie
	 * @Date: 2022/7/12 10:57 AM
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

	/**
	 * @return
	 * @Description: 分布式事务
	 * @Param [dataSource]
	 * @Author: 渡劫 dujie
	 * @Date: 2022/7/12 11:08 AM
	 */
//	public static void transferAccountCloud(DataSource dataSource) throws SQLException, SystemException {
//		UserTransaction userTx = null;
//		Connection connA = null;
//		Statement stmtA = null;
//		Connection connB = null;
//		Statement stmtB = null;
//		try {
//			// 获得 Transaction 管理对象
//
//			userTx = new UserTransactionAdapter(new TransactionManager() {
//				@Override
//				public void begin() throws NotSupportedException, SystemException {
//
//				}
//
//				@Override
//				public void commit() throws RollbackException, HeuristicMixedException, HeuristicRollbackException, SecurityException, IllegalStateException, SystemException {
//
//				}
//
//				@Override
//				public int getStatus() throws SystemException {
//					return 0;
//				}
//
//				@Override
//				public Transaction getTransaction() throws SystemException {
//					return null;
//				}
//
//				@Override
//				public void resume(Transaction tobj) throws InvalidTransactionException, IllegalStateException, SystemException {
//
//				}
//
//				@Override
//				public void rollback() throws IllegalStateException, SecurityException, SystemException {
//
//				}
//
//				@Override
//				public void setRollbackOnly() throws IllegalStateException, SystemException {
//
//				}
//
//				@Override
//				public void setTransactionTimeout(int seconds) throws SystemException {
//
//				}
//
//				@Override
//				public Transaction suspend() throws SystemException {
//					return null;
//				}
//			});
//			connA = dataSource.getConnection();// 从数据库 A 中取得数据库连接
//			connB = dataSource.getConnection();// 从数据库 B 中取得数据库连接
//			userTx.begin(); // 启动事务
//			stmtA = connA.createStatement();// 将 A 账户中的金额减少 500
//			// 将 A 账户中的金额减少 500
//			stmtA.execute("update account_info set blance = blance + 500 where ID = 3");
//			// 将 B 账户中的金额增加 500
//			stmtB = connB.createStatement();
//			stmtB.execute("update account_info set blance = blance - 500 where ID = 4");
//			// 将 B 账户中的金额增加 500
//			userTx.commit();// 提交事务
//			// 事务提交：转账的两步操作同时成功（数据库 A 和数据库 B 中的数据被同时更新）
//		} catch (SQLException sqle) {
//			// 发生异常，回滚在本事务中的操纵
//			userTx.rollback();// 事务回滚：数据库 A 和数据库 B 中的数据更新被同时撤销
//		} catch (Exception ne) {
//		}
//	}

}
