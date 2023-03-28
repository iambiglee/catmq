package com.baracklee.mq.biz.entity;

import com.baracklee.mq.biz.dto.NotifyFailVo;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

public class NotifyCallBack implements Callback {
    private final String url;
    private final AtomicReference<Map<String, NotifyFailVo>> notifyFailMapRef;
    public NotifyCallBack(String url, AtomicReference<Map<String, NotifyFailVo>> notifyFailMapRef) {
        this.url = url;
        this.notifyFailMapRef = notifyFailMapRef;
    }


    @Override
    public void onFailure(Call call, IOException e) {
        setFailStatus(url);
    }

    private void setFailStatus(String url) {
        NotifyFailVo notifyFailVo = notifyFailMapRef.get().get(url);
        if (notifyFailVo==null){
            NotifyFailVo notifyFailVo1 = new NotifyFailVo();
            notifyFailVo1.getIsRetrying().set(false);
            notifyFailVo1.setStatus(false);
            notifyFailMapRef.get().put(url,notifyFailVo1);
        }else {
            notifyFailVo.setStatus(false);
            notifyFailVo.getIsRetrying().set(false);
        }
    }

    @Override
    public void onResponse(Call call, Response response) throws IOException {
        response.close();
    }
}
