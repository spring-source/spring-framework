package com.dujie.web;

import com.dujie.config.DujieSpringbootConfig;
import org.springframework.web.WebApplicationInitializer;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.DispatcherServlet;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRegistration;

/**
 * 在tomcat源码中:
 *  org.apache.catalina.startup.ContextConfig#lifecycleEvent(org.apache.catalina.LifecycleEvent
 *  	org.apache.catalina.startup.ContextConfig#configureStart
 *          org.apache.catalina.startup.ContextConfig#webConfig
 *             org.apache.catalina.startup.ContextConfig#processServletContainerInitializers
 *
 *	 //通过spi的机制加载 classpath下ServletContainerInitializer
 *	 WebappServiceLoader<ServletContainerInitializer> loader = new WebappServiceLoader<>(context);
 detectedScis = loader.load(ServletContainerInitializer.class);
 *
 * Created by dujie on 2019/7/31.
 */
public class DujieWebApplicationInitializer implements WebApplicationInitializer {

    @Override
	public void onStartup(ServletContext servletContext) throws ServletException {
		System.out.println("初始化DujieWebApplicationInitializer...");

        AnnotationConfigWebApplicationContext context = new AnnotationConfigWebApplicationContext();
        context.register(DujieSpringbootConfig.class);

        context.setServletContext(servletContext);

        context.refresh();


        //注册dispatcherServlet
        DispatcherServlet dispatcherServlet = new DispatcherServlet(context);
        ServletRegistration.Dynamic registration = servletContext.addServlet("dispatcherServlet",dispatcherServlet);
        registration.setLoadOnStartup(1);
        registration.addMapping("/");
    }
}
