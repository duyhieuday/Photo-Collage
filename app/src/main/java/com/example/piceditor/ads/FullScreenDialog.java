package com.example.piceditor.ads;

import android.app.Dialog;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;

import com.example.piceditor.R;
import com.example.piceditor.databinding.AbDialogFullScreenBinding;


public class FullScreenDialog extends DialogFragment {
    private AbDialogFullScreenBinding binding;

    private CountDownTimer countDownTimer;

    public static FullScreenDialog display(FragmentManager fragmentManager) {
        FullScreenDialog exampleDialog = new FullScreenDialog();
        exampleDialog.show(fragmentManager, "TAG");
        return exampleDialog;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(DialogFragment.STYLE_NORMAL, R.style.AppTheme_FullScreenDialog);

    }

    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog != null) {
            int width = ViewGroup.LayoutParams.MATCH_PARENT;
            int height = ViewGroup.LayoutParams.MATCH_PARENT;
            if (dialog.getWindow() != null) {
                dialog.getWindow().setLayout(width, height);
                dialog.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
                dialog.getWindow().setWindowAnimations(R.style.Theme_PicEditor);
            }

        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = AbDialogFullScreenBinding.inflate(getLayoutInflater());
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        NativeFullScreen.showNative(requireContext(), binding.flNative, "full_screen");

        binding.ivClose.setOnClickListener(v -> {
            requireDialog().dismiss();
            dismiss();
        });
        Log.e("xxx", "showDialog ");
    }

    public void start() {
        countDownTimer = new CountDownTimer(3000, 1000) {

            public void onTick(long millisUntilFinished) {
                binding.tvCountDown.setText(String.valueOf(millisUntilFinished / 1000));
            }

            public void onFinish() {
                binding.tvCountDown.setVisibility(View.GONE);

                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    binding.ivClose.setVisibility(View.VISIBLE);
                }, 1000);
            }
        }.start();
    }

}
