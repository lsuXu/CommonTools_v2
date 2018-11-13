package com.wxtoplink.base.download;

import com.wxtoplink.base.download.listener.DownloadInterceptor;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;

/**
 * Created by 12852 on 2018/7/24.
 */

public class RetrofitHelper {

    private Retrofit retrofit ;

    private OkHttpClient httpClient ;

    private static final RetrofitHelper instance = new RetrofitHelper();

    private RetrofitHelper(){
        httpClient = new OkHttpClient.Builder()
                .addInterceptor(DownloadInterceptor.getInstance())//添加拦截器
                .retryOnConnectionFailure(true)
                .connectTimeout(2, TimeUnit.SECONDS)
                .build();

        retrofit = new Retrofit.Builder()
                .baseUrl("http://vs-new.geniusshelf.com/")
                .client(httpClient)
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .build();
    }

    public static RetrofitHelper getInstance(){
        return instance ;
    }

    public Retrofit getRetrofit(){
        return retrofit ;
    }

}
