# MovieShelf

An Android app for searching movies on [The Movie Database (TMDB)](https://www.themoviedb.org/), browsing the results, and viewing movie details. It's built offline-first: every search result is stored in a local Room database, and that database is what the UI displays, so earlier searches still work without a connection.

## Features

- **Search**: tap the search bar on the start screen and it expands into a full-screen search page, in the style of Google Maps. That page shows your recent searches, stored on the device. A request is only sent when you submit; typing alone never sends one.
- **Results**: a poster grid showing each movie's title, release date and TMDB rating. There are separate loading, empty, error and offline states, plus pull-to-refresh.
- **Detail**: poster, backdrop, title, release date, runtime, rating, tagline, genres and overview. The poster animates from the results grid into the detail page (a shared-element transition).
- **Offline**:
  - Earlier searches open instantly from the cache.
  - If a refresh fails, the cached results stay on screen with a banner that offers Retry.
  - A search that was never cached shows an error, not an empty list.
  - The detail page shows whatever is stored locally.
- Light and dark themes come from a palette generated with Material Theme Builder. Space Grotesk is used for headings and Inter for body text.

## Build and run

Requirements: a recent Android Studio, Android SDK 37, and a device or emulator running API 24 or newer. The Gradle daemon runs on JDK 25 (`gradle/gradle-daemon-jvm.properties`), which Gradle downloads automatically if it is not installed.

1. Get a TMDB **API key (v3 auth)**. A free account is enough: <https://www.themoviedb.org/settings/api>.
2. Add the key to `local.properties` in the project root. This file is git-ignored and must never be committed:
   ```properties
   TMDB_API_KEY=your_v3_api_key
   ```
   On CI you can supply a `TMDB_API_KEY` environment variable instead.
3. Build and install:
   ```bash
   ./gradlew :app:installDebug
   ```
   You can also run the `app` configuration from Android Studio.

If the key is missing or invalid, the app still builds, and searches show an "API key rejected" message.

> The key is compiled into `BuildConfig`. Keeping it out of Git prevents accidental publication, but any key shipped inside an APK can be extracted. A production app would route requests through its own backend.

### Tests

```bash
./gradlew :app:testDebugUnitTest
```

All tests are plain JVM unit tests. They don't need a network connection or an API key.

## Architecture

There is a single Gradle module. Packages are split into Clean Architecture layers, and dependencies point inward: `presentation -> domain <- data`.

```
com.canolabs.movieshelf
├── app/                 Application (Koin and Coil setup), Navigation 3 host and destinations
├── di/                  Koin modules: network, data, domain, app (ViewModels)
├── domain/              Plain Kotlin, no Android, Room, Ktor or Koin
│   ├── model/           SearchQuery, Movie, MovieDetails, CachedSearch, RecentSearch, RefreshResult
│   ├── repository/      SearchRepository, MovieRepository (contracts)
│   └── usecase/         One interface and implementation per use case, each with a single invoke()
├── data/
│   ├── remote/          Ktor service, DTOs, MovieRemoteDataSource (maps errors to DataError)
│   ├── local/           Room entities, DAOs, database, MovieLocalDataSource (transactional writes)
│   ├── mapper/          DTO -> Entity -> Domain
│   └── repository/      Repository implementations that coordinate the local and remote sources
├── presentation/        Compose screens and ViewModels: search, searchentry, results, detail, shared components
└── ui/theme/            Generated Material Theme Builder theme
```

| Concern | Choice |
|---|---|
| UI | Jetpack Compose, Material 3 |
| Navigation | Navigation 3 (`NavDisplay`, serializable `NavKey` back stack, ViewModel scoping per entry) |
| Networking | Ktor Client (OkHttp engine) with kotlinx.serialization |
| Persistence | Room (KSP) |
| Dependency injection | Koin |
| Images | Coil 3, using its Ktor network fetcher |
| Async | Coroutines and Flow (`StateFlow` with `stateIn(WhileSubscribed(5_000))`) |

### How data moves

```
Submit -> Ktor (one-shot suspend call) -> DTO -> Entity -> Room transaction
                                                              │
Compose <- ViewModel StateFlow <- use case <- repository <- DAO Flow <- (Room is the single source of truth)
```

- A successful response is written to Room in **one transaction** before `refresh()` returns. The UI only ever displays data read back from Room, never the raw network response.
- The schema:
  - `movies`: one row per TMDB movie.
  - `search_queries`: one row per normalized query. It holds the display text, `last_searched_at` (which drives Recent searches) and `last_refreshed_at`.
  - `search_results`: a junction table with a composite key `(query_key, movie_id)` and a `position` column, so API order is preserved.
- Deleting a query also deletes its result rows (`CASCADE`). A movie can't be deleted while any query still points to it (`RESTRICT`). After a result set is replaced, movies that no query references any more are removed.
- `last_refreshed_at == null` means "searched, but never fetched successfully". This is how the app tells a cache miss apart from a valid empty result. An empty success replaces the query's old results; a failure changes nothing.
- A query's metadata and its ordered results are read with a single `LEFT JOIN`. The screen therefore never sees a half-updated combination of the two.
- Search refreshes only update a movie's summary columns. Runtime, genres and tagline fetched from the detail endpoint are kept.

### State and stale requests

- **Results** uses a small, screen-specific MVI contract (`ResultsContract`: `UiState`, `UiIntent`, `UiAction`, plus a pure reducer). That screen combines two independent asynchronous sources, the cached results and the status of the network refresh, and also sends navigation as one-off actions. A reducer makes combinations such as "refresh failed but cached content exists" explicit and testable. The other screens are simpler and use plain MVVM.
- Each submitted query gets its own Results destination and ViewModel, so a screen can't show another query's results. Leaving a destination cancels its request. Within one screen, a new refresh cancels the previous one, and a generation counter discards any result that was already finishing. A late response for an older query only updates that query's own cache.
- A Results destination exists only because a query was submitted. That submission's request runs once. A flag in `SavedStateHandle` ensures that recomposition, configuration changes and process recreation don't send it again.
- The text you type in the search field is kept with `rememberSaveable`. The back stack is made of serializable keys, so it survives process recreation.

### Motion

- Everything is wrapped in a single `SharedTransitionLayout`, and its scope is passed to `NavDisplay`.
- The search bar shares its bounds with the full-screen search field, so the bar appears to expand into the search page.
- Posters use `sharedElement` with a key derived from the movie ID (`poster-<id>`). Results and Detail load the same image URL, so Coil serves the flying poster from its memory cache.
- The Detail destination receives the poster URL as a presentation hint next to the movie ID. That lets it draw the shared poster on the first frame, before Room's first emission. The data itself always comes from Room.
- Screen changes use a fade-through with a slight spring scale. That keeps the shared elements as the main source of movement.

## Tests

51 unit tests:

- `SearchRepositoryImplTest`: response persisted before success is reported; cache kept after a failed refresh; error (not an empty list) when nothing is cached; an empty success replaces old results; queries isolated from each other; normalized cache keys; API order kept; duplicate IDs removed; movies shared by several queries keep their data; recent searches recorded on failure, de-duplicated and ordered by recency; a late response for an older query doesn't touch other queries or the recent-search order.
- `MovieRepositoryImplTest`: details persisted and exposed through the cache; cache kept after a failure; a search refresh doesn't erase fetched details.
- `MovieRemoteDataSourceImplTest` (Ktor `MockEngine`): request parameters; mapping of 401, 404, 5xx, malformed JSON and I/O errors; an empty result list counts as a success.
- `ResultsViewModelTest`: initial state; query submitted exactly once, including after process recreation; loading, content, empty and error states; cached content with a refresh-failure notice; refreshing over cached content; retry; no results leaking in from another query; a stale request can't overwrite the latest result; navigation actions.
- `MovieDetailViewModelTest` and `SearchEntryViewModelTest`: cached rendering without a network call, a single detail fetch, offline handling of extra details, a movie that is neither cached nor fetchable, retry, recent searches, input validation, and no requests while typing.

The repository tests use an in-memory fake of the local data source that follows the same contract as Room.

## Known limitations

- Only the first page of TMDB results (up to 20 movies) is fetched and cached. There is no pagination.
- Repository tests run against a fake local data source. The SQL in the DAOs (the result join, the partial updates, removal of unreferenced movies) isn't covered by instrumented Room tests.
- Material 3 Expressive components and motion (`LoadingIndicator`, `MaterialExpressiveTheme`, `MotionScheme.expressive()`) are internal or missing in the latest stable `material3` release (1.4.0). This project uses stable dependencies only, so the app uses stable Material 3 components plus its own spring-based motion.
- Fonts are downloaded at runtime through Google Play services. On devices without Play services, or before the first download finishes, text falls back to the system font.
- Images you haven't viewed before can't be shown offline. Posters come from Coil's disk cache, which only contains images that have already been displayed.
- Library versions are the newest stable releases that the project's Kotlin 2.2.10 compiler can read: Ktor 3.3.3, kotlinx.serialization 1.9.0, Koin 4.1.1 and Coil 3.3.0. Newer releases require Kotlin 2.3 or later.

---

This product uses the TMDB API but is not endorsed or certified by TMDB.
