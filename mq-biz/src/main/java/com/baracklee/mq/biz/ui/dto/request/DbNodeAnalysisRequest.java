package com.baracklee.mq.biz.ui.dto.request;

import com.baracklee.mq.biz.dto.request.BaseUiRequst;

/**
 * @Author： Barack Lee
 */
public class DbNodeAnalysisRequest extends BaseUiRequst {
    private Long id;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
}
