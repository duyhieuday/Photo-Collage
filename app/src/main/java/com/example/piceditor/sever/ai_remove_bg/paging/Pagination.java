package com.example.piceditor.sever.ai_remove_bg.paging;

public class Pagination implements PagingInteract {

    private int currentPage;
    private boolean isLoading;
    private boolean isLastPage;
    private OnLoadMoreListener onLoadMoreListener;

    public Pagination() {
    }

    public Pagination(OnLoadMoreListener onLoadRunnable) {
        this.onLoadMoreListener = onLoadRunnable;
    }

    public void setOnLoadMoreListener(OnLoadMoreListener onLoadMoreListener) {
        this.onLoadMoreListener = onLoadMoreListener;
    }

    @Override
    public int getCurrentPage() {
        return currentPage;
    }

    @Override
    public boolean isLoading() {
        return isLoading;
    }

    @Override
    public boolean isLastPage() {
        return isLastPage;
    }

    @Override
    public void onLoadMore() {
        if (onLoadMoreListener != null) {
            onLoadMoreListener.onLoadMore(this);
        }
    }

    @Override
    public void reset() {
        currentPage = 0;
        isLoading = false;
        isLastPage = false;
    }

    public int plusPage() {
        return ++currentPage;
    }

    public int minusPage() {
        return --currentPage;
    }

    public void setCurrentPage(int currentPage) {
        this.currentPage = currentPage;
    }

    public void setLoading(boolean loading) {
        isLoading = loading;
    }

    public void setLastPage(boolean lastPage) {
        isLastPage = lastPage;
    }

    public interface OnLoadMoreListener {
        void onLoadMore(Pagination pagination);
    }
}
