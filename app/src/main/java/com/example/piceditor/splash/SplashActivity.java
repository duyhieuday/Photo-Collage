package com.example.piceditor.splash;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import com.example.piceditor.LanguageActivity;
import com.example.piceditor.R;
import com.example.piceditor.ads.iap.PremiumActivity;
import com.example.piceditor.ads.iap.PremiumUpsell;
import com.example.piceditor.WeatherApplication;
import com.example.piceditor.ads.GDPRRequestable;
import com.example.piceditor.ads.InterAds;
import com.example.piceditor.ads.InterAdsSplash;
import com.example.piceditor.ads.NativeFullScreen;
import com.example.piceditor.ads.OpenAds;
import com.example.piceditor.base.BaseActivityNew;
import com.example.piceditor.base.BaseFragment;
import com.example.piceditor.databinding.ActivitySplashBinding;
import com.example.piceditor.utils.BarsUtils;
import com.example.piceditor.utilsApp.Constant;
import com.example.piceditor.utilsApp.PreferenceUtil;
import com.example.piceditor.utilsApp.Prefs;
import com.google.android.gms.ads.nativead.NativeAd;

@SuppressLint("CustomSplashScreen")
public class SplashActivity extends BaseActivityNew<ActivitySplashBinding> {

    private boolean isIntented = false;
    public static NativeAd currentNativeAd;

    @Override
    public int getLayoutRes() {
        return R.layout.activity_splash;
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
        initGDPR();
    }

    @Override
    public void setListener() {

    }

    @Override
    public BaseFragment initFragment() {
        return null;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        BarsUtils.setStatusBarColor(this, Color.parseColor("#01000000"));
        BarsUtils.setAppearanceLightStatusBars(this, true);
    }

    @Override
    protected void onResume() {
        super.onResume();
        BarsUtils.setHideNavigation(SplashActivity.this);
    }

    private void intent(String debug) {
        if (isIntented) return;
        isIntented = true;

        // HEHE là cờ tổng bật IAP/ads. Nó KHÔNG phải remote config bật/tắt tuỳ ý mà do ABRC
        // set theo install referrer (gclid/gad_source/gbraid/facebook/instagram) hoặc
        // countryName == "Viet Nam". Tắt → app chạy "sạch": bỏ qua toàn bộ first flow, vì
        // Language và OnBoarding đều gắn native/inter ad.
        //
        // CỐ Ý không đánh dấu KEY_FIRST_FLOW_DONE ở nhánh này. ABRC chạy bất đồng bộ nên lần
        // mở đầu tiên HEHE có thể còn false dù install có referrer quảng cáo; không đánh dấu
        // thì lần mở sau (HEHE đã true) user vẫn được thấy Language + OnBoarding.
        if (!PremiumUpsell.isIapEnabled(this)) {
            PremiumActivity.startFirstRunPaywallOrHome(this);
            return;
        }

        // Language + OnBoarding hiển thị cho tới khi user THỰC SỰ vào được Home — cờ này chỉ
        // được set trong MainActivity. Thoát app giữa Language/OnBoarding/paywall thì lần mở
        // sau chạy lại từ đầu.
        //
        // Vế thứ hai là migration: bản cũ đánh dấu bằng KEY_LANGUAGE=false ngay tại
        // LanguageActivity. User đã qua first flow ở bản cũ chưa từng có KEY_FIRST_FLOW_DONE,
        // không có dòng này thì bản update sẽ bắt toàn bộ họ xem lại Language + OnBoarding.
        boolean firstFlowDone = Prefs.getBoolean(Prefs.Key.KEY_FIRST_FLOW_DONE, false)
                || !Prefs.getBoolean(Prefs.Key.KEY_LANGUAGE, true);

        // Remote first_flow=="yes" ép chạy lại full first flow để AB test. Riêng user đã trả
        // tiền thì KHÔNG ép: họ đã chọn ngôn ngữ rồi, mà first flow còn kèm native/inter ad
        // nên bắt xem lại mỗi lần mở app là vô lý.
        boolean forceFirstFlow = PreferenceUtil.getInstance(this)
                .getValue(Constant.SharePrefKey.FIRST_FLOW, "no").equals("yes")
                && !isPaidUser();

        if (!firstFlowDone || forceFirstFlow) {
            Intent i = new Intent(this, LanguageActivity.class);
            i.putExtra(LanguageActivity.EXTRA_FROM_SPLASH, true);
            startActivity(i);
            finish();
        } else {
            // Paywall first-run (Day-0, điểm chuyển đổi cao nhất) hoặc thẳng Home. Tự finish().
            PremiumActivity.startFirstRunPaywallOrHome(this);
        }
    }

    /**
     * Đã mua Premium (sub) hoặc Remove-ads. Dùng đúng quy ước sẵn có của app khi quyết định
     * bỏ qua quảng cáo (xem InterAds.showAdsBreak): premium == 1 || isRemoveAd.
     */
    private boolean isPaidUser() {
        try {
            com.example.piceditor.ads.Prefs p = new com.example.piceditor.ads.Prefs(this);
            return p.getPremium() == 1 || p.isRemoveAd();
        } catch (Exception e) {
            return false;
        }
    }

    public boolean isNetworkAvailable(Context context) {
        ConnectivityManager manager = (ConnectivityManager) context.getSystemService(CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = manager != null ? manager.getActiveNetworkInfo() : null;
        return networkInfo != null && networkInfo.isConnected();
    }

    private void initAds() {
        if (isNetworkAvailable(this)) {
            InterAdsSplash.initInterAds(this, () -> {
                InterAdsSplash.showAdsBreakWithoutNT(this, () -> intent("1"));
                WeatherApplication.trackingEvent("inter_splash");
            });
            OpenAds.initOpenAds(this, () -> {
            });
            InterAds.initInterAds(this, null);
        } else {
            new Handler(Looper.getMainLooper()).postDelayed(() -> intent("1"), 400);
        }
    }

    private void initGDPR() {
        if (isNetworkAvailable(this)) {
            WeatherApplication.trackingEvent("check_GDPR");
            GDPRRequestable.getGdprRequestable(this).setOnRequestGDPRCompleted(formError -> {
//                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
//                    DC.init(
//                            getApplicationContext(),
//                            BuildConfig.DEBUG ? ADMOB_AD_UNIT_ID_TEST : IdAds.NATIVE_IN_APP
//                    );
//                } else {
//                    PreferenceUtil.getInstance(WeatherApplication.get())
//                            .setValue(Constant.SharePrefKey.HEHE, false);
//                }
                currentNativeAd = null;
                initAds();
                NativeFullScreen.loadNative(this);
            });
            GDPRRequestable.getGdprRequestable(this).requestGDPR();
        } else {
            new Handler(Looper.getMainLooper()).postDelayed(() -> intent("ex"), 100);
        }
    }

    private static final String ADMOB_AD_UNIT_ID_TEST = "ca-app-pub-3940256099942544/2247696110";
}