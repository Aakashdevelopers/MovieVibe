package com.amstudio.movievibe.network;

import android.content.Context;

import okhttp3.Cache;
import okhttp3.ConnectionPool;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class RetrofitClient {

    private static final String BASE_URL = "https://movie-api.fastapicloud.dev/";
    private static Retrofit retrofit = null;

    public static MovieBoxApiService getApiService(Context context) {
        if (retrofit == null) {
            HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
            logging.setLevel(HttpLoggingInterceptor.Level.BASIC);

            OkHttpClient.Builder builder = new OkHttpClient.Builder()
                    .connectTimeout(15, TimeUnit.SECONDS)
                    .readTimeout(15, TimeUnit.SECONDS)
                    .connectionPool(new ConnectionPool(10, 5, TimeUnit.MINUTES))
                    .addInterceptor(logging);

            if (context != null) {
                try {
                    File cacheDir = new File(context.getApplicationContext().getCacheDir(), "http_cache");
                    Cache cache = new Cache(cacheDir, 15 * 1024 * 1024); // 15MB HTTP Cache
                    builder.cache(cache);

                    builder.addNetworkInterceptor(new Interceptor() {
                        @Override
                        public Response intercept(Chain chain) throws IOException {
                            Response response = chain.proceed(chain.request());
                            return response.newBuilder()
                                    .header("Cache-Control", "public, max-age=180") // Cache GET responses for 3 mins
                                    .build();
                        }
                    });
                } catch (Exception ignored) {}
            }

            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .client(builder.build())
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofit.create(MovieBoxApiService.class);
    }

    public static MovieBoxApiService getApiService() {
        return getApiService(null);
    }
}
