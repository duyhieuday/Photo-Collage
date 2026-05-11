package com.example.piceditor.sever.ai_remove_bg.repository;

import com.huann305.app.data.sever.model.PaginationData;
import com.huann305.app.model.genart.CategoryModel;
import com.huann305.app.model.genart.GenArtModel;

import java.util.List;

import io.reactivex.rxjava3.core.Single;

public interface WorkRepository {
    Single<PaginationData<List<GenArtModel>>> getAllModel(int page);

    Single<PaginationData<List<CategoryModel>>> getAllCategoryModel();
}
