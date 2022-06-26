package com.baracklee.mq.biz.common.util;

public class ConsumerGroupUtil {
    public static String getBroadcastConsumerName(String consumerGroupName, String ip,long consumerId){
        return String.format("%s_%s-%s", consumerGroupName, ip, consumerId);
    }
    public static String getOriginConsumerName(String consumerGroupName){
        if(!consumerGroupName.contains("_")){
            return consumerGroupName;
        }else {
            return consumerGroupName.substring(0,consumerGroupName.lastIndexOf("-"));
        }
    }
}
