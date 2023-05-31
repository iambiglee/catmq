package com.baracklee.ui.controller;


import com.baracklee.mq.biz.dto.response.BaseUiResponse;
import com.baracklee.mq.biz.ui.dto.request.DbNodeAnalysisRequest;
import com.baracklee.mq.biz.ui.dto.request.DbNodeCreateRequest;
import com.baracklee.mq.biz.ui.dto.request.DbNodeGetListRequest;
import com.baracklee.mq.biz.ui.dto.response.*;
import com.baracklee.ui.service.UiDbConnectionsService;
import com.baracklee.ui.service.UiDbNodeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/dbNode")
public class DbNodeController {

    @Autowired
    private UiDbNodeService uiDbNodeService;
    @Autowired
    private UiDbConnectionsService uiDbConnectionsService;

    private Logger log = LoggerFactory.getLogger(TopicController.class);

    @RequestMapping("/list2/data")
    public DbNodeGetListResponse dbNodeListData(DbNodeGetListRequest dbNodeGetListRequest) {
        return uiDbNodeService.queryByPage(dbNodeGetListRequest);
    }

    @PostMapping("/createOrUpdate")
    public DbNodeCreateResponse createOrUpdateDbNode(DbNodeCreateRequest dbNodeCreateRequest) {
        return uiDbNodeService.createOrUpdateDbNode(dbNodeCreateRequest);
    }

    @PostMapping("/delete")
    public DbNodeDeleteResponse deleteDbNode(@RequestParam("id") Long dbNodeId) {
        return uiDbNodeService.deleteDbNode(dbNodeId);
    }

    @PostMapping("/compare")
    public DbNodeCompareResponse compare(@RequestParam("id") Long dbNodeId,
                                         @RequestParam(name = "name") String dbName) {
        return uiDbNodeService.compare(dbNodeId, dbName);
    }

    @PostMapping("/dilatation")
    public DbNodeDilatationResponse dilatation(@RequestParam("id") Long dbNodeId) {
        return uiDbNodeService.dilatation(dbNodeId);
    }

    @PostMapping("/createSql")
    public DbNodeCreateSqlResponse createSql(@RequestParam(name = "name") String dbName,
                                    @RequestParam("quantity") String quantity,@RequestParam("nodeType") String nodeType) {
        return uiDbNodeService.createSql(dbName, quantity,nodeType);
    }

    @PostMapping("/beforeChange")
    public DbNodeBeforeChangeResponse beforeChange(@RequestParam(name = "id") Long id,
                                       @RequestParam(name = "readOnly") Integer readOnly) {
        return uiDbNodeService.beforeChange(id,readOnly);
    }

    @PostMapping("/changeStatus")
    public DbNodeChangeStatusResponse changeStatus(@RequestParam(name = "id") Long id,
                                       @RequestParam(name = "readOnly") Integer readOnly) {
        return uiDbNodeService.changeStatus(id, readOnly);
    }

    @GetMapping("/analyse")
    public DbNodeAnalyseResponse analyse(DbNodeAnalysisRequest dbNodeAnalysisRequest) {
        return uiDbNodeService.analyse(dbNodeAnalysisRequest);
    }

    /**
     * 尝试执行
     * @param dbNodeId 数据节点id
     * @param quantity 建表数量
     * @return
     */
    @PostMapping("/createTable")
    public DbNodeCreateTableResponse createTableIfNecessary(@RequestParam(name = "dbNodeId") Long dbNodeId,
                                                 @RequestParam(name = "quantity") Integer quantity) {
        return uiDbNodeService.createTableIfNecessary(dbNodeId,quantity);
    }

    @PostMapping("/batchDilatation")
    public DbNodeBatchDilatationResponse batchDilatation() {
        return uiDbNodeService.batchDilatation();
    }

    @GetMapping("/connections")
    public DbNodeConnectionsResponse getConnections() {
        return uiDbConnectionsService.getConnections();
    }

    @RequestMapping("/physicalMachineReport/data")
    public PhysicalMachineReportResponse getPhysicalMachineReportData(String ip) {
        return uiDbNodeService.getPhysicalMachineReportData(ip);
    }

    @GetMapping("/onLineNums")
    public BaseUiResponse getOnLineNums() {
        return uiDbConnectionsService.getOnLineNums();
    }
}
