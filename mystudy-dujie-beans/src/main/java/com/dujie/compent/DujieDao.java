package com.dujie.compent;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 测试dujieDao
 * Created by dujie on 2019/7/7.
 */
@Component("dujieDao")
public class DujieDao {

	@Autowired
	private DujieDataSource dujieDataSource;

	public DujieDataSource getdujieDataSource() {
		return dujieDataSource;
	}

	public void setdujieDataSource(DujieDataSource dujieDataSource) {
		this.dujieDataSource = dujieDataSource;
	}

	public DujieDao(DujieDataSource dujieDataSource) {
		this.dujieDataSource = dujieDataSource;
		System.out.println("我是dujieDao的构造方法");
	}

	public DujieDao() {
		System.out.println("我是dujieDao的构造方法");
	}

}
