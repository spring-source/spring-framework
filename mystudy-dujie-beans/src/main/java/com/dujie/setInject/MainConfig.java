package com.dujie.setInject;


import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Created by dujie on 2019/8/22.
 */
@ComponentScan(basePackages = {"com.dujie.setInject"})
//@Import(value = {InstC.class,DujieImportBeanDefinitionRegister.class,DujieImportSelector.class})
//@Import(value = {InstC.class})
//@Import(value = {DujieImportBeanDefinitionRegister.class})
@Import(value = {DujieImportSelector.class})
@Configuration
public class MainConfig {


}
