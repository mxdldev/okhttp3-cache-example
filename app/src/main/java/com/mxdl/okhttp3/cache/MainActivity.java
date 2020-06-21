package com.mxdl.okhttp3.cache;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;
import android.view.View;

import com.ihsanbal.logging.Level;
import com.ihsanbal.logging.LoggingInterceptor;
import com.mxdl.okhttp3.util.NetUtil;

import java.io.File;
import java.io.IOException;

import okhttp3.Cache;
import okhttp3.CacheControl;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.HttpUrl;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {

    private OkHttpClient mOkHttpClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        File file = getCacheDir();
        Log.v("MYTAG", "cache path:" + file.getAbsolutePath());
        mOkHttpClient = new OkHttpClient.Builder()
                .cache(new Cache(new File(file, "okhttp-cache"), 10 * 1024 * 1024))
                .addNetworkInterceptor(new Interceptor() {
                    @Override
                    public Response intercept(Chain chain) throws IOException {
                        Request request = chain.request();
                        Response response = chain.proceed(request);
                        int onlineCacheTime = 60;//在线的时候的缓存过期时间，如果想要不缓存，直接时间设置为0
                        return response.newBuilder()
                                .header("Cache-Control", "public, max-age=" + onlineCacheTime)
                                .removeHeader("Pragma")
                                .build();
                    }
                })
                .addInterceptor(new Interceptor() {
                    @Override
                    public Response intercept(Chain chain) throws IOException {
                        Request request = chain.request();
                        if (!NetUtil.checkNet(MainActivity.this)) {
                            int offlineCacheTime = Integer.MAX_VALUE;//离线的时候的缓存的过期时间
                            request = request.newBuilder()
//                        .cacheControl(new CacheControl
//                                .Builder()
//                                .maxStale(60,TimeUnit.SECONDS)
//                                .onlyIfCached()
//                                .build()
//                        ) 两种方式结果是一样的，写法不同
                                    .header("Cache-Control", "public, only-if-cached, max-stale=" + offlineCacheTime)
                                    .build();
                        }
                        return chain.proceed(request);
                    }
                })
                .build();
    }

    //持久缓存
    public void cacheTest(View view) {
        //创建一个HttpUrl
        HttpUrl httpUrl = HttpUrl.parse("http://192.168.31.105:8080/user/login")
                .newBuilder()
                .addQueryParameter("username", "mxdl")
                .addQueryParameter("password", "123456")
                .build();
        //根据url创建一个Request
        Request request = new Request.Builder()
                .url(httpUrl)
                //.cacheControl(CacheControl.FORCE_CACHE) 永久缓存
                .build();
        //进行请求
        mOkHttpClient.newCall(request)
                .enqueue(new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        Log.v("MYTAG", "onFail start...");
                        Log.v("MYTAG", e.toString());
                    }

                    @Override
                    public void onResponse(Call call, Response response) throws IOException {
                        Log.v("MYTAG", "onResponse start...");
                        if (response.isSuccessful()) {
                            if (response.networkResponse() != null) {
                                Log.v("MYTAG", "net:" + response.body().string());
                            } else if (response.cacheResponse() != null) {
                                Log.v("MYTAG", "cache:" + response.body().string());
                            } else {
                                Log.v("MYTAG", "onResponse null");
                            }
                        } else {
                            Log.v("MYTAG", "onResponse fail");
                        }
                    }
                });
    }

    public void netCache(View view) {

    }
}
