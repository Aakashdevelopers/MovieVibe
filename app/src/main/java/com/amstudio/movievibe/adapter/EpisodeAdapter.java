package com.amstudio.movievibe.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.amstudio.movievibe.databinding.ItemEpisodeBinding;

public class EpisodeAdapter extends RecyclerView.Adapter<EpisodeAdapter.EpisodeViewHolder> {

    public interface OnEpisodeClickListener {
        void onEpisodeClick(int episodeNumber);
    }

    private int totalEpisodes = 0;
    private int selectedEpisode = 1;
    private final OnEpisodeClickListener listener;

    public EpisodeAdapter(OnEpisodeClickListener listener) {
        this.listener = listener;
    }

    public void setTotalEpisodes(int count) {
        this.totalEpisodes = count;
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
        holder.bind(episodeNum, episodeNum == selectedEpisode, listener, num -> {
            int old = selectedEpisode;
            selectedEpisode = num;
            notifyItemChanged(old - 1);
            notifyItemChanged(selectedEpisode - 1);
        });
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

        public void bind(int episodeNum, boolean isSelected, OnEpisodeClickListener listener, OnSelectionChanged internalSelection) {
            binding.btnEpisode.setText("Ep " + episodeNum);
            binding.btnEpisode.setSelected(isSelected);

            binding.btnEpisode.setOnClickListener(v -> {
                internalSelection.onSelected(episodeNum);
                if (listener != null) {
                    listener.onEpisodeClick(episodeNum);
                }
            });
        }
    }

    interface OnSelectionChanged {
        void onSelected(int episodeNum);
    }
}
