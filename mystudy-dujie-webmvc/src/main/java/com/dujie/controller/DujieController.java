package com.dujie.controller;

import com.dujie.initbinder.User;
import com.dujie.service.DujieServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.propertyeditors.CustomDateEditor;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by dujie on 2019/7/31.
 */
@RestController
public class DujieController {

	@Autowired
	private DujieServiceImpl dujieServiceImpl;

	@RequestMapping(value = {"/angle"})
	public String testDujie(HttpServletRequest httpServletRequest) {
		System.out.println("URL:"+httpServletRequest.getRequestURL());
		System.out.println("URI:"+httpServletRequest.getRequestURI());
		System.out.println("contextPath:"+httpServletRequest.getContextPath());
		System.out.println("serlvetPath:"+httpServletRequest.getServletPath());
		ServletContext servletContext = httpServletRequest.getServletContext();
		dujieServiceImpl.sayHello();
		return "dujie";
	}

	@RequestMapping(value = {"/dujie"})
	public String testAngle(HttpServletRequest httpServletRequest) {
		ServletContext servletContext = httpServletRequest.getServletContext();
		return "dujie";
	}

	@RequestMapping("/returnJson")
	public Object returnJson() {
		Map<String,String> retMap = new HashMap<>();
		retMap.put("name","张三");
		return retMap;
	}

	@RequestMapping("/testQuestPram")
	public String testRequestParam(@RequestParam("${name}") String name) {
		System.out.println("name="+name);
		return name;
	}

	public DujieController() {
		System.out.println("dujieController 执行构造方法");
	}


	@RequestMapping("/initbinder/user")
	public User getFormatData(User user) {
		System.out.println("user:"+user.toString());
		return user;
	}

	/**
	 * 作用于单个controller
	 * WebDataBinder 的作用
	 * @param webDataBinder
	 */
	@InitBinder
	public void initWebBinderDataFormatter(WebDataBinder webDataBinder) {
		//作用一:加入类型转化器
		DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		CustomDateEditor dateEditor = new CustomDateEditor(df, true);

		webDataBinder.registerCustomEditor(Date.class,dateEditor);
	}
}
