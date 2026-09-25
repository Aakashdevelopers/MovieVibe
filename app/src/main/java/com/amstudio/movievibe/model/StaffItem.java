package com.amstudio.movievibe.model;

import com.google.gson.annotations.SerializedName;

public class StaffItem {

    @SerializedName("staffId")
    private String staffId;

    @SerializedName("staffType")
    private int staffType;

    @SerializedName("name")
    private String name;

    @SerializedName("character")
    private String character;

    @SerializedName("avatarUrl")
    private String avatarUrl;

    public String getStaffId() {
        return staffId;
    }

    public int getStaffType() {
        return staffType;
    }

    public String getName() {
        return name;
    }

    public String getCharacter() {
        return character;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }
}
