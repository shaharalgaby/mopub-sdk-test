# MoPub Android SDK

Thanks for taking a look at MoPub! We take pride in having an easy-to-use, flexible monetization solution that works across multiple platforms.

Sign up for an account at [http://app.mopub.com/](http://app.mopub.com/).

## Need Help?

You can find integration documentation on our [wiki](https://github.com/mopub/mopub-android-sdk/wiki/Getting-Started) and additional help documentation on our [developer help site](https://developers.mopub.com/publishers/android/).

To file an issue with our team visit the [MoPub Forum](https://twittercommunity.com/c/advertiser-api/mopub) or email [support@mopub.com](mailto:support@mopub.com).

## New Pull Requests?

Thank you for submitting pull requests to the MoPub Android GitHub repository. Our team regularly monitors and investigates all submissions for inclusion in our official SDK releases. Please note that MoPub does not directly merge these pull requests at this time. Please reach out to your account team or [support@mopub.com](mailto:support@mopub.com) if you have further questions.

## Download

The MoPub SDK is available via:

1. **Maven Central Repository**
    
    [ ![Download](https://api.bintray.com/packages/mopub/mopub-android-sdk/mopub-android-sdk/images/download.svg)](https://search.maven.org/search?q=MoPub-sdk)  
    The MoPub SDK is available as an AAR via Maven Central; to use it, add the following to your `build.gradle`.
    
    ```
    repositories {
        mavenCentral() // includes the MoPub SDK
        google() // necessary for Android API
    }

    dependencies {
        implementation('com.mopub:mopub-sdk:5.18.0@aar') {
            transitive = true
        }
    }
    ```

    ***SDK Modularization***

    With the modular SDK, you can choose to include specific formats to decrease overall SDK footprint in your app. To do so, include the line for any combination of components that you want in your `build.gradle` file as follows:

    ```groovy
    repositories {
        // ... other project repositories
        mavenCentral() // includes the MoPub SDK
        google() // necessary for Android API
    }

    dependencies {
        // ... other project dependencies

        // For banners
        implementation('com.mopub:mopub-sdk-banner:5.18.0@aar') {
            transitive = true
        }
        
        // For interstitials and rewarded ads
        implementation('com.mopub:mopub-sdk-fullscreen:5.18.0@aar') {
            transitive = true
        }

        // For native static (images).
        implementation('com.mopub:mopub-sdk-native-static:5.18.0@aar') {
            transitive = true
        }
    }
    ```

    **To continue integration using the mopub-sdk AAR, please see the [Getting Started guide](https://github.com/mopub/mopub-android-sdk/wiki/Getting-Started#updating-your-android-manifest).**

2. **Zipped Source**

    The MoPub SDK is also distributed as zipped source code that you can include in your application:

    **[MoPub Android SDK.zip](http://bit.ly/YUdWhH)**  
    _Includes everything you need to serve MoPub ads.  No third party ad networks are included._
    
    **For additional integration instructions, please see the [Getting Started guide](https://github.com/mopub/mopub-android-sdk/wiki/Getting-Started#requirements-and-dependencies).**

3. **Cloned GitHub repository**
    
    Alternatively, you can obtain the MoPub SDK source by cloning the git repository:
    
    `git clone git://github.com/mopub/mopub-android-sdk.git`
    
    **For additional integration instructions, please see the [Getting Started guide](https://github.com/mopub/mopub-android-sdk/wiki/Getting-Started#requirements-and-dependencies).**

## New in this Version
Please view the [changelog](https://github.com/mopub/mopub-android-sdk/blob/master/CHANGELOG.md) for a complete list of additions, fixes, and enhancements in the latest release.

- **Features**
  - Change when the close button and countdown timer are presented.
  - Add InMobi, Fyber, Ogury, and Mintegral as supported networks.
  - Switch to using XML instead of programmatic layouts for fullscreen ads.
  - Show the reward selection pop-up dialog regardless of number of reward choices in the sample app.

- **Bug Fixes**
  - Fix NPEs in `MoPubInline` caused by `InteractionListener`
  - Fix OM SDK `OutOfMemoryError` crash.
  - Fix custom deeplinks not opening on Android 11.
  - Prevent pre-initialization use of some public APIs from corrupting ad requests.

## Requirements

- Android 4.4 (API Version 19) and up (**Updated in 5.11.0**)
- androidx.legacy:legacy-support-v4:1.0.0 (**Updated in 5.9.0**)
- androidx.annotation:annotation:1.1.0 (**Updated in 5.9.0**)
- androidx.appcompat:appcompat:1.1.0 (**Updated in 5.10.0**)
- androidx.recyclerview:recyclerview:1.0.0 (**Updated in 5.9.0**)
- androidx.core:core-ktx:1.1.0 (**Added in 5.11.0**)
- com.google.code.gson:gson:2.8.6 (**Added in 5.11.0**)
- androidx.media2:media2-session:1.1.3 (**Updated in 5.17.0**)
- androidx.media2:media2-widget:1.1.3 (**Updated in 5.17.0**)
- androidx.media2:media2-player:1.1.3 (**Updated in 5.17.0**)
- com.mopub:omsdk-android:1.3.16 (**Updated in 5.15.0**)
- MoPub Volley Library (mopub-volley-2.1.0.jar - available on Maven Central) (**Updated in 5.17.0**)
- **Recommended** Google Play Services (com.google.android.gms:play-services-ads-identifier:17.0.0 and com.google.android.gms:play-services-base:17.5.0) (**Updated in 5.15.0**)
- If you are integrating with v5.6.0 or later of the MoPub SDK, specify the `sourceCompatibility` and `targetCompatibility` as below to prevent compilation errors with ExoPlayer 2.9.5 and later:

    ```groovy
    android {
        compileOptions {
            sourceCompatibility JavaVersion.VERSION_1_7
            targetCompatibility JavaVersion.VERSION_1_8
        }
    }
    ```

## Upgrading to SDK 5.13.0
AVID and Moat have been removed as MoPub works on a future viewability solution. No action is necessary, though the maven repository for Moat is no longer required.
All supported network adapters have been updated and are not backwards compatible. Please update to the latest network adapters when upgrading to SDK 5.13.0.

## Upgrading to SDK 5.0

Please see the [Getting Started Guide](https://developers.mopub.com/docs/android/getting-started/) for instructions on upgrading from SDK 4.X to SDK 5.0.

For GDPR-specific upgrading instructions, also see the [GDPR Integration Guide](https://developers.mopub.com/docs/publisher/gdpr-guide).

## Working with Android 6.0 Runtime Permissions
If your app's target SDK is 23 or higher _**and**_ the user's device is running Android 6.0 or higher, you are responsible for supporting [runtime permissions](http://developer.android.com/training/permissions/requesting.html), one of the [changes](http://developer.android.com/about/versions/marshmallow/android-6.0-changes.html) introduced in Android 6.0 (API level 23). In addition to listing any dangerous permissions your app needs in the manifest, your app also has to explicitly request the dangerous permission(s) during runtime by calling method `requestPermissions()` in the [`ActivityCompat`](http://developer.android.com/reference/android/support/v4/app/ActivityCompat.html) class.

### Specifically for the MoPub SDK:
- Dangerous permission [`ACCESS_COARSE_LOCATION`](http://developer.android.com/reference/android/Manifest.permission.html#ACCESS_COARSE_LOCATION) is needed to pass network location data to MoPub.
- Dangerous permission [`ACCESS_FINE_LOCATION`](http://developer.android.com/reference/android/Manifest.permission.html#ACCESS_FINE_LOCATION) is needed to pass GPS location data to MoPub.
    - Granting `ACCESS_FINE_LOCATION` also allows network location data to be passed to MoPub without the need to also grant `ACCESS_COARSE_LOCATION`.
- _**Note:** SDK Version 5.9.0 removes the requirement for the dangerous permission [`WRITE_EXTERNAL_STORAGE`](http://developer.android.com/reference/android/Manifest.permission.html#WRITE_EXTERNAL_STORAGE)._
- _**Note:** The user can deny granting any dangerous permissions during runtime, so please make sure your app can handle this properly._
- _**Note:** The user can revoke any permissions granted previously by going to your app's Settings screen, so please make sure your app can handle this properly._

### Additional resources:
- [Android 6.0 Changes](http://developer.android.com/about/versions/marshmallow/android-6.0-changes.html)
- [Requesting Permissions at Run Time](http://developer.android.com/training/permissions/requesting.html)
- [Permissions Best Practices](http://developer.android.com/training/permissions/best-practices.html)
- [Normal vs Dangerous Permissions](http://developer.android.com/guide/topics/security/permissions.html#normal-dangerous)
- [Permission Groups](http://developer.android.com/guide/topics/security/permissions.html#perm-groups)

## License

To view the full license, visit [http://www.mopub.com/legal/sdk-license-agreement/](http://www.mopub.com/legal/sdk-license-agreement/).

## Open Measurement License

We have partnered with the IAB to provide Viewability measurement via the Open Measurement SDK as of version 5.14.0. To view the full license, visit [https://www.mopub.com/en/omlv1](https://www.mopub.com/en/omlv1)
