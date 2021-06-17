package com.backpackingmap.backpackingmap.ui.screen.track

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.backpackingmap.backpackingmap.R
import com.backpackingmap.backpackingmap.model.TrackSettings
import com.backpackingmap.backpackingmap.repo.Repo
import com.backpackingmap.backpackingmap.track_service.TrackService
import com.backpackingmap.backpackingmap.ui.LocalActivity
import com.backpackingmap.backpackingmap.ui.view.ErrorMsg
import com.backpackingmap.backpackingmap.ui.view.ExposedDropdownMenu
import com.backpackingmap.backpackingmap.ui.view.ExposedDropdownMenuItem
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import kotlin.time.Duration

@Composable
fun TrackScreen(repo: Repo, bottomBar: @Composable () -> Unit) {
    Scaffold(bottomBar = bottomBar) { contentPadding ->
        Column(
            Modifier
                .padding(contentPadding)
                .padding(30.dp, 15.dp)
        ) {
            TrackInner(repo)
        }
    }
}

@Composable
private fun TrackInner(repo: Repo) {
    val scope = rememberCoroutineScope()
    val activity = LocalActivity.current!!
    val context = LocalContext.current

    val trackSettingsState = repo.trackSettings().collectAsState(null)
    val trackSettings = trackSettingsState.value

    val activeTrackState = repo.activeTrack().collectAsState(null)
    val activeTrack = activeTrackState.value

    val playServicesPreconditionMetState = remember { mutableStateOf<Boolean?>(null) }
    val settingsPreconditionMetState = remember { mutableStateOf<Boolean?>(null) }
    UpdatePreconditions(activity, playServicesPreconditionMetState, settingsPreconditionMetState)
    val playServicesPreconditionMet = playServicesPreconditionMetState.value
    val settingsPreconditionMet = settingsPreconditionMetState.value
    if (playServicesPreconditionMet == null || settingsPreconditionMet == null) {
        return
    }
    if (!playServicesPreconditionMet) {
        ErrorMsg(R.string.play_services_needed_for_tracking)
        return
    }
    if (!settingsPreconditionMet) {
        ErrorMsg(R.string.location_settings_needed_for_tracking)
        return
    }

    if (trackSettings == null) {
        return
    }

    EditSettingsSection(trackSettings, { scope.launch { repo.setTrackSettings(it) } })

    if (activeTrack == null) {
        Button(onClick = {
            scope.launch {
                repo.beginTrack()
            }
        }) {
            Text(stringResource(R.string.start))
        }
    } else {
        OutlinedButton(onClick = {
            scope.launch {
                repo.endTrack(activeTrack)
            }
        }) {
            Text(stringResource(R.string.stop))
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
private fun EditSettingsSection(
    trackSettings: TrackSettings,
    setTrackSettings: (TrackSettings) -> Unit,
    modifier: Modifier = Modifier
) {
    val isExpanded = remember { mutableStateOf(false) }
    Column(modifier.padding(bottom = 10.dp)) {
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(3.dp))
                .toggleable(isExpanded.value) { isExpanded.value = it }
                .padding(5.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (isExpanded.value) {
                Icon(
                    painterResource(R.drawable.ic_expand_less),
                    stringResource(R.string.collapse)
                )
            } else {
                Icon(
                    painterResource(R.drawable.ic_expand_more),
                    stringResource(R.string.expand)
                )
            }

            Text(stringResource(R.string.settings), style = MaterialTheme.typography.button)
        }

        AnimatedVisibility(
            visible = isExpanded.value,
            enter = expandVertically(expandFrom = Alignment.Top),
            exit = shrinkVertically(shrinkTowards = Alignment.Top),
        ) {
            Column(Modifier.padding(5.dp)) {
                EditInterval(trackSettings, setTrackSettings)
            }
        }
    }
}

@Composable
private fun EditInterval(trackSettings: TrackSettings, setTrackSettings: (TrackSettings) -> Unit) {
    val presetIntervals = listOf(
        ExposedDropdownMenuItem(Duration.seconds(30), stringResource(R.string.thirty_secs)),
        ExposedDropdownMenuItem(Duration.minutes(1), stringResource(R.string.one_min)),
        ExposedDropdownMenuItem(Duration.minutes(5), stringResource(R.string.five_min)),
        ExposedDropdownMenuItem(Duration.minutes(15), stringResource(R.string.fifteen_min)),
        ExposedDropdownMenuItem(Duration.minutes(30), stringResource(R.string.thirty_min)),
        ExposedDropdownMenuItem(Duration.hours(1), stringResource(R.string.one_hour)),
    )

    if (presetIntervals.none { it.id == trackSettings.interval }) {
        throw IllegalStateException("TrackSettings interval must be a preset")
    }

    Column {
        Row {
            ExposedDropdownMenu(
                stringResource(R.string.update_interval),
                values = presetIntervals,
                selected = trackSettings.interval,
                onSelect = { setTrackSettings(trackSettings.copy(interval = it)) }
            )
        }
    }
}

@Composable
private fun UpdatePreconditions(
    activity: Activity,
    playServicesPreconditionMetState: MutableState<Boolean?>,
    settingsPreconditionMetState: MutableState<Boolean?>
) {
    val settingsResolver =
        rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) {
            settingsPreconditionMetState.value = it.resultCode == Activity.RESULT_OK
        }

    LaunchedEffect(activity) {
        // Check for google play services
        val gps = GoogleApiAvailability.getInstance()
        if (gps.isGooglePlayServicesAvailable(activity) == ConnectionResult.SUCCESS) {
            playServicesPreconditionMetState.value = true
        } else {
            playServicesPreconditionMetState.value = false
            try {
                gps.makeGooglePlayServicesAvailable(activity).await()
            } catch (e: Exception) {
                Timber.w(e, "Failed to make play services available")
                playServicesPreconditionMetState.value = false
            }
        }

        // Check for location related settings
        val locationRequest = LocationRequest.create().apply {
            interval = TrackService.FASTEST_UPDATE_INTERVAL_MILLIS
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }

        val settingsRequest = LocationSettingsRequest.Builder()
            .addLocationRequest(locationRequest)
            .build()

        val settingsClient = LocationServices.getSettingsClient(activity)

        try {
            settingsClient.checkLocationSettings(settingsRequest).await()
            settingsPreconditionMetState.value = true
        } catch (e: ResolvableApiException) {
            settingsPreconditionMetState.value = false
            val req = IntentSenderRequest.Builder(e.resolution).build()
            settingsResolver.launch(req)
        } catch (e: ApiException) {
            settingsPreconditionMetState.value = false
        }
    }
}
