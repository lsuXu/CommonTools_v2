package com.wxtoplink.base.download.listener;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.Response;

/**
 * 拦截器，拦截网络请求返回的数据流，包装为自定义的ResponseBody
 * Created by 12852 on 2018/7/24.
 */

public class DownloadInterceptor implements Interceptor{

    private DownloadListener downloadListener ;

    private DownloadInterceptor(){
        downloadListener = DownloadListenerImpl.getInstance();
    }

    private static final DownloadInterceptor instance = new DownloadInterceptor();

    public static final DownloadInterceptor getInstance(){
        return instance ;
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        Response response = chain.proceed(chain.request());
        Response myResponse = response.newBuilder().body(new DownloadResponseBody(response.body(),downloadListener)).build();
        return myResponse;
    }
}
