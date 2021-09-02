// Copyright 2018-2021 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// https://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.common.privacy;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.mopub.common.ClientMetadata;
import com.mopub.common.Constants;
import com.mopub.common.MoPub;
import com.mopub.common.Preconditions;
import com.mopub.common.SdkInitializationListener;
import com.mopub.common.VisibleForTesting;
import com.mopub.common.logging.MoPubLog;
import com.mopub.common.util.ManifestUtils;
import com.mopub.mobileads.MoPubConversionTracker;
import com.mopub.mobileads.MoPubErrorCode;
import com.mopub.network.MoPubNetworkError;
import com.mopub.network.MultiAdResponse;
import com.mopub.network.Networking;

import java.util.Calendar;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static com.mopub.common.logging.MoPubLog.ConsentLogEvent.CUSTOM;
import static com.mopub.common.logging.MoPubLog.ConsentLogEvent.LOAD_ATTEMPTED;
import static com.mopub.common.logging.MoPubLog.ConsentLogEvent.LOAD_FAILED;
import static com.mopub.common.logging.MoPubLog.ConsentLogEvent.SYNC_ATTEMPTED;
import static com.mopub.common.logging.MoPubLog.ConsentLogEvent.SYNC_COMPLETED;
import static com.mopub.common.logging.MoPubLog.ConsentLogEvent.SYNC_FAILED;
import static com.mopub.common.logging.MoPubLog.ConsentLogEvent.UPDATED;

/**
 * The manager handling personal information. If the user is in a GDPR region, MoPub must get
 * user consent to handle and store user data.
 */
public class PersonalInfoManager {

    /**
     * Default minimum sync delay of 5 minutes.
     */
    private static final long MINIMUM_SYNC_DELAY = 5 * 60 * 1000;

    @NonNull private final Context mAppContext;
    @NonNull private final Set<ConsentStatusChangeListener> mConsentStatusChangeListeners;
    @NonNull private final PersonalInfoData mPersonalInfoData;
    @NonNull private final ConsentDialogController mConsentDialogController;
    @NonNull private final MoPubConversionTracker mConversionTracker;
    @NonNull private final SyncRequest.Listener mSyncRequestListener;
    @NonNull private MultiAdResponse.ServerOverrideListener mServerOverrideListener;
    @Nullable private SdkInitializationListener mSdkInitializationListener;

    private long mSyncDelayMs = MINIMUM_SYNC_DELAY;
    @Nullable private Long mLastSyncRequestTimeUptimeMs;
    @Nullable private ConsentStatus mSyncRequestConsentStatus;
    private boolean mSyncRequestInFlight;
    private boolean mForceGdprAppliesChanged;
    private boolean mForceGdprAppliesChangedSending;
    private boolean mLegitimateInterestAllowed;

    public PersonalInfoManager(@NonNull final Context context, @NonNull final String adUnitId,
            @Nullable SdkInitializationListener sdkInitializationListener) {
        Preconditions.checkNotNull(context);
        Preconditions.checkNotNull(adUnitId);

        mAppContext = context.getApplicationContext();
        mConsentStatusChangeListeners = Collections.synchronizedSet(
                new HashSet<ConsentStatusChangeListener>());
        mSyncRequestListener = new PersonalInfoSyncRequestListener();
        mServerOverrideListener = new PersonalInfoServerOverrideListener();
        MultiAdResponse.setServerOverrideListener(mServerOverrideListener);

        mConsentDialogController = new ConsentDialogController(mAppContext);

        mPersonalInfoData = new PersonalInfoData(mAppContext);
        if (!TextUtils.isEmpty(adUnitId) &&
                !adUnitId.equals(mPersonalInfoData.getCachedLastAdUnitIdUsedForInit())) {
            mPersonalInfoData.setAdUnit("");
            mPersonalInfoData.setCachedLastAdUnitIdUsedForInit(adUnitId);
            mPersonalInfoData.writeToDisk();
        }

        mConversionTracker = new MoPubConversionTracker(mAppContext);

        final MoPubIdentifier.AdvertisingIdChangeListener advertisingIdChangeListener =
                new MoPubIdentifier.AdvertisingIdChangeListener() {
                    @Override
                    public void onIdChanged(@NonNull final AdvertisingId oldId,
                            @NonNull final AdvertisingId newId) {
                        Preconditions.checkNotNull(oldId);
                        Preconditions.checkNotNull(newId);

                        if (oldId.isDoNotTrack() && newId.isDoNotTrack()) {
                            return;
                        } else if (!oldId.isDoNotTrack() && newId.isDoNotTrack()) {
                            attemptStateTransition(ConsentStatus.DNT,
                                    ConsentChangeReason.DENIED_BY_DNT_ON);
                            requestSync(true);
                            return;
                        } else if (oldId.isDoNotTrack() && !newId.isDoNotTrack()) {
                            if (ConsentStatus.EXPLICIT_NO.equals(
                                    mPersonalInfoData.getConsentStatusBeforeDnt())) {
                                attemptStateTransition(ConsentStatus.EXPLICIT_NO,
                                        ConsentChangeReason.DNT_OFF);
                                return;
                            }
                            attemptStateTransition(ConsentStatus.UNKNOWN,
                                    ConsentChangeReason.DNT_OFF);
                            return;
                        }
                        // !oldId.isDoNotTrack() && !newId.isDoNotTrack()

                        if (!TextUtils.isEmpty(newId.mAdvertisingId) &&
                                !newId.getIfa().equals(mPersonalInfoData.getIfa()) &&
                                ConsentStatus.EXPLICIT_YES.equals(
                                        mPersonalInfoData.getConsentStatus())) {
                            mPersonalInfoData.setLastSuccessfullySyncedConsentStatus(null);
                            mPersonalInfoData.setLastChangedMs(null);
                            attemptStateTransition(ConsentStatus.UNKNOWN,
                                    ConsentChangeReason.IFA_CHANGED);
                        }
                    }
                };
        mSdkInitializationListener = sdkInitializationListener;

        final MoPubIdentifier moPubIdentifier = ClientMetadata.getInstance(
                mAppContext).getMoPubIdentifier();
        moPubIdentifier.setIdChangeListener(advertisingIdChangeListener);
        moPubIdentifier.setInitializationListener(createInitializationListener());
    }

    /**
     * Checks to see if a publisher should load and then show a consent dialog.
     *
     * @return True for yes, false for no.
     */
    public boolean shouldShowConsentDialog() {
        final Boolean gdprApplies = gdprApplies();
        if (gdprApplies == null || !gdprApplies) {
            return false;
        }

        // Check to see if the server said to reacquire consent.
        if (mPersonalInfoData.shouldReacquireConsent()) {
            return true;
        }

        return mPersonalInfoData.getConsentStatus().equals(ConsentStatus.UNKNOWN);
    }

    /**
     * Whether or not the consent dialog is done loading and ready to show.
     *
     * @return True for yes, false for no.
     */
    public boolean isConsentDialogReady() {
        return mConsentDialogController.isReady();
    }

    /**
     * Sends off a request to load the MoPub consent dialog.
     *
     * @param consentDialogListener This callback will be called when the dialog has finished
     *                              loading or the dialog failed to load.
     */
    public void loadConsentDialog(
            @Nullable final ConsentDialogListener consentDialogListener) {
        MoPubLog.log(LOAD_ATTEMPTED);
        ManifestUtils.checkGdprActivitiesDeclared(mAppContext);

        if (ClientMetadata.getInstance(
                mAppContext).getMoPubIdentifier().getAdvertisingInfo().isDoNotTrack()) {
            if (consentDialogListener != null) {
                new Handler().post(new Runnable() {
                    @Override
                    public void run() {
                        MoPubLog.log(LOAD_FAILED, MoPubErrorCode.DO_NOT_TRACK.getIntCode(),
                                MoPubErrorCode.DO_NOT_TRACK);
                        consentDialogListener.onConsentDialogLoadFailed(
                                MoPubErrorCode.DO_NOT_TRACK);
                    }
                });
            }
            return;
        }
        final Boolean gdprApplies = gdprApplies();
        if (gdprApplies != null && !gdprApplies) {
            if (consentDialogListener != null) {
                new Handler().post(new Runnable() {
                    @Override
                    public void run() {
                        MoPubLog.log(LOAD_FAILED, MoPubErrorCode.GDPR_DOES_NOT_APPLY.getIntCode(),
                                MoPubErrorCode.GDPR_DOES_NOT_APPLY);
                        consentDialogListener.onConsentDialogLoadFailed(
                                MoPubErrorCode.GDPR_DOES_NOT_APPLY);
                    }
                });
            }
            return;
        }
        mConsentDialogController.loadConsentDialog(consentDialogListener, gdprApplies,
                mPersonalInfoData);
    }

    /**
     * If the MoPub consent dialog is loaded, then show it.
     *
     * @return True for successfully shown, false for failed to show.
     */
    public boolean showConsentDialog() {
        return mConsentDialogController.showConsentDialog();
    }

    /**
     * Whether or not the SDK is allowed to collect user data.
     *
     * @return true if able to collect user data.
     */
    public boolean canCollectPersonalInformation() {
        final Boolean gdprApplies = gdprApplies();
        // If we don't know whether or not GDPR applies, then we haven't synced, so we cannot
        // collect personal information.
        if (gdprApplies == null) {
            return false;
        }

        // If we are not in a GDPR region, we can freely collect user data.
        if (!gdprApplies) {
            return true;
        }

        // Return whether or not we have consent and that Do Not Track is disabled.
        return getPersonalInfoConsentStatus().equals(ConsentStatus.EXPLICIT_YES) &&
                !ClientMetadata.getInstance(mAppContext).getMoPubIdentifier().getAdvertisingInfo()
                        .isDoNotTrack();
    }

    /**
     * Set the allowance of legitimate interest.
     *
     * @param allowed is true if legitimate interest is allowed. False if it isn't allowed.
     */
    public void setAllowLegitimateInterest(boolean allowed) {
        mLegitimateInterestAllowed = allowed;
    }

    /**
     * Check this to see if legitimate interest is allowed.
     *
     * @return True if allowed, false otherwise.
     */
    public boolean shouldAllowLegitimateInterest() {
        return mLegitimateInterestAllowed;
    }

    /**
     * Returns whether or not the SDK thinks the user is in a GDPR region or not. Returns true for
     * in a GDPR region, false for not in a GDPR region, and null for unknown. This value can be
     * overwritten to true by setting forceGdprApplies().
     *
     * @return true for in GDPR region, false for not in GDPR region, null for unknown
     */
    @Nullable
    public Boolean gdprApplies() {
        if (mPersonalInfoData.isForceGdprApplies()) {
            return true;
        }
        return mPersonalInfoData.getGdprApplies();
    }

    /**
     * Forces the SDK to treat this app as in a GDPR region. Setting this will permanently force
     * GDPR rules for this user unless this app is uninstalled or the data for this app is cleared.
     */
    public void forceGdprApplies() {
        if (mPersonalInfoData.isForceGdprApplies()) {
            return;
        }
        final boolean oldCanCollectPersonalInformation = canCollectPersonalInformation();
        mPersonalInfoData.setForceGdprApplies(true);
        mForceGdprAppliesChanged = true;
        mPersonalInfoData.writeToDisk();
        final boolean newCanCollectPersonalInformation = canCollectPersonalInformation();
        if (oldCanCollectPersonalInformation != newCanCollectPersonalInformation) {
            fireOnConsentStateChangeListeners(mPersonalInfoData.getConsentStatus(),
                    mPersonalInfoData.getConsentStatus(), newCanCollectPersonalInformation);
        }
        requestSync(true);
    }

    /**
     * The user's current consent status. In general, publishers should not query this directly.
     * It is recommended to use MoPub#canCollectPersonalInformation instead.
     *
     * @return ConsentStatus representing the current consent status.
     */
    @NonNull
    public ConsentStatus getPersonalInfoConsentStatus() {
        return mPersonalInfoData.getConsentStatus();
    }

    /**
     * For use by whitelisted publishers only. Grants consent to collect personally identifiable
     * information for the current user.
     */
    public void grantConsent() {
        if (ClientMetadata.getInstance(mAppContext).getMoPubIdentifier().getAdvertisingInfo()
                .isDoNotTrack()) {
            MoPubLog.log(CUSTOM, "Cannot grant consent because Do Not Track is on.");
            return;
        }

        if (mPersonalInfoData.isWhitelisted()) {
            attemptStateTransition(ConsentStatus.EXPLICIT_YES,
                    ConsentChangeReason.GRANTED_BY_WHITELISTED_PUB);
        } else {
            MoPubLog.log(CUSTOM, "You do not have approval to use the grantConsent API. Please reach out " +
                    "to your account teams or support@mopub.com for more information.");
            attemptStateTransition(ConsentStatus.POTENTIAL_WHITELIST,
                    ConsentChangeReason.GRANTED_BY_NOT_WHITELISTED_PUB);
        }
        requestSync(true);
    }

    /**
     * Denies consent to collect personally identifiable information for the current user.
     */
    public void revokeConsent() {
        if (ClientMetadata.getInstance(mAppContext).getMoPubIdentifier().getAdvertisingInfo()
                .isDoNotTrack()) {
            MoPubLog.log(CUSTOM, "Cannot revoke consent because Do Not Track is on.");
            return;
        }

        attemptStateTransition(ConsentStatus.EXPLICIT_NO, ConsentChangeReason.DENIED_BY_PUB);
        requestSync(true);
    }

    void changeConsentStateFromDialog(@NonNull final ConsentStatus consentStatus) {
        Preconditions.checkNotNull(consentStatus);

        switch (consentStatus) {
            case EXPLICIT_YES:
                attemptStateTransition(consentStatus, ConsentChangeReason.GRANTED_BY_USER);
                requestSync(true);
                break;
            case EXPLICIT_NO:
                attemptStateTransition(consentStatus, ConsentChangeReason.DENIED_BY_USER);
                requestSync(true);
                break;
            default:
                MoPubLog.log(CUSTOM, "Invalid consent status: " + consentStatus + ". This is a bug with " +
                        "the use of changeConsentStateFromDialog.");
        }
    }

    /**
     * Add a listener for consent status changes.
     *
     * @param consentStatusChangeListener This listener will be called when a status transition happens.
     */
    public void subscribeConsentStatusChangeListener(
            @Nullable final ConsentStatusChangeListener consentStatusChangeListener) {
        if (consentStatusChangeListener == null) {
            return;
        }
        mConsentStatusChangeListeners.add(consentStatusChangeListener);
    }

    /**
     * Call this to unsubscribe a consentStatusChangeListener.
     *
     * @param consentStatusChangeListener This listener will no longer be receiving status transitions.
     */
    public void unsubscribeConsentStatusChangeListener(
            @Nullable final ConsentStatusChangeListener consentStatusChangeListener) {
        mConsentStatusChangeListeners.remove(consentStatusChangeListener);
    }

    @VisibleForTesting
    static boolean shouldMakeSyncRequest(final boolean syncRequestInFlight,
            @Nullable final Boolean gdprApplies, final boolean force,
            @Nullable final Long lastSyncRequestTimeMs, final long syncDelay,
            @Nullable final String ifa, final boolean dnt) {
        if (syncRequestInFlight) {
            return false;
        }
        if (gdprApplies == null) {
            return true;
        }
        if (!gdprApplies) {
            return false;
        }
        if (force) {
            return true;
        }
        if (dnt && TextUtils.isEmpty(ifa)) {
            return false;
        }
        if (lastSyncRequestTimeMs == null) {
            return true;
        }
        return SystemClock.uptimeMillis() - lastSyncRequestTimeMs > syncDelay;
    }

    /**
     * Called internally to request a sync to ad server about consent status and other metadata.
     *
     * @param force Call sync even if it has not been mSyncDelayMs. Still won't happen if not in
     *              a GDPR region or if a request is already in flight.
     */
    public void requestSync(final boolean force) {
        if (!MoPub.isSdkInitialized()) {
            return;
        }

        final AdvertisingId advertisingId = ClientMetadata.getInstance(mAppContext)
                .getMoPubIdentifier().getAdvertisingInfo();
        if (!shouldMakeSyncRequest(mSyncRequestInFlight,
                gdprApplies(),
                force,
                mLastSyncRequestTimeUptimeMs,
                mSyncDelayMs,
                mPersonalInfoData.getIfa(),
                advertisingId.isDoNotTrack())) {
            return;
        }

        requestSync();
    }

    @VisibleForTesting
    void requestSync() {
        MoPubLog.log(SYNC_ATTEMPTED);

        mSyncRequestConsentStatus = mPersonalInfoData.getConsentStatus();
        mSyncRequestInFlight = true;

        mLastSyncRequestTimeUptimeMs = SystemClock.uptimeMillis();
        final SyncUrlGenerator syncUrlGenerator = new SyncUrlGenerator(mAppContext,
                mSyncRequestConsentStatus.getValue());
        syncUrlGenerator.withAdUnitId(mPersonalInfoData.chooseAdUnit())
                .withConsentedIfa(mPersonalInfoData.getIfa())
                .withLastChangedMs(mPersonalInfoData.getLastChangedMs())
                .withLastConsentStatus(mPersonalInfoData.getLastSuccessfullySyncedConsentStatus())
                .withConsentChangeReason(mPersonalInfoData.getConsentChangeReason())
                .withConsentedVendorListVersion(mPersonalInfoData.getConsentedVendorListVersion())
                .withConsentedPrivacyPolicyVersion(
                        mPersonalInfoData.getConsentedPrivacyPolicyVersion())
                .withCachedVendorListIabHash(mPersonalInfoData.getCurrentVendorListIabHash())
                .withExtras(mPersonalInfoData.getExtras())
                .withGdprApplies(gdprApplies())
                .withForceGdprApplies(mPersonalInfoData.isForceGdprApplies());
        if (mForceGdprAppliesChanged) {
            mForceGdprAppliesChangedSending = true;
            syncUrlGenerator.withForceGdprAppliesChanged(true);
        }

        final SyncRequest syncRequest = new SyncRequest(mAppContext,
                syncUrlGenerator.generateUrlString(
                        Constants.HOST), mSyncRequestListener);
        Networking.getRequestQueue(mAppContext).add(syncRequest);
    }

    /**
     * For use by whitelisted publishers only. Gets a copy of the current and consented vendor
     * list and privacy policy and their versions.
     *
     * @return ConsentData which is a snapshot of the underlying data store.
     */
    public ConsentData getConsentData() {
        return new PersonalInfoData(mAppContext);
    }

    /**
     * SDK-initiated state transitions should try to use this to keep the consent change reason
     * to one of the reasons we know about.
     *
     * @param newConsentStatus    The new consent status we want to transition to.
     * @param consentChangeReason Why the state changed.
     */
    private void attemptStateTransition(@NonNull final ConsentStatus newConsentStatus,
            @NonNull final ConsentChangeReason consentChangeReason) {
        attemptStateTransition(newConsentStatus, consentChangeReason.getReason());
    }

    /**
     * Server-initiated state transitions may have non-enum change reasons, so use this.
     *
     * @param newConsentStatus    The new consent status we want to transition to.
     * @param consentChangeReason Why the state changed.
     */
    @VisibleForTesting
    void attemptStateTransition(@NonNull final ConsentStatus newConsentStatus,
            @NonNull final String consentChangeReason) {
        Preconditions.checkNotNull(newConsentStatus);
        Preconditions.checkNotNull(consentChangeReason);

        final ConsentStatus oldConsentStatus = mPersonalInfoData.getConsentStatus();
        if (!mPersonalInfoData.shouldReacquireConsent() && oldConsentStatus.equals(newConsentStatus)) {
            MoPubLog.log(CUSTOM, "Consent status is already " + oldConsentStatus +
                    ". Not doing a state transition.");
            return;
        }

        mPersonalInfoData.setLastChangedMs("" + Calendar.getInstance().getTimeInMillis());
        mPersonalInfoData.setConsentChangeReason(consentChangeReason);
        mPersonalInfoData.setConsentStatus(newConsentStatus);
        // Update the versions when going to a POTENTIAL_WHITELIST state, an EXPLICIT_YES state if
        // it wasn't coming from a POTENTIAL_WHITELIST state, and an EXPLICIT_NO state.
        if (shouldSetConsentedVersions(oldConsentStatus, newConsentStatus)) {
            mPersonalInfoData.setConsentedPrivacyPolicyVersion(
                    mPersonalInfoData.getCurrentPrivacyPolicyVersion());
            mPersonalInfoData.setConsentedVendorListVersion(
                    mPersonalInfoData.getCurrentVendorListVersion());
            mPersonalInfoData.setConsentedVendorListIabFormat(
                    mPersonalInfoData.getCurrentVendorListIabFormat());
        }

        if (ConsentStatus.DNT.equals(newConsentStatus) ||
                ConsentStatus.UNKNOWN.equals(newConsentStatus)) {
            mPersonalInfoData.setConsentedPrivacyPolicyVersion(null);
            mPersonalInfoData.setConsentedVendorListVersion(null);
            mPersonalInfoData.setConsentedVendorListIabFormat(null);
        }

        if (ConsentStatus.EXPLICIT_YES.equals(newConsentStatus)) {
            mPersonalInfoData.setIfa(ClientMetadata.getInstance(
                    mAppContext).getMoPubIdentifier().getAdvertisingInfo().getIfa());
        }

        if (ConsentStatus.DNT.equals(newConsentStatus)) {
            mPersonalInfoData.setConsentStatusBeforeDnt(oldConsentStatus);
        }
        mPersonalInfoData.setShouldReacquireConsent(false);
        mPersonalInfoData.writeToDisk();

        final boolean canCollectPersonalInformation = canCollectPersonalInformation();
        if (canCollectPersonalInformation) {
            ClientMetadata.getInstance(mAppContext).repopulateCountryData();
            if (mConversionTracker.shouldTrack()) {
                mConversionTracker.reportAppOpen(false);
            }
        }

        MoPubLog.log(UPDATED, oldConsentStatus, newConsentStatus, canCollectPersonalInformation(), consentChangeReason);

        fireOnConsentStateChangeListeners(oldConsentStatus, newConsentStatus,
                canCollectPersonalInformation);
    }

    /**
     * Checks to see if the consented privacy policy version, vendor list version, and IAB format
     * String should be updated.
     *
     * @param oldConsentStatus The old consent status.
     * @param newConsentStatus The new consent status.
     * @return True if the versions should be updated.
     */
    private static boolean shouldSetConsentedVersions(@Nullable final ConsentStatus oldConsentStatus,
            @Nullable final ConsentStatus newConsentStatus) {
        if (ConsentStatus.EXPLICIT_NO.equals(newConsentStatus)) {
            return true;
        }
        if (ConsentStatus.POTENTIAL_WHITELIST.equals(newConsentStatus)) {
            return true;
        }
        // True if going to EXPLICIT_YES, but only if not coming from POTENTIAL_WHITELIST
        if (!ConsentStatus.POTENTIAL_WHITELIST.equals(oldConsentStatus) &&
                ConsentStatus.EXPLICIT_YES.equals(newConsentStatus)) {
            return true;
        }
        return false;
    }

    private void fireOnConsentStateChangeListeners(@NonNull final ConsentStatus oldConsentStatus,
            @NonNull final ConsentStatus newConsentStatus,
            final boolean canCollectPersonalInformation) {
        synchronized (mConsentStatusChangeListeners) {
            for (final ConsentStatusChangeListener stateChangeListener : mConsentStatusChangeListeners) {
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        stateChangeListener.onConsentStateChange(oldConsentStatus, newConsentStatus,
                                canCollectPersonalInformation);
                    }
                });
            }
        }
    }

    private SdkInitializationListener createInitializationListener() {
        return new SdkInitializationListener() {

            @Override
            public void onInitializationFinished() {
                MoPubLog.log(CUSTOM, "MoPubIdentifier initialized.");
                final AdvertisingId advertisingId = ClientMetadata.getInstance(mAppContext)
                        .getMoPubIdentifier().getAdvertisingInfo();
                if (!shouldMakeSyncRequest(mSyncRequestInFlight,
                        gdprApplies(),
                        false,
                        mLastSyncRequestTimeUptimeMs,
                        mSyncDelayMs,
                        mPersonalInfoData.getIfa(),
                        advertisingId.isDoNotTrack())) {
                    if (mSdkInitializationListener != null) {
                        mSdkInitializationListener.onInitializationFinished();
                        mSdkInitializationListener = null;
                    }
                } else {
                    requestSync();
                }
                new MoPubConversionTracker(mAppContext).reportAppOpen(true);
            }
        };
    }

    private class PersonalInfoSyncRequestListener implements SyncRequest.Listener {

        @Override
        public void onResponse(@NonNull final SyncResponse response) {
            MoPubLog.log(SYNC_COMPLETED);

            final boolean oldCanCollectPersonalInformation = canCollectPersonalInformation();
            if (mPersonalInfoData.getGdprApplies() == null) {
                mPersonalInfoData.setGdprApplies(response.isGdprRegion());
            }
            if (response.isForceGdprApplies()) {
                mForceGdprAppliesChanged = true;
                mPersonalInfoData.setForceGdprApplies(true);
                final boolean newCanCollectPersonalInformation = canCollectPersonalInformation();
                if (oldCanCollectPersonalInformation != newCanCollectPersonalInformation) {
                    fireOnConsentStateChangeListeners(mPersonalInfoData.getConsentStatus(),
                            mPersonalInfoData.getConsentStatus(), newCanCollectPersonalInformation);
                }
            }

            final String cachedLastAdUnitIdUsedForInit =
                    mPersonalInfoData.getCachedLastAdUnitIdUsedForInit();
            if (!TextUtils.isEmpty(cachedLastAdUnitIdUsedForInit) &&
                    mPersonalInfoData.getAdUnitId().isEmpty()) {
                mPersonalInfoData.setAdUnit(cachedLastAdUnitIdUsedForInit);
            }
            mPersonalInfoData.setLastSuccessfullySyncedConsentStatus(mSyncRequestConsentStatus);
            mPersonalInfoData.setWhitelisted(response.isWhitelisted());
            mPersonalInfoData.setCurrentVendorListVersion(response.getCurrentVendorListVersion());
            mPersonalInfoData.setCurrentVendorListLink(response.getCurrentVendorListLink());
            mPersonalInfoData.setCurrentPrivacyPolicyVersion(
                    response.getCurrentPrivacyPolicyVersion());
            mPersonalInfoData.setCurrentPrivacyPolicyLink(response.getCurrentPrivacyPolicyLink());
            final String iabHash = response.getCurrentVendorListIabHash();
            final String iabFormat = response.getCurrentVendorListIabFormat();
            if (!TextUtils.isEmpty(iabHash) && !iabHash.equals(
                    mPersonalInfoData.getCurrentVendorListIabHash()) && !TextUtils.isEmpty(
                    iabFormat)) {
                mPersonalInfoData.setCurrentVendorListIabFormat(iabFormat);
                mPersonalInfoData.setCurrentVendorListIabHash(iabHash);
            }
            final String extras = response.getExtras();
            if (!TextUtils.isEmpty(extras)) {
                mPersonalInfoData.setExtras(extras);
            }
            final String consentChangeReason = response.getConsentChangeReason();

            // Only one of these should happen. Prioritize no.
            if (response.isForceExplicitNo()) {
                mServerOverrideListener.onForceExplicitNo(consentChangeReason);
            } else if (response.isInvalidateConsent()) {
                mServerOverrideListener.onInvalidateConsent(consentChangeReason);
            } else if (response.isReacquireConsent()) {
                mServerOverrideListener.onReacquireConsent(consentChangeReason);
            }

            final String callAgainAfterSecs = response.getCallAgainAfterSecs();
            if (!TextUtils.isEmpty(callAgainAfterSecs)) {
                try {
                    final long callAgainAfterSecsLong = Long.parseLong(callAgainAfterSecs);
                    if (callAgainAfterSecsLong > 0) {
                        mSyncDelayMs = callAgainAfterSecsLong * 1000;
                    } else {
                        MoPubLog.log(CUSTOM, "callAgainAfterSecs is not positive: " + callAgainAfterSecs);
                    }
                } catch (NumberFormatException e) {
                    MoPubLog.log(CUSTOM, "Unable to parse callAgainAfterSecs. Ignoring value");
                }
            }

            // Clear out our cached IFA if we have sent it one last time in case limit ad tracking
            // is turned on.
            if (!ConsentStatus.EXPLICIT_YES.equals(mSyncRequestConsentStatus)) {
                mPersonalInfoData.setIfa(null);
            }

            if (mForceGdprAppliesChangedSending) {
                mForceGdprAppliesChanged = false;
                mForceGdprAppliesChangedSending = false;
            }

            mPersonalInfoData.writeToDisk();

            mSyncRequestInFlight = false;

            if (ConsentStatus.POTENTIAL_WHITELIST.equals(
                    mSyncRequestConsentStatus) && mPersonalInfoData.isWhitelisted()) {
                attemptStateTransition(ConsentStatus.EXPLICIT_YES,
                        ConsentChangeReason.GRANTED_BY_WHITELISTED_PUB);
                requestSync(true);
            }

            if (mSdkInitializationListener != null) {
                mSdkInitializationListener.onInitializationFinished();
                mSdkInitializationListener = null;
            }
        }

        @Override
        public void onErrorResponse(@NonNull final MoPubNetworkError networkError) {
            final int reason = (networkError.getReason() != null)
                    ? networkError.getReason().getCode()
                    : MoPubErrorCode.UNSPECIFIED.getIntCode();
            final String message = (networkError.getMessage() != null)
                    ? networkError.getMessage()
                    : MoPubErrorCode.UNSPECIFIED.toString();
            MoPubLog.log(SYNC_FAILED, reason, message);

            mSyncRequestInFlight = false;
            if (mSdkInitializationListener != null) {
                MoPubLog.log(CUSTOM, "Personal Info Manager initialization finished but ran into errors.");
                mSdkInitializationListener.onInitializationFinished();
                mSdkInitializationListener = null;
            }
        }
    }

    private class PersonalInfoServerOverrideListener implements MultiAdResponse.ServerOverrideListener {
        @Override
        public void onForceExplicitNo(@Nullable final String consentChangeReason) {
            if (TextUtils.isEmpty(consentChangeReason)) {
                attemptStateTransition(ConsentStatus.EXPLICIT_NO,
                        ConsentChangeReason.REVOKED_BY_SERVER);
                return;
            }
            attemptStateTransition(ConsentStatus.EXPLICIT_NO, consentChangeReason);
        }

        @Override
        public void onInvalidateConsent(@Nullable final String consentChangeReason) {
            if (TextUtils.isEmpty(consentChangeReason)) {
                attemptStateTransition(ConsentStatus.UNKNOWN,
                        ConsentChangeReason.REACQUIRE_BY_SERVER);
                return;
            }
            attemptStateTransition(ConsentStatus.UNKNOWN, consentChangeReason);
        }

        @Override
        public void onReacquireConsent(@Nullable final String consentChangeReason) {
            if (!TextUtils.isEmpty(consentChangeReason)) {
                mPersonalInfoData.setConsentChangeReason(consentChangeReason);
            }
            mPersonalInfoData.setShouldReacquireConsent(true);
            mPersonalInfoData.writeToDisk();
        }

        @Override
        public void onForceGdprApplies() {
            forceGdprApplies();
        }

        @Override
        public void onRequestSuccess(@Nullable final String adUnitId) {
            // Cache the ad unit if the ad request succeeded
            if (!TextUtils.isEmpty(mPersonalInfoData.getAdUnitId()) ||
                    TextUtils.isEmpty(adUnitId)) {
                return;
            }
            mPersonalInfoData.setAdUnit(adUnitId);
            mPersonalInfoData.writeToDisk();
        }
    }

    @NonNull
    @Deprecated
    @VisibleForTesting
    PersonalInfoData getPersonalInfoData() {
        return mPersonalInfoData;
    }

    @NonNull
    @Deprecated
    @VisibleForTesting
    MultiAdResponse.ServerOverrideListener getServerOverrideListener() {
        return mServerOverrideListener;
    }
}
