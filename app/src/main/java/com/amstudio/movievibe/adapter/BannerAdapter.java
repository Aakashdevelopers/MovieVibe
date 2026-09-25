package com.amstudio.movievibe.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.amstudio.movievibe.R;
import com.amstudio.movievibe.databinding.ItemBannerBinding;
import com.amstudio.movievibe.model.BannerItem;
import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.List;

public class BannerAdapter extends RecyclerView.Adapter<BannerAdapter.BannerViewHolder> {

    public interface OnBannerClickListener {
        void onBannerClick(BannerItem banner);
    }

    private final List<BannerItem> banners = new ArrayList<>();
    private final OnBannerClickListener listener;

    public BannerAdapter(OnBannerClickListener listener) {
        this.listener = listener;
    }

    public void setBanners(List<BannerItem> newBanners) {
        this.banners.clear();
        if (newBanners != null) {
            this.banners.addAll(newBanners);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public BannerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemBannerBinding binding = ItemBannerBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new BannerViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull BannerViewHolder holder, int position) {
        holder.bind(banners.get(position), listener);
    }

    @Override
    public int getItemCount() {
        return banners.size();
    }

    static class BannerViewHolder extends RecyclerView.ViewHolder {
        private final ItemBannerBinding binding;

        public BannerViewHolder(ItemBannerBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(BannerItem banner, OnBannerClickListener listener) {
            binding.tvBannerTitle.setText(banner.getContent() != null ? banner.getContent() : "");

            Glide.with(binding.ivBanner.getContext())
                    .load(banner.getImageUrl())
                    .placeholder(R.drawable.ic_launcher_background)
                    .into(binding.ivBanner);

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onBannerClick(banner);
                }
            });
        }
    }
}
