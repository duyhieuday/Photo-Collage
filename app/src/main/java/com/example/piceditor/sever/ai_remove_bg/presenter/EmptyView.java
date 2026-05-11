package com.example.piceditor.sever.ai_remove_bg.presenter;

import com.mct.base.ui.core.IBaseView;

public class EmptyView implements IBaseView {

    public static final EmptyView INSTANCE = new EmptyView();

    private EmptyView() {
    }

    @Override
    public void showLoading() {
    }

    @Override
    public void hideLoading() {
    }

    @Override
    public void showError(Throwable t) {
    }
}