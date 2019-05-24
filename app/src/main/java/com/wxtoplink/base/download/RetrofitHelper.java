package com.wxtoplink.base.download;


import java.util.concurrent.TimeUnit;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;

/**
 * 下载网络工具类
 * Created by 12852 on 2018/7/24.
 */

public class RetrofitHelper {

    private Retrofit retrofit ;

    private final OkHttpClient httpClient ;

    private static final RetrofitHelper instance = new RetrofitHelper();

    public RetrofitHelper() {
        this.httpClient = new OkHttpClient.Builder()
                .retryOnConnectionFailure(true)
                .connectTimeout(2, TimeUnit.SECONDS)
                .build();
        this.retrofit = new Retrofit.Builder()
                .baseUrl("http://vs-new.geniusshelf.com/")
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .build();
    }

    private OkHttpClient getHttpClient(Interceptor interceptor){
        return httpClient.newBuilder()
                .addInterceptor(interceptor)//添加拦截器
                .build();
    }

    public static RetrofitHelper getInstance(){
        return instance ;
    }

    public Retrofit getRetrofit(Interceptor interceptor){

        return retrofit.newBuilder()
                .client(getHttpClient(interceptor))
                .build() ;

    }

}
