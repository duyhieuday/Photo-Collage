package com.example.piceditor;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.piceditor.adapters.ImageAdapter;
import com.example.piceditor.base.BaseActivityNew;
import com.example.piceditor.base.BaseFragment;
import com.example.piceditor.databinding.ActivityMyDraftBinding;
import com.example.piceditor.model.ImageModel;
import com.example.piceditor.utils.BarsUtils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MyDraftActivity extends BaseActivityNew<ActivityMyDraftBinding> {

    @Override
    public int getLayoutRes() {
        return R.layout.activity_my_draft;
    }

    @Override
    public int getFrame() {
        return 0;
    }

    @Override
    public void getDataFromIntent() {

    }

    @Override
    public void doAfterOnCreate() {

    }

    @Override
    public void setListener() {

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
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getBinding().btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        List<ImageModel> images = getSavedImages(this);

        if (images.isEmpty()){
            getBinding().llEmptyDraft.setVisibility(VISIBLE);
            getBinding().rcvDrafts.setVisibility(GONE);
        }else {
            getBinding().rcvDrafts.setVisibility(VISIBLE);
            getBinding().llEmptyDraft.setVisibility(GONE);
        }

        ImageAdapter adapter = new ImageAdapter(images);

        getBinding().rcvDrafts.setLayoutManager(new GridLayoutManager(this, 2));
        getBinding().rcvDrafts.setAdapter(adapter);

    }

    public static List<ImageModel> getSavedImages(Context context) {
        List<ImageModel> list = new ArrayList<>();

        Uri collection = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;

        String[] projection = new String[]{
                MediaStore.Images.Media._ID,
                MediaStore.Images.Media.DISPLAY_NAME,
                MediaStore.Images.Media.DATE_TAKEN
        };

        String selection = MediaStore.Images.Media.RELATIVE_PATH + " LIKE ?";
        String[] selectionArgs = new String[]{"%Pictures/MyApp%"};

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
            int dateAddedCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED);

            while (cursor.moveToNext()) {
                long id = cursor.getLong(idCol);
                String name = cursor.getString(nameCol);
                long dateTaken = cursor.getLong(dateTakenCol);
                long dateAdded = cursor.getLong(dateAddedCol);

                // fallback nếu dateTaken = 0
                long finalDate = dateTaken != 0 ? dateTaken : dateAdded;

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