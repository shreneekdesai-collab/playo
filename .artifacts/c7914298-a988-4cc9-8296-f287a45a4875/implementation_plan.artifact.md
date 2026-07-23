# Cleanup: Remove CategoryActivity

Now that `CategoryFragment` is implemented and hosted within `MainActivity`, `CategoryActivity` is redundant and can be safely removed. This moves the project closer to a Single-Activity architecture.

## Proposed Changes

### [MainActivity]
#### [MODIFY] [MainActivity.java](file:///home/shreneek/AndroidStudioProjects/CUTCUT/app/src/main/java/com/example/playo/MainActivity.java)
- Add logic in `onCreate` to check for an intent extra (e.g., "SHOW_FRAGMENT") to determine which fragment to display on startup. This allows other activities (like `OnclickCategory`) to navigate back to specific tabs.

### [OnclickCategory]
#### [MODIFY] [OnclickCategory.java](file:///home/shreneek/AndroidStudioProjects/CUTCUT/app/src/main/java/com/example/playo/OnclickCategory.java)
- Update the bottom navigation listener to navigate back to `MainActivity` with the "SHOW_CATEGORY" extra instead of starting `CategoryActivity`.

### [Cleanup]
#### [DELETE] [CategoryActivity.java](file:///home/shreneek/AndroidStudioProjects/CUTCUT/app/src/main/java/com/example/playo/CategoryActivity.java)
- Remove the redundant activity file.
#### [MODIFY] [AndroidManifest.xml](file:///home/shreneek/AndroidStudioProjects/CUTCUT/app/src/main/AndroidManifest.xml)
- Remove the `<activity>` entry for `CategoryActivity`.

## Verification Plan
1. Build the project.
2. Navigate to a category detail page (`OnclickCategory`).
3. Tap "Category" in the bottom navigation bar.
4. Verify it takes you back to the "Categories" tab in `MainActivity` smoothly.
