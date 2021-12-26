package com.mybatis.v1.config;

import com.mybatis.v2.anno.EnableMapperScanner;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * [来个全套]
 *
 * @date 2020/5/4 15:06
 */
@Configuration
@ComponentScan(value = {"com.mybatis.v1"})
@EnableMapperScanner(basePackage = "com.mybatis.v1.dao")
public class SpringMybatisConfig {

}
