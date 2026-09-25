package com.amstudio.movievibe.model;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class MovieBoxSearchResponse {

    @SerializedName("ok")
    private boolean ok;

    @SerializedName("query")
    private String query;

    @SerializedName("count")
    private int count;

    @SerializedName("items")
    private List<MovieItem> items;

    public boolean isOk() {
        return ok;
    }

    public String getQuery() {
        return query;
    }

    public int getCount() {
        return count;
    }

    public List<MovieItem> getItems() {
        return items;
    }
}
