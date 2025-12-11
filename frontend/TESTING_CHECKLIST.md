# Testing Checklist for Medication Search Feature

## Pre-Test Setup
- [ ] Ensure backend is running at https://backend-ts-theta.vercel.app
- [ ] User is logged in (auth token is available)
- [ ] Camera permissions are granted (for camera scanning)

## Test Scenarios

### 1. Manual NDC Entry - Success Path
- [ ] Open Scan Medication screen
- [ ] Tap "Enter Manually" button
- [ ] Enter valid NDC code: `0378-6208-93`
- [ ] Tap Search button
- [ ] **Expected**: Loading indicator appears
- [ ] **Expected**: Search results screen displays with medications
- [ ] **Expected**: Each medication shows:
  - Medication image or icon
  - Medication title
  - Manufacturer name
  - NDC code
- [ ] Tap on a medication card
- [ ] **Expected**: Loading indicator on that card
- [ ] **Expected**: Success dialog appears
- [ ] **Expected**: "Medication Added!" message
- [ ] Tap "Done" on success dialog
- [ ] **Expected**: Returns to previous screen

### 2. Manual NDC Entry - No Results
- [ ] Open Scan Medication screen
- [ ] Tap "Enter Manually"
- [ ] Enter invalid NDC: `99999-9999-99`
- [ ] Tap Search
- [ ] **Expected**: Error dialog appears
- [ ] **Expected**: Message: "No medications found for NDC: 99999-9999-99"
- [ ] Tap "OK"
- [ ] **Expected**: Returns to manual entry screen

### 3. Camera Scan - Success Path
- [ ] Open Scan Medication screen
- [ ] Point camera at medication barcode
- [ ] Tap capture button
- [ ] **Expected**: Loading indicator appears
- [ ] **Expected**: NDC extracted from barcode
- [ ] **Expected**: Search results screen displays
- [ ] Tap a medication to add
- [ ] **Expected**: Success dialog and medication added

### 4. Camera Scan - Invalid QR Code
- [ ] Open Scan Medication screen
- [ ] Scan non-medication QR code
- [ ] **Expected**: Error dialog: "Could not read barcode from image"
- [ ] Tap "OK"
- [ ] **Expected**: Returns to camera screen

### 5. Network Error Handling
- [ ] Turn off internet/WiFi
- [ ] Try manual entry search
- [ ] **Expected**: Error dialog with network error message
- [ ] Turn on internet
- [ ] Try again
- [ ] **Expected**: Works correctly

### 6. Unauthenticated User
- [ ] Log out of the app
- [ ] Try to add a medication
- [ ] **Expected**: Should fail gracefully (check logs for "No auth token available")
- [ ] **Note**: Ideally should redirect to login screen

### 7. UI/UX Checks
- [ ] Search results are scrollable
- [ ] Images load smoothly
- [ ] Back button works from search results
- [ ] Multiple medications display correctly
- [ ] Loading states are clear
- [ ] Success feedback is obvious
- [ ] Error messages are helpful

### 8. Multiple Results
- [ ] Search for common NDC with multiple results
- [ ] **Expected**: All results display in scrollable list
- [ ] **Expected**: Can scroll through all results
- [ ] **Expected**: Can add any medication from list

### 9. Empty State
- [ ] Search for NDC with no results
- [ ] **Expected**: Empty state shows with:
  - Icon
  - "No Medications Found" message
  - "Try a different search term" suggestion

## Backend Verification

After adding medications:
- [ ] Check medication appears in user's medication list
- [ ] Verify correct data saved:
  - Medication name
  - Set ID
  - NDC code
  - Default dosage: "As prescribed"
  - Default frequency: "Every day"
  - Start date: today's date

## Common NDC Codes for Testing

Valid NDC codes you can test with:
- `0378-6208-93` - Common medication
- `0002-1433-01` - Another common medication
- `50090-0001-0` - Generic medication

## Bug Tracking

| Issue | Severity | Status | Notes |
|-------|----------|--------|-------|
|       |          |        |       |

## Platform-Specific Tests

### Android Only
- [ ] Coil image loading works
- [ ] Camera permission dialog shows
- [ ] Back button behavior correct

### iOS Only
- [ ] Image loading works via Ktor
- [ ] Camera permission dialog shows
- [ ] Navigation gestures work

## Performance Tests
- [ ] Images load within 2-3 seconds
- [ ] Search completes within 5 seconds
- [ ] No UI freezing during API calls
- [ ] Smooth scrolling in results list

