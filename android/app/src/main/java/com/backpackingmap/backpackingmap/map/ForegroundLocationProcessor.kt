package com.backpackingmap.backpackingmap.map

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.pm.PackageManager
import android.location.Location
import android.os.Looper
import androidx.core.app.ActivityCompat
import arrow.core.Either
import com.backpackingmap.backpackingmap.GPSLocation
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.location.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.CoroutineContext

/**
 * @param _launchPermissionRequest On call should request ACCESS_FINE_LOCATION, and the result
 * should be sent to the onPermissionResult of the corresponding instance of
 * ForegroundLocationProcessor
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ForegroundLocationProcessor(
    private val activity: Activity,
    private val _launchPermissionRequest: () -> Unit,
    override val coroutineContext: CoroutineContext,
) : CoroutineScope, LocationCallback() {
    private val _location = MutableStateFlow<GPSLocation>(GPSLocation.Unknown)
    val location get() = _location.asStateFlow()

    data class Client(
        val client: FusedLocationProviderClient,
        val attached: Boolean,
    )

    private var client: Client? = null

    private var permissionRecentlyDenied = AtomicBoolean(false)

    init {
        _location.subscriptionCount
            .map { count -> count > 0 }
            .distinctUntilChanged()
            .onEach { isActive ->
                if (isActive) {
                    tryMakeAttached()
                } else {
                    makeDetached()
                }
            }
            .launchIn(this)
    }

    /** Idempotently attempt to attach to a location provider client and get location every
     * UPDATE_INTERVAL
     */
    private suspend fun tryMakeAttached() {
        Timber.i("Trying to attach")

        if (!hasLocationPermission()) {
            Timber.w("Not attaching as no location permission")
            _location.value = GPSLocation.Error.PermissionNotGranted
            return
        }

        val cached = client
        if (cached != null) {
            if (cached.attached) {
                Timber.i("Already attached")
            } else {
                attachToClient(cached.client)
                Timber.i("Attaching to existing client")
            }
            return
        }

        when (val result = getClient()) {
            is Either.Left -> {
                _location.value = GPSLocation.Error.PlayServicesUnavailable(result.a)
            }

            is Either.Right -> {
                attachToClient(result.b)
            }
        }
    }

    /** Idempotently ensure detached and no longer getting regular update. */
    private fun makeDetached() {
        Timber.w("Detaching")
        if (client == null || client?.attached == false) {
            return
        }

        val cached = client
        if (cached != null && cached.attached) {
            cached.client.removeLocationUpdates(this@ForegroundLocationProcessor)
            client = Client(cached.client, false)
        }
    }

    fun launchPermissionRequest() {
        Timber.i("Launching permission request")
        _launchPermissionRequest()
    }

    fun onPermissionResult(permissions: Array<out String>, grantResults: IntArray) {
        val i = permissions.indexOf(Manifest.permission.ACCESS_FINE_LOCATION)
        val result = grantResults.getOrNull(i)

        if (result == null || result != PackageManager.PERMISSION_GRANTED) {
            Timber.w("Permission denied")
            permissionRecentlyDenied.set(true)
        } else {
            launch {
                tryMakeAttached()
            }
        }
    }

    private suspend fun getClient(): Either<Throwable, FusedLocationProviderClient> =
        // because these play services fns must be called from the main thread
        withContext(Dispatchers.Main) {
            val api = GoogleApiAvailability.getInstance()
            val task = api.makeGooglePlayServicesAvailable(activity)
            try {
                task.await()
            } catch (e: Throwable) {
                Timber.w(e, "Failed to get play services")
                Either.left(e)
            }
            Either.right(LocationServices.getFusedLocationProviderClient(activity))
        }

    private fun hasLocationPermission(): Boolean {
        // Based on <https://medium.com/google-developer-experts/exploring-android-q-location-permissions-64d312b0e2e1>
        return ActivityCompat.checkSelfPermission(activity,
            Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }

    /** Pre-condition: Caller must only call if hasLocationPermission() and if clientMutex locked */
    @SuppressLint("MissingPermission")
    private fun attachToClient(cachedClient: FusedLocationProviderClient) {
        // Get cached
        launch {
            // NOTE: Annotated as non-null, but documentation says can be null
            val platform: Location? = cachedClient.lastLocation.await()
            val loc = GPSLocation.fromPlatform(platform)
            // only use a cached value if we don't have a more recent one
            val current = location.value
            // NOTE: Since timestamp isn't monotonic, this will pick the wrong value in the case of
            // system time updates.
            if (current is GPSLocation.Known && loc is GPSLocation.Known &&
                current.timestamp < loc.timestamp
            ) {
                Timber.i("Got cached location to initialize")
                _location.value = loc
            } else {
                Timber.i("Rejecting cached location as we have more recent")
            }
        }

        // Get current
        launch {
            // NOTE: Annotated as non-null, but documentation says can be null
            val loc: Location? =
                cachedClient.getCurrentLocation(LocationRequest.PRIORITY_HIGH_ACCURACY, null)
                    .await()
            _location.value = GPSLocation.fromPlatform(loc)
            Timber.i("Got current location to initialize")
        }

        // Get updates
        val request = LocationRequest.create().apply {
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
            interval = UPDATE_INTERVAL
        }
        cachedClient.requestLocationUpdates(request, this, Looper.getMainLooper())

        client = Client(cachedClient, true)
    }

    override fun onLocationAvailability(availability: LocationAvailability?) {
        val available = availability?.isLocationAvailable ?: return

        if (available && _location.value is GPSLocation.Error) {
            _location.value = GPSLocation.Unknown
        } else if (!available) {
            _location.value = GPSLocation.Error.LocationUnavailable
        }
    }

    override fun onLocationResult(result: LocationResult?) {
        val loc = result?.lastLocation ?: return
        _location.value = GPSLocation.fromPlatform(loc)
    }

    companion object {
        private const val UPDATE_INTERVAL = 5L // in seconds
    }
}
