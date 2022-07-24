package com.mybatis.v1;

import com.mybatis.v1.config.SpringMybatisConfig;
import com.mybatis.v1.dao.AccountMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

/**
 * [来个全套]
 *
 * @slogan: 高于生活，源于生活
 * @date 2020/5/4 15:07
 */
@Slf4j
public class MainTest {

	public static void main(String[] args) {
		AnnotationConfigApplicationContext context
				= new AnnotationConfigApplicationContext(SpringMybatisConfig.class);
		AccountMapper accountMapper = (AccountMapper) context.getBean("accountMapper");
		System.out.println("获取的mapper实例" + accountMapper);
		System.out.println(accountMapper.qryById("11111") + "==================");

	}

}
