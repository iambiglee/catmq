package com.baracklee.mq.biz.common.util;

import okhttp3.*;

import java.io.IOException;
import java.io.PrintStream;
import java.util.Stack;
import java.util.concurrent.TimeUnit;

public class HttpClient implements IHttpClient {

    private static final MediaType JSONTYPE = MediaType.parse("application/json; charset=utf-8");

    private OkHttpClient client;

    public HttpClient(long connTimeout,long readTimeout){
        ConnectionPool connectionPool = new ConnectionPool(100, 10, TimeUnit.SECONDS);
        client=new OkHttpClient.Builder().connectionPool(connectionPool)
                .connectTimeout(connTimeout,TimeUnit.MILLISECONDS).readTimeout(readTimeout,TimeUnit.MILLISECONDS)
                .build();
    }

    public HttpClient(){
        this(32000L,32000L);
    }

    @Override
    public boolean check(String url) {
        Response response=null;
        Request request = new Request.Builder().url(url).get().build();
        try {
            response=client.newCall(request).execute();
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return response.isSuccessful();
    }

    @Override
    public String post(String url, Object reqObj) throws IOException, BrokerException {
        String json = "";
        if (reqObj != null) {
            json = JsonUtil.toJsonNull(reqObj);
        }
        Response response = null;
        try {
            RequestBody body = RequestBody.create(JSONTYPE, json);
            Request.Builder requestbuilder = new Request.Builder().url(url).post(body);
            Request request=requestbuilder.build();
            response = client.newCall(request).execute();
            if (response.isSuccessful()) {
                return response.body().string();
            } else {
                BrokerException exception = new BrokerException(
                        response.code() + " error,and message is " + response.message()+",json is "+json);
                throw exception;
            }
        } catch (IOException e) {
            throw e;
        }catch (Throwable e) {
            throw e;
        }
        finally {

            try {
                if (response != null) {
                    response.close();
                }
            } catch (Exception e) {

            }
        }
    }

    @Override
    public <T> T post(String url, Object request, Class<T> class1) throws IOException, BrokerException {
        String rs = post(url, request);
        if (rs == null || rs.length() == 0 || rs.trim().length() == 0) {
            return null;
        } else {
            return JsonUtil.parseJson(rs, class1);
        }
    }

    @Override
    public <T> T get(String url, Class<T> class1) throws IOException {
        String rs = get(url);
        if (rs == null || rs.length() == 0 || rs.trim().length() == 0) {
            return null;
        } else {
            return JsonUtil.parseJson(rs, class1);
        }
    }

    @Override
    public void postAsyn(String url, Object reqObj, Callback callback) {
        String json = "";
        if (reqObj != null) {
            json = JsonUtil.toJsonNull(reqObj);
        }

        try {
            RequestBody body = RequestBody.create(JSONTYPE, json);
            okhttp3.Request.Builder requestbuilder = (new okhttp3.Request.Builder()).url(url).post(body);
            Request request = requestbuilder.build();
            if (callback != null) {
                this.client.newCall(request).enqueue(callback);
            } else {
                this.client.newCall(request).enqueue(new Callback() {
                    public void onFailure(Call call, IOException e) {
                    }

                    public void onResponse(Call call, Response response) throws IOException {
                        try {
                            response.close();
                        } catch (Exception var4) {
                            ;
                        }

                    }
                });
            }
        } catch (Exception var8) {
            ;
        }

    }

    @Override
    public void getAsyn(String url, Callback callback) {
        try {
            Request.Builder requestbuilder = new Request.Builder().url(url).get();
            Request request = requestbuilder.build();
            if (callback != null) {
                client.newCall(request).enqueue(callback);
            } else {
                client.newCall(request).enqueue(new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {

                    }

                    @Override
                    public void onResponse(Call call, Response response) throws IOException {
                        try {
                            response.close();
                        } catch (Exception e) {
                            // TODO: handle exception
                        }
                    }
                });

            }
        } catch (Exception e) {

        }

    }

    @Override
    public String get(String url) throws IOException {
        Response response = null;
        try {
            Request request = new Request.Builder().url(url).get().build();
            response = client.newCall(request).execute();
            if (response.isSuccessful()) {
                return response.body().string();
            } else {
                RuntimeException exception = new RuntimeException(
                        response.code() + " error,and message is " + response.message());
                throw exception;
            }
        } catch (Throwable e) {
            throw new RuntimeException(e);
        } finally {
            try {
                if (response != null) {
                    response.close();
                }
            } catch (Exception e) {

            }
        }
    }
}
