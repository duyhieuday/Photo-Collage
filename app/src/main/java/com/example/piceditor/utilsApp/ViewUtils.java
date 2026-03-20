package com.example.piceditor.utilsApp;

import android.graphics.PointF;
import android.view.View;
import android.view.ViewConfiguration;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.core.util.Pair;
import androidx.transition.AutoTransition;
import androidx.transition.Transition;
import androidx.transition.TransitionManager;

import java.util.ArrayList;
import java.util.List;

public class ViewUtils {

    @NonNull
    public static PointF getCenter(@NonNull View view) {
        return new PointF(
                view.getX() + view.getWidth() / 2f,
                view.getY() + view.getHeight() / 2f
        );
    }

    public static boolean isRtl(@NonNull View view) {
        return view.getLayoutDirection() == View.LAYOUT_DIRECTION_RTL;
    }

    public static void applyConstrainSet(@NonNull ConstraintLayout constraint, int layoutRes, Transition.TransitionListener listener) {
        // save children visibility
        List<Pair<View, Integer>> children = new ArrayList<>();
        for (int i = 0; i < constraint.getChildCount(); i++) {
            View child = constraint.getChildAt(i);
            children.add(new Pair<>(child, child.getVisibility()));
        }

        if (listener != null) {
            TransitionManager.beginDelayedTransition(constraint, new AutoTransition().addListener(listener));
        }

        ConstraintSet set = new ConstraintSet();
        set.load(constraint.getContext(), layoutRes);
        set.applyTo(constraint);

        // restore children visibility
        for (Pair<View, Integer> child : children) {
            child.first.setVisibility(child.second);
        }
    }

    public static void disableTapTimeout(@NonNull View view) {
        disableTapTimeout(view, ViewConfiguration.getTapTimeout());
    }

    public static void disableTapTimeout(@NonNull View view, long timeout) {
        if (!view.isClickable()) {
            // already disabled
            return;
        }
        view.setClickable(false);
        view.postDelayed(() -> view.setClickable(true), timeout);
    }

    public static void singleClick(@NonNull View view, View.OnClickListener listener) {
        view.setOnClickListener(new SingleClickListener(listener));
    }

    private static class SingleClickListener implements View.OnClickListener {

        private static long mLastClickTime;
        View.OnClickListener mOnClickListener;

        public SingleClickListener(View.OnClickListener listener) {
            this.mOnClickListener = listener;
        }

        @Override
        public void onClick(@NonNull View v) {
            if (System.currentTimeMillis() - mLastClickTime < 300) {
                return;
            }
            mLastClickTime = System.currentTimeMillis();
            v.post(() -> {
                if (mOnClickListener != null) {
                    mOnClickListener.onClick(v);
                }
            });
        }
    }

    private ViewUtils() {
        //no instance
    }
}
