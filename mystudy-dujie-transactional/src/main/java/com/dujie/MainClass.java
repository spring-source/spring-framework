package com.dujie;

import com.dujie.config.MainConfig;
import com.dujie.service.PayService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

/**
 * Created by dujie on 2019/6/17.
 */
@Slf4j
public class MainClass {

    public static void main(String[] args) {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(MainConfig.class);

        PayService payService = (PayService) context.getBean("payServiceImpl");
		try {
			payService.pay("123456789",10);
			System.out.println("事务测试步骤");
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("查询失败");
		}

	}
}
