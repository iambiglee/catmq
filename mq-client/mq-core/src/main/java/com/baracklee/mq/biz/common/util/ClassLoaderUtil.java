package com.baracklee.mq.biz.common.util;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;

public class ClassLoaderUtil {
    private Logger logger= LoggerFactory.getLogger(this.getClass());

    private static ClassLoader loader= Thread.currentThread().getContextClassLoader();

    private static String classPath="";

    static {
        if(loader==null)
            loader = ClassLoader.getSystemClassLoader();
        try {
            URL url = loader.getResource("");
            //获取classpath
            if(url!=null){
                classPath=url.getPath();
                classPath= URLDecoder.decode(classPath,"utf-8");
            }
            //如果是jar包内，返回单前路径
            if(classPath==null||classPath.length()==0||classPath.contains("jar!")){
                classPath=System.getProperty("user.dir");
            }

        } catch (UnsupportedEncodingException e) {
            classPath=System.getProperty("user.dir");
            e.printStackTrace();
        }
    }

    public static ClassLoader getLoader(){return loader;}
    public static String getClassPath(){return classPath;}
    public static boolean isClassPresent(String className){
        try {
            Class.forName(className);
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
}
