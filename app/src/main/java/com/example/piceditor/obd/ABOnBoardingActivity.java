package com.example.piceditor.obd;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.viewpager2.widget.ViewPager2;

import com.example.piceditor.MainActivity;
import com.example.piceditor.R;
import com.example.piceditor.ads.Callback;
import com.example.piceditor.ads.InterAds;
import com.example.piceditor.base.BaseActivityNew;
import com.example.piceditor.base.BaseFragment;
import com.example.piceditor.databinding.AbActivityOnBoardingBinding;
import com.example.piceditor.utils.BarsUtils;
import com.example.piceditor.utilsApp.Constant;
import com.example.piceditor.utilsApp.PreferenceUtil;
import com.tbuonomo.viewpagerdotsindicator.DotsIndicator;

public class ABOnBoardingActivity extends BaseActivityNew<AbActivityOnBoardingBinding> implements NavigationHost {

    private ViewPager2 viewPagerOnBoarding;
    private TextView iv_next;
    private DotsIndicator dotsIndicator;

    @Override
    public int getLayoutRes() {
        return R.layout.ab_activity_on_boarding;
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
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        BarsUtils.setHideNavigation(this);

        dotsIndicator = findViewById(R.id.dots_indicator);
        viewPagerOnBoarding = findViewById(R.id.pager_on_boarding);
        iv_next = findViewById(R.id.tv_next);

        ABViewPagerAdapter2 viewPagerAdapter2 = new ABViewPagerAdapter2(this);
        viewPagerOnBoarding.setAdapter(viewPagerAdapter2);
        viewPagerOnBoarding.setOffscreenPageLimit(4);
        dotsIndicator.setViewPager2(viewPagerOnBoarding);

        viewPagerOnBoarding.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                if (position == 1 || position == 2) {
                    hideIndicator();
                } else {
                    showIndicator();
                }
            }
        });

        iv_next.setOnClickListener(v -> {
            int currentPos = viewPagerOnBoarding.getCurrentItem();
            if (currentPos != 3) {
                viewPagerOnBoarding.setCurrentItem(currentPos + 1);
            } else {
                if(PreferenceUtil.getInstance(ABOnBoardingActivity.this).getValue(Constant.SharePrefKey.INTER_Onb, "no").equals("yes")) {
                    InterAds.showAdsBreak(ABOnBoardingActivity.this, new Callback() {
                        @Override
                        public void callback() {
                            Intent intent = new Intent(ABOnBoardingActivity.this, MainActivity.class);
                            startActivity(intent);
                            finish();
                        }
                    });
                }else {
                    Intent intent = new Intent(ABOnBoardingActivity.this, MainActivity.class);
                    startActivity(intent);
                    finish();
                }
            }
        });


    }

    @Override
    public void onBackPressed() {
        if (viewPagerOnBoarding.getCurrentItem() > 0) {
            viewPagerOnBoarding.setCurrentItem(viewPagerOnBoarding.getCurrentItem() - 1);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public void navigateToNext() {
        if (viewPagerOnBoarding != null) {
            int nextItem = viewPagerOnBoarding.getCurrentItem() + 1;
            if (nextItem < viewPagerOnBoarding.getAdapter().getItemCount()) {
                viewPagerOnBoarding.setCurrentItem(nextItem);
            }
        }
    }

    public void hideIndicator() {
        if (dotsIndicator != null) {
            dotsIndicator.setVisibility(View.GONE);
        }
        if (iv_next != null) {
            iv_next.setVisibility(View.GONE);
        }
    }

    public void showIndicator() {
        if (dotsIndicator != null) {
            dotsIndicator.setVisibility(View.VISIBLE);
        }
        if (iv_next != null) {
            iv_next.setVisibility(View.VISIBLE);
        }
    }

}