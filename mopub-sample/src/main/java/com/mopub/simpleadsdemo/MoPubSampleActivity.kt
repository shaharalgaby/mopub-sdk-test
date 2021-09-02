// Copyright 2018-2021 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// https://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.simpleadsdemo

import android.Manifest.permission
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.webkit.WebView

import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.caller.notes.BuildConfig
import com.caller.notes.R
import com.facebook.ads.AdSettings

import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.material.navigation.NavigationView

import com.mopub.common.Constants
import com.mopub.common.MoPub
import com.mopub.common.SdkConfiguration
import com.mopub.common.SdkInitializationListener
import com.mopub.common.logging.MoPubLog
import com.mopub.common.logging.MoPubLog.SdkLogEvent
import com.mopub.common.privacy.ConsentDialogListener
import com.mopub.common.privacy.ConsentStatus
import com.mopub.common.privacy.ConsentStatusChangeListener
import com.mopub.common.util.DeviceUtils
import com.mopub.mobileads.MoPubConversionTracker
import com.mopub.mobileads.MoPubErrorCode
import com.mopub.network.ImpressionListener
import com.mopub.network.ImpressionsEmitter
import com.mopub.simpleadsdemo.qrcode.BarcodeCaptureActivity

import org.json.JSONException

import java.util.ArrayList
import java.util.concurrent.LinkedBlockingDeque

class MoPubSampleActivity : AppCompatActivity(),
    NavigationView.OnNavigationItemSelectedListener {
    companion object {
        private val REQUIRED_DANGEROUS_PERMISSIONS: MutableList<String> =
            ArrayList()
        private const val SHOWING_CONSENT_DIALOG_KEY = "ShowingConsentDialog"
        private const val RC_BARCODE_CAPTURE = 9001

        /*
        MoPub Sample specific test code
        */
        private const val PRIVACY_FRAGMENT_TAG = "privacy_info_fragment"
        private const val NETWORKS_FRAGMENT_TAG = "networks_info_fragment"
        private const val LIST_FRAGMENT_TAG = "list_fragment"
        private const val IMPRESSIONS_FRAGMENT_TAG = "impressions_info_fragment"

        init {
            REQUIRED_DANGEROUS_PERMISSIONS.add(permission.ACCESS_COARSE_LOCATION)
            REQUIRED_DANGEROUS_PERMISSIONS.add(permission.CAMERA)
            // Sample app web views are debuggable.
            WebView.setWebContentsDebuggingEnabled(true)
        }
    }

    private lateinit var moPubListFragment: MoPubListFragment
    private var deeplinkIntent: Intent? = null
    private var showingConsentDialog = false
    private lateinit var drawerLayout: DrawerLayout
    private var consentStatusChangeListener: ConsentStatusChangeListener? = null
    private val impressionsList = LinkedBlockingDeque<String>()
    private lateinit var impressionListener: ImpressionListener

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        findViewById<Toolbar>(R.id.toolbar).let {
            setSupportActionBar(it)
            it.title = getString(R.string.app_title)
            setupNavigationDrawer(it)
        }
        val permissionsToBeRequested: MutableList<String> = ArrayList()
        for (permission in REQUIRED_DANGEROUS_PERMISSIONS) {
            if (!DeviceUtils.isPermissionGranted(this, permission)) {
                permissionsToBeRequested.add(permission)
            }
        }

        // Request dangerous permissions
        if (permissionsToBeRequested.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                this,
                permissionsToBeRequested.toTypedArray(),
                Constants.UNUSED_REQUEST_CODE
            )
        }

        // Set location awareness and precision globally for your app:
        MoPub.setLocationAwareness(MoPub.LocationAwareness.TRUNCATED)
        MoPub.setLocationPrecision(4)
        if (savedInstanceState == null) {
            createMoPubListFragment(intent)
        } else {
            showingConsentDialog = savedInstanceState.getBoolean(SHOWING_CONSENT_DIALOG_KEY)
        }
        SdkConfiguration.Builder(getString(R.string.ad_unit_id_native)).let {
            if (BuildConfig.DEBUG) {
                it.withLogLevel(MoPubLog.LogLevel.DEBUG)
            } else {
                it.withLogLevel(MoPubLog.LogLevel.INFO)
            }
            SampleActivityUtils.addDefaultNetworkConfiguration(it)
            MoPub.initializeSdk(this, it.build(), initSdkListener())
            AdSettings.setTestMode(true)
        }
        consentStatusChangeListener = initConsentChangeListener().also {
            MoPub.getPersonalInformationManager()?.subscribeConsentStatusChangeListener(it)
        }


        // Intercepts all logs including Level.FINEST so we can show a toast
        // that is not normally user-facing. This is only used for native ads.
        LoggingUtils.enableCanaryLogging(this)
        impressionListener = createImpressionsListener().also {
            ImpressionsEmitter.addListener(it)
        }
    }

    override fun onDestroy() {
        // unsubscribe or memory leak will occur
        MoPub.getPersonalInformationManager()
            ?.unsubscribeConsentStatusChangeListener(consentStatusChangeListener)
        consentStatusChangeListener = null
        ImpressionsEmitter.removeListener(impressionListener)
        super.onDestroy()
    }

    private fun createMoPubListFragment(intent: Intent) {
        moPubListFragment = MoPubListFragment().also {
            it.arguments = intent.extras
            supportFragmentManager.beginTransaction()
                .replace(
                    R.id.fragment_container,
                    it,
                    LIST_FRAGMENT_TAG
                ).commit()
        }
        deeplinkIntent = intent
    }

    public override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        deeplinkIntent = intent
    }

    public override fun onPostResume() {
        super.onPostResume()
        deeplinkIntent?.let {
            deeplinkIntent = moPubListFragment.addAdUnitViaDeeplink(it.data).run { null }

        }
    }

    private fun initSdkListener(): SdkInitializationListener {
        return SdkInitializationListener {
            syncNavigationMenu()
            Utils.logToast(this@MoPubSampleActivity, "SDK initialized.")
            MoPub.getPersonalInformationManager()?.let {
                if (it.shouldShowConsentDialog()) {
                    it.loadConsentDialog(initDialogLoadListener())
                }
            }
            MoPubConversionTracker(this@MoPubSampleActivity).reportAppOpen()
        }
    }

    private fun initConsentChangeListener(): ConsentStatusChangeListener {
        return ConsentStatusChangeListener { oldConsentStatus, newConsentStatus, canCollectPersonalInformation ->
            Utils.logToast(
                this@MoPubSampleActivity,
                "Consent: " + newConsentStatus.name
            )
            MoPub.getPersonalInformationManager()?.let {
                if (it.shouldShowConsentDialog()) {
                    it.loadConsentDialog(initDialogLoadListener())
                }
            }
        }
    }

    private fun initDialogLoadListener(): ConsentDialogListener {
        return object : ConsentDialogListener {
            override fun onConsentDialogLoaded() {
                MoPub.getPersonalInformationManager()?.showConsentDialog().also {
                    showingConsentDialog = true
                }
            }

            override fun onConsentDialogLoadFailed(moPubErrorCode: MoPubErrorCode) {
                Utils.logToast(
                    this@MoPubSampleActivity,
                    "Consent dialog failed to load."
                )
            }
        }
    }

    override fun onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    private fun setupNavigationDrawer(toolbar: Toolbar) {
        drawerLayout = findViewById<DrawerLayout>(R.id.drawer_layout).also {
            val toggle =
                ActionBarDrawerToggle(
                    this,
                    it,
                    toolbar,
                    R.string.navigation_drawer_open,
                    R.string.navigation_drawer_close
                )
            it.addDrawerListener(toggle)
            toggle.syncState()
        }

        val navigationView = findViewById<NavigationView>(R.id.nav_view)
        navigationView.setNavigationItemSelectedListener(this)
    }

    private fun syncNavigationMenu() {
        val navigationView =
            findViewById<NavigationView>(R.id.nav_view)
        SampleActivityInternalUtils.updateEndpointMenu(navigationView.menu)
        MoPub.getPersonalInformationManager()?.let {
            navigationView.menu.findItem(R.id.nav_force_gdpr).isChecked =
                it.consentData.isForceGdprApplies
            val consentStatus = it.personalInfoConsentStatus
            if (consentStatus == ConsentStatus.POTENTIAL_WHITELIST) {
                navigationView.menu.findItem(R.id.nav_privacy_grant).isChecked = true
            } else if (consentStatus == ConsentStatus.EXPLICIT_NO) {
                navigationView.menu.findItem(R.id.nav_privacy_revoke).isChecked = true
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_impressions -> {
                onImpressionsMenu()
                true
            }
            R.id.action_clear_logs -> {
                onClearLogs()
                true
            }
            R.id.qr_scan -> {
                onCaptureQrCode()
                true
            }
            else -> super.onContextItemSelected(item)
        }
    }

    override fun onNavigationItemSelected(menuItem: MenuItem): Boolean {
        SampleActivityInternalUtils.handleEndpointMenuSelection(menuItem)
        when (menuItem.itemId) {
            R.id.nav_privacy_info -> onNavPrivacyInfo()
            R.id.nav_privacy_grant -> onNavChangeConsent(true)
            R.id.nav_privacy_revoke -> onNavChangeConsent(false)
            R.id.nav_force_gdpr -> onNavForceGdpr()
            R.id.nav_adapters_info -> onNavAdaptersInfo()
        }
        syncNavigationMenu()
        drawerLayout.closeDrawers()
        return false
    }

    public override fun onSaveInstanceState(savedInstanceState: Bundle) {
        super.onSaveInstanceState(savedInstanceState)
        savedInstanceState.putBoolean(
            SHOWING_CONSENT_DIALOG_KEY,
            showingConsentDialog
        )
    }

    public override fun onResume() {
        super.onResume()
        if (showingConsentDialog) {
            showingConsentDialog = false
            Utils.logToast(
                this@MoPubSampleActivity,
                "Consent dialog dismissed"
            )
        }
    }

    private fun onImpressionsMenu() {
        supportFragmentManager.let {
            if (it.findFragmentByTag(IMPRESSIONS_FRAGMENT_TAG) == null) {
                it.beginTransaction()
                    .replace(
                        R.id.fragment_container,
                        ImpressionsInfoFragment.newInstance(ArrayList(impressionsList)),
                        IMPRESSIONS_FRAGMENT_TAG
                    )
                    .addToBackStack(IMPRESSIONS_FRAGMENT_TAG)
                    .commit()
            }
        }
    }

    private fun onNavPrivacyInfo() {
        supportFragmentManager.let {
            if (it.findFragmentByTag(PRIVACY_FRAGMENT_TAG) == null) {
                it.beginTransaction()
                    .replace(
                        R.id.fragment_container,
                        PrivacyInfoFragment(),
                        PRIVACY_FRAGMENT_TAG
                    )
                    .addToBackStack(PRIVACY_FRAGMENT_TAG)
                    .commit()
            }
        }
    }

    private fun onNavChangeConsent(grant: Boolean) {
        val listFragment =
            supportFragmentManager.findFragmentByTag(LIST_FRAGMENT_TAG) as MoPubListFragment?
        if (listFragment == null) {
            MoPubLog.log(
                SdkLogEvent.CUSTOM,
                getString(R.string.list_fragment_not_found)
            )
            return  // fragment is not ready to update the consent
        }
        if (!listFragment.onChangeConsent(grant)) {
            return  // fragment is not ready to update the consent
        }

        findViewById<NavigationView>(R.id.nav_view).let {
            it.menu.findItem(R.id.nav_privacy_grant).isChecked = grant
            it.menu.findItem(R.id.nav_privacy_revoke).isChecked = !grant
        }
    }

    private fun onNavForceGdpr() {
        MoPub.getPersonalInformationManager()?.forceGdprApplies()
    }

    private fun onNavAdaptersInfo() {
        supportFragmentManager.let {
            if (it.findFragmentByTag(NETWORKS_FRAGMENT_TAG) == null) {
                it.beginTransaction()
                    .replace(
                        R.id.fragment_container,
                        NetworksInfoFragment(),
                        NETWORKS_FRAGMENT_TAG
                    )
                    .addToBackStack(NETWORKS_FRAGMENT_TAG)
                    .commit()
            }
        }

    }

    private fun onCaptureQrCode(): Boolean {
        // launch barcode activity.
        Intent(this, BarcodeCaptureActivity::class.java).apply {
            this.putExtra(BarcodeCaptureActivity.AutoFocus, true)
            this.putExtra(BarcodeCaptureActivity.UseFlash, false)
        }.also {
            startActivityForResult(it, RC_BARCODE_CAPTURE)
        }
        return true
    }

    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {
        if (requestCode == RC_BARCODE_CAPTURE && resultCode == CommonStatusCodes.SUCCESS && data != null) {
            data.getParcelableExtra<Uri>(BarcodeCaptureActivity.DEEPLINK_URI_KEY)?.let { uri ->
                this.deeplinkIntent = Intent().apply {
                    this.data = uri
                }
                return@onActivityResult
            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    private fun onClearLogs() {
        (supportFragmentManager.findFragmentByTag(IMPRESSIONS_FRAGMENT_TAG) as ImpressionsInfoFragment?)?.onClear()
        impressionsList.clear()
    }

    private fun createImpressionsListener(): ImpressionListener {
        return ImpressionListener { adUnitId, impressionData ->
            MoPubLog.log(SdkLogEvent.CUSTOM, "impression for adUnitId: $adUnitId")
            if (impressionData == null) {
                impressionsList.addFirst("adUnitId: $adUnitId\ndata= null")
            } else {
                try {
                    impressionsList.addFirst(
                        impressionData.jsonRepresentation.toString(2)
                    )
                } catch (e: JSONException) {
                    MoPubLog.log(
                        SdkLogEvent.CUSTOM_WITH_THROWABLE,
                        "Can't format impression data.",
                        e
                    )
                }
            }
        }
    }
}
