package com.dujie.parsebeandefinition;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Created by dujie on 2019/7/15.
 */
@Configuration
@Import(value = {CompentC.class, DujieImportSelect.class, DujieImportBeanfinitionRegister.class})
@ComponentScan(basePackages = {"com.dujie.parsebeandefinition"})
public class MainConfig {

	@Bean
	public CompentD compentD() {
		return new CompentD();
	}

	@Bean
	public CompentC compentC() {
		return new CompentC();
	}
}
