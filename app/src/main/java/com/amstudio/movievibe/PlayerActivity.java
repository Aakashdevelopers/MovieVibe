package com.amstudio.movievibe;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.OptIn;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.media3.common.C;
import androidx.media3.common.Format;
import androidx.media3.common.MediaItem;
import androidx.media3.common.MimeTypes;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.PlaybackParameters;
import androidx.media3.common.Player;
import androidx.media3.common.TrackSelectionOverride;
import androidx.media3.common.Tracks;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.datasource.DefaultHttpDataSource;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.dash.DashMediaSource;
import androidx.media3.exoplayer.hls.HlsMediaSource;
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory;
import androidx.media3.exoplayer.source.MediaSource;
import androidx.media3.exoplayer.source.ProgressiveMediaSource;
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector;
import androidx.media3.ui.AspectRatioFrameLayout;

import com.amstudio.movievibe.adapter.EpisodeAdapter;
import com.amstudio.movievibe.adapter.MovieAdapter;
import com.amstudio.movievibe.databinding.ActivityPlayerBinding;
import com.amstudio.movievibe.db.FavoriteRepository;
import com.amstudio.movievibe.db.WatchHistoryRepository;
import com.amstudio.movievibe.db.WatchlistRepository;
import com.amstudio.movievibe.model.HomeFeedResponse;
import com.amstudio.movievibe.model.MovieBoxDetailResponse;
import com.amstudio.movievibe.model.MovieBoxPlayResponse;
import com.amstudio.movievibe.model.MovieBoxSearchResponse;
import com.amstudio.movievibe.model.MovieItem;
import com.amstudio.movievibe.model.StreamItem;
import com.amstudio.movievibe.network.RetrofitClient;

import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

@OptIn(markerClass = UnstableApi.class)
public class PlayerActivity extends AppCompatActivity {

    public static final String EXTRA_STREAM_URL = "extra_stream_url";
    public static final String EXTRA_FORMAT = "extra_format";
    public static final String EXTRA_TITLE = "extra_title";
    public static final String EXTRA_HEADERS = "extra_headers";
    public static final String EXTRA_SUBJECT_ID = "extra_subject_id";
    public static final String EXTRA_POSTER_URL = "extra_poster_url";
    public static final String EXTRA_SEASON = "extra_season";
    public static final String EXTRA_EPISODE = "extra_episode";
    public static final String EXTRA_MAX_EPISODES = "extra_max_episodes";
    public static final String EXTRA_IS_SERIES = "extra_is_series";
    public static final String EXTRA_START_POSITION = "extra_start_position";

    private ActivityPlayerBinding binding;
    private ExoPlayer player;
    private DefaultTrackSelector trackSelector;

    private WatchHistoryRepository repository;
    private WatchlistRepository watchlistRepository;
    private FavoriteRepository favoriteRepository;

    private EpisodeAdapter playerEpisodeAdapter;
    private MovieAdapter customersWatchedAdapter;
    private MovieAdapter topCategoryAdapter;
    private MovieAdapter popularHindiAdapter;

    private String currentStreamUrl;
    private String streamFormat;
    private HashMap<String, String> requestHeaders;
    private String subjectId;
    private String title;
    private String posterUrl;
    private int season = 1;
    private int episode = 1;
    private int maxEpisodes = 1;
    private boolean isSeries = false;
    private long startPosition = 0;
    private boolean hasSeeked = false;

    private boolean isWatchlisted = false;
    private boolean isFavorite = false;

    private boolean isLandscape = false;
    private float currentSpeed = 1.0f;
    private int resizeModeIndex = 0; // 0: FIT, 1: ZOOM (Crop), 2: FILL (Stretch)

    private android.media.AudioManager audioManager;
    private final Handler overlayHideHandler = new Handler(Looper.getMainLooper());
    private final Runnable hideOverlayRunnable = new Runnable() {
        @Override
        public void run() {
            if (binding != null) {
                binding.layoutBrightnessOverlay.setVisibility(View.GONE);
                binding.layoutVolumeOverlay.setVisibility(View.GONE);
            }
        }
    };

    private float startY = 0f;
    private boolean isLeftDrag = false;
    private float initialBrightness = 0.5f;
    private int initialVolume = 0;
    private int maxVolume = 100;

    private final Handler progressHandler = new Handler(Looper.getMainLooper());
    private final Runnable progressRunnable = new Runnable() {
        @Override
        public void run() {
            saveCurrentProgress();
            progressHandler.postDelayed(this, 5000);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (CookieHandler.getDefault() == null) {
            CookieManager cookieManager = new CookieManager();
            cookieManager.setCookiePolicy(CookiePolicy.ACCEPT_ALL);
            CookieHandler.setDefault(cookieManager);
        }

        binding = ActivityPlayerBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        repository = new WatchHistoryRepository(this);
        watchlistRepository = new WatchlistRepository(this);
        favoriteRepository = new FavoriteRepository(this);

        currentStreamUrl = getIntent().getStringExtra(EXTRA_STREAM_URL);
        streamFormat = getIntent().getStringExtra(EXTRA_FORMAT);
        title = getIntent().getStringExtra(EXTRA_TITLE);

        subjectId = getIntent().getStringExtra(EXTRA_SUBJECT_ID);
        if (subjectId != null) {
            subjectId = sanitizeId(subjectId);
        }
        posterUrl = getIntent().getStringExtra(EXTRA_POSTER_URL);
        season = getIntent().getIntExtra(EXTRA_SEASON, 1);
        episode = getIntent().getIntExtra(EXTRA_EPISODE, 1);
        maxEpisodes = getIntent().getIntExtra(EXTRA_MAX_EPISODES, 1);
        isSeries = getIntent().getBooleanExtra(EXTRA_IS_SERIES, false);
        startPosition = getIntent().getLongExtra(EXTRA_START_POSITION, 0);

        @SuppressWarnings("unchecked")
        HashMap<String, String> headers = (HashMap<String, String>) getIntent().getSerializableExtra(EXTRA_HEADERS);
        this.requestHeaders = headers;

        if (currentStreamUrl == null || currentStreamUrl.isEmpty()) {
            Toast.makeText(this, "Playback URL invalid", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        setupCustomControls();
        setupGestureControls();
        setupErrorRetry();
        setupEpisodesAndCarousels();
        setupInPlayerActions();

        fetchMovieOverview();

        initializePlayer(currentStreamUrl, streamFormat, requestHeaders);
    }

    private void setupCustomControls() {
        updatePlayerTitleText();

        ImageButton btnBack = binding.playerView.findViewById(R.id.btnBack);
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

        ImageButton btnRewind10 = binding.playerView.findViewById(R.id.btnRewind10);
        if (btnRewind10 != null) btnRewind10.setOnClickListener(v -> seekRelative(-10000));

        ImageButton btnForward10 = binding.playerView.findViewById(R.id.btnForward10);
        if (btnForward10 != null) btnForward10.setOnClickListener(v -> seekRelative(10000));

        ImageButton btnPlayerSettings = binding.playerView.findViewById(R.id.btnPlayerSettings);
        if (btnPlayerSettings != null) btnPlayerSettings.setOnClickListener(v -> showPlayerSettingsDialog());

        ImageButton btnLock = binding.playerView.findViewById(R.id.btnLock);
        if (btnLock != null) btnLock.setOnClickListener(v -> lockScreen());

        ImageButton btnResizeMode = binding.playerView.findViewById(R.id.btnResizeMode);
        if (btnResizeMode != null) btnResizeMode.setOnClickListener(v -> toggleResizeMode());

        binding.btnUnlockScreen.setOnClickListener(v -> unlockScreen());

        ImageButton btnFullscreen = binding.playerView.findViewById(R.id.btnFullscreen);
        if (btnFullscreen != null) btnFullscreen.setOnClickListener(v -> toggleFullscreen());

        ImageButton btnNextEpisode = binding.playerView.findViewById(R.id.btnNextEpisode);
        if (btnNextEpisode != null) {
            if (isSeries && episode < maxEpisodes) {
                btnNextEpisode.setVisibility(View.VISIBLE);
                btnNextEpisode.setOnClickListener(v -> playNextEpisode());
            } else {
                btnNextEpisode.setVisibility(View.GONE);
            }
        }
    }

    private void showPlayerSettingsDialog() {
        String[] options = {
            "⚙️ Video Quality (Auto / Highest)",
            "💬 Subtitles & Audio",
            "⚡ Playback Speed (" + String.format("%.2fx", currentSpeed) + ")"
        };

        new AlertDialog.Builder(this)
                .setTitle("Player Settings")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        showQualityDialog();
                    } else if (which == 1) {
                        showSubtitleDialog();
                    } else if (which == 2) {
                        showSpeedDialog();
                    }
                })
                .setNegativeButton("Close", null)
                .show();
    }

    private void toggleResizeMode() {
        resizeModeIndex = (resizeModeIndex + 1) % 3;
        if (resizeModeIndex == 0) {
            binding.playerView.setResizeMode(AspectRatioFrameLayout.RESIZE_MODE_FIT);
            Toast.makeText(this, "Aspect Ratio: Fit Screen", Toast.LENGTH_SHORT).show();
        } else if (resizeModeIndex == 1) {
            binding.playerView.setResizeMode(AspectRatioFrameLayout.RESIZE_MODE_ZOOM);
            Toast.makeText(this, "Aspect Ratio: Zoom In / Crop", Toast.LENGTH_SHORT).show();
        } else if (resizeModeIndex == 2) {
            binding.playerView.setResizeMode(AspectRatioFrameLayout.RESIZE_MODE_FILL);
            Toast.makeText(this, "Aspect Ratio: Fill Screen / Stretch", Toast.LENGTH_SHORT).show();
        }
    }

    private void setupGestureControls() {
        audioManager = (android.media.AudioManager) getSystemService(Context.AUDIO_SERVICE);
        if (audioManager != null) {
            maxVolume = audioManager.getStreamMaxVolume(android.media.AudioManager.STREAM_MUSIC);
        }

        binding.playerView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, android.view.MotionEvent event) {
                int action = event.getAction();
                float x = event.getX();
                float y = event.getY();
                int width = v.getWidth();
                int height = v.getHeight();

                if (width <= 0 || height <= 0) return false;

                switch (action) {
                    case android.view.MotionEvent.ACTION_DOWN:
                        startY = y;
                        isLeftDrag = (x < width / 2f);

                        android.view.WindowManager.LayoutParams lp = getWindow().getAttributes();
                        initialBrightness = lp.screenBrightness < 0 ? 0.5f : lp.screenBrightness;

                        if (audioManager != null) {
                            initialVolume = audioManager.getStreamVolume(android.media.AudioManager.STREAM_MUSIC);
                        }
                        break;

                    case android.view.MotionEvent.ACTION_MOVE:
                        float deltaY = startY - y;
                        if (Math.abs(deltaY) > 20) {
                            overlayHideHandler.removeCallbacks(hideOverlayRunnable);

                            float percentDelta = deltaY / (height * 0.75f);

                            if (isLeftDrag) {
                                float newBrightness = Math.max(0.01f, Math.min(1.0f, initialBrightness + percentDelta));
                                android.view.WindowManager.LayoutParams params = getWindow().getAttributes();
                                params.screenBrightness = newBrightness;
                                getWindow().setAttributes(params);

                                int brightPct = Math.round(newBrightness * 100);
                                binding.tvBrightnessLevel.setText(brightPct + "%");
                                binding.layoutBrightnessOverlay.setVisibility(View.VISIBLE);
                                binding.layoutVolumeOverlay.setVisibility(View.GONE);
                            } else {
                                if (audioManager != null && maxVolume > 0) {
                                    int volDelta = Math.round(percentDelta * maxVolume);
                                    int newVolume = Math.max(0, Math.min(maxVolume, initialVolume + volDelta));
                                    audioManager.setStreamVolume(android.media.AudioManager.STREAM_MUSIC, newVolume, 0);

                                    int volPct = Math.round((newVolume / (float) maxVolume) * 100);
                                    binding.tvVolumeLevel.setText(volPct + "%");
                                    binding.layoutVolumeOverlay.setVisibility(View.VISIBLE);
                                    binding.layoutBrightnessOverlay.setVisibility(View.GONE);
                                }
                            }
                        }
                        break;

                    case android.view.MotionEvent.ACTION_UP:
                    case android.view.MotionEvent.ACTION_CANCEL:
                        overlayHideHandler.postDelayed(hideOverlayRunnable, 1000);
                        break;
                }
                return false;
            }
        });
    }

    private void updatePlayerTitleText() {
        TextView tvTitle = binding.playerView.findViewById(R.id.tvPlayerTitle);
        if (tvTitle != null && title != null) {
            if (isSeries) {
                tvTitle.setText(title + " (S" + season + ":E" + episode + ")");
            } else {
                tvTitle.setText(title);
            }
        }
        binding.tvPlayingTitle.setText(title != null ? title : "MovieVibe Player");
    }

    private void fetchMovieOverview() {
        if (subjectId == null) return;
        RetrofitClient.getApiService().getMovieDetail(subjectId).enqueue(new Callback<MovieBoxDetailResponse>() {
            @Override
            public void onResponse(Call<MovieBoxDetailResponse> call, Response<MovieBoxDetailResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    MovieBoxDetailResponse d = response.body();
                    if (d.getPosterUrl() != null && !d.getPosterUrl().isEmpty()) {
                        posterUrl = d.getPosterUrl();
                    }
                    if (d.getDescription() != null && !d.getDescription().isEmpty()) {
                        binding.tvPlayingOverview.setText(d.getDescription());
                    }
                    if (d.getRaw() != null) {
                        String yr = d.getYear() != null ? d.getYear() : "";
                        String dur = d.getRaw().getDuration() != null ? d.getRaw().getDuration() : "";
                        String gen = d.getRaw().getGenre() != null ? d.getRaw().getGenre() : "";
                        binding.tvPlayingInfo.setText(yr + " · " + dur + " · " + gen);
                    }

                    if ("series".equalsIgnoreCase(d.getType()) || isSeries) {
                        isSeries = true;
                        binding.layoutPlayerEpisodes.setVisibility(View.VISIBLE);
                        int epCount = 10;
                        if (d.getSeasonsInfo() != null && d.getSeasonsInfo().getSeasons() != null && !d.getSeasonsInfo().getSeasons().isEmpty()) {
                            epCount = d.getSeasonsInfo().getSeasons().get(0).getMaxEpisodes();
                            if (epCount <= 0) epCount = maxEpisodes > 0 ? maxEpisodes : 10;
                        } else if (maxEpisodes > 0) {
                            epCount = maxEpisodes;
                        }
                        playerEpisodeAdapter.setEpisodeData(epCount, posterUrl, title, d.getDescription());
                    } else {
                        binding.layoutPlayerEpisodes.setVisibility(View.GONE);
                    }
                }
            }

            @Override
            public void onFailure(Call<MovieBoxDetailResponse> call, Throwable t) {}
        });
    }

    private void setupEpisodesAndCarousels() {
        playerEpisodeAdapter = new EpisodeAdapter(this::playEpisodeInPlayer);
        binding.rvPlayerEpisodes.setAdapter(playerEpisodeAdapter);

        customersWatchedAdapter = new MovieAdapter(this::switchMovieInPlayer);
        binding.rvCustomersAlsoWatched.setAdapter(customersWatchedAdapter);

        topCategoryAdapter = new MovieAdapter(this::switchMovieInPlayer);
        binding.rvTopCategoryMovies.setAdapter(topCategoryAdapter);

        popularHindiAdapter = new MovieAdapter(this::switchMovieInPlayer);
        binding.rvPopularHindiMovies.setAdapter(popularHindiAdapter);

        fetchPrimeCarouselsData();
    }

    private void playEpisodeInPlayer(int targetEpisodeNum) {
        saveCurrentProgress();
        episode = targetEpisodeNum;
        updatePlayerTitleText();

        binding.playerLoading.setVisibility(View.VISIBLE);
        RetrofitClient.getApiService().getPlaybackStream(subjectId, season, targetEpisodeNum).enqueue(new Callback<MovieBoxPlayResponse>() {
            @Override
            public void onResponse(Call<MovieBoxPlayResponse> call, Response<MovieBoxPlayResponse> response) {
                binding.playerLoading.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null && response.body().getStreams() != null && !response.body().getStreams().isEmpty()) {
                    StreamItem stream = response.body().getStreams().get(0);
                    currentStreamUrl = stream.getUrl();
                    streamFormat = stream.getFormat();
                    requestHeaders = extractHeaders(stream);
                    hasSeeked = false;
                    startPosition = 0;

                    if (player != null) {
                        player.release();
                        player = null;
                    }
                    initializePlayer(currentStreamUrl, streamFormat, requestHeaders);
                    Toast.makeText(PlayerActivity.this, "Playing Episode " + targetEpisodeNum, Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(PlayerActivity.this, "Episode " + targetEpisodeNum + " stream unavailable", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<MovieBoxPlayResponse> call, Throwable t) {
                binding.playerLoading.setVisibility(View.GONE);
                Toast.makeText(PlayerActivity.this, "Failed to load episode: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void fetchPrimeCarouselsData() {
        RetrofitClient.getApiService().getHomeFeed().enqueue(new Callback<HomeFeedResponse>() {
            @Override
            public void onResponse(Call<HomeFeedResponse> call, Response<HomeFeedResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                    List<HomeFeedResponse.HomeSectionItem> sections = response.body().getData().getItems();
                    if (sections != null) {
                        List<MovieItem> list = new ArrayList<>();
                        for (HomeFeedResponse.HomeSectionItem sec : sections) {
                            if (sec.getSubjects() != null) {
                                for (MovieItem m : sec.getSubjects()) {
                                    if (m != null && m.getSubjectId() != null && !m.getSubjectId().trim().equals(subjectId)) {
                                        list.add(m);
                                    }
                                }
                            }
                        }
                        customersWatchedAdapter.setMovies(list);
                    }
                }
            }

            @Override
            public void onFailure(Call<HomeFeedResponse> call, Throwable t) {}
        });

        String catQuery = isSeries ? "Series" : "Action Movie";
        RetrofitClient.getApiService().searchMovies(catQuery, 1).enqueue(new Callback<MovieBoxSearchResponse>() {
            @Override
            public void onResponse(Call<MovieBoxSearchResponse> call, Response<MovieBoxSearchResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getItems() != null) {
                    List<MovieItem> list = new ArrayList<>();
                    for (MovieItem m : response.body().getItems()) {
                        if (m != null && m.getSubjectId() != null && !m.getSubjectId().trim().equals(subjectId)) {
                            list.add(m);
                        }
                    }
                    topCategoryAdapter.setMovies(list);
                }
            }

            @Override
            public void onFailure(Call<MovieBoxSearchResponse> call, Throwable t) {}
        });

        RetrofitClient.getApiService().searchMovies("Hindi Dubbed", 1).enqueue(new Callback<MovieBoxSearchResponse>() {
            @Override
            public void onResponse(Call<MovieBoxSearchResponse> call, Response<MovieBoxSearchResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getItems() != null) {
                    List<MovieItem> list = new ArrayList<>();
                    for (MovieItem m : response.body().getItems()) {
                        if (m != null && m.getSubjectId() != null && !m.getSubjectId().trim().equals(subjectId)) {
                            list.add(m);
                        }
                    }
                    popularHindiAdapter.setMovies(list);
                }
            }

            @Override
            public void onFailure(Call<MovieBoxSearchResponse> call, Throwable t) {}
        });
    }

    private void switchMovieInPlayer(MovieItem movie) {
        if (movie == null || movie.getSubjectId() == null) return;

        saveCurrentProgress();

        subjectId = sanitizeId(movie.getSubjectId());
        title = movie.getTitle();
        posterUrl = movie.getPosterUrl();
        season = 1;
        episode = 1;
        isSeries = "series".equalsIgnoreCase(movie.getType());
        startPosition = 0;
        hasSeeked = false;

        updatePlayerTitleText();
        checkLibraryStates();
        fetchMovieOverview();
        fetchPrimeCarouselsData();

        Toast.makeText(this, "Loading " + title + "...", Toast.LENGTH_SHORT).show();

        binding.playerLoading.setVisibility(View.VISIBLE);
        RetrofitClient.getApiService().getPlaybackStream(subjectId, 1, 1).enqueue(new Callback<MovieBoxPlayResponse>() {
            @Override
            public void onResponse(Call<MovieBoxPlayResponse> call, Response<MovieBoxPlayResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getStreams() != null && !response.body().getStreams().isEmpty()) {
                    StreamItem stream = response.body().getStreams().get(0);
                    currentStreamUrl = stream.getUrl();
                    streamFormat = stream.getFormat();
                    requestHeaders = extractHeaders(stream);

                    if (player != null) {
                        player.release();
                        player = null;
                    }
                    initializePlayer(currentStreamUrl, streamFormat, requestHeaders);
                } else {
                    switchMovieFallback(0, 0);
                }
            }

            @Override
            public void onFailure(Call<MovieBoxPlayResponse> call, Throwable t) {
                binding.playerLoading.setVisibility(View.GONE);
                Toast.makeText(PlayerActivity.this, "Stream Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void switchMovieFallback(int fallbackSeason, int fallbackEpisode) {
        RetrofitClient.getApiService().getPlaybackStream(subjectId, fallbackSeason, fallbackEpisode).enqueue(new Callback<MovieBoxPlayResponse>() {
            @Override
            public void onResponse(Call<MovieBoxPlayResponse> call, Response<MovieBoxPlayResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getStreams() != null && !response.body().getStreams().isEmpty()) {
                    StreamItem stream = response.body().getStreams().get(0);
                    currentStreamUrl = stream.getUrl();
                    streamFormat = stream.getFormat();
                    requestHeaders = extractHeaders(stream);

                    if (player != null) {
                        player.release();
                        player = null;
                    }
                    initializePlayer(currentStreamUrl, streamFormat, requestHeaders);
                } else {
                    binding.playerLoading.setVisibility(View.GONE);
                    Toast.makeText(PlayerActivity.this, "Suggested stream unavailable", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<MovieBoxPlayResponse> call, Throwable t) {
                binding.playerLoading.setVisibility(View.GONE);
                Toast.makeText(PlayerActivity.this, "Stream Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupInPlayerActions() {
        binding.btnPlayerWatchlist.setOnClickListener(v -> toggleWatchlist());
        binding.btnPlayerFavorite.setOnClickListener(v -> toggleFavorite());

        binding.btnPlayerShare.setOnClickListener(v -> {
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, title);
            shareIntent.putExtra(Intent.EXTRA_TEXT, "Watch " + title + " on MovieVibe!");
            startActivity(Intent.createChooser(shareIntent, "Share via"));
        });

        checkLibraryStates();
    }

    private void checkLibraryStates() {
        if (subjectId == null) return;

        watchlistRepository.isWatchlisted(subjectId, item -> {
            isWatchlisted = (item != null);
            runOnUiThread(() -> {
                if (isWatchlisted) {
                    binding.btnPlayerWatchlist.setText("✓ In Watchlist");
                    binding.btnPlayerWatchlist.setIconResource(R.drawable.ic_bookmark_added);
                } else {
                    binding.btnPlayerWatchlist.setText("+ Watchlist");
                    binding.btnPlayerWatchlist.setIconResource(R.drawable.ic_bookmark);
                }
            });
        });

        favoriteRepository.isFavorite(subjectId, item -> {
            isFavorite = (item != null);
            runOnUiThread(() -> {
                if (isFavorite) {
                    binding.btnPlayerFavorite.setText("Favorited");
                    binding.btnPlayerFavorite.setIconResource(R.drawable.ic_heart_filled);
                } else {
                    binding.btnPlayerFavorite.setText("Favorite");
                    binding.btnPlayerFavorite.setIconResource(R.drawable.ic_heart_outline);
                }
            });
        });
    }

    private void toggleWatchlist() {
        if (subjectId == null) return;

        String type = isSeries ? "series" : "movie";
        if (isWatchlisted) {
            watchlistRepository.removeFromWatchlist(subjectId);
            isWatchlisted = false;
            binding.btnPlayerWatchlist.setText("+ Watchlist");
            binding.btnPlayerWatchlist.setIconResource(R.drawable.ic_bookmark);
            Toast.makeText(this, "Removed from Watchlist", Toast.LENGTH_SHORT).show();
        } else {
            watchlistRepository.addToWatchlist(subjectId, title, posterUrl, type);
            isWatchlisted = true;
            binding.btnPlayerWatchlist.setText("✓ In Watchlist");
            binding.btnPlayerWatchlist.setIconResource(R.drawable.ic_bookmark_added);
            Toast.makeText(this, "Added to Watchlist", Toast.LENGTH_SHORT).show();
        }
    }

    private void toggleFavorite() {
        if (subjectId == null) return;

        String type = isSeries ? "series" : "movie";
        if (isFavorite) {
            favoriteRepository.removeFromFavorites(subjectId);
            isFavorite = false;
            binding.btnPlayerFavorite.setText("Favorite");
            binding.btnPlayerFavorite.setIconResource(R.drawable.ic_heart_outline);
            Toast.makeText(this, "Removed from Favorites", Toast.LENGTH_SHORT).show();
        } else {
            favoriteRepository.addToFavorites(subjectId, title, posterUrl, type);
            isFavorite = true;
            binding.btnPlayerFavorite.setText("Favorited");
            binding.btnPlayerFavorite.setIconResource(R.drawable.ic_heart_filled);
            Toast.makeText(this, "Added to Favorites", Toast.LENGTH_SHORT).show();
        }
    }

    private void setupErrorRetry() {
        binding.btnRetry.setOnClickListener(v -> retryPlayback());
    }

    private String sanitizeId(String id) {
        return id != null ? id.trim().replaceAll("[\\r\\n\\t]", "") : "";
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

    private void initializePlayer(String url, String format, HashMap<String, String> headers) {
        binding.layoutError.setVisibility(View.GONE);
        binding.playerLoading.setVisibility(View.VISIBLE);

        DefaultHttpDataSource.Factory dataSourceFactory = new DefaultHttpDataSource.Factory()
                .setAllowCrossProtocolRedirects(true);

        Map<String, String> requestProps = new HashMap<>();
        requestProps.put("User-Agent", "com.community.oneroom/50020119 (Linux; U; Android 13; en_US; 23078RKD5C; Build/TQ2A.230405.003; Cronet/135.0.7012.3)");
        requestProps.put("Referer", "https://sportslive.wine");

        if (headers != null) {
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                if (entry.getKey().equalsIgnoreCase("User-Agent")) {
                    dataSourceFactory.setUserAgent(entry.getValue());
                }
                requestProps.put(entry.getKey(), entry.getValue());
            }
        }
        dataSourceFactory.setDefaultRequestProperties(requestProps);

        trackSelector = new DefaultTrackSelector(this);
        trackSelector.setParameters(trackSelector.buildUponParameters()
                .setForceHighestSupportedBitrate(true)
                .setMaxVideoSize(3840, 2160)
                .setMaxVideoFrameRate(60)
                .setViewportSizeToPhysicalDisplaySize(this, true)
                .setPreferredTextLanguage("en")
                .setPreferredTextRoleFlags(C.ROLE_FLAG_SUBTITLE | C.ROLE_FLAG_CAPTION)
                .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
                .build());

        player = new ExoPlayer.Builder(this)
                .setTrackSelector(trackSelector)
                .build();

        binding.playerView.setPlayer(player);

        if (binding.playerView.getSubtitleView() != null) {
            binding.playerView.getSubtitleView().setStyle(
                    new androidx.media3.ui.CaptionStyleCompat(
                            android.graphics.Color.WHITE,
                            android.graphics.Color.argb(180, 0, 0, 0),
                            android.graphics.Color.TRANSPARENT,
                            androidx.media3.ui.CaptionStyleCompat.EDGE_TYPE_DROP_SHADOW,
                            android.graphics.Color.BLACK,
                            android.graphics.Typeface.DEFAULT_BOLD
                    )
            );
            binding.playerView.getSubtitleView().setFixedTextSize(
                    android.util.TypedValue.COMPLEX_UNIT_SP, 18f
            );
        }

        MediaSource mediaSource = buildMediaSource(url, format, dataSourceFactory);
        player.setMediaSource(mediaSource);
        player.prepare();
        player.setPlayWhenReady(true);
        player.setPlaybackParameters(new PlaybackParameters(currentSpeed));

        player.addListener(new Player.Listener() {
            @Override
            public void onPlaybackStateChanged(int playbackState) {
                if (playbackState == Player.STATE_BUFFERING) {
                    binding.playerLoading.setVisibility(View.VISIBLE);
                } else if (playbackState == Player.STATE_READY) {
                    binding.playerLoading.setVisibility(View.GONE);
                    binding.layoutError.setVisibility(View.GONE);
                    if (!hasSeeked && startPosition > 0) {
                        player.seekTo(startPosition);
                        hasSeeked = true;
                    }
                    startProgressTracking();
                } else if (playbackState == Player.STATE_ENDED) {
                    binding.playerLoading.setVisibility(View.GONE);
                    saveCurrentProgress();
                    stopProgressTracking();
                    if (isSeries && episode < maxEpisodes) {
                        promptAutoPlayNextEpisode();
                    }
                }
            }

            @Override
            public void onPlayerError(PlaybackException error) {
                binding.playerLoading.setVisibility(View.GONE);
                stopProgressTracking();

                String errorMsg = "Playback Error: Unable to play stream";
                if (error.errorCode == PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED
                        || error.errorCode == PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT) {
                    errorMsg = "Network Error: Connection failed";
                } else if (error.errorCode == PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS
                        || error.errorCode == PlaybackException.ERROR_CODE_IO_INVALID_HTTP_CONTENT_TYPE) {
                    errorMsg = "Stream link expired or access denied";
                } else if (error.errorCode == PlaybackException.ERROR_CODE_PARSING_CONTAINER_UNSUPPORTED
                        || error.errorCode == PlaybackException.ERROR_CODE_PARSING_MANIFEST_MALFORMED) {
                    errorMsg = "Unsupported stream format";
                }

                binding.tvErrorMessage.setText(errorMsg);
                binding.layoutError.setVisibility(View.VISIBLE);
            }
        });
    }

    private MediaSource buildMediaSource(String url, String format, DefaultHttpDataSource.Factory dataSourceFactory) {
        String lowerUrl = url != null ? url.toLowerCase() : "";
        String lowerFormat = format != null ? format.toLowerCase() : "";

        if (lowerFormat.contains("hls") || lowerFormat.contains("m3u8") || lowerUrl.contains(".m3u8")) {
            MediaItem mediaItem = new MediaItem.Builder()
                    .setUri(url)
                    .setMimeType(MimeTypes.APPLICATION_M3U8)
                    .build();
            return new HlsMediaSource.Factory(dataSourceFactory).createMediaSource(mediaItem);
        } else if (lowerFormat.contains("dash") || lowerFormat.contains("mpd") || lowerUrl.contains(".mpd")) {
            MediaItem mediaItem = new MediaItem.Builder()
                    .setUri(url)
                    .setMimeType(MimeTypes.APPLICATION_MPD)
                    .build();
            return new DashMediaSource.Factory(dataSourceFactory).createMediaSource(mediaItem);
        } else if (lowerFormat.contains("mp4") || lowerUrl.contains(".mp4") || lowerUrl.contains(".mkv")) {
            MediaItem mediaItem = MediaItem.fromUri(url);
            return new ProgressiveMediaSource.Factory(dataSourceFactory).createMediaSource(mediaItem);
        } else {
            MediaItem mediaItem = MediaItem.fromUri(url);
            return new DefaultMediaSourceFactory(dataSourceFactory).createMediaSource(mediaItem);
        }
    }

    private void seekRelative(long offsetMs) {
        if (player != null) {
            long newPos = player.getCurrentPosition() + offsetMs;
            newPos = Math.max(0, Math.min(player.getDuration(), newPos));
            player.seekTo(newPos);
        }
    }

    private void showSpeedDialog() {
        String[] speedLabels = {"0.5x", "1.0x (Normal)", "1.25x", "1.5x", "2.0x"};
        float[] speedValues = {0.5f, 1.0f, 1.25f, 1.5f, 2.0f};

        int selectedIndex = 1;
        for (int i = 0; i < speedValues.length; i++) {
            if (Math.abs(speedValues[i] - currentSpeed) < 0.05f) {
                selectedIndex = i;
                break;
            }
        }

        new AlertDialog.Builder(this)
                .setTitle("Playback Speed")
                .setSingleChoiceItems(speedLabels, selectedIndex, (dialog, which) -> {
                    currentSpeed = speedValues[which];
                    if (player != null) {
                        player.setPlaybackParameters(new PlaybackParameters(currentSpeed));
                    }
                    Toast.makeText(this, "Speed: " + speedLabels[which], Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                })
                .show();
    }

    private void showQualityDialog() {
        if (player == null || trackSelector == null) return;

        Tracks currentTracks = player.getCurrentTracks();
        List<Format> videoFormats = new ArrayList<>();
        List<TrackSelectionOverride> overrides = new ArrayList<>();

        for (Tracks.Group group : currentTracks.getGroups()) {
            if (group.getType() == C.TRACK_TYPE_VIDEO) {
                for (int i = 0; i < group.length; i++) {
                    videoFormats.add(group.getTrackFormat(i));
                    overrides.add(new TrackSelectionOverride(group.getMediaTrackGroup(), i));
                }
            }
        }

        if (videoFormats.isEmpty()) {
            Toast.makeText(this, "Quality selection not available for this stream", Toast.LENGTH_SHORT).show();
            return;
        }

        List<String> options = new ArrayList<>();
        options.add("Auto");
        for (Format f : videoFormats) {
            String resLabel = f.height > 0 ? f.height + "p" : "Standard";
            if (f.bitrate > 0) {
                resLabel += " (" + (f.bitrate / 1000) + " kbps)";
            }
            options.add(resLabel);
        }

        new AlertDialog.Builder(this)
                .setTitle("Video Quality")
                .setItems(options.toArray(new String[0]), (dialog, which) -> {
                    if (which == 0) {
                        trackSelector.setParameters(trackSelector.buildUponParameters()
                                .clearOverridesOfType(C.TRACK_TYPE_VIDEO)
                                .build());
                        Toast.makeText(this, "Quality set to Auto", Toast.LENGTH_SHORT).show();
                    } else {
                        TrackSelectionOverride override = overrides.get(which - 1);
                        trackSelector.setParameters(trackSelector.buildUponParameters()
                                .setOverrideForType(override)
                                .build());
                        Toast.makeText(this, "Quality set to " + options.get(which), Toast.LENGTH_SHORT).show();
                    }
                })
                .show();
    }

    private String formatLanguageName(String langCode, String label) {
        if (label != null && !label.trim().isEmpty()) {
            return label.trim() + " (CC)";
        }
        if (langCode == null || langCode.isEmpty()) {
            return "Subtitle (CC)";
        }
        try {
            java.util.Locale loc = new java.util.Locale(langCode);
            String display = loc.getDisplayLanguage();
            if (display != null && !display.isEmpty()) {
                return display.substring(0, 1).toUpperCase() + display.substring(1) + " (CC)";
            }
        } catch (Exception ignored) {}
        return langCode.toUpperCase() + " (CC)";
    }

    private void showSubtitleDialog() {
        if (player == null || trackSelector == null) return;

        Tracks currentTracks = player.getCurrentTracks();
        List<String> options = new ArrayList<>();
        options.add("Off");

        List<TrackSelectionOverride> overrides = new ArrayList<>();

        for (Tracks.Group group : currentTracks.getGroups()) {
            if (group.getType() == C.TRACK_TYPE_TEXT) {
                for (int i = 0; i < group.length; i++) {
                    Format format = group.getTrackFormat(i);
                    String label = formatLanguageName(format.language, format.label);
                    options.add(label);
                    overrides.add(new TrackSelectionOverride(group.getMediaTrackGroup(), i));
                }
            }
        }

        if (options.size() == 1) {
            Toast.makeText(this, "No subtitles available for this video", Toast.LENGTH_SHORT).show();
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle("Subtitles & Closed Captions")
                .setItems(options.toArray(new String[0]), (dialog, which) -> {
                    if (which == 0) {
                        trackSelector.setParameters(trackSelector.buildUponParameters()
                                .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
                                .clearOverridesOfType(C.TRACK_TYPE_TEXT)
                                .build());
                        Toast.makeText(this, "Subtitles turned off", Toast.LENGTH_SHORT).show();
                    } else {
                        TrackSelectionOverride override = overrides.get(which - 1);
                        trackSelector.setParameters(trackSelector.buildUponParameters()
                                .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
                                .setOverrideForType(override)
                                .build());
                        Toast.makeText(this, "Subtitle set to " + options.get(which), Toast.LENGTH_SHORT).show();
                    }
                })
                .show();
    }

    private void lockScreen() {
        binding.playerView.hideController();
        binding.playerView.setUseController(false);
        binding.btnUnlockScreen.setVisibility(View.VISIBLE);
        Toast.makeText(this, "Controls Locked", Toast.LENGTH_SHORT).show();
    }

    private void unlockScreen() {
        binding.btnUnlockScreen.setVisibility(View.GONE);
        binding.playerView.setUseController(true);
        binding.playerView.showController();
        Toast.makeText(this, "Controls Unlocked", Toast.LENGTH_SHORT).show();
    }

    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }

    private void toggleFullscreen() {
        isLandscape = !isLandscape;
        WindowInsetsControllerCompat insetsController = WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());

        LinearLayout.LayoutParams playerParams = (LinearLayout.LayoutParams) binding.playerContainer.getLayoutParams();

        if (isLandscape) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
            insetsController.hide(WindowInsetsCompat.Type.systemBars());
            insetsController.setSystemBarsBehavior(WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);

            binding.scrollDetails.setVisibility(View.GONE);
            playerParams.height = LinearLayout.LayoutParams.MATCH_PARENT;
            binding.playerContainer.setLayoutParams(playerParams);
        } else {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
            insetsController.show(WindowInsetsCompat.Type.systemBars());

            binding.scrollDetails.setVisibility(View.VISIBLE);
            playerParams.height = dpToPx(220);
            binding.playerContainer.setLayoutParams(playerParams);
        }
    }

    private void playNextEpisode() {
        if (!isSeries || episode >= maxEpisodes || subjectId == null) return;

        int nextEp = episode + 1;
        saveCurrentProgress();
        binding.playerLoading.setVisibility(View.VISIBLE);

        RetrofitClient.getApiService().getPlaybackStream(subjectId, season, nextEp).enqueue(new Callback<MovieBoxPlayResponse>() {
            @Override
            public void onResponse(Call<MovieBoxPlayResponse> call, Response<MovieBoxPlayResponse> response) {
                binding.playerLoading.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null && response.body().getStreams() != null && !response.body().getStreams().isEmpty()) {
                    StreamItem stream = response.body().getStreams().get(0);
                    episode = nextEp;
                    currentStreamUrl = stream.getUrl();
                    streamFormat = stream.getFormat();
                    requestHeaders = extractHeaders(stream);
                    hasSeeked = false;
                    startPosition = 0;

                    updatePlayerTitleText();
                    if (player != null) {
                        player.release();
                        player = null;
                    }
                    initializePlayer(currentStreamUrl, streamFormat, requestHeaders);
                    Toast.makeText(PlayerActivity.this, "Playing Episode " + episode, Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(PlayerActivity.this, "Next episode stream unavailable", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<MovieBoxPlayResponse> call, Throwable t) {
                binding.playerLoading.setVisibility(View.GONE);
                Toast.makeText(PlayerActivity.this, "Failed to load next episode: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void promptAutoPlayNextEpisode() {
        new AlertDialog.Builder(this)
                .setTitle("Episode Ended")
                .setMessage("Play Episode " + (episode + 1) + "?")
                .setPositiveButton("Play Next", (dialog, which) -> playNextEpisode())
                .setNegativeButton("Close", null)
                .show();
    }

    private void retryPlayback() {
        binding.layoutError.setVisibility(View.GONE);
        binding.playerLoading.setVisibility(View.VISIBLE);

        if (subjectId != null && !subjectId.isEmpty()) {
            int reqSeason = isSeries ? season : 1;
            int reqEpisode = isSeries ? episode : 1;

            RetrofitClient.getApiService().getPlaybackStream(subjectId, reqSeason, reqEpisode).enqueue(new Callback<MovieBoxPlayResponse>() {
                @Override
                public void onResponse(Call<MovieBoxPlayResponse> call, Response<MovieBoxPlayResponse> response) {
                    if (response.isSuccessful() && response.body() != null && response.body().getStreams() != null && !response.body().getStreams().isEmpty()) {
                        StreamItem stream = response.body().getStreams().get(0);
                        currentStreamUrl = stream.getUrl();
                        streamFormat = stream.getFormat();
                        requestHeaders = extractHeaders(stream);

                        if (player != null) {
                            player.release();
                            player = null;
                        }
                        initializePlayer(currentStreamUrl, streamFormat, requestHeaders);
                    } else if (player != null) {
                        binding.layoutError.setVisibility(View.GONE);
                        binding.playerLoading.setVisibility(View.VISIBLE);
                        player.prepare();
                        player.setPlayWhenReady(true);
                    } else {
                        binding.playerLoading.setVisibility(View.GONE);
                        binding.tvErrorMessage.setText("Stream link unavailable");
                        binding.layoutError.setVisibility(View.VISIBLE);
                    }
                }

                @Override
                public void onFailure(Call<MovieBoxPlayResponse> call, Throwable t) {
                    if (player != null) {
                        binding.layoutError.setVisibility(View.GONE);
                        binding.playerLoading.setVisibility(View.VISIBLE);
                        player.prepare();
                        player.setPlayWhenReady(true);
                    } else {
                        binding.playerLoading.setVisibility(View.GONE);
                        binding.tvErrorMessage.setText("Network error: " + t.getMessage());
                        binding.layoutError.setVisibility(View.VISIBLE);
                    }
                }
            });
        } else if (player != null) {
            player.prepare();
            player.setPlayWhenReady(true);
        }
    }

    private void startProgressTracking() {
        stopProgressTracking();
        progressHandler.post(progressRunnable);
    }

    private void stopProgressTracking() {
        progressHandler.removeCallbacks(progressRunnable);
    }

    private void saveCurrentProgress() {
        if (player != null && subjectId != null && !subjectId.isEmpty()) {
            long pos = player.getCurrentPosition();
            long dur = player.getDuration();
            if (dur > 0 && pos >= 0) {
                repository.saveProgress(subjectId, title, posterUrl, pos, dur, season, episode, isSeries);
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        saveCurrentProgress();
        stopProgressTracking();
        if (player != null) {
            player.pause();
        }
    }

    @Override
    protected void onDestroy() {
        saveCurrentProgress();
        stopProgressTracking();
        if (player != null) {
            player.release();
            player = null;
        }
        super.onDestroy();
    }
}
