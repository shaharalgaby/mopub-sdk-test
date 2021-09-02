// Copyright 2018-2021 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// https://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.common;

import android.content.Context;
import android.location.Location;
import android.location.LocationManager;
import android.os.SystemClock;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.mopub.common.logging.MoPubLog;
import com.mopub.common.util.DeviceUtils;

import java.math.BigDecimal;

import static android.Manifest.permission.ACCESS_COARSE_LOCATION;
import static android.Manifest.permission.ACCESS_FINE_LOCATION;
import static com.mopub.common.logging.MoPubLog.SdkLogEvent.CUSTOM;

public class LocationService {
    public enum LocationAwareness {
        NORMAL, TRUNCATED, DISABLED;

        // These deprecated methods are only used to support the deprecated methods
        // MoPubView#setLocationAwareness, MoPubInterstitial#setLocationAwareness
        // and should not be used elsewhere. Unless interacting with those methods, use
        // the type MoPub.LocationAwareness

        @Deprecated
        public MoPub.LocationAwareness getNewLocationAwareness() {
            if (this == TRUNCATED) {
                return MoPub.LocationAwareness.TRUNCATED;
            } else if (this == DISABLED) {
                return MoPub.LocationAwareness.DISABLED;
            } else {
                return MoPub.LocationAwareness.NORMAL;
            }
        }

        @Deprecated
        public static LocationAwareness
        fromMoPubLocationAwareness(MoPub.LocationAwareness awareness) {
            if (awareness == MoPub.LocationAwareness.DISABLED) {
                return DISABLED;
            } else if (awareness == MoPub.LocationAwareness.TRUNCATED) {
                return TRUNCATED;
            } else {
                return NORMAL;
            }
        }
    }

    private static final int DEFAULT_LOCATION_PRECISION = 6;
    private static final long DEFAULT_LOCATION_REFRESH_TIME_MILLIS = 10 * 60 * 1000; // 10 minutes

    private static volatile LocationService sInstance;
    @VisibleForTesting
    @Nullable
    Location mLastKnownLocation;
    @VisibleForTesting long mLocationLastUpdatedMillis;
    @NonNull private volatile MoPub.LocationAwareness mLocationAwareness = MoPub.LocationAwareness.NORMAL;
    private volatile int mLocationPrecision = DEFAULT_LOCATION_PRECISION;
    private volatile long mMinimumLocationRefreshTimeMillis = DEFAULT_LOCATION_REFRESH_TIME_MILLIS;

    private LocationService() {
    }

    @VisibleForTesting
    @NonNull
    static LocationService getInstance() {
        LocationService locationService = sInstance;
        if (locationService == null) {
            synchronized (LocationService.class) {
                locationService = sInstance;
                if (locationService == null) {
                    locationService = new LocationService();
                    sInstance = locationService;
                }
            }
        }
        return locationService;
    }

    public enum ValidLocationProvider {
        NETWORK(LocationManager.NETWORK_PROVIDER),
        GPS(LocationManager.GPS_PROVIDER);

        @NonNull final String name;

        ValidLocationProvider(@NonNull final String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }

        private boolean hasRequiredPermissions(@NonNull final Context context) {
            switch (this) {
                case NETWORK:
                    return DeviceUtils.isPermissionGranted(context, ACCESS_FINE_LOCATION)
                            || DeviceUtils.isPermissionGranted(context, ACCESS_COARSE_LOCATION);
                case GPS:
                    return DeviceUtils.isPermissionGranted(context, ACCESS_FINE_LOCATION);
                default:
                    return false;
            }
        }
    }

    @NonNull
    MoPub.LocationAwareness getLocationAwareness() {
        return mLocationAwareness;
    }

    void setLocationAwareness(@NonNull final MoPub.LocationAwareness locationAwareness) {
        Preconditions.checkNotNull(locationAwareness);

        mLocationAwareness = locationAwareness;
    }

    int getLocationPrecision() {
        return mLocationPrecision;
    }

    /**
     * Sets the precision to use when the SDK's location awareness is set
     * to {@link com.mopub.common.MoPub.LocationAwareness#TRUNCATED}.
     */
    void setLocationPrecision(int precision) {
        mLocationPrecision = Math.min(Math.max(0, precision), DEFAULT_LOCATION_PRECISION);
    }

    void setMinimumLocationRefreshTimeMillis(
            final long minimumLocationRefreshTimeMillis) {
        mMinimumLocationRefreshTimeMillis = minimumLocationRefreshTimeMillis;
    }

    long getMinimumLocationRefreshTimeMillis() {
        return mMinimumLocationRefreshTimeMillis;
    }

    /**
     * Returns the last known location of the device using its GPS and network location providers.
     * This only checks Android location providers as often as
     * {@link MoPub#getMinimumLocationRefreshTimeMillis()} says to, in milliseconds.
     * <p>
     * May be {@code null} if:
     * <ul>
     * <li> Location permissions are not requested in the Android manifest file
     * <li> The location providers don't exist
     * <li> Location awareness is disabled in the parent MoPubView
     * <li> context is null
     * </ul>
     */
    @Nullable
    public static Location getLastKnownLocation(@Nullable final Context context) {
        if (!MoPub.canCollectPersonalInformation()) {
            return null;
        }

        final LocationService locationService = getInstance();

        final MoPub.LocationAwareness locationAwareness = locationService.mLocationAwareness;
        final int locationPrecision = locationService.mLocationPrecision;

        if (locationAwareness == MoPub.LocationAwareness.DISABLED) {
            return null;
        }

        if (isLocationFreshEnough()) {
            return locationService.mLastKnownLocation;
        }

        if (context == null) {
            return null;
        }

        Location location = getLocationFromProvider(context, ValidLocationProvider.GPS);
        if (location == null) {
            location = getLocationFromProvider(context, ValidLocationProvider.NETWORK);
        }

        // Truncate latitude/longitude to the number of digits specified by locationPrecision.
        if (locationAwareness == MoPub.LocationAwareness.TRUNCATED) {
            truncateLocationLatLon(location, locationPrecision);
        }

        if (location != null) {
            locationService.setLastLocation(location);
        }

        return locationService.mLastKnownLocation;
    }

    @VisibleForTesting
    @Nullable
    static Location getLocationFromProvider(@NonNull final Context context,
            @NonNull final ValidLocationProvider provider) {
        Preconditions.checkNotNull(context);
        Preconditions.checkNotNull(provider);

        if (!MoPub.canCollectPersonalInformation()) {
            return null;
        }

        if (!provider.hasRequiredPermissions(context)) {
            return null;
        }

        final LocationManager locationManager =
                (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        try {
            // noinspection ResourceType
            return locationManager.getLastKnownLocation(provider.toString());
        } catch (SecurityException e) {
            MoPubLog.log(CUSTOM, "Failed to retrieve location from " +
                    provider.toString() + " provider: access appears to be disabled.");
        } catch (IllegalArgumentException e) {
            MoPubLog.log(CUSTOM, "Failed to retrieve location: device has no " +
                    provider.toString() + " location provider.");
        } catch (NullPointerException e) { // This happens on 4.2.2 on a few Android TV devices
            MoPubLog.log(CUSTOM, "Failed to retrieve location: device has no " +
                    provider.toString() + " location provider.");
        }

        return null;
    }

    @VisibleForTesting
    @Nullable
    static Location getMostRecentValidLocation(@Nullable final Location a, @Nullable final Location b) {
        if (a == null) {
            return b;
        }

        if (b == null) {
            return a;
        }

        // At this point, locations A and B are non-null, so return the more recent one
        return (a.getTime() > b.getTime()) ? a : b;
    }

    @VisibleForTesting
    static void truncateLocationLatLon(@Nullable final Location location,
            final int precision) {
        if (location == null || precision < 0) {
            return;
        }

        double lat = location.getLatitude();
        double truncatedLat = BigDecimal.valueOf(lat)
                .setScale(precision, BigDecimal.ROUND_HALF_DOWN)
                .doubleValue();
        location.setLatitude(truncatedLat);

        double lon = location.getLongitude();
        double truncatedLon = BigDecimal.valueOf(lon)
                .setScale(precision, BigDecimal.ROUND_HALF_DOWN)
                .doubleValue();
        location.setLongitude(truncatedLon);
    }

    private static boolean isLocationFreshEnough() {
        final LocationService locationService = getInstance();
        if (locationService.mLastKnownLocation == null) {
            return false;
        }
        return SystemClock.elapsedRealtime() - locationService.mLocationLastUpdatedMillis <=
                locationService.getMinimumLocationRefreshTimeMillis();
    }

    @Deprecated
    @VisibleForTesting
    public static void clearLastKnownLocation() {
        getInstance().mLastKnownLocation = null;
    }

    void setLastLocation(@Nullable Location location) {
        if (location == null) {
            return;
        }
        final LocationService locationService = getInstance();
        locationService.mLastKnownLocation = location;
        locationService.mLocationLastUpdatedMillis = SystemClock.elapsedRealtime();

    }
}
