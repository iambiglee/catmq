package com.baracklee.ui.controller;

import com.baracklee.mq.biz.ui.dto.response.PanelNodeGetListResponse;
import com.baracklee.mq.biz.ui.vo.PanelNodeVo;
import com.baracklee.ui.service.UiPanelService;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * @author Barack Lee
 */
@RestController
@RequestMapping("/dataPanel")
public class DataPanelController {

    private UiPanelService uiPanelService;

    public DataPanelController(UiPanelService uiPanelService) {
        this.uiPanelService = uiPanelService;
    }

    @GetMapping("/node")
    public PanelNodeGetListResponse getNodePanel() {
        List<PanelNodeVo> panelNodeVoList = uiPanelService.getNodePanel();
        if (CollectionUtils.isEmpty(panelNodeVoList)) {
            return new PanelNodeGetListResponse(0L, null);
        } else {
            return new PanelNodeGetListResponse((long) panelNodeVoList.size(), panelNodeVoList);
        }

    }
}
