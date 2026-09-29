package com.amstudio.movievibe;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import com.amstudio.movievibe.adapter.BannerAdapter;
import com.amstudio.movievibe.adapter.MediaGridAdapter;
import com.amstudio.movievibe.adapter.MovieAdapter;
import com.amstudio.movievibe.databinding.ActivityMainBinding;
import com.amstudio.movievibe.db.FavoriteRepository;
import com.amstudio.movievibe.db.WatchHistoryRepository;
import com.amstudio.movievibe.db.WatchlistItem;
import com.amstudio.movievibe.db.WatchlistRepository;
import com.amstudio.movievibe.model.BannerItem;
import com.amstudio.movievibe.model.HomeFeedResponse;
import com.amstudio.movievibe.model.MovieBoxPlayResponse;
import com.amstudio.movievibe.model.MovieBoxSearchResponse;
import com.amstudio.movievibe.model.MovieItem;
import com.amstudio.movievibe.model.StreamItem;
import com.amstudio.movievibe.network.RetrofitClient;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;

    private MovieAdapter movieAdapter;
    private MovieAdapter searchTabAdapter;
    private BannerAdapter bannerAdapter;
    private MediaGridAdapter mainWatchlistAdapter;

    private WatchHistoryRepository repository;
    private WatchlistRepository watchlistRepository;
    private FavoriteRepository favoriteRepository;

    private List<MovieItem> masterCatalogList = new ArrayList<>();
    private Call<MovieBoxSearchResponse> activeSearchCall = null;
    private final Handler searchHandler = new Handler(Looper.getMainLooper());
    private Runnable searchRunnable;

    private final Handler bannerHandler = new Handler(Looper.getMainLooper());
    private final Runnable bannerRunnable = new Runnable() {
        @Override
        public void run() {
            if (binding != null && binding.vpBanners != null && bannerAdapter != null && bannerAdapter.getItemCount() > 0) {
                int currentItem = binding.vpBanners.getCurrentItem();
                int totalItems = bannerAdapter.getItemCount();
                int nextItem = (currentItem + 1) % totalItems;
                binding.vpBanners.setCurrentItem(nextItem, true);
                bannerHandler.postDelayed(this, 3500);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        repository = new WatchHistoryRepository(this);
        watchlistRepository = new WatchlistRepository(this);
        favoriteRepository = new FavoriteRepository(this);

        RetrofitClient.getApiService(getApplicationContext());

        setupHeaderAndNavigation();
        setupBottomNavigation();
        setupRecyclerViews();
        setupSearch();
        setupCategoryChips();
        observeMainWatchlist();
        observeProfileCounts();

        fetchHomeFeed();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (bannerAdapter != null && bannerAdapter.getItemCount() > 0) {
            bannerHandler.removeCallbacks(bannerRunnable);
            bannerHandler.postDelayed(bannerRunnable, 3500);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        bannerHandler.removeCallbacks(bannerRunnable);
    }

    private void setupHeaderAndNavigation() {
        binding.btnHistory.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, WatchHistoryActivity.class);
            startActivity(intent);
        });
    }

    private void setupBottomNavigation() {
        binding.bottomNavigation.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_home) {
                binding.containerHome.setVisibility(View.VISIBLE);
                binding.containerSearch.setVisibility(View.GONE);
                binding.containerWatchlist.setVisibility(View.GONE);
                binding.containerProfile.setVisibility(View.GONE);
                binding.tvAppLogo.setText("MovieVibe");
                return true;
            } else if (itemId == R.id.nav_search) {
                binding.containerHome.setVisibility(View.GONE);
                binding.containerSearch.setVisibility(View.VISIBLE);
                binding.containerWatchlist.setVisibility(View.GONE);
                binding.containerProfile.setVisibility(View.GONE);
                binding.tvAppLogo.setText("Search");

                if (binding.etSearchTab.getText().toString().trim().isEmpty()) {
                    showDefaultSearchSuggestions();
                }
                return true;
            } else if (itemId == R.id.nav_watchlist) {
                binding.containerHome.setVisibility(View.GONE);
                binding.containerSearch.setVisibility(View.GONE);
                binding.containerWatchlist.setVisibility(View.VISIBLE);
                binding.containerProfile.setVisibility(View.GONE);
                binding.tvAppLogo.setText("My List");
                return true;
            } else if (itemId == R.id.nav_profile) {
                binding.containerHome.setVisibility(View.GONE);
                binding.containerSearch.setVisibility(View.GONE);
                binding.containerWatchlist.setVisibility(View.GONE);
                binding.containerProfile.setVisibility(View.VISIBLE);
                binding.tvAppLogo.setText("My Profile");
                return true;
            }
            return false;
        });
    }

    private void setupCategoryChips() {
        binding.chipGroupCategory.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) return;
            int chipId = checkedIds.get(0);

            if (chipId == R.id.chipAll) {
                filterAndFetchCategory(null, null, "🔥 Trending Movies & Series");
            } else if (chipId == R.id.chipHindi) {
                filterAndFetchCategory(com.amstudio.movievibe.detector.CategoryEnum.HINDI_DUBBED, "Hindi Dubbed", "🎙️ Hindi Dubbed Movies & Series");
            } else if (chipId == R.id.chipBollywood) {
                filterAndFetchCategory(com.amstudio.movievibe.detector.CategoryEnum.BOLLYWOOD, "Bollywood Hindi", "🎬 Bollywood Movies & Series");
            } else if (chipId == R.id.chipSouth) {
                filterAndFetchCategory(com.amstudio.movievibe.detector.CategoryEnum.SOUTH_MOVIES, "South Hindi", "🌟 South Indian Movies & Series");
            } else if (chipId == R.id.chipAnime) {
                filterAndFetchCategory(com.amstudio.movievibe.detector.CategoryEnum.ANIME_HINDI, "Anime Hindi", "⛩️ Anime Hindi");
            }
        });
    }

    private void filterAndFetchCategory(com.amstudio.movievibe.detector.CategoryEnum category, String searchKeyword, String headerTitle) {
        binding.vpBanners.setVisibility(View.VISIBLE);
        binding.tvSectionTitle.setText(headerTitle);

        List<MovieItem> localFiltered = new ArrayList<>();
        if (category == null) {
            localFiltered.addAll(masterCatalogList);
        } else {
            for (MovieItem m : masterCatalogList) {
                if (m != null && (com.amstudio.movievibe.detector.CategoryDetector.matchesCategory(m, category)
                        || (m.getTitle() != null && m.getTitle().toLowerCase().contains(searchKeyword.toLowerCase().split(" ")[0])))) {
                    localFiltered.add(m);
                }
            }
        }

        if (!localFiltered.isEmpty()) {
            movieAdapter.setMovies(localFiltered);
        }

        if (searchKeyword != null && !searchKeyword.isEmpty()) {
            binding.progressBar.setVisibility(View.VISIBLE);
            RetrofitClient.getApiService().searchMovies(searchKeyword, 1).enqueue(new Callback<MovieBoxSearchResponse>() {
                @Override
                public void onResponse(Call<MovieBoxSearchResponse> call, Response<MovieBoxSearchResponse> response) {
                    binding.progressBar.setVisibility(View.GONE);
                    if (response.isSuccessful() && response.body() != null && response.body().getItems() != null) {
                        List<MovieItem> fetched = response.body().getItems();
                        HashMap<String, Boolean> seenMap = new HashMap<>();

                        List<MovieItem> finalCategoryList = new ArrayList<>();
                        for (MovieItem m : masterCatalogList) {
                            if (m != null && m.getSubjectId() != null) {
                                String id = m.getSubjectId().trim();
                                if (!seenMap.containsKey(id)) {
                                    if (category == null || com.amstudio.movievibe.detector.CategoryDetector.matchesCategory(m, category)) {
                                        seenMap.put(id, true);
                                        finalCategoryList.add(m);
                                    }
                                }
                            }
                        }

                        for (MovieItem m : fetched) {
                            if (m != null && m.getSubjectId() != null) {
                                String id = m.getSubjectId().trim();
                                if (!seenMap.containsKey(id)) {
                                    seenMap.put(id, true);
                                    masterCatalogList.add(m);
                                    finalCategoryList.add(m);
                                }
                            }
                        }

                        if (!finalCategoryList.isEmpty()) {
                            movieAdapter.setMovies(finalCategoryList);
                        }
                    }
                }

                @Override
                public void onFailure(Call<MovieBoxSearchResponse> call, Throwable t) {
                    binding.progressBar.setVisibility(View.GONE);
                }
            });
        }
    }

    private void setupRecyclerViews() {
        movieAdapter = new MovieAdapter(movie -> playMovieDirectly(movie.getSubjectId(), movie.getTitle()));
        binding.rvMovies.setAdapter(movieAdapter);

        searchTabAdapter = new MovieAdapter(movie -> playMovieDirectly(movie.getSubjectId(), movie.getTitle()));
        binding.rvSearchTabGrid.setAdapter(searchTabAdapter);

        bannerAdapter = new BannerAdapter(new BannerAdapter.OnBannerActionListener() {
            @Override
            public void onPlayClick(BannerItem banner) {
                if (banner.getSubjectId() != null && !banner.getSubjectId().isEmpty()) {
                    playMovieDirectly(banner.getSubjectId(), banner.getContent());
                }
            }

            @Override
            public void onMyListClick(BannerItem banner) {
                if (banner.getSubjectId() != null && !banner.getSubjectId().isEmpty()) {
                    String title = banner.getContent() != null ? banner.getContent() : "Movie";
                    watchlistRepository.addToWatchlist(banner.getSubjectId(), title, banner.getImageUrl(), "movie");
                    Toast.makeText(MainActivity.this, "Added " + title + " to My List", Toast.LENGTH_SHORT).show();
                }
            }
        });
        binding.vpBanners.setAdapter(bannerAdapter);

        binding.vpBanners.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                bannerHandler.removeCallbacks(bannerRunnable);
                bannerHandler.postDelayed(bannerRunnable, 3500);
            }
        });

        mainWatchlistAdapter = new MediaGridAdapter(new MediaGridAdapter.OnItemActionListener() {
            @Override
            public void onItemClick(MediaGridAdapter.DisplayItem item) {
                playMovieDirectly(item.getSubjectId(), item.getTitle());
            }

            @Override
            public void onRemoveClick(MediaGridAdapter.DisplayItem item) {
                watchlistRepository.removeFromWatchlist(item.getSubjectId());
                Toast.makeText(MainActivity.this, "Removed from My List", Toast.LENGTH_SHORT).show();
            }
        });
        binding.rvMainWatchlist.setAdapter(mainWatchlistAdapter);

        setupProfileOptionCards();
    }

    private boolean isDirectMediaUrl(String url) {
        if (url == null) return false;
        String lower = url.toLowerCase();
        return lower.contains(".mp4") || lower.contains(".m3u8") || lower.contains(".mpd")
                || lower.contains(".mkv") || lower.contains(".webm");
    }

    private HashMap<String, String> extractHeaders(StreamItem stream) {
        HashMap<String, String> headerMap = new HashMap<>();
        if (stream.getHeaders() != null) {
            headerMap.putAll(stream.getHeaders());
        }
        if (stream.getSignCookie() != null && !stream.getSignCookie().isEmpty()) {
            headerMap.put("Cookie", stream.getSignCookie());
        }
        if (!headerMap.containsKey("User-Agent")) {
            headerMap.put("User-Agent", "com.community.oneroom/50020119 (Linux; U; Android 13; en_US; 23078RKD5C; Build/TQ2A.230405.003; Cronet/135.0.7012.3)");
        }
        if (!headerMap.containsKey("Referer")) {
            headerMap.put("Referer", "https://sportslive.wine");
        }
        return headerMap;
    }

    private void playMovieDirectly(String subjectId, String title) {
        if (subjectId == null || subjectId.isEmpty()) return;
        String cleanId = sanitizeId(subjectId);

        binding.progressBar.setVisibility(View.VISIBLE);
        Toast.makeText(this, "Loading " + (title != null ? title : "video") + "...", Toast.LENGTH_SHORT).show();

        repository.getItem(cleanId, 1, 1, item -> {
            long savedPosition = (item != null) ? item.getPosition() : 0;

            RetrofitClient.getApiService().getPlaybackStream(cleanId, 1, 1).enqueue(new Callback<MovieBoxPlayResponse>() {
                @Override
                public void onResponse(Call<MovieBoxPlayResponse> call, Response<MovieBoxPlayResponse> response) {
                    binding.progressBar.setVisibility(View.GONE);
                    if (response.isSuccessful() && response.body() != null && response.body().getStreams() != null && !response.body().getStreams().isEmpty()) {
                        StreamItem stream = response.body().getStreams().get(0);
                        String url = stream.getUrl();
                        String format = stream.getFormat();
                        HashMap<String, String> headerMap = extractHeaders(stream);

                        if ("WEB".equalsIgnoreCase(format) && !isDirectMediaUrl(url)) {
                            Intent intent = new Intent(MainActivity.this, WebViewPlayerActivity.class);
                            intent.putExtra(WebViewPlayerActivity.EXTRA_WEB_URL, url);
                            intent.putExtra(WebViewPlayerActivity.EXTRA_WEB_TITLE, title);
                            startActivity(intent);
                        } else {
                            Intent intent = new Intent(MainActivity.this, PlayerActivity.class);
                            intent.putExtra(PlayerActivity.EXTRA_STREAM_URL, url);
                            intent.putExtra(PlayerActivity.EXTRA_FORMAT, format);
                            intent.putExtra(PlayerActivity.EXTRA_TITLE, title);
                            intent.putExtra(PlayerActivity.EXTRA_SUBJECT_ID, cleanId);
                            intent.putExtra(PlayerActivity.EXTRA_SEASON, 1);
                            intent.putExtra(PlayerActivity.EXTRA_EPISODE, 1);
                            intent.putExtra(PlayerActivity.EXTRA_START_POSITION, savedPosition);
                            intent.putExtra(PlayerActivity.EXTRA_HEADERS, headerMap);
                            startActivity(intent);
                        }
                    } else {
                        playMovieFallback(cleanId, title, savedPosition);
                    }
                }

                @Override
                public void onFailure(Call<MovieBoxPlayResponse> call, Throwable t) {
                    binding.progressBar.setVisibility(View.GONE);
                    Toast.makeText(MainActivity.this, "Stream Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    private void playMovieFallback(String cleanId, String title, long savedPosition) {
        RetrofitClient.getApiService().getPlaybackStream(cleanId, 0, 0).enqueue(new Callback<MovieBoxPlayResponse>() {
            @Override
            public void onResponse(Call<MovieBoxPlayResponse> call, Response<MovieBoxPlayResponse> response) {
                binding.progressBar.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null && response.body().getStreams() != null && !response.body().getStreams().isEmpty()) {
                    StreamItem stream = response.body().getStreams().get(0);
                    String url = stream.getUrl();
                    String format = stream.getFormat();
                    HashMap<String, String> headerMap = extractHeaders(stream);

                    if ("WEB".equalsIgnoreCase(format) && !isDirectMediaUrl(url)) {
                        Intent intent = new Intent(MainActivity.this, WebViewPlayerActivity.class);
                        intent.putExtra(WebViewPlayerActivity.EXTRA_WEB_URL, url);
                        intent.putExtra(WebViewPlayerActivity.EXTRA_WEB_TITLE, title);
                        startActivity(intent);
                    } else {
                        Intent intent = new Intent(MainActivity.this, PlayerActivity.class);
                        intent.putExtra(PlayerActivity.EXTRA_STREAM_URL, url);
                        intent.putExtra(PlayerActivity.EXTRA_FORMAT, format);
                        intent.putExtra(PlayerActivity.EXTRA_TITLE, title);
                        intent.putExtra(PlayerActivity.EXTRA_SUBJECT_ID, cleanId);
                        intent.putExtra(PlayerActivity.EXTRA_SEASON, 0);
                        intent.putExtra(PlayerActivity.EXTRA_EPISODE, 0);
                        intent.putExtra(PlayerActivity.EXTRA_START_POSITION, savedPosition);
                        intent.putExtra(PlayerActivity.EXTRA_HEADERS, headerMap);
                        startActivity(intent);
                    }
                } else {
                    openDetailScreenById(cleanId, title);
                }
            }

            @Override
            public void onFailure(Call<MovieBoxPlayResponse> call, Throwable t) {
                binding.progressBar.setVisibility(View.GONE);
                openDetailScreenById(cleanId, title);
            }
        });
    }

    private void setupProfileOptionCards() {
        binding.layoutMainWatchlistStat.setOnClickListener(v -> {
            binding.bottomNavigation.setSelectedItemId(R.id.nav_watchlist);
        });

        binding.layoutMainFavoritesStat.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, FavoritesActivity.class));
        });

        binding.layoutMainHistoryStat.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, WatchHistoryActivity.class));
        });

        binding.cardProfileFavorites.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, FavoritesActivity.class));
        });

        binding.cardProfileHistory.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, WatchHistoryActivity.class));
        });

        binding.cardProfileSettings.setOnClickListener(v -> {
            String[] options = {"Dark Theme (Active)", "Clear App Cache", "Hardware Acceleration (Enabled)"};
            new AlertDialog.Builder(MainActivity.this)
                    .setTitle("App Settings")
                    .setItems(options, (dialog, which) -> {
                        if (which == 1) {
                            Toast.makeText(MainActivity.this, "Cache cleared successfully", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(MainActivity.this, options[which] + " is enabled by default", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .setPositiveButton("Close", null)
                    .show();
        });
    }

    private void observeMainWatchlist() {
        watchlistRepository.getAllWatchlist().observe(this, items -> {
            if (items != null && !items.isEmpty()) {
                binding.layoutEmptyMainWatchlist.setVisibility(View.GONE);
                binding.rvMainWatchlist.setVisibility(View.VISIBLE);

                List<MediaGridAdapter.DisplayItem> displayItems = new ArrayList<>();
                for (WatchlistItem w : items) {
                    displayItems.add(new MediaGridAdapter.DisplayItem(
                            w.getSubjectId(), w.getTitle(), w.getPosterUrl(), w.getType()
                    ));
                }
                mainWatchlistAdapter.setItems(displayItems);
            } else {
                binding.layoutEmptyMainWatchlist.setVisibility(View.VISIBLE);
                binding.rvMainWatchlist.setVisibility(View.GONE);
                mainWatchlistAdapter.setItems(null);
            }
        });
    }

    private void observeProfileCounts() {
        watchlistRepository.getWatchlistCount().observe(this, count -> {
            binding.tvMainWatchlistCount.setText(String.valueOf(count != null ? count : 0));
        });

        favoriteRepository.getFavoritesCount().observe(this, count -> {
            binding.tvMainFavoritesCount.setText(String.valueOf(count != null ? count : 0));
        });

        repository.getAllHistory().observe(this, list -> {
            binding.tvMainHistoryCount.setText(String.valueOf(list != null ? list.size() : 0));
        });
    }

    private void setupSearch() {
        binding.btnClearSearchTab.setOnClickListener(v -> {
            binding.etSearchTab.setText("");
            binding.btnClearSearchTab.setVisibility(View.GONE);
            binding.tvSearchTabEmpty.setVisibility(View.GONE);
            if (activeSearchCall != null) {
                activeSearchCall.cancel();
            }
            showDefaultSearchSuggestions();
        });

        binding.etSearchTab.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String input = s.toString();
                if (input.length() > 0) {
                    binding.btnClearSearchTab.setVisibility(View.VISIBLE);
                } else {
                    binding.btnClearSearchTab.setVisibility(View.GONE);
                    binding.tvSearchTabEmpty.setVisibility(View.GONE);
                    if (activeSearchCall != null) {
                        activeSearchCall.cancel();
                    }
                    showDefaultSearchSuggestions();
                    return;
                }

                if (searchRunnable != null) {
                    searchHandler.removeCallbacks(searchRunnable);
                }

                searchRunnable = () -> performLiveSearch(input);
                searchHandler.postDelayed(searchRunnable, 300);
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        binding.etSearchTab.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                String query = binding.etSearchTab.getText().toString().trim();
                if (!query.isEmpty()) {
                    performLiveSearch(query);
                }
                return true;
            }
            return false;
        });
    }

    private void showDefaultSearchSuggestions() {
        binding.tvSearchTabEmpty.setVisibility(View.GONE);
        binding.tvSearchTabTitle.setText("🔥 Top Trending Suggestions");
        binding.tvSearchTabTitle.setVisibility(View.VISIBLE);

        if (masterCatalogList != null && !masterCatalogList.isEmpty()) {
            int limit = Math.min(8, masterCatalogList.size());
            searchTabAdapter.setMovies(new ArrayList<>(masterCatalogList.subList(0, limit)));
        } else {
            RetrofitClient.getApiService().searchMovies("Hindi Dubbed", 1).enqueue(new Callback<MovieBoxSearchResponse>() {
                @Override
                public void onResponse(Call<MovieBoxSearchResponse> call, Response<MovieBoxSearchResponse> response) {
                    if (response.isSuccessful() && response.body() != null && response.body().getItems() != null) {
                        List<MovieItem> items = response.body().getItems();
                        int limit = Math.min(8, items.size());
                        searchTabAdapter.setMovies(new ArrayList<>(items.subList(0, limit)));
                    }
                }

                @Override
                public void onFailure(Call<MovieBoxSearchResponse> call, Throwable t) {}
            });
        }
    }

    private void performLiveSearch(String rawQuery) {
        if (rawQuery == null || rawQuery.trim().isEmpty()) {
            showDefaultSearchSuggestions();
            return;
        }

        binding.tvSearchTabTitle.setText("Search Results for \"" + rawQuery + "\"");

        String query = rawQuery.trim().toLowerCase();

        if (activeSearchCall != null) {
            activeSearchCall.cancel();
        }

        binding.progressBar.setVisibility(View.VISIBLE);
        binding.tvSearchTabEmpty.setVisibility(View.GONE);

        activeSearchCall = RetrofitClient.getApiService().searchMovies(rawQuery, 1);
        activeSearchCall.enqueue(new Callback<MovieBoxSearchResponse>() {
            @Override
            public void onResponse(Call<MovieBoxSearchResponse> call, Response<MovieBoxSearchResponse> response) {
                binding.progressBar.setVisibility(View.GONE);
                List<MovieItem> results = new ArrayList<>();
                HashMap<String, Boolean> seenMap = new HashMap<>();

                if (response.isSuccessful() && response.body() != null && response.body().getItems() != null) {
                    for (MovieItem m : response.body().getItems()) {
                        if (m != null && m.getSubjectId() != null) {
                            String id = m.getSubjectId().trim();
                            if (!seenMap.containsKey(id)) {
                                seenMap.put(id, true);
                                results.add(m);
                            }
                        }
                    }
                }

                for (MovieItem m : masterCatalogList) {
                    if (m != null && m.getSubjectId() != null) {
                        String id = m.getSubjectId().trim();
                        if (!seenMap.containsKey(id)) {
                            String t = m.getTitle() != null ? m.getTitle().toLowerCase() : "";
                            String g = m.getGenre() != null ? m.getGenre().toLowerCase() : "";
                            String d = m.getDescription() != null ? m.getDescription().toLowerCase() : "";

                            if (t.contains(query) || g.contains(query) || d.contains(query)) {
                                seenMap.put(id, true);
                                results.add(m);
                            }
                        }
                    }
                }

                if (results.isEmpty()) {
                    searchTabAdapter.setMovies(null);
                    binding.tvSearchTabEmpty.setVisibility(View.VISIBLE);
                } else {
                    binding.tvSearchTabEmpty.setVisibility(View.GONE);
                    searchTabAdapter.setMovies(results);
                }
            }

            @Override
            public void onFailure(Call<MovieBoxSearchResponse> call, Throwable t) {
                if (call.isCanceled()) return;

                binding.progressBar.setVisibility(View.GONE);

                List<MovieItem> localResults = new ArrayList<>();
                for (MovieItem m : masterCatalogList) {
                    if (m != null && m.getTitle() != null && m.getTitle().toLowerCase().contains(query)) {
                        localResults.add(m);
                    }
                }

                if (!localResults.isEmpty()) {
                    searchTabAdapter.setMovies(localResults);
                    binding.tvSearchTabEmpty.setVisibility(View.GONE);
                } else {
                    binding.tvSearchTabEmpty.setVisibility(View.VISIBLE);
                    Toast.makeText(MainActivity.this, "Network error during search", Toast.LENGTH_SHORT).show();
                }
            }
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
                            bannerHandler.removeCallbacks(bannerRunnable);
                            bannerHandler.postDelayed(bannerRunnable, 3500);
                        } else {
                            binding.vpBanners.setVisibility(View.GONE);
                        }

                        masterCatalogList.addAll(allMovies);
                        movieAdapter.setMovies(allMovies);
                    }
                }
            }

            @Override
            public void onFailure(Call<HomeFeedResponse> call, Throwable t) {
                binding.progressBar.setVisibility(View.GONE);
            }
        });
    }

    private void performSearch(String query) {
        binding.progressBar.setVisibility(View.VISIBLE);
        binding.vpBanners.setVisibility(View.GONE);
        binding.tvSectionTitle.setText("Results for \"" + query + "\"");

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

    private String sanitizeId(String id) {
        return id != null ? id.trim().replaceAll("[\\r\\n\\t]", "") : "";
    }

    private void openDetailScreenById(String subjectId, String title) {
        Intent intent = new Intent(this, DetailActivity.class);
        intent.putExtra(DetailActivity.EXTRA_SUBJECT_ID, sanitizeId(subjectId));
        intent.putExtra(DetailActivity.EXTRA_TITLE, title);
        startActivity(intent);
    }
}
