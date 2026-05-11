package com.example.piceditor.sever.ai_remove_bg;


import com.huann305.app.App;
import com.huann305.app.data.sever.impl.RepositoryImpl;
import com.huann305.app.data.sever.repository.Repository;

public class DataInjection {

    public static Repository provideRepository() {
        return RepositoryHolder.HOLDER;
    }

    private static final class RepositoryHolder {
        static final Repository HOLDER = new RepositoryImpl(App.Companion.getInstance());
    }

    private DataInjection() {
        //no instance
    }
}
