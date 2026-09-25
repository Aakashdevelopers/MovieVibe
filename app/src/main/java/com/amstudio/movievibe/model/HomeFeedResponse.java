package com.amstudio.movievibe.model;

import com.google.gson.annotations.SerializedName;
import java.util.ArrayList;
import java.util.List;

public class HomeFeedResponse {

    @SerializedName("ok")
    private boolean ok;

    @SerializedName("data")
    private HomeData data;

    public boolean isOk() {
        return ok;
    }

    public HomeData getData() {
        return data;
    }

    public static class HomeData {
        @SerializedName("items")
        private List<HomeSectionItem> items;

        public List<HomeSectionItem> getItems() {
            return items;
        }
    }

    public static class HomeSectionItem {
        @SerializedName("type")
        private String type;

        @SerializedName("title")
        private String title;

        @SerializedName("banner")
        private BannerContainer banner;

        @SerializedName("subjects")
        private List<MovieItem> subjects;

        public String getType() {
            return type;
        }

        public String getTitle() {
            return title;
        }

        public List<BannerItem> getBanners() {
            return banner != null ? banner.getBanners() : new ArrayList<>();
        }

        public List<MovieItem> getSubjects() {
            return subjects != null ? subjects : new ArrayList<>();
        }
    }

    public static class BannerContainer {
        @SerializedName("banners")
        private List<BannerItem> banners;

        public List<BannerItem> getBanners() {
            return banners;
        }
    }
}
