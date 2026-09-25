package com.amstudio.movievibe.model;

import com.google.gson.annotations.SerializedName;

public class DubItem {

    @SerializedName("lanName")
    private String lanName;

    @SerializedName("lanCode")
    private String lanCode;

    @SerializedName("original")
    private boolean original;

    public String getLanName() {
        return lanName;
    }

    public String getLanCode() {
        return lanCode;
    }

    public boolean isOriginal() {
        return original;
    }
}
