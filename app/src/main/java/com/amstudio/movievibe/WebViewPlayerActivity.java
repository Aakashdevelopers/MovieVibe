package com.amstudio.movievibe;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.amstudio.movievibe.databinding.ActivityWebViewPlayerBinding;

public class WebViewPlayerActivity extends AppCompatActivity {

    public static final String EXTRA_WEB_URL = "extra_web_url";
    public static final String EXTRA_WEB_TITLE = "extra_web_title";

    private ActivityWebViewPlayerBinding binding;
    private View customView;
    private WebChromeClient.CustomViewCallback customViewCallback;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityWebViewPlayerBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        String webUrl = getIntent().getStringExtra(EXTRA_WEB_URL);
        String webTitle = getIntent().getStringExtra(EXTRA_WEB_TITLE);

        if (webUrl == null || webUrl.isEmpty()) {
            Toast.makeText(this, "Invalid Web URL", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        binding.tvWebTitle.setText(webTitle != null ? webTitle : "Web Player");
        binding.btnWebBack.setOnClickListener(v -> onBackPressed());
        binding.btnWebReload.setOnClickListener(v -> binding.webView.reload());

        setupWebView(webUrl);
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void setupWebView(String url) {
        WebSettings settings = binding.webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);
        settings.setMediaPlaybackRequiresUserGesture(false);
        settings.setUserAgentString("Mozilla/5.0 (Linux; Android 13; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36");

        binding.webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
                binding.webProgressBar.setVisibility(View.VISIBLE);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                binding.webProgressBar.setVisibility(View.GONE);
            }
        });

        binding.webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onShowCustomView(View view, CustomViewCallback callback) {
                if (customView != null) {
                    onHideCustomView();
                    return;
                }
                customView = view;
                customViewCallback = callback;
                binding.fullScreenContainer.addView(view);
                binding.fullScreenContainer.setVisibility(View.VISIBLE);
                binding.topBarWeb.setVisibility(View.GONE);
                binding.webView.setVisibility(View.GONE);

                getWindow().getDecorView().setSystemUiVisibility(
                        View.SYSTEM_UI_FLAG_FULLSCREEN
                                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
            }

            @Override
            public void onHideCustomView() {
                if (customView == null) return;

                binding.fullScreenContainer.removeView(customView);
                binding.fullScreenContainer.setVisibility(View.GONE);
                binding.topBarWeb.setVisibility(View.VISIBLE);
                binding.webView.setVisibility(View.VISIBLE);

                customView = null;
                if (customViewCallback != null) {
                    customViewCallback.onCustomViewHidden();
                    customViewCallback = null;
                }

                getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
            }
        });

        binding.webView.loadUrl(url);
    }

    @Override
    public void onBackPressed() {
        if (customView != null) {
            binding.webView.getWebChromeClient().onHideCustomView();
        } else if (binding.webView.canGoBack()) {
            binding.webView.goBack();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {
        if (binding != null && binding.webView != null) {
            binding.webView.destroy();
        }
        super.onDestroy();
    }
}
