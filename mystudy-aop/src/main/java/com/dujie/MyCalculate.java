package com.dujie;


import org.springframework.aop.framework.AopContext;

public class MyCalculate implements Calculate{


    @Override
	public int add(int numA, int numB) {
        System.out.println("执行目标方法:add");
        return numA+numB;
    }

    @Override
	public int reduce(int numA, int numB) {
        System.out.println("执行目标方法:reduce");
        return numA-numB;
    }

    @Override
	public int div(int numA, int numB) {
        System.out.println("执行目标方法:div");

        return numA/numB;
    }

    @Override
	public int multi(int numA, int numB) {
        System.out.println("执行目标方法:multi");

        return numA*numB;
    }

    @Override
	public int mod(int numA, int numB){
        System.out.println("执行目标方法:mod");

		int retVal = ((Calculate) AopContext.currentProxy()).add(numA,numB);

        return retVal%numA;

    }

}
