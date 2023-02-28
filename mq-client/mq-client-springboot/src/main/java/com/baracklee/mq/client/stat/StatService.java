package com.baracklee.mq.client.stat;

import com.baracklee.mq.biz.common.thread.SoaThreadFactory;
import com.baracklee.mq.biz.common.util.HttpClient;
import com.baracklee.mq.biz.common.util.Util;
import com.baracklee.mq.client.MqClient;
import org.eclipse.jetty.server.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
public class StatService {
    private static final Logger logger = LoggerFactory.getLogger(StatService.class);
    private AtomicBoolean startFlag=new AtomicBoolean(false);
    private HttpClient httpClient= new HttpClient(1000,1000);
    @Resource
    private MqHandler mqHandler;

    public void start(){
        if(startFlag.compareAndSet(false,true)){
            ExecutorService statService = Executors.newSingleThreadExecutor(SoaThreadFactory.create("statService_", true));
            statService.execute(new Runnable() {
                @Override
                public void run() {
                    checkStat();
                }
            });
        }
    }

    private void checkStat() {
        try {
            int port = Integer.parseInt(MqClient.getContext().getConfig().getServerPort());
            String url = "http://localhost:" + port + "/mq/client/hs";
            int count =0;
            while (count<4){
                if(httpClient.check(url)){return;}
                else {count++;
                Util.sleep(1000);}
            }
            count=0;
                Server server = new Server(port);
                server.start();
                logger.warn(port+"端口启动成功");


        } catch (Exception e) {
            logger.error("statService_error",e);
        }
    }


}
