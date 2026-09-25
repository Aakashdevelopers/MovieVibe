package com.amstudio.movievibe.model;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class ResourceDetector {

    @SerializedName("type")
    private int type;

    @SerializedName("resolutionList")
    private List<ResolutionItem> resolutionList;

    public int getType() {
        return type;
    }

    public List<ResolutionItem> getResolutionList() {
        return resolutionList;
    }
}
