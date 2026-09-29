# MovieVibe 🎬

[![Android](https://img.shields.io/badge/Platform-Android-green.svg?style=flat&logo=android)](https://www.android.com/)
[![Language](https://img.shields.io/badge/Language-Java-orange.svg?style=flat&logo=openjdk)](https://www.java.com/)
[![Min SDK](https://img.shields.io/badge/Min%20SDK-26%20%28Android%208.0%29-blue.svg)](https://developer.android.com)
[![Target SDK](https://img.shields.io/badge/Target%20SDK-35-brightgreen.svg)](https://developer.android.com)
[![Media3](https://img.shields.io/badge/Player-Media3%20ExoPlayer-red.svg)](https://developer.android.com/guide/topics/media/media3)

**MovieVibe** is a modern Android streaming application designed for seamless browsing, searching, and viewing of movies and TV series. Built with native Java, AndroidX Media3 ExoPlayer, and Retrofit 2, MovieVibe offers rich metadata displays, multi-language audio dubbing indicators, custom media player controls, and full-screen video streaming.

---

## 📌 Table of Contents
- [Features](#-features)
- [Screens & Architecture](#-screens--architecture)
- [Tech Stack & Libraries](#-tech-stack--libraries)
- [Project Structure](#-project-structure)
- [API Integration](#-api-integration)
- [Prerequisites & Getting Started](#-prerequisites--getting-started)
- [Building & Running](#-building--running)

---

## ✨ Features

- 🍿 **Trending & Home Feed:** Interactive auto-scrolling hero banner carousel (ViewPager2) showcasing top trending releases alongside organized content grids.
- 🏷️ **Category Filtering:** Quick category chips to browse content across genres and regions like *Hindi*, *Bollywood*, *South*, and *Anime*.
- 🔍 **Real-Time Search:** Instant search with live results and clear controls.
- 📜 **Detailed Information Page:**
  - Full synopsis, release year, and IMDb rating badge.
  - Available audio dubbing language indicators.
  - Cast & staff list with photo avatars.
  - Dynamic season and episode selection for TV series.
- ⏯️ **Native ExoPlayer Integration:** High-performance playback engine powered by AndroidX Media3:
  - Supports MP4 Progressive and DASH (`.mpd`) adaptive streams.
  - Custom HTTP header configuration for restricted media sources.
  - Integrated loading indicators and buffering error handling.
- 🌐 **WebView Fallback Player:** Secondary web-based media player with custom full-screen HTML5 video controls.

---

## 📱 Screens & Architecture

MovieVibe follows clean code principles with Android **ViewBinding** for type-safe view interaction and **Retrofit 2** for asynchronous REST networking.

1. **`MainActivity`**
   - Displays promotional banners via `ViewPager2` and `BannerAdapter`.
   - Manages content grids with `RecyclerView` and `MovieAdapter`.
   - Provides live search with `TextWatcher` and chip category filtering.
2. **`DetailActivity`**
   - Loads comprehensive metadata and cast details.
   - Handles stream resolution lookup and playback dispatching.
   - Provides episode grid navigation for multi-season series.
3. **`PlayerActivity`**
   - Implements native `ExoPlayer` (`Media3`).
   - Supports custom player control overlay (`custom_player_control.xml`).
4. **`WebViewPlayerActivity`**
   - Fallback browser-based video playback handling custom views and full-screen immersion.

---

## 🛠️ Tech Stack & Libraries

- **Language:** Java (JDK 11)
- **UI & Layouts:** Material Design 3, ConstraintLayout, ViewPager2, RecyclerView, Material Chips
- **View Binding:** Enabled (`buildFeatures.viewBinding = true`)
- **Networking:**
  - [Retrofit 2](https://square.github.io/retrofit/) - Type-safe HTTP client
  - [OkHttp 3 Logging Interceptor](https://github.org/square/okhttp) - HTTP logging & network inspection
  - [Gson](https://github.com/google/gson) - JSON parsing
- **Image Loading:**
  - [Glide](https://github.com/bumptech/glide) - Asynchronous image fetching & caching
- **Video & Streaming:**
  - [AndroidX Media3 ExoPlayer](https://developer.android.com/guide/topics/media/media3)
  - AndroidX Media3 DASH Extension
  - AndroidX Media3 UI Components

---

## 📁 Project Structure

```
MovieVibe/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/amstudio/movievibe/
│   │   │   │   ├── adapter/               # Banner, Cast, Episode & Movie Adapters
│   │   │   │   │   ├── BannerAdapter.java
│   │   │   │   │   ├── CastAdapter.java
│   │   │   │   │   ├── EpisodeAdapter.java
│   │   │   │   │   └── MovieAdapter.java
│   │   │   │   ├── model/                 # Data models & API responses
│   │   │   │   │   ├── BannerItem.java
│   │   │   │   │   ├── HomeFeedResponse.java
│   │   │   │   │   ├── MovieBoxDetailResponse.java
│   │   │   │   │   ├── MovieBoxPlayResponse.java
│   │   │   │   │   ├── MovieBoxSearchResponse.java
│   │   │   │   │   └── ...
│   │   │   │   ├── network/               # Retrofit client and API definitions
│   │   │   │   │   ├── MovieBoxApiService.java
│   │   │   │   │   └── RetrofitClient.java
│   │   │   │   ├── DetailActivity.java    # Content details & stream resolver
│   │   │   │   ├── MainActivity.java      # Home screen, categories & search
│   │   │   │   ├── PlayerActivity.java    # ExoPlayer native player
│   │   │   │   └── WebViewPlayerActivity.java # Web player fallback
│   │   │   ├── res/
│   │   │   │   ├── layout/                # Activity and item layout XMLs
│   │   │   │   ├── values/                # Strings, colors, themes
│   │   │   │   └── ...
│   │   │   └── AndroidManifest.xml
│   └── build.gradle.kts
├── gradle/
│   └── libs.versions.toml                 # Gradle Version Catalog
├── build.gradle.kts
└── settings.gradle.kts
```

---

## 🌐 API Integration

MovieVibe integrates with the MovieBox REST backend API using `MovieBoxApiService`:

| Endpoint | Method | Description |
| :--- | :--- | :--- |
| `/mb/home` | `GET` | Fetches homepage section feeds and top promotional banners |
| `/mb/trending` | `GET` | Fetches trending movies and television series |
| `/mb/search` | `GET` | Searches movies by query string and page number |
| `/mb/detail` | `GET` | Retrieves full item details, cast list, and dubbing info |
| `/mb/play` | `GET` | Resolves playback stream URLs and HTTP headers |

---

## 🚀 Prerequisites & Getting Started

### Prerequisites
- **Android Studio:** Ladybug (2024.2.1) or newer recommended
- **JDK:** Java Development Kit 11
- **Android SDK:**
  - Minimum SDK: 26 (Android 8.0 Oreo)
  - Target SDK: 35
  - Compile SDK: 36

### Installation

1. **Clone the Repository:**
   ```bash
   git clone https://github.com/your-username/MovieVibe.git
   cd MovieVibe
   ```

2. **Open in Android Studio:**
   - Launch Android Studio.
   - Select **Open an Existing Project** and navigate to the cloned `MovieVibe` directory.

3. **Gradle Sync:**
   - Allow Gradle to sync dependencies defined in `gradle/libs.versions.toml`.

---

## ⚙️ Building & Running

To build the APK via command line:

```bash
# On Linux/macOS
./gradlew assembleDebug

# On Windows
gradlew.bat assembleDebug
```

To run unit tests:
```bash
./gradlew test
```

---

## 📄 License

This project is maintained by **AM Studio**. Feel free to customize and expand upon it for your own media player application needs.
