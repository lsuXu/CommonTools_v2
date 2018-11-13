package com.wxtoplink.base.download;

import io.reactivex.Observable;
import okhttp3.ResponseBody;
import retrofit2.http.GET;
import retrofit2.http.Streaming;
import retrofit2.http.Url;

/**
 * Created by 12852 on 2018/7/24.
 */

public interface DownloadService {

    @Streaming
    @GET
    Observable<ResponseBody> download(@Url String url);
}
