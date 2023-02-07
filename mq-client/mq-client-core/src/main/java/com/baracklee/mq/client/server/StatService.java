package com.baracklee.mq.client.server;

import com.baracklee.mq.biz.common.thread.SoaThreadFactory;
import com.baracklee.mq.biz.common.util.HttpClient;
import com.baracklee.mq.biz.common.util.Util;
import com.baracklee.mq.client.MqClient;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.Server;
import org.apache.catalina.core.StandardServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class StatService {
    private static final Logger logger = LoggerFactory.getLogger(StatService.class);

    private static Thread thread;

    private static AtomicBoolean startFlag= new AtomicBoolean(false);
    private static HttpClient httpClient= new HttpClient(1000,1000);

    private static Server server=null;

    {Runtime.getRuntime().addShutdownHook(new Thread(){
        public void run(){
            close();
        }
    });}

    public static void start(){
        if(startFlag.compareAndSet(false,true)){
            ExecutorService statusCheck = Executors.newSingleThreadExecutor(SoaThreadFactory.create("statusCheck_", false));
            statusCheck.execute(new Runnable() {
                @Override
                public void run() {
                    Util.sleep(60000);
                    checkStatus();
                }
            });
        }

    }

    private static void checkStatus() {
        String port = MqClient.getContext().getConfig().getServerPort();
        int port1 = Integer.parseInt(port);
        String url = "http://localhost:" + port + "/mq/client/hs";
        int count = 0;
        while (count<4){
            if (httpClient.check(url)){return;}
            else {
                count++;
                Util.sleep(1000);
            }
            }
        count=0;
        while (count<10){

            try {
                server=new StandardServer();
                server.setPort(port1);
                server.start();
                logger.warn(port + "端口启动成功");
                break;
            } catch (LifecycleException e) {
                logger.warn(e.getMessage());
            }
        }

    }

    public static void close(){
        if(server!=null){
            try {
                server.stop();
            } catch (LifecycleException ignored) {
            }
        }
    }

}

