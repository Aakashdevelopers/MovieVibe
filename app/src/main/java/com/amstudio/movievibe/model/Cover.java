package com.amstudio.movievibe.model;

import com.google.gson.annotations.SerializedName;

public class Cover {
    @SerializedName("url")
    private String url;

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }
}
