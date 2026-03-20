package com.example.piceditor.draw.test;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.piceditor.CollageActivity;
import com.example.piceditor.R;
import com.example.piceditor.base.BaseFragment;
import com.example.piceditor.databinding.FragmentBeardBinding;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.List;

public class BeardFragment extends BaseFragment<FragmentBeardBinding> {

    private BeardAdapter beardAdapter;
    private List<Beard> beardList;

    @Override
    public int getLayoutRes() {
        return R.layout.fragment_beard;
    }

    @Override
    public void initView() {

    }

    @Override
    public void initData() {

    }

    @Override
    public void setListener() {

    }

    @Override
    public void setObserver() {

    }

    @Override
    public int getFrame() {
        return 0;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        bindView(inflater, container, R.layout.fragment_beard);

        Gson gson = new Gson();
        Type beard = new TypeToken<List<Beard>>() {
        }.getType();
        List<Beard> beards = null;
        try {
            beards = gson.fromJson(new InputStreamReader(requireActivity().getAssets().open("beard.json")), beard);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        getBinding().rcvBeard.setLayoutManager(new LinearLayoutManager(requireActivity(), LinearLayoutManager.HORIZONTAL
                , false
        ));

        beardList = beards;
        beardAdapter = new BeardAdapter();
        beardAdapter.setData(beardList);

        getBinding().rcvBeard.setAdapter(beardAdapter);
        getBinding().rcvBeard.smoothScrollToPosition(0);

        beardAdapter.setClickListener(new BeardListener() {
            @Override
            public void onClickInsect(int position, Beard beard) {
                ((CollageActivity)requireActivity()).onSelectModel(beard.getImageAsset());
            }
        });

        return getBinding().getRoot();
    }



}