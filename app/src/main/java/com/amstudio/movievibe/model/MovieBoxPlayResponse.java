package com.amstudio.movievibe.model;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class MovieBoxPlayResponse {

    @SerializedName("ok")
    private boolean ok;

    @SerializedName("subject_id")
    private String subjectId;

    @SerializedName("season")
    private int season;

    @SerializedName("episode")
    private int episode;

    @SerializedName("title")
    private String title;

    @SerializedName("display_resolutions")
    private String displayResolutions;

    @SerializedName("streams")
    private List<StreamItem> streams;

    public boolean isOk() {
        return ok;
    }

    public String getSubjectId() {
        return subjectId;
    }

    public int getSeason() {
        return season;
    }

    public int getEpisode() {
        return episode;
    }

    public String getTitle() {
        return title;
    }

    public String getDisplayResolutions() {
        return displayResolutions;
    }

    public List<StreamItem> getStreams() {
        return streams;
    }
}
