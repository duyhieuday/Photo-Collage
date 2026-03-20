package com.example.piceditor.draw.test;

import android.content.Context;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.piceditor.R;

import java.util.List;

public class BeardAdapter extends RecyclerView.Adapter<BeardAdapter.BeardViewHolder>{

    private List<Beard> beardList;
    private BeardListener beardListener;

    public void setClickListener(BeardListener beardListener){
        this.beardListener = beardListener;
    }

    public BeardAdapter() {
    }

    public void setData(List<Beard> list) {
        this.beardList = list;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public BeardViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new BeardViewHolder(
                LayoutInflater.from(parent.getContext()).inflate(
                        R.layout.item_image_beard,
                        parent,
                        false
                )
        );
    }

    @Override
    public void onBindViewHolder(@NonNull BeardViewHolder holder, int position) {
        Beard beard = beardList.get(position);
        Context context = holder.itemView.getContext();

        if (beard.getImage() != null){
            Glide.with(context)
                    .load(Uri.parse(beard.getImageAsset()))
                    .into(holder.imageBeard);
        }else {
            holder.imageBeard.setImageResource(R.drawable.ic_backward);
        }

        holder.layoutBeard.setOnClickListener(v -> beardListener.onClickInsect(holder.getAdapterPosition(), beard));

    }

    @Override
    public int getItemCount() {
        return beardList.size();
    }

    static class BeardViewHolder extends RecyclerView.ViewHolder {

        ImageView imageBeard;
        RelativeLayout layoutBeard;

        public BeardViewHolder(@NonNull View itemView) {
            super(itemView);
            imageBeard = itemView.findViewById(R.id.imageBeard);
            layoutBeard = itemView.findViewById(R.id.layoutBeard);
        }
    }

}
