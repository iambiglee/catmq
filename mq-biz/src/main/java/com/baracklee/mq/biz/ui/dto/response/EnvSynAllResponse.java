package com.baracklee.mq.biz.ui.dto.response;


import com.baracklee.mq.biz.dto.response.BaseUiResponse;

public class EnvSynAllResponse extends BaseUiResponse<Void> {
    public EnvSynAllResponse(){
        super();
    }

    public EnvSynAllResponse(String code, String msg){
        super(code,msg);
    }
}
