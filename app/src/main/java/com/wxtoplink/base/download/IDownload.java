package com.wxtoplink.base.download;

import io.reactivex.Observable;
import okhttp3.ResponseBody;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Streaming;
import retrofit2.http.Url;

/**
 * Created by 12852 on 2018/7/24.
 */

public interface IDownload {

    @Streaming
    @GET
    Observable<ResponseBody> download(@Url String url);

    @Streaming
    @GET
    Observable<ResponseBody> download(@Header("Range") String range ,@Url() String url);
}
