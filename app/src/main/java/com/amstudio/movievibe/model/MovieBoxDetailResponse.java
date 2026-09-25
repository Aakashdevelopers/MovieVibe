package com.amstudio.movievibe.model;

import com.google.gson.annotations.SerializedName;

public class MovieBoxDetailResponse {

    @SerializedName("ok")
    private boolean ok;

    @SerializedName("subject_id")
    private String subjectId;

    @SerializedName("title")
    private String title;

    @SerializedName("type")
    private String type;

    @SerializedName("description")
    private String description;

    @SerializedName("rating")
    private String rating;

    @SerializedName("year")
    private String year;

    @SerializedName("poster")
    private Cover poster;

    @SerializedName("seasons")
    private SeasonsInfo seasonsInfo;

    @SerializedName("raw")
    private RawDetail raw;

    public boolean isOk() {
        return ok;
    }

    public String getSubjectId() {
        return subjectId;
    }

    public String getTitle() {
        return title;
    }

    public String getType() {
        return type;
    }

    public String getDescription() {
        return description;
    }

    public String getRating() {
        return rating;
    }

    public String getYear() {
        return year;
    }

    public String getPosterUrl() {
        return poster != null ? poster.getUrl() : "";
    }

    public SeasonsInfo getSeasonsInfo() {
        return seasonsInfo;
    }

    public RawDetail getRaw() {
        return raw;
    }
}
