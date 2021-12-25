package com.dujie.config;


import com.dujie.compent.TulingDataSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan(basePackages = {"com.dujie.compent"})
public class MainConfig {


	@Bean
	public TulingDataSource tulingDataSource() {
		TulingDataSource tulingDataSource = new TulingDataSource();
		tulingDataSource.setFlag(1);
		return tulingDataSource;
	}

	@Bean
	public TulingDataSource tulingDataSource2() {
		TulingDataSource tulingDataSource = new TulingDataSource();
		tulingDataSource.setFlag(2);
		return tulingDataSource;
	}
}
