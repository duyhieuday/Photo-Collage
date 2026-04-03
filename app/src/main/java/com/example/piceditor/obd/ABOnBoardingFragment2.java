package com.example.piceditor.obd;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

import com.example.piceditor.R;
import com.example.piceditor.utils.BarsUtils;
import com.tbuonomo.viewpagerdotsindicator.DotsIndicator;

public class ABOnBoardingFragment2 extends Fragment {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    private NavigationHost navigationHost;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof NavigationHost) {
            navigationHost = (NavigationHost) context;
        } else {
            throw new ClassCastException(context.toString() + " must implement NavigationHost");
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        hideSystemUI();
    }

    private void hideSystemUI() {
        WindowInsetsControllerCompat windowInsetsController =
                WindowCompat.getInsetsController(requireActivity().getWindow(), requireActivity().getWindow().getDecorView());
        windowInsetsController.setSystemBarsBehavior(
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        );

        windowInsetsController.hide(WindowInsetsCompat.Type.navigationBars());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.ab_fragment_on_boarding_2, container, false);
        TextView iv_next = view.findViewById(R.id.tv_next);
        DotsIndicator dots_indicator = view.findViewById(R.id.dots_indicator);
        ViewPager2 viewPager = view.findViewById(R.id.viewPager);

        BarsUtils.setStatusBarColor(requireActivity(), Color.parseColor("#01000000"));
        BarsUtils.setAppearanceLightStatusBars(requireActivity(), false);

        viewPager.setAdapter(new ABViewPagerAdapter(requireActivity()));
        dots_indicator.setViewPager2(viewPager);

        viewPager.setCurrentItem(1, false);

        iv_next.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (navigationHost != null) {
                    navigationHost.navigateToNext();
                }
            }
        });

        return view;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        navigationHost = null;
    }
}