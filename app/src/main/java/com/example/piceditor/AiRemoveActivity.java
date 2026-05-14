package com.example.piceditor;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;

import com.bumptech.glide.Glide;
import com.example.piceditor.base.BaseActivityNew;
import com.example.piceditor.base.BaseFragment;
import com.example.piceditor.databinding.ActivityAiRemoveBinding;
import com.example.piceditor.utils.BarsUtils;
import com.example.piceditor.utilsApp.Constant;
import com.example.piceditor.utilsApp.PreferenceUtil;

import java.io.File;

public class AiRemoveActivity extends BaseActivityNew<ActivityAiRemoveBinding> {

    public static final String EXTRA_IMAGE_PATH = "image_path";

    private String imagePath;

    @Override
    public int getLayoutRes() {
        return R.layout.activity_ai_remove;
    }

    @Override
    public int getFrame() {
        return 0;
    }

    @Override
    public void getDataFromIntent() {
        // ✅ Lấy đường dẫn ảnh từ Intent
        if (getIntent() != null) {
            imagePath = getIntent().getStringExtra(EXTRA_IMAGE_PATH);
        }
    }

    @Override
    public void doAfterOnCreate() {
        if (PreferenceUtil.getInstance(this)
                .getValue(Constant.SharePrefKey.BANNER_COL, "no").equals("yes")) {
            initBanner(getBinding().banner.adViewContainer);
        } else {
            initBanner(getBinding().adViewContainer);
            getBinding().banner.getRoot().setVisibility(View.GONE);
        }
    }

    @Override
    public void setListener() {
        getBinding().btnBack.setOnClickListener(v -> finish());

        getBinding().btnNext.setOnClickListener(v -> {
            // TODO: xử lý remove background ở đây
            // Ví dụ: chuyển sang activity xử lý kết quả, hoặc gọi API, v.v.
            if (imagePath == null || imagePath.isEmpty()) {
                android.widget.Toast.makeText(this,
                        "Chưa có ảnh để xử lý", android.widget.Toast.LENGTH_SHORT).show();
                return;
            }
            startRemoveBackground();
        });
    }

    @Override
    public BaseFragment initFragment() {
        return null;
    }

    @Override
    public void afterSetContentView() {
        super.afterSetContentView();
        BarsUtils.setHideNavigation(this);
        BarsUtils.setStatusBarColor(this, Color.parseColor("#01000000"));
        BarsUtils.setAppearanceLightStatusBars(this, true);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!PreferenceUtil.getInstance(this)
                .getValue(Constant.SharePrefKey.BANNER_COL, "no").equals("yes")) {
            initBanner(getBinding().adViewContainer);
            getBinding().banner.getRoot().setVisibility(View.GONE);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // ✅ Hiển thị ảnh bằng Glide
        loadImageToView();
    }

    private void loadImageToView() {
        if (imagePath == null || imagePath.isEmpty()) {
            android.widget.Toast.makeText(this,
                    "Không tìm thấy ảnh", android.widget.Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Load ảnh từ path (có thể là file path hoặc content URI)
        Object source;
        if (imagePath.startsWith("content://") || imagePath.startsWith("file://")) {
            source = android.net.Uri.parse(imagePath);
        } else {
            source = new File(imagePath);
        }

        Glide.with(this)
                .load(source)
                .into(getBinding().imgBackground);
    }

    private void startRemoveBackground() {
        // ✅ TODO: Implement logic remove background ở đây
        // Có thể dùng:
        //   - ML Kit Subject Segmentation (Google)
        //   - Remove.bg API
        //   - TensorFlow Lite model
        //   - Hoặc thư viện khác

        android.widget.Toast.makeText(this,
                "Đang xử lý remove background...",
                android.widget.Toast.LENGTH_SHORT).show();

        // Ví dụ chuyển sang activity kết quả:
        // Intent intent = new Intent(this, RemoveBgResultActivity.class);
        // intent.putExtra("image_path", imagePath);
        // startActivity(intent);
    }
}