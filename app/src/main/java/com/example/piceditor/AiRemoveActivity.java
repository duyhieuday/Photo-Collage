package com.example.piceditor;

import android.content.ContentResolver;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.example.piceditor.ads.InterAds;
import com.example.piceditor.base.BaseActivityNew;
import com.example.piceditor.base.BaseFragment;
import com.example.piceditor.databinding.ActivityAiRemoveBinding;
import com.example.piceditor.sever.ai_remove_bg.presenter.WorkPresenter;
import com.example.piceditor.utils.BarsUtils;
import com.example.piceditor.utilsApp.Constant;
import com.example.piceditor.utilsApp.PreferenceUtil;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class AiRemoveActivity extends BaseActivityNew<ActivityAiRemoveBinding> {

    private String imagePath;
    private WorkPresenter workPresenter;
    private boolean isProcessing = false;
    public static final String EXTRA_IMAGE_PATH = "image_path";

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
        getBinding().btnBack.setOnClickListener(v -> {
            if (isProcessing) {
                Toast.makeText(this, "Đang xử lý, vui lòng đợi...", Toast.LENGTH_SHORT).show();
                return;
            }
            InterAds.showAdsBreak(AiRemoveActivity.this, this::finish);
        });

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
    protected void onDestroy() {
        if (workPresenter != null) {
            workPresenter.dispose();
        }
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        if (isProcessing) {
            Toast.makeText(this, "Đang xử lý, vui lòng đợi...", Toast.LENGTH_SHORT).show();
            return;
        }
        InterAds.showAdsBreak(this, super::onBackPressed);
        super.onBackPressed();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        workPresenter = new WorkPresenter();
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

    private void setLoading(boolean loading) {
        if (isDestroyed() || isFinishing()) return;  // ✅ Safety check
        isProcessing = loading;
        getBinding().progressLoading.setVisibility(loading ? View.VISIBLE : View.GONE);
        getBinding().btnNext.setEnabled(!loading);
        getBinding().btnNext.setAlpha(loading ? 0.5f : 1f);
    }
    private void startRemoveBackground() {
        setLoading(true);                          // show ProgressBar, disable btnNext

        new Thread(() -> {
            try {
                File inputFile = prepareInputFile();       // I/O off main thread
                runOnUiThread(() -> callRemoveBgApi(inputFile));
            } catch (IOException e) {
                runOnUiThread(() -> {
                    setLoading(false);
                    Toast.makeText(AiRemoveActivity.this,
                            "Không đọc được file ảnh", Toast.LENGTH_SHORT ).show();
                });
            }
        }).start();
    }

    private File prepareInputFile() throws IOException {
        if (imagePath.startsWith("content://")) {
            Uri uri = Uri.parse(imagePath);
            ContentResolver resolver = getContentResolver();
            File outFile = new File(getCacheDir(),
                    "ai_remove_input_" + System.currentTimeMillis() + ".jpg");
            try (InputStream in = resolver.openInputStream(uri);
                 FileOutputStream out = new FileOutputStream(outFile)) {
                if (in == null) throw new IOException("Cannot open input stream for " + uri);
                byte[] buf = new byte[8192];
                int n;
                while ((n = in.read(buf)) > 0) {
                    out.write(buf, 0, n);
                }
            }
            return outFile;
        }
        if (imagePath.startsWith("file://")) {
            String path = Uri.parse(imagePath).getPath();
            if (path == null) throw new IOException("Invalid file uri: " + imagePath);
            return new File(path);
        }
        return new File(imagePath);
    }

    private void callRemoveBgApi(File inputFile) {
        workPresenter.removeBg(inputFile, result -> {
            setLoading(false);

            // ✅ Check chặt chẽ hơn
            if (result == null || result.getValue().getUrl().isEmpty()) {
                Toast.makeText(AiRemoveActivity.this,
                        "Remove background thất bại", Toast.LENGTH_SHORT).show();
                return;
            }

            String resultUrl = result.getValue().getUrl();
            Glide.with(AiRemoveActivity.this)
                    .load(resultUrl)
                    .into(getBinding().imgBackground);

            Toast.makeText(AiRemoveActivity.this,
                    "Xóa nền thành công", Toast.LENGTH_SHORT).show();
        });
    }

}