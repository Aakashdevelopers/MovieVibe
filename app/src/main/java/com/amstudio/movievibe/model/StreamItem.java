package com.amstudio.movievibe.model;

import com.google.gson.annotations.SerializedName;
import java.util.Map;

public class StreamItem {

    @SerializedName("id")
    private String id;

    @SerializedName("url")
    private String url;

    @SerializedName("cdn_url")
    private String cdnUrl;

    @SerializedName("format")
    private String format;

    @SerializedName("resolution")
    private String resolution;

    @SerializedName("sign_cookie")
    private String signCookie;

    @SerializedName("headers")
    private Map<String, String> headers;

    public String getId() {
        return id;
    }

    public String getUrl() {
        return (cdnUrl != null && !cdnUrl.isEmpty()) ? cdnUrl : url;
    }

    public String getCdnUrl() {
        return cdnUrl;
    }

    public String getFormat() {
        return format;
    }

    public String getResolution() {
        return resolution;
    }

    public String getSignCookie() {
        return signCookie;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }
}
