package com.example.piceditor.sever.ai_remove_bg.api;

import com.example.piceditor.sever.ai_remove_bg.model.CategoryModel;
import com.example.piceditor.sever.ai_remove_bg.model.Data;
import com.example.piceditor.sever.ai_remove_bg.model.GenArtModel;
import com.example.piceditor.sever.ai_remove_bg.model.GenArtResult;
import com.example.piceditor.sever.ai_remove_bg.model.PaginationData;
import com.google.gson.JsonObject;

import java.util.List;

import io.reactivex.rxjava3.core.Single;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface GenArtApiService {
    @Multipart
    @POST("images/edit-image")
    Single<Data<JsonObject>> getQueueGenArt(@Part MultipartBody.Part image,
                                            @Part("model_id") RequestBody modelId,
                                            @Part("type") RequestBody type,
                                            @Part("app_id") RequestBody appId,
                                            @Header("AuthorizationApi") String token);

    @Multipart
    @POST("images/edit-image")
    Single<Data<JsonObject>> getQueueEnhanceArt(@Part MultipartBody.Part image,
                                                @Part("app_id") RequestBody appId,
                                                @Part("type") RequestBody type,
                                                @Header("AuthorizationApi") String token);

    @Multipart
    @POST("images/remove-background")
    Single<Data<JsonObject>> getQueueRemoveBg(@Part MultipartBody.Part image,
                                              @Part("app_id") RequestBody appId,
                                              @Header("AuthorizationApi") String token);

    @Multipart
    @POST("images/remove-object")
    Single<Data<JsonObject>> getQueueRemoveObj(@Part MultipartBody.Part image,
                                               @Part("app_id") RequestBody appId,
                                               @Part MultipartBody.Part mask,
                                               @Header("AuthorizationApi") String token);
    @GET("queues-ai/{queue_id}")
    Single<Data<GenArtResult>> getQueueResult(@Path("queue_id") String queueId,
                                              @Header("AuthorizationApi") String token);
    @GET("synapz/models")
    Single<Data<PaginationData<List<GenArtModel>>>> getAllModel(@Query("with") String with,
                                                                @Query("page") int page,
                                                                @Header("AuthorizationApi") String token);
    @GET("synapz/categories")
    Single<Data<PaginationData<List<CategoryModel>>>> getAllCategoryModel(@Query("with") String with,
                                                                          @Query("order_by") String order_by,
                                                                          @Query("apps") int apps,
                                                                          @Header("AuthorizationApi") String token);
}
