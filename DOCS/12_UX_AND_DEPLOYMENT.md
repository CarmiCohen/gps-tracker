# Project Revolution: UX & Deployment Strategy

## 1. Elimination of the Landing Page
In the legacy monolithic app, the user had to select a role (Tracker vs. Viewer) upon startup. In the Revolution architecture, this is completely eliminated.
- **Distinct APKs**: The Sensor and Monitor are built as two separate Android applications.
- **Implicit Role**: The "Mode" is defined by which application is installed. There is no shared UI for role selection.
- **Startup Speed**: By removing the landing page, the Monitor can boot directly into the Map view, and the Sensor can boot directly into the Headless Service, cutting down interaction time.

## 2. Deployment Profiles
- **Sensor Deployment**: Targeted at devices connected to a vehicle or permanent power source. The installation package is minimized for background stability.
- **Monitor Deployment**: Targeted at the user's primary mobile device. The installation package includes rich UI assets and map libraries.

## 3. Configuration & Provisioning
- **First-Run Sync**: Upon the first launch of the Monitor, the user provides the "Target Sensor ID."
- **Deep Linking**: Remote setup can be achieved via a QR code or configuration link, allowing the Monitor to instantly pair with a Sensor without manual ID entry.
