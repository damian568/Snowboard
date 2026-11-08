# Snowboard

Android app for snowboarders — nearby slopes, weather, videos and more. The Android project lives in the `Snowboard/` folder.

## Setup

### OpenWeather API key

Weather data comes from [OpenWeather](https://openweathermap.org/api). The API key is **not** stored in the repo — you need to add your own locally.

1. Create a free account at [openweathermap.org](https://home.openweathermap.org/users/sign_up) and copy a key from the [API keys](https://home.openweathermap.org/api_keys) page. New keys can take a little while to activate.
2. Open `Snowboard/local.properties` (Android Studio creates it on first sync; it is gitignored) and add:

   ```properties
   WEATHER_API_KEY=your_key_here
   ```

3. Sync Gradle and rebuild. The key is exposed to the app as `BuildConfig.WEATHER_API_KEY`.

If the key is missing, the build still succeeds but logs a warning and weather won't load.

**Never commit the key.** Don't put it in source files, `gradle.properties`, or any other tracked file.

#### CI

GitHub Actions reads the key from the `WEATHER_API_KEY` repository secret (Settings → Secrets and variables → Actions). It's optional — CI builds without it.

### Git hooks

Commit messages must follow `SNOW-<number> <Description>`. The hook in `.githooks/` is installed automatically on the first Gradle sync.
