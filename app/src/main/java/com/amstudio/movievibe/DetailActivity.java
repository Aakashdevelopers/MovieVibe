package com.amstudio.movievibe;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.amstudio.movievibe.adapter.CastAdapter;
import com.amstudio.movievibe.adapter.EpisodeAdapter;
import com.amstudio.movievibe.adapter.MovieAdapter;
import com.amstudio.movievibe.databinding.ActivityDetailBinding;
import com.amstudio.movievibe.db.FavoriteRepository;
import com.amstudio.movievibe.db.WatchHistoryRepository;
import com.amstudio.movievibe.db.WatchlistRepository;
import com.amstudio.movievibe.model.DubItem;
import com.amstudio.movievibe.model.MovieBoxDetailResponse;
import com.amstudio.movievibe.model.MovieBoxPlayResponse;
import com.amstudio.movievibe.model.MovieBoxSearchResponse;
import com.amstudio.movievibe.model.MovieItem;
import com.amstudio.movievibe.model.SeasonItem;
import com.amstudio.movievibe.model.StreamItem;
import com.amstudio.movievibe.network.RetrofitClient;
import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DetailActivity extends AppCompatActivity {

    public static final String EXTRA_SUBJECT_ID = "extra_subject_id";
    public static final String EXTRA_TITLE = "extra_title";

    private ActivityDetailBinding binding;
    private CastAdapter castAdapter;
    private EpisodeAdapter episodeAdapter;
    private MovieAdapter recommendationsAdapter;

    private WatchHistoryRepository repository;
    private WatchlistRepository watchlistRepository;
    private FavoriteRepository favoriteRepository;

    private String subjectId;
    private String movieTitle;
    private boolean isSeries = false;
    private int currentSeason = 1;

    private boolean isInWatchlist = false;
    private boolean isFavorite = false;

    private MovieBoxDetailResponse detailData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        repository = new WatchHistoryRepository(this);
        watchlistRepository = new WatchlistRepository(this);
        favoriteRepository = new FavoriteRepository(this);

        subjectId = getIntent().getStringExtra(EXTRA_SUBJECT_ID);
        if (subjectId != null) {
            subjectId = sanitizeId(subjectId);
        }
        movieTitle = getIntent().getStringExtra(EXTRA_TITLE);

        if (subjectId == null || subjectId.isEmpty()) {
            Toast.makeText(this, "Invalid Movie ID", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        setupToolbar();
        setupRecyclerViews();

        binding.btnPlay.setOnClickListener(v -> playVideo(currentSeason, 1, false));
        binding.btnWebPlay.setOnClickListener(v -> playVideo(currentSeason, 1, true));

        setupWatchlistAndFavorites();
        fetchMovieDetail();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updatePlayButtonText(currentSeason, 1);
        checkLibraryStates();
    }

    private void setupToolbar() {
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(movieTitle != null ? movieTitle : "");
        }
        binding.toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupRecyclerViews() {
        castAdapter = new CastAdapter();
        binding.rvCast.setAdapter(castAdapter);

        episodeAdapter = new EpisodeAdapter(episodeNum -> playVideo(currentSeason, episodeNum, false));
        binding.rvEpisodes.setAdapter(episodeAdapter);

        recommendationsAdapter = new MovieAdapter(this::openRecommendedDetail);
        binding.rvRecommendations.setAdapter(recommendationsAdapter);
    }

    private void openRecommendedDetail(MovieItem movie) {
        if (movie == null || movie.getSubjectId() == null) return;
        Intent intent = new Intent(DetailActivity.this, DetailActivity.class);
        intent.putExtra(EXTRA_SUBJECT_ID, sanitizeId(movie.getSubjectId()));
        intent.putExtra(EXTRA_TITLE, movie.getTitle());
        startActivity(intent);
    }

    private void setupWatchlistAndFavorites() {
        binding.btnWatchlist.setOnClickListener(v -> toggleWatchlist());
        binding.btnFavorite.setOnClickListener(v -> toggleFavorite());
    }

    private void checkLibraryStates() {
        if (subjectId == null) return;

        watchlistRepository.isWatchlisted(subjectId, item -> {
            isInWatchlist = (item != null);
            runOnUiThread(() -> {
                if (isInWatchlist) {
                    binding.btnWatchlist.setText("✓ In Watchlist");
                    binding.btnWatchlist.setIconResource(R.drawable.ic_bookmark_added);
                } else {
                    binding.btnWatchlist.setText("+ Watchlist");
                    binding.btnWatchlist.setIconResource(R.drawable.ic_bookmark);
                }
            });
        });

        favoriteRepository.isFavorite(subjectId, item -> {
            isFavorite = (item != null);
            runOnUiThread(() -> {
                if (isFavorite) {
                    binding.btnFavorite.setText("Favorited");
                    binding.btnFavorite.setIconResource(R.drawable.ic_heart_filled);
                } else {
                    binding.btnFavorite.setText("Favorite");
                    binding.btnFavorite.setIconResource(R.drawable.ic_heart_outline);
                }
            });
        });
    }

    private void toggleWatchlist() {
        if (subjectId == null) return;
        String titleText = detailData != null ? detailData.getTitle() : movieTitle;
        String posterUrl = detailData != null ? detailData.getPosterUrl() : "";
        String type = isSeries ? "series" : "movie";

        if (isInWatchlist) {
            watchlistRepository.removeFromWatchlist(subjectId);
            isInWatchlist = false;
            binding.btnWatchlist.setText("+ Watchlist");
            binding.btnWatchlist.setIconResource(R.drawable.ic_bookmark);
            Toast.makeText(this, "Removed from Watchlist", Toast.LENGTH_SHORT).show();
        } else {
            watchlistRepository.addToWatchlist(subjectId, titleText, posterUrl, type);
            isInWatchlist = true;
            binding.btnWatchlist.setText("✓ In Watchlist");
            binding.btnWatchlist.setIconResource(R.drawable.ic_bookmark_added);
            Toast.makeText(this, "Added to Watchlist", Toast.LENGTH_SHORT).show();
        }
    }

    private void toggleFavorite() {
        if (subjectId == null) return;
        String titleText = detailData != null ? detailData.getTitle() : movieTitle;
        String posterUrl = detailData != null ? detailData.getPosterUrl() : "";
        String type = isSeries ? "series" : "movie";

        if (isFavorite) {
            favoriteRepository.removeFromFavorites(subjectId);
            isFavorite = false;
            binding.btnFavorite.setText("Favorite");
            binding.btnFavorite.setIconResource(R.drawable.ic_heart_outline);
            Toast.makeText(this, "Removed from Favorites", Toast.LENGTH_SHORT).show();
        } else {
            favoriteRepository.addToFavorites(subjectId, titleText, posterUrl, type);
            isFavorite = true;
            binding.btnFavorite.setText("Favorited");
            binding.btnFavorite.setIconResource(R.drawable.ic_heart_filled);
            Toast.makeText(this, "Added to Favorites", Toast.LENGTH_SHORT).show();
        }
    }

    private void fetchMovieDetail() {
        binding.progressBarDetail.setVisibility(View.VISIBLE);

        RetrofitClient.getApiService().getMovieDetail(subjectId).enqueue(new Callback<MovieBoxDetailResponse>() {
            @Override
            public void onResponse(Call<MovieBoxDetailResponse> call, Response<MovieBoxDetailResponse> response) {
                binding.progressBarDetail.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null) {
                    detailData = response.body();
                    if (detailData.getSubjectId() != null && !detailData.getSubjectId().isEmpty()) {
                        subjectId = sanitizeId(detailData.getSubjectId());
                    }
                    populateUI(detailData);
                } else {
                    Toast.makeText(DetailActivity.this, "Failed to load details", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<MovieBoxDetailResponse> call, Throwable t) {
                binding.progressBarDetail.setVisibility(View.GONE);
                Toast.makeText(DetailActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void populateUI(MovieBoxDetailResponse detail) {
        binding.tvDetailTitle.setText(detail.getTitle());
        binding.tvOverview.setText(detail.getDescription() != null ? detail.getDescription() : "No overview available.");

        if (detail.getRating() != null && !detail.getRating().isEmpty()) {
            binding.tvDetailRating.setVisibility(View.VISIBLE);
            binding.tvDetailRating.setText("★ " + detail.getRating());
        } else {
            binding.tvDetailRating.setVisibility(View.GONE);
        }

        binding.tvDetailYear.setText(detail.getYear() != null ? detail.getYear() : "");

        if (!isFinishing() && !isDestroyed()) {
            Glide.with(this)
                    .load(detail.getPosterUrl())
                    .into(binding.ivBackdrop);
        }

        if (detail.getRaw() != null) {
            binding.tvDetailDuration.setText(detail.getRaw().getDuration() != null ? detail.getRaw().getDuration() : "");
            binding.tvDetailGenre.setText(detail.getRaw().getGenre() != null ? detail.getRaw().getGenre() : "");

            if (detail.getRaw().getDubs() != null && !detail.getRaw().getDubs().isEmpty()) {
                StringBuilder dubsText = new StringBuilder("Languages: ");
                for (DubItem dub : detail.getRaw().getDubs()) {
                    dubsText.append(dub.getLanName()).append(" · ");
                }
                binding.tvDetailDubs.setVisibility(View.VISIBLE);
                binding.tvDetailDubs.setText(dubsText.substring(0, Math.max(0, dubsText.length() - 3)));
            }

            if (detail.getRaw().getStaffList() != null) {
                castAdapter.setCastList(detail.getRaw().getStaffList());
            }
        }

        if ("series".equalsIgnoreCase(detail.getType())) {
            isSeries = true;
            binding.layoutEpisodes.setVisibility(View.VISIBLE);
            int epCount = 10;
            if (detail.getSeasonsInfo() != null && detail.getSeasonsInfo().getSeasons() != null && !detail.getSeasonsInfo().getSeasons().isEmpty()) {
                SeasonItem season = detail.getSeasonsInfo().getSeasons().get(0);
                currentSeason = season.getSeasonNumber();
                epCount = season.getMaxEpisodes() > 0 ? season.getMaxEpisodes() : 10;
            }
            episodeAdapter.setEpisodeData(epCount, detail.getPosterUrl(), detail.getTitle(), detail.getDescription());
        } else {
            binding.layoutEpisodes.setVisibility(View.GONE);
        }

        updatePlayButtonText(currentSeason, 1);
        checkLibraryStates();
        fetchRecommendations(detail);
    }

    private void fetchRecommendations(MovieBoxDetailResponse detail) {
        java.util.Set<com.amstudio.movievibe.detector.CategoryEnum> detected = com.amstudio.movievibe.detector.CategoryDetector.detect(detail);
        String query = "Hindi Dubbed";

        if (detected.contains(com.amstudio.movievibe.detector.CategoryEnum.ANIME_HINDI)) {
            query = "Anime Hindi";
            binding.tvRecommendationHeader.setText("Customers Also Watched · Anime");
        } else if (detected.contains(com.amstudio.movievibe.detector.CategoryEnum.SOUTH_MOVIES)) {
            query = "South Hindi";
            binding.tvRecommendationHeader.setText("Customers Also Watched · South Movies");
        } else if (detected.contains(com.amstudio.movievibe.detector.CategoryEnum.BOLLYWOOD)) {
            query = "Bollywood Hindi";
            binding.tvRecommendationHeader.setText("Customers Also Watched · Bollywood Hits");
        } else if (detail.getRaw() != null && detail.getRaw().getGenre() != null && !detail.getRaw().getGenre().isEmpty()) {
            String[] genres = detail.getRaw().getGenre().split(",");
            if (genres.length > 0 && !genres[0].trim().isEmpty()) {
                query = genres[0].trim();
            }
            binding.tvRecommendationHeader.setText("Customers Also Watched · More Like This");
        } else {
            binding.tvRecommendationHeader.setText("Customers Also Watched");
        }

        List<MovieItem> combinedRecs = new ArrayList<>();
        java.util.HashMap<String, Boolean> seenMap = new java.util.HashMap<>();

        RetrofitClient.getApiService().searchMovies(query, 1).enqueue(new Callback<MovieBoxSearchResponse>() {
            @Override
            public void onResponse(Call<MovieBoxSearchResponse> call, Response<MovieBoxSearchResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getItems() != null) {
                    for (MovieItem m : response.body().getItems()) {
                        if (m != null && m.getSubjectId() != null) {
                            String id = m.getSubjectId().trim();
                            if (!id.equals(subjectId) && !seenMap.containsKey(id)) {
                                seenMap.put(id, true);
                                combinedRecs.add(m);
                            }
                        }
                    }
                }
                fetchHomeFeedRecommendations(combinedRecs, seenMap);
            }

            @Override
            public void onFailure(Call<MovieBoxSearchResponse> call, Throwable t) {
                fetchHomeFeedRecommendations(combinedRecs, seenMap);
            }
        });
    }

    private void fetchHomeFeedRecommendations(List<MovieItem> combinedRecs, java.util.HashMap<String, Boolean> seenMap) {
        RetrofitClient.getApiService().getHomeFeed().enqueue(new Callback<com.amstudio.movievibe.model.HomeFeedResponse>() {
            @Override
            public void onResponse(Call<com.amstudio.movievibe.model.HomeFeedResponse> call, Response<com.amstudio.movievibe.model.HomeFeedResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                    List<com.amstudio.movievibe.model.HomeFeedResponse.HomeSectionItem> sections = response.body().getData().getItems();
                    if (sections != null) {
                        for (com.amstudio.movievibe.model.HomeFeedResponse.HomeSectionItem sec : sections) {
                            if (sec.getSubjects() != null) {
                                for (MovieItem m : sec.getSubjects()) {
                                    if (m != null && m.getSubjectId() != null) {
                                        String id = m.getSubjectId().trim();
                                        if (!id.equals(subjectId) && !seenMap.containsKey(id)) {
                                            seenMap.put(id, true);
                                            combinedRecs.add(m);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                recommendationsAdapter.setMovies(combinedRecs);
            }

            @Override
            public void onFailure(Call<com.amstudio.movievibe.model.HomeFeedResponse> call, Throwable t) {
                recommendationsAdapter.setMovies(combinedRecs);
            }
        });
    }

    private void updatePlayButtonText(int season, int episode) {
        if (subjectId == null) return;
        repository.getItem(subjectId, season, episode, item -> {
            runOnUiThread(() -> {
                if (item != null && item.getPosition() > 0) {
                    int pct = item.getProgressPercentage();
                    binding.btnPlay.setText("Resume (" + pct + "%)");
                } else {
                    binding.btnPlay.setText("Watch Native");
                }
            });
        });
    }

    private String sanitizeId(String id) {
        return id != null ? id.trim().replaceAll("[\\r\\n\\t]", "") : "";
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

    private void playVideo(int season, int episode, boolean useWebView) {
        if (subjectId != null) {
            subjectId = sanitizeId(subjectId);
        }

        int targetSeason = isSeries ? season : 1;
        int targetEpisode = isSeries ? episode : 1;

        binding.progressBarDetail.setVisibility(View.VISIBLE);
        RetrofitClient.getApiService().getPlaybackStream(subjectId, targetSeason, targetEpisode).enqueue(new Callback<MovieBoxPlayResponse>() {
            @Override
            public void onResponse(Call<MovieBoxPlayResponse> call, Response<MovieBoxPlayResponse> response) {
                binding.progressBarDetail.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null && response.body().getStreams() != null && !response.body().getStreams().isEmpty()) {
                    StreamItem stream = response.body().getStreams().get(0);
                    String url = stream.getUrl();
                    String format = stream.getFormat();
                    HashMap<String, String> headerMap = extractHeaders(stream);

                    if (useWebView || ("WEB".equalsIgnoreCase(format) && !isDirectMediaUrl(url))) {
                        launchWebViewPlayer(url, movieTitle);
                    } else {
                        launchPlayer(url, format, headerMap, movieTitle, targetSeason, targetEpisode);
                    }
                } else {
                    if (targetSeason == 1) {
                        fetchPlaybackStreamFallback(0, 0, useWebView);
                    } else {
                        Toast.makeText(DetailActivity.this, "Stream not available", Toast.LENGTH_SHORT).show();
                    }
                }
            }

            @Override
            public void onFailure(Call<MovieBoxPlayResponse> call, Throwable t) {
                binding.progressBarDetail.setVisibility(View.GONE);
                Toast.makeText(DetailActivity.this, "Stream Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void fetchPlaybackStreamFallback(int fallbackSeason, int fallbackEpisode, boolean useWebView) {
        binding.progressBarDetail.setVisibility(View.VISIBLE);
        RetrofitClient.getApiService().getPlaybackStream(subjectId, fallbackSeason, fallbackEpisode).enqueue(new Callback<MovieBoxPlayResponse>() {
            @Override
            public void onResponse(Call<MovieBoxPlayResponse> call, Response<MovieBoxPlayResponse> response) {
                binding.progressBarDetail.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null && response.body().getStreams() != null && !response.body().getStreams().isEmpty()) {
                    StreamItem stream = response.body().getStreams().get(0);
                    String url = stream.getUrl();
                    String format = stream.getFormat();
                    HashMap<String, String> headerMap = extractHeaders(stream);

                    if (useWebView || ("WEB".equalsIgnoreCase(format) && !isDirectMediaUrl(url))) {
                        launchWebViewPlayer(url, movieTitle);
                    } else {
                        launchPlayer(url, format, headerMap, movieTitle, fallbackSeason, fallbackEpisode);
                    }
                } else {
                    Toast.makeText(DetailActivity.this, "Stream not available", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<MovieBoxPlayResponse> call, Throwable t) {
                binding.progressBarDetail.setVisibility(View.GONE);
                Toast.makeText(DetailActivity.this, "Stream Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void launchWebViewPlayer(String webUrl, String title) {
        Intent intent = new Intent(this, WebViewPlayerActivity.class);
        intent.putExtra(WebViewPlayerActivity.EXTRA_WEB_URL, webUrl);
        intent.putExtra(WebViewPlayerActivity.EXTRA_WEB_TITLE, title);
        startActivity(intent);
    }

    private void launchPlayer(String streamUrl, String format, HashMap<String, String> headers, String title, int season, int episode) {
        repository.getItem(subjectId, season, episode, item -> {
            long savedPosition = (item != null) ? item.getPosition() : 0;
            runOnUiThread(() -> {
                Intent intent = new Intent(DetailActivity.this, PlayerActivity.class);
                intent.putExtra(PlayerActivity.EXTRA_STREAM_URL, streamUrl);
                intent.putExtra(PlayerActivity.EXTRA_FORMAT, format);
                intent.putExtra(PlayerActivity.EXTRA_TITLE, title != null ? title : movieTitle);
                intent.putExtra(PlayerActivity.EXTRA_SUBJECT_ID, subjectId);
                intent.putExtra(PlayerActivity.EXTRA_POSTER_URL, detailData != null ? detailData.getPosterUrl() : null);
                intent.putExtra(PlayerActivity.EXTRA_SEASON, season);
                intent.putExtra(PlayerActivity.EXTRA_EPISODE, episode);
                intent.putExtra(PlayerActivity.EXTRA_MAX_EPISODES, episodeAdapter != null ? episodeAdapter.getItemCount() : 1);
                intent.putExtra(PlayerActivity.EXTRA_IS_SERIES, isSeries);
                intent.putExtra(PlayerActivity.EXTRA_START_POSITION, savedPosition);

                if (headers != null) {
                    intent.putExtra(PlayerActivity.EXTRA_HEADERS, headers);
                }
                startActivity(intent);
            });
        });
    }
}
