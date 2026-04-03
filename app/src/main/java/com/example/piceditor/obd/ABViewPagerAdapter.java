package com.example.piceditor.obd;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;


public class ABViewPagerAdapter extends FragmentStateAdapter {

    public ABViewPagerAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        return null;
    }


    @Override
    public int getItemCount() {
        return 4;
    }
}
