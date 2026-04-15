package com.example.piceditor;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import android.app.PendingIntent;
import android.app.RecoverableSecurityException;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.IntentSenderRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.GridLayoutManager;

import com.example.piceditor.adapters.ImageAdapter;
import com.example.piceditor.base.BaseActivityNew;
import com.example.piceditor.base.BaseFragment;
import com.example.piceditor.databinding.ActivityMyDraftBinding;
import com.example.piceditor.model.ImageModel;
import com.example.piceditor.utils.BarsUtils;
import com.example.piceditor.utilsApp.Constant;
import com.example.piceditor.utilsApp.PreferenceUtil;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MyDraftActivity extends BaseActivityNew<ActivityMyDraftBinding> {

    private ImageAdapter adapter;
    private ImageAdapter pendingAdapter;

    private final ActivityResultLauncher<IntentSenderRequest> deleteRequestLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.StartIntentSenderForResult(),
                    result -> {
                        if (result.getResultCode() == RESULT_OK && pendingAdapter != null) {
                            pendingAdapter.deleteSelected();
                            resetSelectMode();
                        }
                    }
            );

    @Override
    public int getLayoutRes() {
        return R.layout.activity_my_draft;
    }

    @Override
    public int getFrame() {
        return 0;
    }

    @Override
    public void getDataFromIntent() {}

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
    protected void onResume() {
        super.onResume();
        if (PreferenceUtil.getInstance(this)
                .getValue(Constant.SharePrefKey.BANNER_COL, "no").equals("yes")) {
        } else {
            initBanner(getBinding().adViewContainer);
            getBinding().banner.getRoot().setVisibility(View.GONE);
        }
    }

    @Override
    public void setListener() {}

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
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        List<ImageModel> images = getSavedImages(this);

        if (images.isEmpty()) {
            getBinding().llEmptyDraft.setVisibility(VISIBLE);
            getBinding().rcvDrafts.setVisibility(GONE);
        } else {
            getBinding().rcvDrafts.setVisibility(VISIBLE);
            getBinding().llEmptyDraft.setVisibility(GONE);
        }

        adapter = new ImageAdapter(new ArrayList<>(images));
        getBinding().rcvDrafts.setLayoutManager(new GridLayoutManager(this, 2));
        getBinding().rcvDrafts.setAdapter(adapter);

        // Cập nhật số lượng trên nút Delete
        adapter.setOnSelectionChanged(count -> {
            if (count > 0) {
                getBinding().layoutDelete.setVisibility(VISIBLE);
                getBinding().tvDelete.setText("Delete(" + count + ")");
            } else {
                getBinding().layoutDelete.setVisibility(GONE);
            }
            return null;
        });

        // Nút Back
        getBinding().btnBack.setOnClickListener(v -> finish());

        // Nút Select
        getBinding().btnNext.setOnClickListener(v -> {
            adapter.setSelectMode(true);
            getBinding().btnNext.setVisibility(GONE);
            getBinding().btnCancel.setVisibility(VISIBLE);
        });

        // Nút Cancel
        getBinding().btnCancel.setOnClickListener(v -> {
            resetSelectMode();
        });

        // Nút Delete
        getBinding().layoutDelete.setOnClickListener(v -> {
            showDeleteDialog();
        });
    }

    private void showDeleteDialog() {
        int count = adapter.getSelectedCount();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            new AlertDialog.Builder(this)
                    .setTitle("Xóa ảnh")
                    .setMessage("Bạn có chắc muốn xóa " + count + " ảnh?")
                    .setPositiveButton("Xóa", (dialog, which) -> deleteSelectedImages())
                    .setNegativeButton("Huỷ", null)
                    .show();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.Q)
    private void deleteSelectedImages() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            List<Uri> uris = new ArrayList<>();
            for (ImageModel item : adapter.getSelectedItems()) {
                uris.add(item.getUri());
            }
            try {
                PendingIntent pi = MediaStore.createDeleteRequest(getContentResolver(), uris);
                pendingAdapter = adapter;
                IntentSenderRequest request = new IntentSenderRequest.Builder(
                        pi.getIntentSender()
                ).build();
                deleteRequestLauncher.launch(request);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            for (ImageModel item : adapter.getSelectedItems()) {
                try {
                    getContentResolver().delete(item.getUri(), null, null);
                } catch (RecoverableSecurityException e) {
                    pendingAdapter = adapter;
                    try {
                        IntentSenderRequest request = new IntentSenderRequest.Builder(
                                e.getUserAction().getActionIntent().getIntentSender()
                        ).build();
                        deleteRequestLauncher.launch(request);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            }
            adapter.deleteSelected();
            resetSelectMode();
        }
    }

    private void resetSelectMode() {
        adapter.clearSelection();
        getBinding().btnNext.setVisibility(VISIBLE);
        getBinding().btnCancel.setVisibility(GONE);
        getBinding().layoutDelete.setVisibility(GONE);

        if (adapter.getItemCount() == 0) {
            getBinding().llEmptyDraft.setVisibility(VISIBLE);
            getBinding().rcvDrafts.setVisibility(GONE);
        }
    }

    public static List<ImageModel> getSavedImages(Context context) {
        List<ImageModel> list = new ArrayList<>();

        Uri collection = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;

        String[] projection = new String[]{
                MediaStore.Images.Media._ID,
                MediaStore.Images.Media.DISPLAY_NAME,
                MediaStore.Images.Media.DATE_TAKEN,
                MediaStore.Images.Media.DATE_ADDED
        };

        String selection = MediaStore.Images.Media.RELATIVE_PATH + " LIKE ?";
        String[] selectionArgs = new String[]{"%Pictures/PhotoCollage%"};

        String sortOrder = MediaStore.Images.Media.DATE_TAKEN + " DESC";

        Cursor cursor = context.getContentResolver().query(
                collection,
                projection,
                selection,
                selectionArgs,
                sortOrder
        );

        if (cursor != null) {
            int idCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID);
            int nameCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME);
            int dateTakenCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_TAKEN);
            int dateAddedCol = cursor.getColumnIndex(MediaStore.Images.Media.DATE_ADDED);

            while (cursor.moveToNext()) {
                long id = cursor.getLong(idCol);
                String name = cursor.getString(nameCol);
                long dateTaken = cursor.getLong(dateTakenCol);

                long dateAdded = 0;
                if (dateAddedCol != -1) {
                    dateAdded = cursor.getLong(dateAddedCol);
                }

                long finalDate;
                if (dateTaken != 0) {
                    finalDate = dateTaken;
                } else if (dateAdded > 0) {
                    finalDate = dateAdded * 1000;
                } else {
                    finalDate = System.currentTimeMillis();
                }

                Uri uri = ContentUris.withAppendedId(collection, id);
                list.add(new ImageModel(uri, name, finalDate));
            }

            cursor.close();
        }

        return list;
    }

    public static String formatDateTime(long time) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
        return sdf.format(new Date(time));
    }
}