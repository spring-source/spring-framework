package com.dujie;

import com.dujie.web.DujieSpringBootApplication;
import org.apache.catalina.LifecycleException;

import javax.servlet.ServletException;

/**
 * Created by dujie on 2019/8/18.
 */


public class MainStarter {

    public static void main(String[] args) throws ServletException, LifecycleException {
        DujieSpringBootApplication.run();
    }
}
