# CalorieSnap

An Android app that estimates calories and macros from a photo of your food. Uses on-device AI (Gemini Nano via ML Kit) on Pixel 8+ and falls back to Firebase AI (Gemini 2.5 Flash) on other devices.

## How it works

1. Snap a photo of your meal
2. AI identifies the food and estimates calories, protein, carbs, and fat (on-device on Pixel, cloud on other devices)
3. Optionally toggle extras (butter, oil, cheese, sauce, dressing) to adjust the estimate
4. Save the entry to your daily food log

## Architecture

Single-activity Compose app with MVVM and constructor-injected dependencies.

```
data/
  FoodAnalyzer       # Interface for AI analysis + model download
  CalorieEstimator   # ML Kit on-device implementation (Pixel 8+)
  FirebaseAnalyzer   # Firebase AI cloud implementation (all devices)
  FallbackAnalyzer   # Tries on-device, falls back to cloud
  FoodRepository     # Interface for persistence
  RoomFoodRepository # Room/DAO implementation
  EstimateParser     # Pure functions for prompt building + JSON parsing
ui/
  CameraScreen       # CameraX viewfinder + capture
  ResultCard         # Analysis results + extras chips
  HistoryScreen      # Today's food log
  SetupScreen        # Model download onboarding
```

`MainViewModel` depends only on `FoodAnalyzer` and `FoodRepository` interfaces, making it fully testable with fakes (no Android runtime needed beyond Robolectric for `Bitmap`).

## Building

```
./gradlew assembleDebug
```

Requires Android SDK (API 35) and JDK 17+. If `JAVA_HOME` isn't set, you can use the JDK bundled with Android Studio:

```
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew assembleDebug
```

## Testing

```
./gradlew testDebugUnitTest
```

Tests cover `EstimateParser` (prompt construction, JSON parsing, malformed input), `MainViewModel` (download state machine, capture/analyze/save flow, error handling), and `FallbackAnalyzer` (on-device success, cloud fallback, progress forwarding).

## Requirements

- Android 12+ (API 31)
- Device with camera
- Pixel 8+: Gemini Nano model downloaded on first launch (offline)
- Other devices: internet connection for Firebase AI cloud analysis
