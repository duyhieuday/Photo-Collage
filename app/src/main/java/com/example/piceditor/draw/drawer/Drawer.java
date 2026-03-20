package com.example.piceditor.draw.drawer;

public abstract class Drawer<D> implements IDrawer<D> {

    private final Callback<D> callback;

    protected Drawer(Callback<D> callback) {
        this.callback = callback;
    }

    public Callback<D> getCallback() {
        return callback;
    }
}
