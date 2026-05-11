package com.example.piceditor.sever.ai_remove_bg.impl;

import androidx.annotation.NonNull;

import com.huann305.app.data.sever.RetrofitClient;
import com.huann305.app.data.sever.model.Data;
import com.huann305.app.data.sever.model.PaginationData;
import com.huann305.app.data.sever.repository.WorkRepository;
import com.huann305.app.data.sever.token.genart.DeviceToken;
import com.huann305.app.model.genart.CategoryModel;
import com.huann305.app.model.genart.GenArtModel;

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
        return new SingleObserver<>() {
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

    @NonNull
    private <T> SingleObserver<T> createObserver2(SingleEmitter<T> emitter) {
        return new SingleObserver<>() {
            @Override
            public void onSubscribe(@NonNull Disposable d) {
                emitter.setDisposable(d);
            }

            @Override
            public void onSuccess(@NonNull T t) {
                if (!emitter.isDisposed()) {
                    emitter.onSuccess(t);
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
