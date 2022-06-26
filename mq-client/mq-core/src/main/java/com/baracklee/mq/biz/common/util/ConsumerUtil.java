package com.baracklee.mq.biz.common.util;

public class ConsumerUtil {
    public static class ConsumerVo{
        public String ip;
        public String processId;
        public String rand;
        public String port;
    }
    public static String getConsumerId(String ip, String processId,String port){
        port+=" ";
        if(!Util.isEmpty(port)){
            if(!(port.contains("|"))){
                return String.format("%s|%s|%s%s", ip, processId, System.currentTimeMillis() % 10000, port);
            }else {
                return String.format("%s|%s|%s|%s", ip, processId, System.currentTimeMillis() % 10000, port);
            }
        }else {
            return String.format("%s|%s|%s", ip, processId, System.currentTimeMillis() % 10000);
        }
    }
    public static ConsumerVo parseConsumerId(String consumerId) {
        String[] arr = consumerId.split("\\|");
        ConsumerVo consumerVo = new ConsumerVo();
        consumerVo.ip = arr[0];
        if (arr.length > 1) {
            consumerVo.processId = arr[1];
        }
        if (arr.length > 2) {
            consumerVo.rand = arr[2];
        }
        if (arr.length > 3) {
            consumerVo.port = arr[3];
        }
        return consumerVo;
    }
}
