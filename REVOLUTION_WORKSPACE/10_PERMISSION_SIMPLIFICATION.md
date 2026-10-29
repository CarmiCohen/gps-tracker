# Project Revolution: Permission Hardening & Simplification

## 1. Monitor (Viewer) - "The Trustworthy Monitor"
To maximize battery life and user privacy, the Monitor app is stripped of all non-essential permissions.

### ✅ Retained Permissions
- `INTERNET`: For relay communication.
- `POST_NOTIFICATIONS`: To alert the user of violations.
- `VIBRATE`: For alarm feedback.
- `ACCESS_FINE_LOCATION`: (Foreground Only) Only to show the user's position relative to the Sensor on the map.

### ❌ Removed Permissions (Code Shortened)
- `ACCESS_BACKGROUND_LOCATION`: Not needed; the Monitor only tracks in the foreground.
- `RECORD_AUDIO`: No microphone access required.
- `RECEIVE_BOOT_COMPLETED`: The Monitor does not need to start automatically.
- `WAKE_LOCK`: No background processing allowed.

---

## 2. Sensor (Tracker) - "The Dedicated Sentinel"
The Sensor retains powerful permissions but removes all UI-related overhead.

### ✅ Retained Permissions
- `ACCESS_FINE_LOCATION` & `ACCESS_BACKGROUND_LOCATION`.
- `FOREGROUND_SERVICE`: For persistent sensing.
- `RECEIVE_BOOT_COMPLETED`: To ensure automatic start after power loss.
- `WAKE_LOCK`: To maintain sensing loops while the screen is off.

### ❌ Removed Permissions (Code Shortened)
- `QUERY_ALL_PACKAGES`: Not needed for a headless service.
- UI-specific hardware acceleration permissions (if any).
- Any permissions related to analytics or social sharing.
