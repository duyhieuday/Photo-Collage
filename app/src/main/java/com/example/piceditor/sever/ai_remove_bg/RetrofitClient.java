
package com.example.piceditor.sever.ai_remove_bg;

import androidx.annotation.NonNull;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.huann305.app.data.sever.UnsafeOkHttpClient;
import com.example.piceditor.sever.ai_remove_bg.api.DeviceApiService;
import com.huann305.app.data.sever.api.GenArtApiService;
import com.huann305.app.data.sever.api.VideoApiService;
import com.huann305.app.data.sever.api.WalletApi;
import com.huann305.app.data.sever.token.genart.DeviceToken;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava3.RxJava3CallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitClient {

    private static final String BASE_DOMAIN_GEN_ART = "https://ai.eztechglobal.com/api/v1/";
    private static Retrofit genArtRetrofit;
    private static Retrofit videoRetrofit;
    private static Retrofit deviceRetrofit;
    private static Retrofit walletRetrofit;
    private static final Gson gson = new GsonBuilder().setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'").create();

    private static final Interceptor headerInterceptor = new Interceptor() {
        @NonNull
        @Override
        public Response intercept(@NonNull Chain chain) throws IOException {
            Request originalRequest = chain.request();

            Request newRequest = originalRequest.newBuilder()
                    .addHeader("AuthorizationApi", DeviceToken.getAccessToken())
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Accept", "application/json")
                    .build();

            return chain.proceed(newRequest);
        }
    };

    private static final OkHttpClient client = new OkHttpClient.Builder()
            .readTimeout(120, TimeUnit.SECONDS)
            .connectTimeout(120, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .addInterceptor(headerInterceptor)
            .build();

    private static Retrofit getGenArtRetrofit() {
        if (genArtRetrofit == null) {

            genArtRetrofit = new Retrofit.Builder()
                    .baseUrl(BASE_DOMAIN_GEN_ART)
                    .client(UnsafeOkHttpClient.getUnsafeOkHttpClient())
                    .addConverterFactory(GsonConverterFactory.create(gson))
                    .addCallAdapterFactory(RxJava3CallAdapterFactory.create())
                    .client(client)
                    .build();
        }
        return genArtRetrofit;
    }

    @NonNull
    public static GenArtApiService getGenArtApiService() {
        return getGenArtRetrofit().create(GenArtApiService.class);
    }

    @NonNull
    public static DeviceApiService getDeviceApiService() {
        return getGenArtRetrofit().create(DeviceApiService.class);
    }

    @NonNull
    public static VideoApiService getVideoApiService() {
        return getGenArtRetrofit().create(VideoApiService.class);
    }

    @NonNull
    public static WalletApi getWalletApiService() {
        return getGenArtRetrofit().create(WalletApi.class);
    }
}