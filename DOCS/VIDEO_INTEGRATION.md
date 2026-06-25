# Video Streaming and CameraX Integration Documentation (v8.9.37)

This document describes the historical integration and subsequent removal of video streaming capabilities to maintain a high-assurance, tracking-focused architecture.

## 1. Core Components (Legacy)

### WebRtcManager.kt
The heart of the streaming logic. It managed:
- **WebRTC Factory**: Initialization of `PeerConnectionFactory`.
- **CameraX Integration**: Used `ProcessCameraProvider` to bind `Preview` and `ImageCapture`.
- **Audio**: Managed `AudioSource` and `AudioTrack`.

### VideoComponents.kt
Contained Compose UI components for video, including `VideoContainer` and manual controls.

## 2. Signaling and Communication (Legacy)
Socket-based signaling events (`webrtc_offer`, `video_cmd`) were used to negotiate P2P sessions via the relay server.

## 3. Removal Summary (v4.351 onwards)
Video functionality was removed to prioritize battery efficiency, reduce attack surface, and ensure forensic continuity of the tracking engine.

### Current State (v8.9.37)
- **Permissions**: `CAMERA` and `RECORD_AUDIO` are not requested.
- **Service Types**: `TrackerService` requests `microphone` only for the Acoustic Sentinel (Issue #247), not for streaming.
- **UI**: All video icons and containers remain purged.
- **Dependencies**: CameraX and WebRTC libraries are excluded from the build.

---
*Note: Video streaming is not part of the v8.9.37 "Hardened Engine" specification. Legacy versioning fields have been purged from the system.*
