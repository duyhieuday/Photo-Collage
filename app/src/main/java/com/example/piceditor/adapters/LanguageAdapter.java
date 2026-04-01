package com.example.piceditor.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.piceditor.R;
import com.example.piceditor.model.LanguageApp;
import com.example.piceditor.model.LanguageListener;

import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class LanguageAdapter extends RecyclerView.Adapter<LanguageAdapter.LanguageViewHolder>{

    private List<LanguageApp> languageList;
    private int  selectedPosition = RecyclerView.NO_POSITION;
    private LanguageListener languageListener;

    public void setLanguageListener(LanguageListener languageListener) {
        this.languageListener = languageListener;
    }

    public void setData(List<LanguageApp> list){
        this.languageList = list;
        notifyDataSetChanged();
    }
    @NonNull
    @Override
    public LanguageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new LanguageViewHolder(
                LayoutInflater.from(parent.getContext()).inflate(
                        R.layout.item_container_language,
                        parent,
                        false
                )
        );
    }

    @Override
    public void onBindViewHolder(@NonNull LanguageViewHolder holder, int position) {
        LanguageApp language = languageList.get(position);
        holder.textItemLanguage.setText(language.getLanguageName());

        if (selectedPosition == position) {
//            holder.iconItemCheck.setVisibility(View.VISIBLE);
            holder.iconItemCheck.setImageResource(R.drawable.ic_select);
            holder.layoutItemLanguage.setBackgroundResource(R.drawable.bg_language);
        } else {
            holder.iconItemCheck.setImageResource(R.drawable.ic_unselect);
            holder.layoutItemLanguage.setBackgroundResource(R.drawable.bg_language_unselected);
        }

        holder.layoutItemLanguage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                int previousPosition = selectedPosition;
                selectedPosition = holder.getAdapterPosition();
                notifyItemChanged(selectedPosition);
                if (previousPosition != RecyclerView.NO_POSITION) {
                    notifyItemChanged(previousPosition);
                }
                languageListener.onLanguageClick(holder.getAdapterPosition(), language);

            }
        });

        Context context = holder.itemView.getContext();
        int drawableRes = context.getResources().getIdentifier(language.getFlag(), null, context.getPackageName());

        Glide.with(context)
                .load(drawableRes)
                .into(holder.circleFlagImage);
    }

    @Override
    public int getItemCount() {
        return languageList.size();
    }

    static class LanguageViewHolder extends RecyclerView.ViewHolder{
        TextView textItemLanguage;
        RelativeLayout layoutItemLanguage;
        CircleImageView circleFlagImage;
        ImageView iconItemCheck;
        public LanguageViewHolder(@NonNull View itemView) {
            super(itemView);

            layoutItemLanguage = itemView.findViewById(R.id.layoutItemLanguage);
            textItemLanguage = itemView.findViewById(R.id.textItemLanguage);
            circleFlagImage = itemView.findViewById(R.id.circleFlagImage);
            iconItemCheck = itemView.findViewById(R.id.iconItemCheck);

        }
    }

}
