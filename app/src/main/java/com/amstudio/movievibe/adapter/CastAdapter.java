package com.amstudio.movievibe.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.amstudio.movievibe.R;
import com.amstudio.movievibe.databinding.ItemCastBinding;
import com.amstudio.movievibe.model.StaffItem;
import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.List;

public class CastAdapter extends RecyclerView.Adapter<CastAdapter.CastViewHolder> {

    private final List<StaffItem> castList = new ArrayList<>();

    public void setCastList(List<StaffItem> newCast) {
        this.castList.clear();
        if (newCast != null) {
            this.castList.addAll(newCast);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public CastViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemCastBinding binding = ItemCastBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new CastViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull CastViewHolder holder, int position) {
        holder.bind(castList.get(position));
    }

    @Override
    public int getItemCount() {
        return castList.size();
    }

    static class CastViewHolder extends RecyclerView.ViewHolder {
        private final ItemCastBinding binding;

        public CastViewHolder(ItemCastBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(StaffItem cast) {
            binding.tvCastName.setText(cast.getName() != null ? cast.getName() : "");
            binding.tvCharacter.setText(cast.getCharacter() != null ? cast.getCharacter() : "");

            Glide.with(binding.ivCastAvatar.getContext())
                    .load(cast.getAvatarUrl())
                    .placeholder(R.drawable.ic_launcher_foreground)
                    .circleCrop()
                    .into(binding.ivCastAvatar);
        }
    }
}
