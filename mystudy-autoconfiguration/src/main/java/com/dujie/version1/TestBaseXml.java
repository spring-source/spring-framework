package com.dujie.version1;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.data.redis.core.RedisTemplate;

@Slf4j
public class TestBaseXml {

	public static void main(String[] args) {
		/**
		 * 1、初始化前的准备工作
		 * 2、初始化BeanFactory 并进行 XML文件读取
		 * 3、对BeanFactory进行功能属性的填充
		 * 4、子类覆盖方法需要做额外处理
		 * 5、激活各种BeanFactory处理器
		 * 6、注册拦截bean创建的bean处理器，仅注册，调用在getBean的时候
		 * 7、为上下文初始化message园
		 * 8、初始化应用消息广播器
		 * 9、留给子类来初始化其他的Bean
		 * 10、在所有的注册的Bean中查找listenerBean 注册到消息广播器中
		 * 11、初始化剩下的单实例
		 * 12、完成刷新过程，通知生命周期处理器lifecycleProcessor 刷新过程 同时发出 Context—RefreshEvent通知别人
		 * TODO: MyClassPathXmlApplicationContext 为自定义的环境变量测试环境变量校验方法
		 */
		ClassPathXmlApplicationContext context =
				new ClassPathXmlApplicationContext("classpath:spring/spring-redis.xml");
//		ClassPathXmlApplicationContext context =
//				new MyClassPathXmlApplicationContext("classpath:spring/spring-redis.xml");
		RedisTemplate redisTemplate = (RedisTemplate) context.getBean("redisTemplate");

		redisTemplate.opsForValue().set("redisXmlTest","原生自动引入redis的xml配置");

		System.out.println("从redis获取===="+redisTemplate.opsForValue().get("redisXmlTest"));
	}

}
