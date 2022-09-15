package com.dujie.version3.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * 自动装配读取器
 * Created by dujie on 2019/9/17.
 */
public class DujieAutoCfgReader {

	public static Properties readerProperties(String resource) {
		Properties properties = new Properties();
		InputStream inputStream = DujieAutoCfgReader.class.getResourceAsStream(resource);
		try {
			properties.load(inputStream);
		} catch (IOException e) {
			System.out.println("读取配置文件异常"+e.getMessage());
		}
		return properties;
	}

}
