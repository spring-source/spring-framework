package com.dujie.service;

import org.springframework.stereotype.Service;

/**
 * Created by dujie on 2019/7/31.
 */
@Service("dujieServiceImpl")
public class DujieServiceImpl {

	public void sayHello() {
		System.out.println("dujieServiceImpl.....sayHello");
	}

	public DujieServiceImpl() {
		System.out.println("dujieServiceImpl");
	}

}
