# Understanding Bayesian Growth in GPS Tracker

In the context of the **GtoEngine** and the "Location Pending" state, **Bayesian growth** refers to the mathematical modeling of **increasing spatial uncertainty** over time when a fresh GPS fix is unavailable.

### 1. The Core Concept
When the device loses its GPS signal (entering the "Pending" state), the system knows exactly where the device *was*, but it becomes increasingly uncertain about where it *is* now. 

Instead of showing a static, stale location, the app uses a **Bayesian growth rate** to expand the accuracy circle. This represents the "Probability Area" where the device could potentially be located, assuming a certain maximum drift speed.

### 2. Implementation in the Code
As seen in `EngineConstants.kt` and `MapComponents.kt`, the uncertainty is calculated as follows:

*   **Growth Constant**: `PENDING_UNCERTAINTY_GROWTH_RATE_MPS = 15.0f` (approx. 54 km/h).
*   **Formula**: `Effective Accuracy = Last Known Accuracy + (Growth Rate × Seconds Since Last Fix)`.

### 3. Why "Bayesian"?
The term is used because it follows **Bayesian Inference** principles:
*   **The Prior**: The last valid GPS coordinates and accuracy.
*   **The Likelihood**: As time ($t$) passes without new "evidence" (a new GPS fix), the probability distribution of the device's location "spreads out."
*   **The Posterior**: The resulting expanded circle represents the updated belief of the device's position, incorporating the high probability that the device has moved since the last fix.

### 4. Visual Impact
On the map, this prevents a "false sense of security." If a tracker goes offline, the circle starts small and slowly scales outward. If it has been offline for 60 seconds, the circle will have grown by **900 meters** (15m/s * 60s), visually communicating to the user that the tracker could be anywhere within that nearly 1km radius.

This "conservative drift" approach ensures that if the device is being moved (e.g., in a vehicle), the UI accurately reflects the expanding search area for forensic or recovery purposes.