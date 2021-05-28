package com.backpackingmap.backpackingmap

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.compose.ui.platform.ComposeView
import com.backpackingmap.backpackingmap.ui.Main
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first

class MainActivity : ComponentActivity() {
    private val fineLocationStatus = MutableStateFlow(PermissionStatus.Unchecked)
    private lateinit var requestFineLocationLauncher: ActivityResultLauncher<String>
    private lateinit var composeView: ComposeView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestFineLocationLauncher =
            registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
                if (isGranted) {
                    fineLocationStatus.value = PermissionStatus.Granted
                } else {
                    fineLocationStatus.value = PermissionStatus.Denied
                }
            }

        composeView = ComposeView(this).apply {
            setContent {
                Main(::ensureFineLocation)
            }
        };
        setContentView(composeView)
    }

    /** Returns if the permission is granted */
    private suspend fun ensureFineLocation(): Boolean {
        when (fineLocationStatus.value) {
            PermissionStatus.Denied -> {
                fineLocationDeniedMsg()
                return false
            }
            PermissionStatus.Granted -> return true
            PermissionStatus.Unchecked -> Unit
        }

        val permission = Manifest.permission.ACCESS_FINE_LOCATION

        if (checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED) {
            return true
        }

        if (shouldShowRequestPermissionRationale(permission)) {
            if (!fineLocationRationalePrompt()) {
                fineLocationDeniedMsg()
                return false
            }
        }

        requestFineLocationLauncher.launch(permission)

        return when (fineLocationStatus.value) {
            PermissionStatus.Denied -> {
                fineLocationDeniedMsg()
                false
            }
            PermissionStatus.Granted -> true
            PermissionStatus.Unchecked -> {
                when (fineLocationStatus.first { it != PermissionStatus.Unchecked }) {
                    PermissionStatus.Denied -> {
                        fineLocationDeniedMsg()
                        false
                    }
                    PermissionStatus.Granted -> true
                    PermissionStatus.Unchecked -> throw IllegalStateException("Unreachable")
                }
            }
        }
    }

    private suspend fun fineLocationRationalePrompt(): Boolean {
        val status = CompletableDeferred<Boolean>()

        AlertDialog.Builder(this)
            .setTitle(R.string.location_permission)
            .setMessage(R.string.location_permission_rationale)
            .setPositiveButton(R.string.ok) { _, _ ->
                status.complete(true)
            }
            .setNegativeButton(R.string.cancel) { _, _ ->
                status.complete(false)
            }
            .create()
            .show()

        return status.await()
    }

    private fun fineLocationDeniedMsg() {
        Snackbar.make(composeView, R.string.fine_location_denied, Snackbar.LENGTH_LONG)
            .show()
    }
}

enum class PermissionStatus {
    Unchecked,
    Denied,
    Granted,
}
