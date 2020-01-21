package com.lagou.edu.utils;

import com.alibaba.druid.pool.DruidDataSource;

/**
 * @author 应癫
 */
public class DruidUtils {

    private DruidUtils(){
    }

    private static DruidDataSource druidDataSource = new DruidDataSource();


    static {
        druidDataSource.setDriverClassName("com.mysql.jdbc.Driver");
        druidDataSource.setUrl("jdbc:mysql:///bank?characterEncoding=utf8");
        druidDataSource.setUsername("root");
        druidDataSource.setPassword("root123456");

    }

    public static DruidDataSource getInstance() {
        return druidDataSource;
    }

}
