package com.amstudio.movievibe.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.amstudio.movievibe.R;
import com.amstudio.movievibe.databinding.ItemMovieBinding;
import com.amstudio.movievibe.model.MovieItem;
import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.List;

public class MovieAdapter extends RecyclerView.Adapter<MovieAdapter.MovieViewHolder> {

    public interface OnMovieClickListener {
        void onMovieClick(MovieItem movieItem);
    }

    private final List<MovieItem> movies = new ArrayList<>();
    private final OnMovieClickListener listener;

    public MovieAdapter(OnMovieClickListener listener) {
        this.listener = listener;
    }

    public void setMovies(List<MovieItem> newMovies) {
        this.movies.clear();
        if (newMovies != null) {
            this.movies.addAll(newMovies);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public MovieViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemMovieBinding binding = ItemMovieBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new MovieViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull MovieViewHolder holder, int position) {
        holder.bind(movies.get(position), listener);
    }

    @Override
    public int getItemCount() {
        return movies.size();
    }

    static class MovieViewHolder extends RecyclerView.ViewHolder {
        private final ItemMovieBinding binding;

        public MovieViewHolder(ItemMovieBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(MovieItem movie, OnMovieClickListener listener) {
            binding.tvTitle.setText(movie.getTitle());
            binding.tvYear.setText(movie.getYear() != null ? movie.getYear() : "");

            if (movie.getRating() != null && !movie.getRating().isEmpty()) {
                binding.tvRating.setVisibility(View.VISIBLE);
                binding.tvRating.setText("★ " + movie.getRating());
            } else {
                binding.tvRating.setVisibility(View.GONE);
            }

            Glide.with(binding.ivPoster.getContext())
                    .load(movie.getPosterUrl())
                    .placeholder(R.drawable.ic_launcher_background)
                    .into(binding.ivPoster);

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onMovieClick(movie);
                }
            });
        }
    }
}
