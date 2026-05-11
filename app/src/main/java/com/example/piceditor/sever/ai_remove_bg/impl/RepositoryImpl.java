package com.example.piceditor.sever.ai_remove_bg.impl;

import android.content.Context;

import com.huann305.app.data.sever.repository.Repository;
import com.huann305.app.data.sever.repository.WorkRepository;

public class RepositoryImpl implements Repository {
    private final Context context;

    private volatile WorkRepository workRepository;

    public RepositoryImpl(Context context) {
        this.context = context;
    }
    @Override
    public WorkRepository workRepository() {
        if (workRepository == null) {
            synchronized (RepositoryImpl.class) {
                if (workRepository == null) {
                    workRepository = new WorkRepositoryImpl();
                }
            }
        }
        return workRepository;
    }
}
