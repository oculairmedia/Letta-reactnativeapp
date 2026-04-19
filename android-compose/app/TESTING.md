# App snapshot testing

Run the RunBlock snapshot suite with Roborazzi:

```bash
./gradlew :app:verifyRoborazziDebug --tests com.letta.mobile.ui.screens.chat.RunBlockScreenshotTest --max-workers=1
```

Regenerate baselines after intentional visual changes:

```bash
./gradlew :app:recordRoborazziDebug --tests com.letta.mobile.ui.screens.chat.RunBlockScreenshotTest --max-workers=1
```

This bead pivoted away from Paparazzi because Paparazzi is currently not functional in this repo environment; see `letta-mobile-zwsk` for the tooling follow-up (tracks the investigate-or-commit-to-Roborazzi decision for `:designsystem` Paparazzi tests and the broader tooling question).
