package com.amstudio.movievibe.model;

import com.google.gson.annotations.SerializedName;

public class MovieItem {

    @SerializedName(value = "subject_id", alternate = {"subjectId"})
    private String subjectId;

    @SerializedName(value = "title", alternate = {"name"})
    private String title;

    @SerializedName("type")
    private String type; // "movie" or "series"

    @SerializedName("year")
    private String year;

    @SerializedName("rating")
    private String rating;

    @SerializedName("poster_url")
    private String posterUrlStr;

    @SerializedName("poster")
    private Cover posterCover;

    @SerializedName("cover")
    private Cover cover;

    @SerializedName("genre")
    private String genre;

    @SerializedName("description")
    private String description;

    public String getSubjectId() {
        return subjectId;
    }

    public void setSubjectId(String subjectId) {
        this.subjectId = subjectId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getYear() {
        return year;
    }

    public void setYear(String year) {
        this.year = year;
    }

    public String getRating() {
        return rating;
    }

    public void setRating(String rating) {
        this.rating = rating;
    }

    public String getPosterUrl() {
        if (posterUrlStr != null && !posterUrlStr.isEmpty()) {
            return posterUrlStr;
        }
        if (posterCover != null && posterCover.getUrl() != null && !posterCover.getUrl().isEmpty()) {
            return posterCover.getUrl();
        }
        if (cover != null && cover.getUrl() != null && !cover.getUrl().isEmpty()) {
            return cover.getUrl();
        }
        return "";
    }

    public String getGenre() {
        return genre;
    }

    public void setGenre(String genre) {
        this.genre = genre;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
