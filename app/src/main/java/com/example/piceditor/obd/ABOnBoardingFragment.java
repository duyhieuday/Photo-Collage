package com.example.piceditor.obd;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.piceditor.R;
import com.example.piceditor.ads.NativeFullScreen;

public class ABOnBoardingFragment extends Fragment {

    public static ABOnBoardingFragment newInstance(int i) {
        Bundle args = new Bundle();
        args.putInt("pager", i);
        ABOnBoardingFragment fragment = new ABOnBoardingFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        int i = getArguments().getInt("pager");
        if (i == 0) {
            return inflater.inflate(R.layout.ab_fragment_on_boarding_1, container, false);
        } else if (i == 1) {
            return inflater.inflate(R.layout.ab_fragment_on_boarding_2, container, false);
        } else if (i == 2) {
            return inflater.inflate(R.layout.ab_fragment_on_boarding5, container, false);
        } else {
            return inflater.inflate(R.layout.ab_fragment_on_boarding_3, container, false);
        }


    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        int i = getArguments().getInt("pager");
        if (i == 2) {
            FrameLayout flNative = view.findViewById(R.id.fl_native);
            flNative.setVisibility(View.VISIBLE);
            NativeFullScreen.showNative(requireContext(), flNative, "onboarding");
        }
    }
}
