# Simplification Ideas 2.0 (Aug.31.05)

*   **Acoustic Floor Calibration Audit (R810-M) - COMPLETE**: Verified adaptive floor recovery logic via `AcousticCalibrationTest`. Confirmed that the time-based contraction factor ensures forensic recovery even during duty-cycle off-cycles.
*   **Forensic Metadata Sanitization (R779) - COMPLETE**: Implemented `ForensicSanitizer` to scrub absolute internal paths and normalize hardware identifiers at the logging edge.
*   **History Sampling (R650) - COMPLETE**: Hardened the ribbon rendering pipeline by integrating `sample()` in `MainViewModel.kt`.
*   **MainViewModel Boilerplate**: Evaluate consolidating the 6 history scale flows (4M, 1H, etc.) into a single map-based StateFlow if the UI can be refactored to consume a keyed subscription. This would significantly reduce repetitive boilerplate in the ViewModel and improve maintainability of the analytical pipeline.
*   **Acoustic Contraction Optimization**: Evaluate replacing `Math.pow` in the acoustic floor contraction logic with a linear approximation or a pre-calculated decay table to further reduce CPU load in the sensor hot-path.
