package com.wxtoplink.base.download.listener;

import java.io.IOException;

import io.reactivex.annotations.Nullable;
import okhttp3.MediaType;
import okhttp3.ResponseBody;
import okio.Buffer;
import okio.BufferedSource;
import okio.ForwardingSource;
import okio.Okio;
import okio.Source;

/**
 * 自定义ResponseBody，计算当前下载任务的进度（通过数据流的大小进行比较）
 * Created by 12852 on 2018/7/24.
 */

public class DownloadResponseBody extends ResponseBody{

    private ResponseBody responseBody;

    private DownloadListener downloadListener ;

    private BufferedSource bufferedSource ;

    public DownloadResponseBody(ResponseBody responseBody, DownloadListener downloadListener) {
        this.responseBody = responseBody;
        this.downloadListener = downloadListener;
    }

    @Nullable
    @Override
    public MediaType contentType() {
        return responseBody.contentType();
    }

    @Override
    public long contentLength() {
        return responseBody.contentLength();
    }

    @Override
    public BufferedSource source() {
        if(bufferedSource == null){
            bufferedSource = Okio.buffer(source(responseBody.source()));
        }
        return bufferedSource;
    }

    private Source source(Source source){

        ForwardingSource forwardingSource = new ForwardingSource(source) {

            long receiverByteSize = 0l;

            @Override
            public long read(Buffer sink, long byteCount) throws IOException {
                long bytesRead = super.read(sink, byteCount);
                receiverByteSize =  receiverByteSize + (bytesRead != -1 ? bytesRead:0 );
                if(downloadListener != null){
                    //已接收文件大小，总大小，下载进度
                    downloadListener.onProgress(receiverByteSize,responseBody.contentLength(),(int) (receiverByteSize * 100 /responseBody.contentLength()));
                }
                return bytesRead ;
            }
        };

        return forwardingSource ;

    }
}
