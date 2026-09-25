package com.amstudio.movievibe.model;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class RawDetail {

    @SerializedName("duration")
    private String duration;

    @SerializedName("genre")
    private String genre;

    @SerializedName("countryName")
    private String countryName;

    @SerializedName("language")
    private String language;

    @SerializedName("imdbRatingValue")
    private String imdbRating;

    @SerializedName("staffList")
    private List<StaffItem> staffList;

    @SerializedName("dubs")
    private List<DubItem> dubs;

    @SerializedName("resourceDetectors")
    private List<ResourceDetector> resourceDetectors;

    public String getDuration() {
        return duration;
    }

    public String getGenre() {
        return genre;
    }

    public String getCountryName() {
        return countryName;
    }

    public String getLanguage() {
        return language;
    }

    public String getImdbRating() {
        return imdbRating;
    }

    public List<StaffItem> getStaffList() {
        return staffList;
    }

    public List<DubItem> getDubs() {
        return dubs;
    }

    public List<ResourceDetector> getResourceDetectors() {
        return resourceDetectors;
    }
}
