package com.amstudio.movievibe.model;

import com.google.gson.annotations.SerializedName;

public class ResolutionItem {

    @SerializedName("resolution")
    private int resolution;

    @SerializedName("resourceLink")
    private String resourceLink;

    @SerializedName("title")
    private String title;

    @SerializedName("size")
    private String size;

    public int getResolution() {
        return resolution;
    }

    public String getResourceLink() {
        return resourceLink;
    }

    public String getTitle() {
        return title;
    }

    public String getSize() {
        return size;
    }
}
