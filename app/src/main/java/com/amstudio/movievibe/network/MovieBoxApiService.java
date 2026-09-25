package com.amstudio.movievibe.network;

import com.amstudio.movievibe.model.HomeFeedResponse;
import com.amstudio.movievibe.model.MovieBoxDetailResponse;
import com.amstudio.movievibe.model.MovieBoxPlayResponse;
import com.amstudio.movievibe.model.MovieBoxSearchResponse;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface MovieBoxApiService {

    @GET("mb/home")
    Call<HomeFeedResponse> getHomeFeed();

    @GET("mb/trending")
    Call<HomeFeedResponse> getTrendingFeed();

    @GET("mb/search")
    Call<MovieBoxSearchResponse> searchMovies(
        @Query("q") String query,
        @Query("page") int page
    );

    @GET("mb/detail")
    Call<MovieBoxDetailResponse> getMovieDetail(
        @Query("subjectId") String subjectId
    );

    @GET("mb/play")
    Call<MovieBoxPlayResponse> getPlaybackStream(
        @Query("subjectId") String subjectId,
        @Query("season") int season,
        @Query("episode") int episode
    );
}
