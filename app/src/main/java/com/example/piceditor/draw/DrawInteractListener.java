package com.example.piceditor.draw;

import androidx.annotation.Nullable;

import com.example.piceditor.draw.model.sticker.StickerData;

public interface DrawInteractListener {

    void interactStickerFocusChange(@Nullable StickerData stickerData);

    void interactTouchDown();

    void interactTouchUp();

    void interactUndoRedoChange();

    void interactUpdateBackground(String url);
}
