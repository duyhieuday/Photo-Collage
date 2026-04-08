package com.example.piceditor.adapters;

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
import com.example.piceditor.templates_editor.Template;

import java.util.List;

public class TemplateAdapter extends RecyclerView.Adapter<TemplateAdapter.TempViewHolder>{

    private List<Template> templateList;
    private TemplateListener templateListener;

    public void setClickListener(TemplateListener templateListener){
        this.templateListener = templateListener;
    }

    public TemplateAdapter() {
    }

    public void setData(List<Template> list) {
        this.templateList = list;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public TempViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new TempViewHolder(
                LayoutInflater.from(parent.getContext()).inflate(
                        R.layout.item_image_template,
                        parent,
                        false
                )
        );
    }

    @Override
    public void onBindViewHolder(@NonNull TempViewHolder holder, int position) {
        Template template = templateList.get(position);
        Context context = holder.itemView.getContext();

        if (template.getImage() != null){
            Glide.with(context)
                    .load(Uri.parse(template.getImageAsset()))
                    .into(holder.imgTemp);
        }else {
            holder.imgTemp.setImageResource(R.drawable.ic_backward);
        }

        holder.rlTemp.setOnClickListener(v -> templateListener.onClick(holder.getAdapterPosition(), template));

    }

    @Override
    public int getItemCount() {
        return templateList.size();
    }

    static class TempViewHolder extends RecyclerView.ViewHolder {

        ImageView imgTemp;
        RelativeLayout rlTemp;

        public TempViewHolder(@NonNull View itemView) {
            super(itemView);
            imgTemp = itemView.findViewById(R.id.imgTemp);
            rlTemp = itemView.findViewById(R.id.rlTemp);
        }
    }

}
