package com.example.piceditor.ads.iap;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.piceditor.R;

/**
 * Adapter cho carousel feature ở màn IAP (ViewPager2): mỗi trang = ảnh hero + caption.
 */
class IapFeatureAdapter extends RecyclerView.Adapter<IapFeatureAdapter.VH> {

    // "Remove ads" (iap_feature_2) để ĐẦU — benefit #1 cho app ad-first (theo nghiên cứu paywall)
    private final int[] images = {
            R.drawable.iap_feature_2,
            R.drawable.iap_feature_1,
            R.drawable.iap_feature_3
    };
    private final String[] captions;

    IapFeatureAdapter(String[] captions) {
        this.captions = captions;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_iap_feature, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        h.img.setImageResource(images[position]);
        h.caption.setText(captions[position % captions.length]);
    }

    @Override
    public int getItemCount() {
        return images.length;
    }

    static class VH extends RecyclerView.ViewHolder {
        final ImageView img;
        final TextView caption;

        VH(@NonNull View v) {
            super(v);
            img = v.findViewById(R.id.imgFeature);
            caption = v.findViewById(R.id.tvCaption);
        }
    }
}
