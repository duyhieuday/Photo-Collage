package com.example.piceditor.sever.ai_remove_bg.presenter;

import com.example.piceditor.sever.ai_remove_bg.RetrofitClient;
import com.example.piceditor.sever.ai_remove_bg.model.Data;
import com.example.piceditor.sever.ai_remove_bg.model.GenArtResult;
import com.example.piceditor.sever.ai_remove_bg.paging.Pagination;
import com.example.piceditor.sever.ai_remove_bg.token.genart.DeviceToken;
import com.mct.base.ui.core.IBaseView;

import java.io.File;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.core.SingleObserver;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;

public class WorkPresenter extends BaseRxPresenter<IBaseView> {
    private final String TAG = "WorkPresenter";
    private final Pagination modelPagination;

    public WorkPresenter() {
        modelPagination = new Pagination();
    }

    public Pagination getModelPagination() {
        return modelPagination;
    }

    public void removeBg(File img, Consumer<GenArtResult> complete) {
        RetrofitClient.getGenArtApiService()
                .getQueueRemoveBg(
                        MultipartBody.Part.createFormData("image", img.getName(), RequestBody.create(img, MultipartBody.FORM)),
                        RequestBody.create("14", MultipartBody.FORM),
                        DeviceToken.getAccessToken()
                ).map(data -> data.getData().get("id").getAsString())
                .flatMap(queueId -> RetrofitClient.getGenArtApiService().getQueueResult(queueId,
                                DeviceToken.getAccessToken())
                        .doOnSubscribe(disposable -> getCompositeDisposable().add(disposable))
                        .repeatWhen(flowable -> flowable.delay(300, TimeUnit.MILLISECONDS))
                        .filter(genArtResultData -> genArtResultData.getData() != null && genArtResultData.getData().isDone())
                        .firstOrError()
                ).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new SingleObserver<Data<GenArtResult>>() {
                    @Override
                    public void onSubscribe(@NonNull Disposable d) {
                        getCompositeDisposable().add(d);
                    }

                    @Override
                    public void onSuccess(@NonNull Data<GenArtResult> result) {
                        if (result.getData() != null) {
                            actionResult(complete, result.getData());
                        } else {
                            actionResult(complete, null);
                        }
                    }

                    @Override
                    public void onError(@NonNull Throwable e) {
                        actionResult(complete, null);
                    }
                });
    }

    public void removeObj(File img,File mask, Consumer<GenArtResult> complete) {
        RetrofitClient.getGenArtApiService()
                .getQueueRemoveObj(
                        MultipartBody.Part.createFormData("image", img.getName(), RequestBody.create(img, MultipartBody.FORM)),
                        RequestBody.create("14", MultipartBody.FORM),
                        MultipartBody.Part.createFormData("mask", mask.getName(), RequestBody.create(mask, MultipartBody.FORM)),
                        DeviceToken.getAccessToken()
                )
                .map(data -> data.getData().get("id").getAsString())
                .flatMap(queueId -> RetrofitClient.getGenArtApiService().getQueueResult(queueId,
                                DeviceToken.getAccessToken())
                        .doOnSubscribe(disposable -> getCompositeDisposable().add(disposable))
                        .repeatWhen(flowable -> flowable.delay(300, TimeUnit.MILLISECONDS))
                        .filter(genArtResultData -> genArtResultData.getData() != null && genArtResultData.getData().isDone())
                        .firstOrError()
                )
                .timeout(3,TimeUnit.MINUTES)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new SingleObserver<Data<GenArtResult>>() {
                    @Override
                    public void onSubscribe(@NonNull Disposable d) {
                        getCompositeDisposable().add(d);
                    }

                    @Override
                    public void onSuccess(@NonNull Data<GenArtResult> result) {
                        if (result.getData() != null) {
                            actionResult(complete, result.getData());
                        } else {
                            actionResult(complete, null);
                        }
                    }

                    @Override
                    public void onError(@NonNull Throwable e) {
                        actionResult(complete, null);
                    }
                });
    }

    public <T> void actionResult(Consumer<T> consumer, T result) {
        if (consumer != null) {
            consumer.accept(result);
        }
    }
}
