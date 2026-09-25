package com.amstudio.movievibe.model;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class SeasonsInfo {

    @SerializedName("seasons")
    private List<SeasonItem> seasons;

    public List<SeasonItem> getSeasons() {
        return seasons;
    }
}
