package com.amstudio.movievibe.model;

import com.google.gson.annotations.SerializedName;
import java.util.Map;

public class StreamItem {

    @SerializedName("id")
    private String id;

    @SerializedName("url")
    private String url;

    @SerializedName("format")
    private String format;

    @SerializedName("resolution")
    private String resolution;

    @SerializedName("headers")
    private Map<String, String> headers;

    public String getId() {
        return id;
    }

    public String getUrl() {
        return url;
    }

    public String getFormat() {
        return format;
    }

    public String getResolution() {
        return resolution;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }
}
