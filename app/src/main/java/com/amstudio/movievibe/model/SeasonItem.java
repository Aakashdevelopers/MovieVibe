package com.amstudio.movievibe.model;

import com.google.gson.annotations.SerializedName;

public class SeasonItem {

    @SerializedName("se")
    private int seasonNumber;

    @SerializedName("maxEp")
    private int maxEpisodes;

    public int getSeasonNumber() {
        return seasonNumber;
    }

    public int getMaxEpisodes() {
        return maxEpisodes;
    }
}
