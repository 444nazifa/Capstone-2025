# CareCapsule - A Perscription Management App (Capstone-2025)
Our project, named **CareCapsule**, is a mobile app designed for managing medications by keeping track of and explaining a patient's prescriptions safely.  

The app lets users scan their prescription labels or barcodes and change drug codes, like UPC, EAN, or NDC, into standard drug identifiers known as **RxNorm**. It then uses protected data from secure sources such as **DailyMed**, **MedlinePlus**, and the **FDA**.  

The system warns users about possible drug interactions and repeated doses while offering easy-to-understand messages on dosage instructions, proper usage, and potential side effects. Additionally, it logs medication usage and includes reminders to help users take their medication on time.  

This app is built to work across platforms using **Kotlin Multiplatform** (for both iOS and Android), with a **Python backend** and **SQL database** for secure data storage.


This is a Kotlin Multiplatform project targeting Android, iOS.

* [/composeApp](./composeApp/src) is for code that will be shared across your Compose Multiplatform applications.
  It contains several subfolders:
  - [commonMain](./composeApp/src/commonMain/kotlin) is for code that’s common for all targets.
  - Other folders are for Kotlin code that will be compiled for only the platform indicated in the folder name.
    For example, if you want to use Apple’s CoreCrypto for the iOS part of your Kotlin app,
    the [iosMain](./composeApp/src/iosMain/kotlin) folder would be the right place for such calls.
    Similarly, if you want to edit the Desktop (JVM) specific part, the [jvmMain](./composeApp/src/jvmMain/kotlin)
    folder is the appropriate location.

* [/iosApp](./iosApp/iosApp) contains iOS applications. Even if you’re sharing your UI with Compose Multiplatform,
  you need this entry point for your iOS app. This is also where you should add SwiftUI code for your project.

### Build and Run Android Application

To build and run the development version of the Android app, use the run configuration from the run widget
in your IDE’s toolbar or build it directly from the terminal:
- on macOS/Linux
  ```shell
  ./gradlew :composeApp:assembleDebug
  ```
- on Windows
  ```shell
  .\gradlew.bat :composeApp:assembleDebug
  ```

### Build and Run iOS Application

To build and run the development version of the iOS app, use the run configuration from the run widget
in your IDE’s toolbar or open the [/iosApp](./iosApp) directory in Xcode and run it from there.

---

Learn more about [Kotlin Multiplatform](https://www.jetbrains.com/help/kotlin-multiplatform-dev/get-started.html)…
