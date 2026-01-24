# Offline Mode Test Plan - Pet Safety Android App

## Overview
This document outlines the testing strategy for offline mode on Android, including Room caching, action queuing, network monitoring, and background sync.

## Test Environment
- Android Studio Electric Eel or later
- Android 13+ device/emulator
- Backend API accessible
- Network toggling (Airplane mode, WiFi, Cellular)

## Unit Tests
1. **NetworkMonitor**
   - Connectivity transitions (online/offline)
   - Override mode for offline testing

2. **OfflineDataManager**
   - Save/fetch pets and alerts
   - Queue actions and retry behavior
   - Delete queued actions after success

3. **SyncService**
   - Queue action while offline
   - Process queue when online
   - Fetch fresh data on sync

## Integration Tests
1. **Offline Queue E2E**
   - Queue missing pet alert while offline
   - Switch online and verify sync + removal from queue

2. **Cache Load**
   - Fetch pets online then load cached data offline

## UI Tests
1. **Offline Indicator**
   - Show offline state
   - Pending actions count
   - Syncing state

## Success Criteria
- No data loss
- Actions sync within 10 seconds of reconnection
- UI reflects offline vs online state
