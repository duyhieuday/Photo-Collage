package com.example.piceditor.obd;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

public class ABViewPagerAdapter2 extends FragmentStateAdapter {
    public ABViewPagerAdapter2(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case 0:
                return new ABOnBoardingFragment1();
            case 1:
                return new ABOnBoardingFragment2();
            case 2:
                return new ABOnBoardingFragment5();
            case 3:
                return new ABOnBoardingFragment3();
        }
        return null;

    }

    @Override
    public int getItemCount() {
        return 4;
    }
}
