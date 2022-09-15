package com.dujie.config;


import com.dujie.compent.DujieDataSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan(basePackages = {"com.dujie.compent"})
public class MainConfig {


	@Bean
	public DujieDataSource dujieDataSource() {
		DujieDataSource dujieDataSource = new DujieDataSource();
		dujieDataSource.setFlag(1);
		return dujieDataSource;
	}

	@Bean
	public DujieDataSource dujieDataSource1() {
		DujieDataSource dujieDataSource = new DujieDataSource();
		dujieDataSource.setFlag(2);
		return dujieDataSource;
	}
}
