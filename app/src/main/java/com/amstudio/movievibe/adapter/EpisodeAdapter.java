package com.amstudio.movievibe.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.amstudio.movievibe.R;
import com.amstudio.movievibe.databinding.ItemEpisodeBinding;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;

public class EpisodeAdapter extends RecyclerView.Adapter<EpisodeAdapter.EpisodeViewHolder> {

    public interface OnEpisodeClickListener {
        void onEpisodeClick(int episodeNumber);
    }

    private int totalEpisodes = 0;
    private int selectedEpisode = 1;
    private String posterUrl;
    private String seriesTitle;
    private String seriesOverview;

    private final OnEpisodeClickListener listener;

    public EpisodeAdapter(OnEpisodeClickListener listener) {
        this.listener = listener;
    }

    public void setTotalEpisodes(int count) {
        setEpisodeData(count, null, null, null);
    }

    public void setEpisodeData(int count, String posterUrl, String title, String overview) {
        this.totalEpisodes = count;
        this.posterUrl = posterUrl;
        this.seriesTitle = title;
        this.seriesOverview = overview;
        this.selectedEpisode = 1;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public EpisodeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemEpisodeBinding binding = ItemEpisodeBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new EpisodeViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull EpisodeViewHolder holder, int position) {
        int episodeNum = position + 1;
        holder.bind(episodeNum, posterUrl, seriesTitle, seriesOverview, listener);
    }

    @Override
    public int getItemCount() {
        return totalEpisodes;
    }

    static class EpisodeViewHolder extends RecyclerView.ViewHolder {
        private final ItemEpisodeBinding binding;

        public EpisodeViewHolder(ItemEpisodeBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(int episodeNum, String posterUrl, String title, String overview, OnEpisodeClickListener listener) {
            binding.tvEpisodeTitle.setText(episodeNum + ". " + (title != null && !title.isEmpty() ? title : "Episode " + episodeNum));
            binding.tvEpisodeDuration.setText("45 mins · HD");

            String descText = overview != null && !overview.isEmpty() ? overview : "Watch Episode " + episodeNum + " in HD quality on MovieVibe.";
            binding.tvEpisodeOverview.setText(descText);

            if (binding.ivEpisodeThumbnail.getContext() instanceof android.app.Activity) {
                android.app.Activity act = (android.app.Activity) binding.ivEpisodeThumbnail.getContext();
                if (act.isFinishing() || act.isDestroyed()) return;
            }

            if (posterUrl != null && !posterUrl.isEmpty()) {
                Glide.with(binding.ivEpisodeThumbnail.getContext().getApplicationContext())
                        .load(posterUrl)
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .override(240, 144)
                        .centerCrop()
                        .placeholder(R.drawable.ic_launcher_background)
                        .into(binding.ivEpisodeThumbnail);
            } else {
                binding.ivEpisodeThumbnail.setImageResource(R.drawable.ic_launcher_background);
            }

            binding.btnPlayEpisode.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onEpisodeClick(episodeNum);
                }
            });

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onEpisodeClick(episodeNum);
                }
            });
        }
    }
}
