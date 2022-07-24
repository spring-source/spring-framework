package com.dujie;

import com.dujie.config.MainConfig;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

public class MainStart {

	/***
	 * @Description: 已经执行过可以debug
	 * @Param [args]
	 * @return
	 */
	public static void main(String[] args) {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(MainConfig.class);
		ctx.getBean("tulingDao");
		/*System.out.println(ctx.getBean(TulingDao.class).getTulingDataSource().getFlag());*/
	}
}
