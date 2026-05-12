package com.example.piceditor.sever.ai_remove_bg.repository;

import com.example.piceditor.sever.ai_remove_bg.model.CategoryModel;
import com.example.piceditor.sever.ai_remove_bg.model.GenArtModel;
import com.example.piceditor.sever.ai_remove_bg.model.PaginationData;

import java.util.List;

import io.reactivex.rxjava3.core.Single;

public interface WorkRepository {
    Single<PaginationData<List<GenArtModel>>> getAllModel(int page);

    Single<PaginationData<List<CategoryModel>>> getAllCategoryModel();
}
