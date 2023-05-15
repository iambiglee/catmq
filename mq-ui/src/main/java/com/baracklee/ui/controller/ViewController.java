package com.baracklee.ui.controller;

import com.baracklee.mq.biz.common.SoaConfig;
import com.baracklee.mq.biz.dto.UserInfo;
import com.baracklee.mq.biz.dto.UserRoleEnum;
import com.baracklee.mq.biz.entity.ConsumerGroupEntity;
import com.baracklee.mq.biz.entity.DbNodeEntity;
import com.baracklee.mq.biz.entity.QueueEntity;
import com.baracklee.mq.biz.entity.TopicEntity;
import com.baracklee.mq.biz.service.*;
import com.baracklee.mq.biz.ui.vo.MonitorUrlVo;
import com.baracklee.ui.service.UiQueueOffsetService;
import com.baracklee.ui.service.UiQueueService;
import com.baracklee.ui.util.CookieUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.*;

@Controller
public class ViewController {

    @Autowired
    private DbNodeService dbNodeService;
    @Autowired
    private TopicService topicService;
    @Autowired
    private ConsumerGroupService consumerGroupService;

    private UiQueueService uiQueueService;

    private UiQueueOffsetService uiQueueOffsetService;

    @Autowired
    private RoleService roleService;
    @Autowired
    private QueueService queueService;
    @Autowired
    private SoaConfig soaConfig;
    @Autowired
    private Environment env;
    @Autowired
    private UserInfoHolder userInfoHolder;

    @Autowired
    private ServerService serverService;

    public ViewController(DbNodeService dbNodeService,
                          TopicService topicService,
                          ConsumerGroupService consumerGroupService,
                          UiQueueService uiQueueService,
                          UiQueueOffsetService uiQueueOffsetService,
                          RoleService roleService,
                          QueueService queueService,
                          SoaConfig soaConfig,
                          Environment env,
                          UserInfoHolder userInfoHolder,
                          ServerService serverService) {
        this.dbNodeService = dbNodeService;
        this.topicService = topicService;
        this.consumerGroupService = consumerGroupService;
        this.uiQueueService = uiQueueService;
        this.uiQueueOffsetService = uiQueueOffsetService;
        this.roleService = roleService;
        this.queueService = queueService;
        this.soaConfig = soaConfig;
        this.env = env;
        this.userInfoHolder = userInfoHolder;
        this.serverService = serverService;
    }

    Map<String, String> keysMap = new HashMap<>();



    {
        keysMap.put("defaultTopicThreadSize", SoaConfig.env_getConsumerGroupTopicThreadSize_key);
        keysMap.put("defaultTopicRetryCount", SoaConfig.env_getConsumerGroupTopicRetryCount_key);
        keysMap.put("defaultTopicLag", SoaConfig.env_getConsumerGroupTopicLag_key);
        keysMap.put("defaultTopicDelayProcessTime", SoaConfig.env_getDelayProcessTime_key);
        keysMap.put("defaultPullBatchSize", SoaConfig.env_getPullBatchSize_key);
        keysMap.put("defaultConsumerBatchSize", SoaConfig.env_getConsumerBatchSize_key);
        keysMap.put("defaultTopicDelayPullTime", SoaConfig.env_getMinDelayPullTime_key);
    }

    @RequestMapping("/login")
    public String login(HttpServletRequest request, Model model) {
        model.addAttribute("timeStamp", System.currentTimeMillis());
        return "login";
    }

    @RequestMapping("/logout")
    public String logout(HttpServletRequest request, HttpServletResponse response) {
        Cookie cookie = new Cookie("userSessionId", null);
        // 将cookie的有效期设置为0，命令浏览器删除该cookie
        cookie.setMaxAge(0);
        cookie.setPath("/");
        response.addCookie(cookie);
        return "redirect:/logout";
    }
    @RequestMapping("/index")
    public String index(HttpServletRequest request,Model model){
        model.addAttribute("userName", CookieUtil.getUserName(request));
        model.addAttribute("roleName", roleService.getRoleName(userInfoHolder.getUserId()));
        model.addAttribute("sdkVersion", soaConfig.getSdkVersion());
        int onlineServerNum = serverService.getOnlineServerNum();
        int role = roleService.getRole(userInfoHolder.getUserId());
        model.addAttribute("userRole",role);
        if (soaConfig.isPro()){
            model.addAttribute("proEnv",1);
        }else {
            model.addAttribute("proEnv",0);
        }
        model.addAttribute("onLineServerNum","在线server数量："+onlineServerNum);
        return "index";
    }

    @RequestMapping("/")
    public String first(HttpServletRequest request, HttpServletResponse response, Model model) {
        model.addAttribute("timeStamp", System.currentTimeMillis());
        return "redirect:/index";
    }

    @RequestMapping("/topic/list")
    public String topicList(HttpServletRequest request, Model model) {
        model.addAttribute("userName", userInfoHolder.getUser().getName());
        model.addAttribute("userId", userInfoHolder.getUser().getUserId());
        return "topic/topicList";
    }

    @RequestMapping("/topic/report")
    public String topicReport(HttpServletRequest request, Model model) {
        return "topic/topicReport";
    }

    @RequestMapping("/topic/remove/{topicId}/{topicName}")
    public String topicRemove(@PathVariable Long topicId, @PathVariable String topicName, Model model) {
        model.addAttribute("timeStamp", System.currentTimeMillis());
        model.addAttribute("topicId", topicId);
        model.addAttribute("topicName", topicName);
        return "topic/queueRemove";
    }

    @RequestMapping("/topic/expand/{topicId}/{topicName}")
    public String topicExpand(@PathVariable Long topicId, @PathVariable String topicName, Model model) {
        model.addAttribute("timeStamp", System.currentTimeMillis());
        model.addAttribute("topicId", topicId);
        model.addAttribute("topicName", topicName);
        model.addAttribute("role", roleService.getRole(userInfoHolder.getUserId()));
        return "topic/topicExpand";
    }

    @RequestMapping("/topic/form")
    public String topicForm(HttpServletRequest request, Model model) {
        model.addAttribute("timeStamp", System.currentTimeMillis());
        return "topic/createTopicForm";
    }

    @RequestMapping("/dbNode/list2")
    public String dbNodeList(HttpServletRequest request, Model model) {
        model.addAttribute("timeStamp", System.currentTimeMillis());
        return "dbNode/dbNode";
    }

    @RequestMapping("/consumerGroup/list")
    public String consumerGroupList(HttpServletRequest request, Model model) {
        model.addAttribute("timeStamp", System.currentTimeMillis());
        UserInfo userInfo = userInfoHolder.getUser();
        String userName = userInfo.getName();
        model.addAttribute("userName", userName);
        model.addAttribute("userId", userInfo.getUserId());
        List<String> subEnvList=consumerGroupService.getSubEnvList();
        model.addAttribute("subEnvList",subEnvList);
        return "consumerGroup/consumerGroupList";
    }

    @RequestMapping("/consumerGroup/toCreate/{consumerGroupId}")
    public String toCreateConsumerGroup(@PathVariable String consumerGroupId, HttpServletRequest request, Model model) {
        model.addAttribute("timeStamp", System.currentTimeMillis());
        if (StringUtils.isNotEmpty(consumerGroupId) && !StringUtils.equals(consumerGroupId, "0")) {
            model.addAttribute("consumerGroupId", consumerGroupId);
        } else {
            model.addAttribute("consumerGroupId", "");
        }
        String userId = userInfoHolder.getUserId();
        boolean isAdmin = roleService.isAdmin(userId);
        if (isAdmin) {//如果是超级管理员
            model.addAttribute("isAdmin", 0);
        } else {
            model.addAttribute("isAdmin", 2);
        }

        String email = userInfoHolder.getUser() != null ? userInfoHolder.getUser().getEmail() : "";
        model.addAttribute("email", email);
        return "consumerGroup/createConsumerGroupForm";
    }

    @RequestMapping("/consumerGroup/toDelete")
    public String toDeleteConsumerGroup(HttpServletRequest request, Model model,
                                        @RequestParam(name = "consumerGroupId") long consumerGroupId) {
        ConsumerGroupEntity consumerGroupEntity = consumerGroupService.get(consumerGroupId);
        model.addAttribute("consumerGroupName", consumerGroupEntity.getName());
        model.addAttribute("consumerGroupId", consumerGroupId);
        return "consumerGroup/toDeleteConsumerGroup";
    }

    @RequestMapping("/consumerGroup/createConsumerGroup")
    public String createConsumerGroup(HttpServletRequest request, Model model) {
        model.addAttribute("timeStamp", System.currentTimeMillis());
        return "consumerGroup/createConsumerGroup";
    }

    @RequestMapping("/dataPanel/panel")
    public String dataPanel(HttpServletRequest request, Model model) {
        model.addAttribute("timeStamp", System.currentTimeMillis());
        model.addAttribute("userName", "123456");
        return "dataPanel/panel";
    }

    @RequestMapping("/consumerGroupTopic/list")
    public String consumerGroupTopicList(HttpServletRequest request, Model model,
                                         @RequestParam(name = "consumerGroupId") long consumerGroupId) {
        model.addAttribute("timeStamp", System.currentTimeMillis());
        model.addAttribute("consumerGroupId", consumerGroupId);
        model.addAttribute("maxThreadSize", soaConfig.getConsumerGroupTopicMaxThreadSize());
        model.addAttribute("maxRetryCount", soaConfig.getConsumerGroupTopicMaxRetryCount());
        model.addAttribute("maxPullBatchSize", soaConfig.getMaxPullBatchSize());
        model.addAttribute("maxDelayProcessTime", soaConfig.getMaxDelayProcessTime());
        model.addAttribute("maxAlarmLag", soaConfig.getConsumerGroupTopicMaxLag());
        model.addAttribute("maxDelayPullTime", soaConfig.getMaxDelayPullTime());
        model.addAttribute("minDelayPullTime", soaConfig.getMinDelayPullTime());
        model.addAttribute("minPullBatchSize",soaConfig.getMinPullBatchSize());

        String description = "配置项默认值对应的key为：";
        model.addAttribute("keysMap", addTitile(keysMap, description));

        return "consumerGroup/consumerGroupTopicList";
    }

    private Map<String, String> addTitile(Map<String, String> keysMap, String description) {
        Map<String, String> map = new HashMap<>();
        if (roleService.getRoleName(userInfoHolder.getUserId()).equals(UserRoleEnum.SUPER_USER.getDescription())) {
            for (String key : keysMap.keySet()) {
                map.put(key, description + keysMap.get(key));
            }
        } else {
            for (String key : keysMap.keySet()) {
                map.put(key, "");
            }
        }
        return map;
    }


    @RequestMapping("/queue/list")
    public String queueList(HttpServletRequest request,
                            @RequestParam(name = "queueId", required = false) String queueId, Model model) {
        Map<Long, DbNodeEntity> dataSources = dbNodeService.getCache();
        model.addAttribute("timeStamp", System.currentTimeMillis());
        model.addAttribute("dataSources", dataSources);
        model.addAttribute("loginUserRole",roleService.getRole(userInfoHolder.getUserId(), null));
        if (StringUtils.isNotEmpty(queueId)) {
            model.addAttribute("queueId", queueId);
        }
        return "queue/queue";
    }

    @RequestMapping("/queue/report")
    public String queueReport(HttpServletRequest request, Model model) {
        model.addAttribute("timeStamp", System.currentTimeMillis());
        model.addAttribute("ipList", dbNodeService.getCacheByIp().keySet());
        model.addAttribute("messageNum", "全部消息总量：" + uiQueueService.getMessageCount());
        model.addAttribute("messageAvg", "平均消息总量：" + uiQueueService.getMessageAvg() + " /天");
        return "queue/queueReport";
    }

    @RequestMapping("/queueOffset/list")
    public String queueOffsetList(HttpServletRequest request, Model model) {
        model.addAttribute("timeStamp", System.currentTimeMillis());
        List<String> subEnvList=consumerGroupService.getSubEnvList();
        model.addAttribute("subEnvList",subEnvList);
        return "queueOffset/queueOffsetList";
    }

    @RequestMapping("/queueOffset/accumulation")
    public String queueOffsetAccumulation(HttpServletRequest request, Model model) {
        model.addAttribute("timeStamp", System.currentTimeMillis());
        UserInfo userInfo = userInfoHolder.getUser();
        String userName = userInfo.getName();
        model.addAttribute("userName", userName);
        if(soaConfig.isPro()){
            model.addAttribute("proEnv",1);
        }else{
            model.addAttribute("proEnv",0);
        }
        model.addAttribute("userId", userInfo.getUserId());
        model.addAttribute("consumerGroupNum", "消费者组总数：" + uiQueueOffsetService.getConsumerGroupNum());
        model.addAttribute("usingConsumerGroupNum", "在线消费者组个数：" + uiQueueOffsetService.getUsingConsumerGroupNum());
        model.addAttribute("uselessConsumerGroupNum", "离线消费者组个数：" + uiQueueOffsetService.getUselessConsumerGroupNum());
        return "queueOffset/queueOffsetAccumulation";
    }

    @RequestMapping("/queueOffset/toEditQueueOffset")
    public String toEditQueueOffset(HttpServletRequest request, Model model,
                                    @RequestParam(name = "queueOffsetId") long queueOffsetId) {
        model.addAttribute("queueOffsetId", queueOffsetId);
        model.addAttribute("timeStamp", System.currentTimeMillis());
        return "queueOffset/toEditQueueOffset";
    }

    @RequestMapping("/queueOffset/toEditStopFlag")
    public String toEditStopFlag(HttpServletRequest request, Model model,
                                 @RequestParam(name = "queueOffsetId") long queueOffsetId) {
        model.addAttribute("queueOffsetId", queueOffsetId);
        model.addAttribute("timeStamp", System.currentTimeMillis());
        return "queueOffset/toEditStopFlag";
    }

    @RequestMapping("/message/list")
    public String messageList(HttpServletRequest request,@RequestParam(name = "queueOffsetQueueId", required = false) String queueOffsetQueueId,
                              @RequestParam(name = "queueOffsetTopicName", required = false) String queueOffsetTopicName, Model model) {

        // 模糊查询时，可以查询的最大消息量
        String maxNumber = env.getProperty("mq.message.max.number", "300000");
        model.addAttribute("timeStamp", System.currentTimeMillis());
        model.addAttribute("maxNumber", maxNumber);
        UserInfo userInfo = userInfoHolder.getUser();
        int userRole=roleService.getRole(userInfo.getUserId());
        model.addAttribute("userRole",userRole);
        if (StringUtils.isNotEmpty(queueOffsetQueueId)) {
            model.addAttribute("queueOffsetQueueId", queueOffsetQueueId);
        }
        if(StringUtils.isNotEmpty(queueOffsetTopicName)){
            model.addAttribute("queueOffsetTopicName", queueOffsetTopicName);
        }
        return "message/messageList";
    }


    @RequestMapping("/message/tool")
    public String messageTool(HttpServletRequest request, Model model) {
        String userId = CookieUtil.getUserName(request);
        Map<String, TopicEntity> myTopicList = new HashMap<>(topicService.getCache());

        if (soaConfig.isPro() && !roleService.isAdmin(userId)) {
            myTopicList.clear();
        }

        if (myTopicList != null && myTopicList.size() > 0) {
            List<String> keys = new ArrayList<>(myTopicList.keySet());
            keys.forEach(k -> {
                if (k.endsWith("_fail")) {
                    myTopicList.remove(k);
                }
            });
        }
        List<String> subEnvList=consumerGroupService.getSubEnvList();
        model.addAttribute("subEnvList",subEnvList);
        model.addAttribute("topicList", myTopicList);
        return "message/messageTool";
    }

    @RequestMapping("/topic/editPage/{topicId}")
    public String topicEditPage(@PathVariable String topicId, Model model) {
        if (StringUtils.isNotEmpty(topicId) && !StringUtils.equals(topicId, "0")) {
            model.addAttribute("topicId", topicId);
        } else {
            model.addAttribute("topicId", "");
        }
        String email = userInfoHolder.getUser() != null ? userInfoHolder.getUser().getEmail() : "";
        model.addAttribute("userEmail", email);
        model.addAttribute("options", soaConfig.getExpectDayCountOptions());
        //获取超级管理员的配置
        if (roleService.getRole(userInfoHolder.getUserId()) == 0) {
            model.addAttribute("saveDayNums", soaConfig.getAdminMsgSaveDayNum());
        } else {
            model.addAttribute("saveDayNums", soaConfig.getMsgSaveDayNum());
        }
        return "topic/createTopicForm";
    }

    @RequestMapping("/topic/delete/{topicId}/{topicName}")
    public String topicDelete(@PathVariable Long topicId, @PathVariable String topicName, Model model) {
        model.addAttribute("topicId", topicId);
        model.addAttribute("topicName", topicName);
        return "topic/topicDelete";
    }

    @RequestMapping("/auditLog/listPage/{tbName}/{refId}")
    public String auditLogList(@PathVariable String tbName, @PathVariable String refId, Model model) {
        model.addAttribute("tbName", tbName);
        model.addAttribute("refId", refId);
        return "auditLog/auditLogList";
    }

    @RequestMapping("/auditLog/listPage")
    public String auditLogList(Model model) {
        model.addAttribute("tbName", "");
        model.addAttribute("refId", "");
        return "auditLog/auditLogList";
    }

    @RequestMapping("/queue/queueEdit")
    public String queueEdit(String queueId, Model model) {
        Map<String, TopicEntity> cacheData = topicService.getCache();
        QueueEntity queueEntity = queueService.get(Long.valueOf(queueId));
        int nodeType = queueEntity.getNodeType();
        Map<String, TopicEntity> newCacheData = new HashMap<>();
        Iterator<Map.Entry<String, TopicEntity>> it = cacheData.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, TopicEntity> entry = it.next();
            if (entry.getValue().getTopicType() == nodeType) {
                newCacheData.put(entry.getKey(), entry.getValue());
            }
        }
        model.addAttribute("timeStamp", System.currentTimeMillis());
        model.addAttribute("topicList", newCacheData);
        model.addAttribute("queueId", queueId);
        model.addAttribute("loginUserRole",roleService.getRole(userInfoHolder.getUserId(), null));
        return "queue/queueEdit";
    }

    @GetMapping("/mqLock/lock")
    public String lock(Model model) {
        return "mqLock/lock";
    }

    @RequestMapping("/consumer/list")
    public String consumerGroupConsumerList() {
        return "consumer/consumer";
    }

    @RequestMapping("/dbNode/toAnalyse/{dbNodeId}")
    public String dbNodeToAnalyse(@PathVariable Long dbNodeId, Model model) {
        model.addAttribute("dbNodeId", dbNodeId);
        return "dbNode/analysis";
    }

    @RequestMapping("/notify/list")
    public String notifyList() {
        return "notify/notify";
    }

    @RequestMapping("/compareDbNode")
    public String compareDbNode() {
        return "syn/compareDbNode";
    }

    @RequestMapping("/compareQueue")
    public String compareQueue() {
        return "syn/compareQueue";
    }

    @RequestMapping("/server/list")
    public String serverList(HttpServletRequest request, Model model) {
        model.addAttribute("timeStamp", System.currentTimeMillis());
        return "server/server";
    }

    @RequestMapping("/dataBase/connections")
    public String connections(HttpServletRequest request, Model model) {
        return "dbNode/connections";
    }

    @RequestMapping("/monitor/list")
    public String monitor(HttpServletRequest request, Model model) {
        String urlString = env.getProperty("monitorUrl");
        return "monitor";
    }

    @RequestMapping("/monitorManager/clientMonitor")
    public String getClientMonitor(Model model, @RequestParam(name = "hostPort") String hostPort,String consumerGroupName, long queueId) {
        model.addAttribute("hostPort", hostPort);
        model.addAttribute("consumerGroupName",consumerGroupName);
        model.addAttribute("queueId",queueId);
        return "monitor/clientMonitor";
    }

    @RequestMapping("/physical/machine/report")
    public String PhysicalMachineReport(Model model) {
        model.addAttribute("ipList", dbNodeService.getCacheByIp().keySet());
        return "dbNode/physicalMachineReport";
    }

    @RequestMapping("/syn/envSynTool")
    public String EnvSynTool(Model model) {
        boolean isPro = soaConfig.isPro();
        model.addAttribute("isPro", isPro);
        return "syn/envSynTool";
    }

    @RequestMapping("/redundance/dataCheck")
    public String dataCheck(Model model) {
        return "redundanceCheck/dataCheck";
    }

    @RequestMapping("/department/departmentReport")
    public String getDepartmentReport(Model model){
        return "department/departmentReport";
    }



}
