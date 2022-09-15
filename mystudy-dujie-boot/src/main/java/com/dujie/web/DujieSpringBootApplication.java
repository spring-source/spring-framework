package com.dujie.web;

import org.apache.catalina.startup.Tomcat;

/**
 * Created by dujie on 2019/8/18.
 */
public class DujieSpringBootApplication {

    public static void run() {

        Tomcat tomcat = new Tomcat();

        tomcat.setPort(8080);

        try {
            tomcat.addWebapp("/","/Users/mac/logs");

            tomcat.start();

            tomcat.getServer().await();
        }catch (Exception e) {
			System.out.println("容器启动失败");
		}
    }

}
