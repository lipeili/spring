package com.lagou.edu.listener;

import com.lagou.edu.factory.BeanFactory;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

/**
 * @Description TODO
 * @Date 2020-01-16 23:51
 * @Created by videopls
 */
public class SpringInit implements ServletContextListener {

    @Override
    public void contextInitialized(ServletContextEvent servletContextEvent) {
//        BeanFactory
    }

    @Override
    public void contextDestroyed(ServletContextEvent servletContextEvent) {

    }
}
