package com.amstudio.movievibe.model;

import com.google.gson.annotations.SerializedName;

public class BannerItem {

    @SerializedName("content")
    private String content;

    @SerializedName("subjectId")
    private String subjectId;

    @SerializedName("image")
    private Cover image;

    @SerializedName("subject")
    private MovieItem subject;

    public String getContent() {
        return content;
    }

    public String getSubjectId() {
        return subjectId;
    }

    public String getImageUrl() {
        return image != null ? image.getUrl() : "";
    }

    public MovieItem getSubject() {
        return subject;
    }
}
