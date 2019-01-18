package register.before.production.playfinity.raw

import android.Manifest
import android.arch.lifecycle.Observer
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.provider.Settings
import android.support.annotation.RawRes
import android.support.design.widget.Snackbar
import android.support.transition.AutoTransition
import android.support.transition.TransitionManager
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.View
import android.view.WindowManager
import io.playfinity.sdk.PlayfinitySDK
import io.playfinity.sdk.SensorEvent
import io.playfinity.sdk.SensorEventType
import io.playfinity.sdk.bluetooth.PFIBluetoothManager
import io.playfinity.sdk.callbacks.DiscoverSensorListener
import io.playfinity.sdk.device.Sensor
import io.playfinity.sdk.device.SensorEventsSubscriber
import io.playfinity.sdk.errors.PlayfinityThrowable
import io.playfinity.sdk.sound.PlayfinitySoundManager
import io.playfinity.sdk.sound.PlayfinitySoundSettings
import io.playfinity.sdk.sound.PlayfinitySoundVolume
import kotlinx.android.synthetic.main.activity_main.*
import java.util.*
import java.util.concurrent.TimeUnit


class MainActivity : AppCompatActivity(),
    DiscoverSensorListener,
    SensorEventsSubscriber,
    PlayfinitySoundManager.PlayfinitySoundManagerListener {

    private val apiServices by lazy { NetworkClient.getApi("https://testapps001.playfinity.io/") }
    private val myApiManager: BellApiManager by lazy { BellApiManager(apiServices) }

    private var playfinitySDK: PlayfinitySDK? = null
    private val pfiBluetoothManager: PFIBluetoothManager?
        get() = playfinitySDK?.getBluetoothManager()

    private var sensor: Sensor? = null
    private var isConnectedToSensor = false

    private val handler = Handler()
    private val updateListTask = Runnable { }

    private var counter: Int = 0
        get() {
            field += 1
            executors.mainThread().execute {
                samples_tv.text = samples_text.format(field)
            }
            return field
        }
        set(value) {
            field = value
            executors.mainThread().execute {
                samples_tv.text = samples_text.format(field)
            }
        }
    private var collectRecords = true

    private val bleStateReceiver: BroadcastReceiver =
        object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val action = intent.action

                if (action == BluetoothAdapter.ACTION_STATE_CHANGED) {
                    val state = intent.getIntExtra(
                        BluetoothAdapter.EXTRA_STATE,
                        BluetoothAdapter.ERROR
                    )
                    when (state) {
                        BluetoothAdapter.STATE_ON -> onBleReady()
                    }
                }
            }
        }


    private val playfinitySoundManager: PlayfinitySoundManager by lazy {
        PlayfinitySoundManager.create(App.getApp(this), this)
    }
    private val dingDongSound by lazy {
        createSoundSettings(
            R.raw.dingdong1,
            PlayfinitySoundVolume.high()
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        status_tv.text = "Loading Playfinity SDK"
        App.getApp(this).getPlayfinitySdkLiveData().observe(this, Observer { playfinitySDK ->
            if (playfinitySDK != null) {
                this.playfinitySDK = playfinitySDK
                status_tv.text = "Playfinity SDK is Ready"
                handelSensorDiscovery()
            } else {
                status_tv.text = "Loading Playfinity SDK"
            }
        })

        setupUi()
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterBleReceiver()

        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    override fun onResume() {
        super.onResume()

        if (canStartBleScanner()) {
            onBleReady()
        }

        unregisterBallScanner()
        handler.removeCallbacks(updateListTask)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode) {
            SHARE_REQUEST -> {
                collectRecords = true
                fab.isEnabled = true
                progress_bar.visibility = View.GONE

                sensor?.subscribeToEvents(this)

            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            LOCATION_PERMISSION_REQUEST -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    onLocationPermissionGranted()
                } else {
                    onLocationPermissionDenied()
                }
            }
            else -> {
                super.onRequestPermissionsResult(requestCode, permissions, grantResults)
            }
        }
    }

    override fun onSensorDiscovered(sensor: Sensor) {
        pfiBluetoothManager?.removeSensorDiscoverListener(this)
        TransitionManager.beginDelayedTransition(root, AutoTransition().apply { duration = 200 })
        status_tv.text = "Door Bell Found! Press red connect button to pair."
        this.sensor = sensor
        sensor_header_layout.visibility = View.VISIBLE
        separator_1.visibility = View.VISIBLE
        sensor_name_tv.text = "Name: %s".format(sensor.givenName)
        sensor_firmware_tv.text = "Firmware: %s".format(sensor.firmwareVersion)
        counter = 0

        fab.show()
    }

    override fun onSensorEvent(event: SensorEvent) {
        if (event.eventType != SensorEventType.ButtonTop) {
            return
        }

        counter++
        status_tv.text = "Event: " + event.eventType + " " + counter

        myApiManager.chime(event.sensorId, "A89191CBBBAA")

        playfinitySoundManager.play(dingDongSound)
    }

    override fun onSensorDiscoverError(playfinityThrowable: PlayfinityThrowable) {

    }

    override fun onLanguageDataMissing() {

    }

    private fun setupUi() {
        fab.setOnClickListener {
            val foundSensor = sensor
            val bluetoothManager = pfiBluetoothManager
            if (foundSensor == null || bluetoothManager == null) {
                return@setOnClickListener
            }

            isConnectedToSensor = !isConnectedToSensor

            fab.setImageResource(if (isConnectedToSensor) R.drawable.ic_bluetooth_connected_white_24dp else R.drawable.ic_bluetooth_disabled_white_24dp)
            list_header_group.visibility = View.GONE

            if (isConnectedToSensor) {
                status_tv.text = "Door Bell is ready"
                sensor?.subscribeToEvents(this)
            } else {
                sensor?.unSubscribeEvents(this)
                status_tv.text = "Door Bell Found! Press red connect button to pair."
            }
        }

        handler.postDelayed(updateListTask, updateListInterval)

        list_header_group.visibility = View.GONE

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    private fun handelSensorDiscovery() {
        if (App.isAtLestM()) {
            if (hasPermission(Manifest.permission.ACCESS_COARSE_LOCATION)) {
                onLocationPermissionGranted()
            } else {
                requestLocationPermissions()
            }
        } else {
            onLocationPermissionGranted()
        }
    }

    /**
     * Stops ball discover event
     */
    private fun unregisterBallScanner() {
        sensor?.unSubscribeEvents(this)
        pfiBluetoothManager?.run {
            removeSensorDiscoverListener(this@MainActivity)
            stopScanner()
        }
    }

    /**
     * Checks if ble scanner can be started
     */
    private fun canStartBleScanner(): Boolean {
        return isBluetoothEnabled() && hasLocationPermissions()
    }

    private fun onLocationPermissionGranted() {
        if (isBluetoothEnabled()) {
            onBleReady()
        } else {
            onBluetoothNotEnabled()
        }
    }

    private fun hasLocationPermissions(): Boolean {
        return if (App.isAtLestM()) {
            hasPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
        } else {
            true
        }
    }

    private fun onLocationPermissionDenied() {
        AlertDialog.Builder(this)
            .setTitle("Warning")
            .setMessage("Location permission\nis required for\nble scanning")
            .setPositiveButton("Got it") { _, _ ->

            }
            .create()
            .show()
    }

    private fun onBluetoothNotEnabled() {
        AlertDialog.Builder(this)
            .setTitle("Warning")
            .setMessage("You must turn on\nBluetooth to\nDiscover the\nball.")
            .setPositiveButton("Got it") { _, _ ->
                val filter = IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
                registerReceiver(bleStateReceiver, filter)
            }
            .create()
            .show()
    }

    private fun requestLocationPermissions() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        ) {
            Snackbar.make(
                root,
                "Location permission\nis required for\nble scanning",
                Snackbar.LENGTH_LONG
            )
                .setAction("Grant") { goToAppSettings() }
                .show()
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ),
                LOCATION_PERMISSION_REQUEST
            )
        }
    }

    private fun onBleReady() {
        if (!hasLocationPermissions()) {
            return
        }

        unregisterBleReceiver()

        executors.mainThread().execute {
            pfiBluetoothManager?.run {

                runOnUiThread {
                    status_tv.text = "Press Door Bell to wake it up"
                }
                addSensorDiscoverListener(this@MainActivity)
                startScanner(false)
            }
        }
    }

    private fun unregisterBleReceiver() = try {
        unregisterReceiver(bleStateReceiver)
    } catch (e: IllegalArgumentException) {
        //ignore
    }

    private fun isBluetoothEnabled(): Boolean {
        val mBluetoothAdapter =
            (getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager?)?.adapter
        return mBluetoothAdapter?.isEnabled ?: false
    }

    private fun goToAppSettings() {
        val intent = Intent()
        intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
        val uri = Uri.fromParts("package", packageName, null)
        intent.data = uri
        startActivity(intent)
    }

    private fun hasPermission(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            permission
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun createSoundSettings(
        @RawRes soundId: Int,
        volume: PlayfinitySoundVolume,
        queuedPlay: Boolean = false,
        looped: Boolean = false,
        willBlockPlaybackUntilFinished: Boolean = false
    ) =
        PlayfinitySoundSettings(
            UUID.randomUUID().toString(),
            Uri.parse("android.resource://$packageName/$soundId"),
            soundId,
            volume = volume,
            queuedPlay = queuedPlay,
            looped = looped,
            willBlockPlaybackUntilFinished = willBlockPlaybackUntilFinished
        )

    companion object {
        val executors = AppExecutors()

        const val TAG = "MainActivity"

        private const val samples_text = "Samples: %d"
        private const val LOCATION_PERMISSION_REQUEST = 10
        private const val SHARE_REQUEST = 11
        private val updateListInterval = TimeUnit.SECONDS.toMillis(3)
    }
}
