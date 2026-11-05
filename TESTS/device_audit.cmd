@echo off
rem Forensic audit of one running app process (Completion Sequence step 2, Chapter 0.1).
rem Counts only that process, so other apps on the phone cannot cause false alarms.
rem Usage:   device_audit.cmd <serial> <package>
rem Example: device_audit.cmd R58X40GV2AR com.jd.app.sensor
rem Exit code: 0 clean, 1 anomalies found, 2 app not running.
if "%~2"=="" (
    echo Usage: device_audit.cmd ^<serial^> ^<package^>
    exit /b 2
)
adb -s %1 shell "P=$(pidof %2); if [ -z \"$P\" ]; then echo NOT_RUNNING; exit 2; fi; L=$(logcat -d --pid=$P); D=$(echo \"$L\" | grep -oE 'Davey! duration=[0-9]+' | cut -d= -f2 | awk '$1>500' | wc -l); S=$(echo \"$L\" | grep -c StackLog); F=$(echo \"$L\" | grep -cE 'FATAL EXCEPTION|SQLiteDatabaseLocked|NaNG'); echo pid=$P davey_over_500ms=$D stacklog=$S fatal=$F; [ $((D+S+F)) -eq 0 ] || exit 1"
exit /b %errorlevel%
