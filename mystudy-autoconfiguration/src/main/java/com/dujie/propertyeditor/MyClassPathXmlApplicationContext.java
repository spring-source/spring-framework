package com.dujie.propertyeditor;

import org.springframework.context.support.ClassPathXmlApplicationContext;

public class MyClassPathXmlApplicationContext extends ClassPathXmlApplicationContext {
	public MyClassPathXmlApplicationContext(String... configLocations){
		super(configLocations);
	}
	@Override
	protected void initPropertySources() {
		super.initPropertySources();
		getEnvironment().setRequiredProperties("VAR");
	}
}
