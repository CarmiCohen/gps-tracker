# Project Revolution: Alarm & Event Lifecycle

## 1. The "Silent Sensor" Philosophy
Following the Revolution principles, the **Sensor (Tracker)** is a passive data producer. 
- **Internal Detection**: The Sensor logic continuously checks for geofence breaches, vibration spikes, or tampering.
- **Immediate Dispatch**: Upon detecting a violation, it pushes a `ViolationEvent` to the relay server.
- **No Local Feedback**: The Sensor **does not** sound a local siren, vibrate, or show an alert UI on its own screen. It assumes it is unattended and connected to a power supply.

## 2. The "Active Monitor" Responsibility
The **Monitor (Viewer)** is the sole interface for human interaction.
- **Alert Reception**: Receives `ViolationEvent` from the relay.
- **Local Alarm Generation**: The Monitor device is responsible for:
    - Playing alarm sounds/sirens.
    - Displaying high-priority UI overlays.
    - Vibrating the handheld device.
- **Alarm Acknowledgment**: The user acknowledges the alarm on the Monitor. This state is then synced back to the relay to stop the alert flow.

## 3. Remote Siren Control (Optional)
If the hardware setup includes a physical siren attached to the Sensor, it is **never** triggered by the Sensor's internal logic. It can only be triggered by an explicit "Remote Command" sent from the Monitor UI.
