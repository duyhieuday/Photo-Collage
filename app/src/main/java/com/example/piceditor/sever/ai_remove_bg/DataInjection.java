package com.example.piceditor.sever.ai_remove_bg;


import com.example.piceditor.WeatherApplication;
import com.example.piceditor.sever.ai_remove_bg.impl.RepositoryImpl;
import com.example.piceditor.sever.ai_remove_bg.repository.Repository;

public class DataInjection {

    public static Repository provideRepository() {
        return RepositoryHolder.HOLDER;
    }

    private static final class RepositoryHolder {
        static final Repository HOLDER = new RepositoryImpl(WeatherApplication.getInstance());
    }

    private DataInjection() {
        //no instance
    }
}
