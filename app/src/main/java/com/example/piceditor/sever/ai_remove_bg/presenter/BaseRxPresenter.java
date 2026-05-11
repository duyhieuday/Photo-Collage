package com.example.piceditor.sever.ai_remove_bg.presenter;

import com.mct.base.ui.BasePresenter;
import com.mct.base.ui.core.IBaseView;

import io.reactivex.rxjava3.disposables.CompositeDisposable;

public abstract class BaseRxPresenter<V extends IBaseView> extends BasePresenter<V> {

    private CompositeDisposable compositeDisposable;

    @Override
    public void detachView() {
        super.detachView();
        dispose();
    }

    public void dispose() {
        if (compositeDisposable != null) {
            compositeDisposable.dispose();
            compositeDisposable = null;
        }
    }

    public CompositeDisposable getCompositeDisposable() {
        if (compositeDisposable == null || compositeDisposable.isDisposed()) {
            compositeDisposable = new CompositeDisposable();
        }
        return compositeDisposable;
    }
}
