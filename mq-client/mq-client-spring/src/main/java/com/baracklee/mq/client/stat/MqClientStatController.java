package com.baracklee.mq.client.stat;

import com.baracklee.mq.biz.common.util.JsonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import com.baracklee.mq.client.MqClient;

import java.util.HashMap;
import java.util.Map;

@RestController
public class MqClientStatController {
    private static final Logger logger = LoggerFactory.getLogger(MqClientStatController.class);

    final String MQ_CLINET_STAT_OPEN = "mq.client.stat.open";

    @Autowired
    private Environment env;

    private boolean isOpenFlag() {
        return "true".equalsIgnoreCase(env.getProperty(MQ_CLINET_STAT_OPEN, "true"));
    }

    @GetMapping("/mq/client/cache")
    public String data() {
        if (isOpenFlag()) {
            MqClient.getContext().setBrokerIp(MqClient.getContext().getMqResource().getBrokerIp());
            return JsonUtil.toJsonNull(MqClient.getContext());
        }
        return "";
    }

    @GetMapping("/mq/client/th")
    public String th() {
        if (isOpenFlag()) {
            StringBuilder rs = new StringBuilder();
            Map<Thread.State, Integer> state = new HashMap<>();
            for (Map.Entry<Thread, StackTraceElement[]> t1 : Thread.getAllStackTraces().entrySet()) {
                Thread thread = t1.getKey();
                StackTraceElement[] stackTraceElements = t1.getValue();
                state.putIfAbsent(thread.getState(), 0);
                state.put(thread.getState(), state.get(thread.getState()) + 1);
                rs.append("\n<br/>线程名称：" + thread.getName() + ",线程id:" + thread.getId() + ",16进制为："
                        + Long.toHexString(thread.getId()) + ",线程优先级为：" + thread.getPriority() + "，线程状态："
                        + thread.getState() + "<br/>\n");
                for (StackTraceElement st : stackTraceElements) {
                    rs.append(st.toString() + "<br/>\n");
                }
            }
            StringBuilder rs1 = new StringBuilder();
            for (Map.Entry<Thread.State, Integer> t1 : state.entrySet()) {
                rs1.append("线程状态：" + t1.getKey() + ",数量：" + t1.getValue() + "<br/>\n");
            }
            return rs1.toString() + rs.toString();
        }
        return "";
    }

}
