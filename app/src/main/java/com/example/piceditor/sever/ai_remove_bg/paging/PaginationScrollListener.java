package com.example.piceditor.sever.ai_remove_bg.paging;

import android.view.View;

import androidx.annotation.CallSuper;
import androidx.annotation.NonNull;
import androidx.core.widget.NestedScrollView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class PaginationScrollListener extends RecyclerView.OnScrollListener implements NestedScrollView.OnScrollChangeListener {

    private final PagingInteract pagingInteract;
    private final LinearLayoutManager layoutManager;

    public PaginationScrollListener(PagingInteract pagingInteract) {
        this.pagingInteract = pagingInteract;
        this.layoutManager = null;
    }

    public PaginationScrollListener(PagingInteract pagingInteract, LinearLayoutManager layoutManager) {
        this.pagingInteract = pagingInteract;
        this.layoutManager = layoutManager;
    }

    @Override
    @CallSuper
    public void onScrollChange(@NonNull NestedScrollView v, int scrollX, int scrollY, int oldScrollX, int oldScrollY) {
        if (pagingInteract.isLoading() || pagingInteract.isLastPage()) {
            return;
        }
        View view = v.getChildAt(v.getChildCount() - 1);
        int diff = (view.getBottom() - (v.getHeight() + v.getScrollY()));
        if (diff == 0) {
            onLoadMore();
        }
    }

    @Override
    @CallSuper
    public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
        if (pagingInteract.isLoading() || pagingInteract.isLastPage()) {
            return;
        }
        if (layoutManager != null && layoutManager.findLastCompletelyVisibleItemPosition() == layoutManager.getItemCount() - 1) {
            onLoadMore();
        }
    }

    @CallSuper
    protected void onLoadMore() {
        pagingInteract.onLoadMore();
    }

}
