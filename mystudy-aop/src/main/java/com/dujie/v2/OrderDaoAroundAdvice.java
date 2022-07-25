package com.dujie.v2;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
/**
* 增强代码
* 环绕增强
*
* @author c语言中文网 c.biancheng.net
*/
public class OrderDaoAroundAdvice implements MethodInterceptor {
    @Override
    public Object invoke(MethodInvocation methodInvocation) throws Throwable {
        System.out.println("环绕增强前********");
        //执行被代理对象中的逻辑
        Object result = methodInvocation.proceed();
        System.out.println("环绕增强后********");
        return result;
    }
}