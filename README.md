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

### Facebook Login

Facebook sign-in needs a Facebook app (developers.facebook.com) with the Facebook Login use case, and the Facebook provider enabled in Firebase Authentication (App ID + App secret; the secret goes only in the Firebase console). The Android platform in the Facebook app needs package `com.example.snowboard`, class `com.example.snowboard.MainActivity` and your key hashes.

Add the app ID and client token (App settings → Basic / Advanced) to `Snowboard/local.properties`:

```properties
FACEBOOK_APP_ID=your_app_id
FACEBOOK_CLIENT_TOKEN=your_client_token
```

`app/build.gradle` turns them into the string resources the Facebook SDK reads and sets `BuildConfig.FACEBOOK_ENABLED`. **If either value is missing the Facebook buttons on the Login and Register screens stay hidden**, so builds without them (e.g. CI) still work. Never put the App secret in the app or the repo.

CI reads `FACEBOOK_APP_ID` and `FACEBOOK_CLIENT_TOKEN` from repository secrets — optional.

### Git hooks

Commit messages must follow `SNOW-<number> <Description>`. The hook in `.githooks/` is installed automatically on the first Gradle sync.
