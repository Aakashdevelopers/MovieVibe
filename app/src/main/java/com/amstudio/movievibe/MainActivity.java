package com.amstudio.movievibe;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.amstudio.movievibe.adapter.BannerAdapter;
import com.amstudio.movievibe.adapter.MovieAdapter;
import com.amstudio.movievibe.databinding.ActivityMainBinding;
import com.amstudio.movievibe.model.BannerItem;
import com.amstudio.movievibe.model.HomeFeedResponse;
import com.amstudio.movievibe.model.MovieItem;
import com.amstudio.movievibe.model.MovieBoxSearchResponse;
import com.amstudio.movievibe.network.RetrofitClient;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private MovieAdapter movieAdapter;
    private BannerAdapter bannerAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setupRecyclerViews();
        setupSearch();
        setupCategoryChips();

        fetchHomeFeed();
    }

    private void setupCategoryChips() {
        binding.chipGroupCategory.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) return;
            int chipId = checkedIds.get(0);

            if (chipId == R.id.chipAll) {
                binding.vpBanners.setVisibility(View.VISIBLE);
                binding.tvSectionTitle.setText("Trending Movies & Series");
                fetchHomeFeed();
            } else if (chipId == R.id.chipHindi) {
                performSearch("Hindi");
            } else if (chipId == R.id.chipBollywood) {
                performSearch("Bollywood Hindi");
            } else if (chipId == R.id.chipSouth) {
                performSearch("South Hindi");
            } else if (chipId == R.id.chipAnime) {
                performSearch("Anime Hindi");
            }
        });
    }

    private void setupRecyclerViews() {
        movieAdapter = new MovieAdapter(this::openDetailScreen);
        binding.rvMovies.setAdapter(movieAdapter);

        bannerAdapter = new BannerAdapter(banner -> {
            if (banner.getSubjectId() != null && !banner.getSubjectId().isEmpty()) {
                openDetailScreenById(banner.getSubjectId(), banner.getContent());
            }
        });
        binding.vpBanners.setAdapter(bannerAdapter);
    }

    private void setupSearch() {
        binding.etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() > 0) {
                    binding.btnClearSearch.setVisibility(View.VISIBLE);
                } else {
                    binding.btnClearSearch.setVisibility(View.GONE);
                    binding.vpBanners.setVisibility(View.VISIBLE);
                    binding.tvSectionTitle.setText("Trending Movies & Series");
                    fetchHomeFeed();
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        binding.etSearch.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                String query = binding.etSearch.getText().toString().trim();
                if (!query.isEmpty()) {
                    performSearch(query);
                }
                return true;
            }
            return false;
        });

        binding.btnClearSearch.setOnClickListener(v -> {
            binding.etSearch.setText("");
            binding.btnClearSearch.setVisibility(View.GONE);
            binding.vpBanners.setVisibility(View.VISIBLE);
            binding.tvSectionTitle.setText("Trending Movies & Series");
            fetchHomeFeed();
        });
    }

    private void fetchHomeFeed() {
        binding.progressBar.setVisibility(View.VISIBLE);

        RetrofitClient.getApiService().getHomeFeed().enqueue(new Callback<HomeFeedResponse>() {
            @Override
            public void onResponse(Call<HomeFeedResponse> call, Response<HomeFeedResponse> response) {
                binding.progressBar.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                    List<HomeFeedResponse.HomeSectionItem> sections = response.body().getData().getItems();
                    if (sections != null) {
                        List<BannerItem> allBanners = new ArrayList<>();
                        List<MovieItem> allMovies = new ArrayList<>();

                        for (HomeFeedResponse.HomeSectionItem section : sections) {
                            if (section.getBanners() != null && !section.getBanners().isEmpty()) {
                                allBanners.addAll(section.getBanners());
                            }
                            if (section.getSubjects() != null && !section.getSubjects().isEmpty()) {
                                allMovies.addAll(section.getSubjects());
                            }
                        }

                        if (!allBanners.isEmpty()) {
                            binding.vpBanners.setVisibility(View.VISIBLE);
                            bannerAdapter.setBanners(allBanners);
                        } else {
                            binding.vpBanners.setVisibility(View.GONE);
                        }

                        movieAdapter.setMovies(allMovies);
                    }
                } else {
                    Toast.makeText(MainActivity.this, "Failed to load home feed", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<HomeFeedResponse> call, Throwable t) {
                binding.progressBar.setVisibility(View.GONE);
                Toast.makeText(MainActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void performSearch(String query) {
        binding.progressBar.setVisibility(View.VISIBLE);
        binding.vpBanners.setVisibility(View.GONE);
        binding.tvSectionTitle.setText("Search Results for \"" + query + "\"");

        RetrofitClient.getApiService().searchMovies(query, 1).enqueue(new Callback<MovieBoxSearchResponse>() {
            @Override
            public void onResponse(Call<MovieBoxSearchResponse> call, Response<MovieBoxSearchResponse> response) {
                binding.progressBar.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null && response.body().getItems() != null) {
                    movieAdapter.setMovies(response.body().getItems());
                } else {
                    Toast.makeText(MainActivity.this, "No results found", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<MovieBoxSearchResponse> call, Throwable t) {
                binding.progressBar.setVisibility(View.GONE);
                Toast.makeText(MainActivity.this, "Search failed: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void openDetailScreen(MovieItem movie) {
        openDetailScreenById(movie.getSubjectId(), movie.getTitle());
    }

    private void openDetailScreenById(String subjectId, String title) {
        Intent intent = new Intent(this, DetailActivity.class);
        intent.putExtra(DetailActivity.EXTRA_SUBJECT_ID, subjectId);
        intent.putExtra(DetailActivity.EXTRA_TITLE, title);
        startActivity(intent);
    }
}
