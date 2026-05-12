package com.example.piceditor.sever.ai_remove_bg.impl;

import androidx.annotation.NonNull;

import com.example.piceditor.sever.ai_remove_bg.RetrofitClient;
import com.example.piceditor.sever.ai_remove_bg.model.CategoryModel;
import com.example.piceditor.sever.ai_remove_bg.model.Data;
import com.example.piceditor.sever.ai_remove_bg.model.GenArtModel;
import com.example.piceditor.sever.ai_remove_bg.model.PaginationData;
import com.example.piceditor.sever.ai_remove_bg.repository.WorkRepository;
import com.example.piceditor.sever.ai_remove_bg.token.genart.DeviceToken;

import java.util.List;

import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.core.SingleEmitter;
import io.reactivex.rxjava3.core.SingleObserver;
import io.reactivex.rxjava3.disposables.Disposable;

public class WorkRepositoryImpl implements WorkRepository {
    @Override
    public Single<PaginationData<List<GenArtModel>>> getAllModel(int page) {
        return Single.create(emitter -> RetrofitClient.getGenArtApiService()
                .getAllModel(
                        "categories+id,label",
                        page,
                        DeviceToken.getAccessToken()
                ).subscribe(createObserver(emitter)));
    }

    @Override
    public Single<PaginationData<List<CategoryModel>>> getAllCategoryModel() {
        return Single.create(emitter -> RetrofitClient.getGenArtApiService()
                .getAllCategoryModel(
                        "models+id,label,images",
                        "order+desc,daily_rating+desc,set_count+desc",
                        1,
                        DeviceToken.getAccessToken()
                ).subscribe(createObserver(emitter)));
    }

    @NonNull
    private <T> SingleObserver<Data<T>> createObserver(SingleEmitter<T> emitter) {
        return new SingleObserver<Data<T>>() {
            @Override
            public void onSubscribe(@NonNull Disposable d) {
                emitter.setDisposable(d);
            }

            @Override
            public void onSuccess(@NonNull Data<T> t) {
                if (!emitter.isDisposed()) {
                    emitter.onSuccess(t.getData());
                }
            }

            @Override
            public void onError(@NonNull Throwable e) {
                if (!emitter.isDisposed()) {
                    emitter.onError(e);
                }
            }
        };
    }

}
