package com.example.piceditor.base;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.databinding.ViewDataBinding;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;


public abstract class BaseFragment<T extends ViewDataBinding> extends Fragment {
    public abstract int getLayoutRes();
    public abstract void initView();
    public abstract void initData();
    public abstract void setListener();
    public abstract void setObserver();

    public abstract int getFrame();
    protected BaseActivityBlank mActivity;
    protected View rootView;

    private T binding;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            if (!(getActivity() instanceof BaseActivityBlank)) {
                new Throwable("Activity no override BaseActivity");
            }
            mActivity = (BaseActivityBlank) getActivity();
        } catch (Exception e) {
            Log.e("exception", e.getMessage() + "");
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        try {
            bindView(inflater, container, getLayoutRes());
        } catch (Exception e) {
            Log.e("exception", e.getMessage() + "");
        }
        return binding.getRoot();
    }

    public void bindView(LayoutInflater inflater, ViewGroup viewGroup, int res) {
        binding = DataBindingUtil.inflate(
                inflater, res, viewGroup, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        try {
            super.onViewCreated(view, savedInstanceState);
            initView();
            setListener();
            initData();
            setObserver();
        } catch (Exception e) {
            Log.e("exception", e.getMessage() + "");
        }
    }

    public void addFragment(BaseFragment fragment, int frame) {
        try {
            FragmentManager frm = mActivity.getSupportFragmentManager();
            frm.beginTransaction()
                    .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                    .add(frame, fragment)
                    .addToBackStack(fragment.getClass().getSimpleName()).commit();
        } catch (Exception e) {
        }
    }

    public void addFragment(BaseFragment fragment) {
        try {
            FragmentManager frm = mActivity.getSupportFragmentManager();
            frm.beginTransaction()
                    .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                    .add(getFrame(), fragment)
                    .addToBackStack(fragment.getClass().getSimpleName()).commit();
        } catch (Exception e) {
        }
    }

    public void addFragmentNoAnimation(BaseFragment fragment) {
        try {
            FragmentManager frm = mActivity.getSupportFragmentManager();
            frm.beginTransaction()
                    .add(getFrame(), fragment)
                    .addToBackStack(fragment.getClass().getSimpleName()).commit();
        } catch (Exception e) {
        }
    }

    public void addFragmentWithTag(BaseFragment fragment, int frame, String tag) {
        try {
            FragmentManager frm = mActivity.getSupportFragmentManager();
            frm.beginTransaction()
                    .add(frame, fragment, tag)
                    .addToBackStack(fragment.getClass().getSimpleName())
                    .commit();
        } catch (Exception e) {
        }
    }


    public T getBinding() {
        return binding;
    }

    public void showToast(String mess){
        Toast.makeText(mActivity, mess, Toast.LENGTH_SHORT).show();
    }

}
