package com.amstudio.movievibe;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.OptIn;
import androidx.appcompat.app.AppCompatActivity;
import androidx.media3.common.MediaItem;
import androidx.media3.common.MimeTypes;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.datasource.DefaultHttpDataSource;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.source.MediaSource;
import androidx.media3.exoplayer.source.ProgressiveMediaSource;
import androidx.media3.exoplayer.dash.DashMediaSource;

import com.amstudio.movievibe.databinding.ActivityPlayerBinding;

import java.util.HashMap;
import java.util.Map;

@OptIn(markerClass = UnstableApi.class)
public class PlayerActivity extends AppCompatActivity {

    public static final String EXTRA_STREAM_URL = "extra_stream_url";
    public static final String EXTRA_FORMAT = "extra_format";
    public static final String EXTRA_TITLE = "extra_title";
    public static final String EXTRA_HEADERS = "extra_headers";

    private ActivityPlayerBinding binding;
    private ExoPlayer player;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityPlayerBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        String streamUrl = getIntent().getStringExtra(EXTRA_STREAM_URL);
        String format = getIntent().getStringExtra(EXTRA_FORMAT);
        String title = getIntent().getStringExtra(EXTRA_TITLE);
        @SuppressWarnings("unchecked")
        HashMap<String, String> headers = (HashMap<String, String>) getIntent().getSerializableExtra(EXTRA_HEADERS);

        if (streamUrl == null || streamUrl.isEmpty()) {
            Toast.makeText(this, "Playback URL invalid", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        setupCustomControls(title);
        initializePlayer(streamUrl, format, headers);
    }

    private void setupCustomControls(String title) {
        TextView tvTitle = binding.playerView.findViewById(R.id.tvPlayerTitle);
        if (tvTitle != null && title != null) {
            tvTitle.setText(title);
        }

        ImageButton btnBack = binding.playerView.findViewById(R.id.btnBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }
    }

    private void initializePlayer(String url, String format, HashMap<String, String> headers) {
        DefaultHttpDataSource.Factory dataSourceFactory = new DefaultHttpDataSource.Factory()
                .setAllowCrossProtocolRedirects(true);

        if (headers != null) {
            Map<String, String> requestProps = new HashMap<>();
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                if (entry.getKey().equalsIgnoreCase("User-Agent")) {
                    dataSourceFactory.setUserAgent(entry.getValue());
                } else {
                    requestProps.put(entry.getKey(), entry.getValue());
                }
            }
            dataSourceFactory.setDefaultRequestProperties(requestProps);
        }

        player = new ExoPlayer.Builder(this).build();
        binding.playerView.setPlayer(player);

        MediaSource mediaSource;
        if ("DASH".equalsIgnoreCase(format) || url.contains(".mpd")) {
            MediaItem mediaItem = new MediaItem.Builder()
                    .setUri(url)
                    .setMimeType(MimeTypes.APPLICATION_MPD)
                    .build();
            mediaSource = new DashMediaSource.Factory(dataSourceFactory).createMediaSource(mediaItem);
        } else {
            MediaItem mediaItem = MediaItem.fromUri(url);
            mediaSource = new ProgressiveMediaSource.Factory(dataSourceFactory).createMediaSource(mediaItem);
        }

        player.setMediaSource(mediaSource);
        player.prepare();
        player.setPlayWhenReady(true);

        player.addListener(new Player.Listener() {
            @Override
            public void onPlaybackStateChanged(int playbackState) {
                if (playbackState == Player.STATE_BUFFERING) {
                    binding.playerLoading.setVisibility(View.VISIBLE);
                } else if (playbackState == Player.STATE_READY) {
                    binding.playerLoading.setVisibility(View.GONE);
                } else if (playbackState == Player.STATE_ENDED) {
                    binding.playerLoading.setVisibility(View.GONE);
                }
            }

            @Override
            public void onPlayerError(PlaybackException error) {
                binding.playerLoading.setVisibility(View.GONE);
                Toast.makeText(PlayerActivity.this, "Playback Error: " + error.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (player != null) {
            player.pause();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (player != null) {
            player.release();
            player = null;
        }
    }
}
