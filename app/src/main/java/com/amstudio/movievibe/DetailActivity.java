package com.amstudio.movievibe;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.amstudio.movievibe.adapter.CastAdapter;
import com.amstudio.movievibe.adapter.EpisodeAdapter;
import com.amstudio.movievibe.databinding.ActivityDetailBinding;
import com.amstudio.movievibe.model.DubItem;
import com.amstudio.movievibe.model.MovieBoxDetailResponse;
import com.amstudio.movievibe.model.MovieBoxPlayResponse;
import com.amstudio.movievibe.model.ResolutionItem;
import com.amstudio.movievibe.model.ResourceDetector;
import com.amstudio.movievibe.model.SeasonItem;
import com.amstudio.movievibe.model.StreamItem;
import com.amstudio.movievibe.network.RetrofitClient;
import com.bumptech.glide.Glide;

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

    private String subjectId;
    private String movieTitle;
    private boolean isSeries = false;
    private int currentSeason = 1;

    private MovieBoxDetailResponse detailData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        subjectId = getIntent().getStringExtra(EXTRA_SUBJECT_ID);
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

        fetchMovieDetail();
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
    }

    private void fetchMovieDetail() {
        binding.progressBarDetail.setVisibility(View.VISIBLE);

        RetrofitClient.getApiService().getMovieDetail(subjectId).enqueue(new Callback<MovieBoxDetailResponse>() {
            @Override
            public void onResponse(Call<MovieBoxDetailResponse> call, Response<MovieBoxDetailResponse> response) {
                binding.progressBarDetail.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null) {
                    detailData = response.body();
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

        Glide.with(this)
                .load(detail.getPosterUrl())
                .into(binding.ivBackdrop);

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
            if (detail.getSeasonsInfo() != null && detail.getSeasonsInfo().getSeasons() != null && !detail.getSeasonsInfo().getSeasons().isEmpty()) {
                SeasonItem season = detail.getSeasonsInfo().getSeasons().get(0);
                currentSeason = season.getSeasonNumber();
                episodeAdapter.setTotalEpisodes(season.getMaxEpisodes() > 0 ? season.getMaxEpisodes() : 10);
            } else {
                episodeAdapter.setTotalEpisodes(10);
            }
        } else {
            binding.layoutEpisodes.setVisibility(View.GONE);
        }
    }

    private void playVideo(int season, int episode, boolean useWebView) {
        // First check if direct resolution link exists in raw detail
        if (detailData != null && detailData.getRaw() != null && detailData.getRaw().getResourceDetectors() != null) {
            for (ResourceDetector detector : detailData.getRaw().getResourceDetectors()) {
                if (detector.getResolutionList() != null && !detector.getResolutionList().isEmpty()) {
                    ResolutionItem bestRes = detector.getResolutionList().get(detector.getResolutionList().size() - 1);
                    if (bestRes.getResourceLink() != null && !bestRes.getResourceLink().isEmpty()) {
                        if (useWebView) {
                            launchWebViewPlayer(bestRes.getResourceLink(), detailData.getTitle());
                        } else {
                            launchPlayer(bestRes.getResourceLink(), "MP4", null, detailData.getTitle());
                        }
                        return;
                    }
                }
            }
        }

        // Otherwise fetch stream from /mb/play endpoint
        binding.progressBarDetail.setVisibility(View.VISIBLE);
        RetrofitClient.getApiService().getPlaybackStream(subjectId, season, episode).enqueue(new Callback<MovieBoxPlayResponse>() {
            @Override
            public void onResponse(Call<MovieBoxPlayResponse> call, Response<MovieBoxPlayResponse> response) {
                binding.progressBarDetail.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null && response.body().getStreams() != null && !response.body().getStreams().isEmpty()) {
                    StreamItem stream = response.body().getStreams().get(0);
                    if (useWebView) {
                        launchWebViewPlayer(stream.getUrl(), movieTitle);
                    } else {
                        HashMap<String, String> headerMap = stream.getHeaders() != null ? new HashMap<>(stream.getHeaders()) : null;
                        launchPlayer(stream.getUrl(), stream.getFormat(), headerMap, movieTitle);
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

    private void launchPlayer(String streamUrl, String format, HashMap<String, String> headers, String title) {
        Intent intent = new Intent(this, PlayerActivity.class);
        intent.putExtra(PlayerActivity.EXTRA_STREAM_URL, streamUrl);
        intent.putExtra(PlayerActivity.EXTRA_FORMAT, format);
        intent.putExtra(PlayerActivity.EXTRA_TITLE, title);
        if (headers != null) {
            intent.putExtra(PlayerActivity.EXTRA_HEADERS, headers);
        }
        startActivity(intent);
    }
}
