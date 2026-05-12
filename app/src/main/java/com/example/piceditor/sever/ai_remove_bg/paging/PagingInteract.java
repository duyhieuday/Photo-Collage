package com.example.piceditor.sever.ai_remove_bg.paging;

public interface PagingInteract {

    int getCurrentPage();

    boolean isLoading();

    boolean isLastPage();

    void onLoadMore();

    void reset();

}
