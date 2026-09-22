# GTR MOTORCYCLES Android test

Test-only Android wrapper for the GTR customer app with a foreground location service.

## Current test scope

- Loads the live GTR customer app.
- Requests precise and background location permission.
- Runs GPS updates while the app is in the background.
- Shows a permanent Android notification while tracking.
- Stores the last location locally on the phone.

This test build does not yet transmit location to GTR remotely. A protected server endpoint, authenticated client-to-rental link, retention policy and admin map must be added before operational use.

## Build

Run the GitHub Actions workflow and download the GTR-Motorcycles-Test-APK artifact.
