package com.example.piceditor;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import com.example.piceditor.ads.Callback;
import com.example.piceditor.ads.InterAds;
import com.example.piceditor.base.BaseActivityNew;
import com.example.piceditor.base.BaseFragment;
import com.example.piceditor.databinding.ActivitySettingBinding;
import com.example.piceditor.utilsApp.BarsUtils;
import com.example.piceditor.utilsApp.Constant;
import com.example.piceditor.utilsApp.PreferenceUtil;
import com.example.piceditor.utilsApp.Prefs;

import java.util.Locale;

public class SettingActivity extends BaseActivityNew<ActivitySettingBinding> {

    @Override
    public int getLayoutRes() {
        return R.layout.activity_setting;
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
        if (PreferenceUtil.getInstance(this).getValue(Constant.SharePrefKey.BANNER_COL, "no")
                .equals("yes")
        ) {
            initBanner(getBinding().banner.adViewContainer);
        } else {
            initBanner(getBinding().adViewContainer);
            getBinding().banner.getRoot().setVisibility(View.GONE);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (PreferenceUtil.getInstance(this).getValue(Constant.SharePrefKey.BANNER_COL, "no")
                .equals("yes")
        ) {
        } else {
            initBanner(getBinding().adViewContainer);
            getBinding().banner.getRoot().setVisibility(View.GONE);
        }
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
        BarsUtils.setStatusBarColor(this, Color.parseColor("#01000000"));
        BarsUtils.setAppearanceLightStatusBars(this, true);
        BarsUtils.setHideNavigation(SettingActivity.this);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getBinding().icBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                InterAds.showAdsBreak(SettingActivity.this, () -> finish());
            }
        });

        getBinding().layoutLanguage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                InterAds.showAdsBreak(SettingActivity.this, new Callback() {
                    @Override
                    public void callback() {
                        Intent intent = new Intent(SettingActivity.this, LanguageActivity.class);
                        startActivity(intent);
                        finish();
                    }
                });
            }
        });

        getBinding().layoutFeedback.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                feedBack();
            }
        });

        getBinding().layoutShare.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                shareApp();
            }
        });

        getBinding().layoutRate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                rateApp();
            }
        });

        getBinding().layoutPolicy.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    String url = "";
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                    if (intent.resolveActivity(getPackageManager()) != null) {
                        startActivity(intent); // Start the intent to open the link
                    }
                } catch (Exception e) {
                }
            }
        });

        String s = Prefs.getString(Prefs.Key.LANGUAGE_NAME, "");
        if (s.isEmpty()) {
            String defaultLanguage = Locale.getDefault().getDisplayLanguage();
            getBinding().textLanguage.setText(defaultLanguage);
        } else {
            getBinding().textLanguage.setText(s);
        }

    }

    private void rateApp() {
        try {
            String packageName = getPackageName();
            Uri uri = Uri.parse("market://details?id=" + packageName);
            Intent rateIntent = new Intent(Intent.ACTION_VIEW, uri);
            if (!(getPackageManager().queryIntentActivities(rateIntent, 0).size() > 0)) {
                Uri webUri = Uri.parse("https://play.google.com/store/apps/details?id=" + packageName);
                rateIntent = new Intent(Intent.ACTION_VIEW, webUri);
            }
            startActivity(rateIntent);
        } catch (Exception e) {
        }
    }

    private void feedBack() {
            Intent Email = new Intent(Intent.ACTION_SEND);
            Email.setType("text/email");
            Email.putExtra(Intent.EXTRA_EMAIL, new String[]{"duyhieuworks@gmail.com"});
            Email.putExtra(Intent.EXTRA_SUBJECT, "Feedback Photo Collage");
            Email.putExtra(Intent.EXTRA_TEXT, "Message: ");

            Email.setPackage("com.google.android.gm");

            try {
                startActivity(Email);
            } catch (Exception e) {
                Toast.makeText(SettingActivity.this, getString(R.string.gmail_not_install), Toast.LENGTH_SHORT).show();
            }
    }

    private void shareApp() {
        try {
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, "My application name");
            String shareMessage = "\nLet me recommend you this application\n\n";
            shareMessage = shareMessage + "https://play.google.com/store/apps/details?id=" + BuildConfig.APPLICATION_ID + "\n\n";
            shareIntent.putExtra(Intent.EXTRA_TEXT, shareMessage);
            startActivity(Intent.createChooser(shareIntent, "choose one"));
        } catch (Exception e) {
            //e.toString();
        }
    }

}