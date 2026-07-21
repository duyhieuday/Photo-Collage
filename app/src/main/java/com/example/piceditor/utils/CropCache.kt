package com.example.piceditor.utils

import android.content.Context
import java.io.File

/**
 * File tạm của uCrop trong `cacheDir`: `crop_src_*` (ảnh đưa vào crop) và `crop_dest_*` (kết quả).
 *
 * Trước đây không chỗ nào xoá → mỗi lần crop để lại 2 ảnh full-size nằm lại cho tới khi
 * hệ thống dọn cache. Gom việc đặt tên + dọn về đây để prefix luôn khớp nhau.
 */
object CropCache {

    private const val PREFIX_SRC  = "crop_src_"
    private const val PREFIX_DEST = "crop_dest_"

    /** File tạm cũ hơn mốc này chắc chắn không còn màn nào đang đọc. */
    private const val STALE_MS = 60 * 60 * 1000L   // 1 giờ

    fun newSrcFile(context: Context, ext: String = "jpg"): File =
        File(context.cacheDir, "$PREFIX_SRC${System.currentTimeMillis()}.$ext")

    fun newDestFile(context: Context, ext: String = "jpg"): File =
        File(context.cacheDir, "$PREFIX_DEST${System.currentTimeMillis()}.$ext")

    /**
     * Xoá rác của các phiên crop TRƯỚC. Gọi mỗi lần mở crop.
     *
     * Chỉ đụng file đã cũ: `crop_dest_` có thể đang được màn khác đọc
     * (CollageActivity forward URI sang FilterCollageActivity, AfterRemoveActivity load bằng Glide).
     */
    fun purgeStale(context: Context) {
        val deadline = System.currentTimeMillis() - STALE_MS
        runCatching {
            context.cacheDir.listFiles { f ->
                (f.name.startsWith(PREFIX_SRC) || f.name.startsWith(PREFIX_DEST)) &&
                        f.lastModified() < deadline
            }?.forEach { it.delete() }
        }
    }

    /** Xoá ngay 1 file tạm đã dùng xong. Bỏ qua mọi lỗi — dọn cache không được làm sập app. */
    fun delete(file: File?) {
        runCatching { file?.delete() }
    }
}
