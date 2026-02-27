package com.samsung.android.scan3d.screens

import android.content.Context
import android.content.Intent
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.core.net.toUri
import com.samsung.android.scan3d.R
import com.samsung.android.scan3d.fragments.*
import com.samsung.android.scan3d.serv.Cam
import com.samsung.android.scan3d.util.SettingsManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PowerSettingsScreen(
    onBackClicked: () -> Unit
) {
    val context = LocalContext.current

    var showAutoDimDialog by remember { mutableStateOf(false) }
    var showPortDialog by remember { mutableStateOf(false) }

    var currentPort by remember { mutableIntStateOf(SettingsManager.loadPort(context)) }
    var currentDimDelay by remember { mutableIntStateOf(SettingsManager.loadAutoDimDelay(context)) }
    var lockInput by remember { mutableStateOf(SettingsManager.loadLockInputOnDim(context)) }
    var backgroundStreaming by remember { mutableStateOf(SettingsManager.loadBackgroundStreaming(context)) }
    var allowReconnects by remember { mutableStateOf(SettingsManager.loadAllowReconnects(context)) }

    val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    val lifecycleOwner = LocalLifecycleOwner.current

    var isIgnoringOptimizations by remember {
        mutableStateOf(powerManager.isIgnoringBatteryOptimizations(context.packageName))
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isIgnoringOptimizations = powerManager.isIgnoringBatteryOptimizations(context.packageName)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    if (showAutoDimDialog) {
        AutoDimDialog(currentDimDelay, onDismiss = { showAutoDimDialog = false }) { newDelay ->
            currentDimDelay = newDelay; SettingsManager.saveAutoDimDelay(context, newDelay)
        }
    }

    if (showPortDialog) {
        PortSettingDialog(currentPort, onDismiss = { showPortDialog = false }) { newPort ->
            currentPort = newPort; SettingsManager.savePort(context, newPort)
            val intent = Intent(context, Cam::class.java).apply {
                action = "set_http_port"
                putExtra("port", newPort)
            }
            context.startService(intent)
        }
    }

    SettingsScaffold(
        title = stringResource(R.string.settings_power_title),
        onBackClick = onBackClicked
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = padding.calculateTopPadding(), bottom = 32.dp)
        ) {
            item {
                SettingsGroup(
                    title = stringResource(R.string.settings_power_title),
                    items = listOf(
                        { shape ->
                            SettingsItem(
                                shape = shape,
                                title = stringResource(R.string.settings_power_ignore_optimizations),
                                subtitle = stringResource(R.string.settings_power_ignore_optimizations_desc),
                                icon = Icons.Rounded.BatteryChargingFull,
                                hasSwitch = true,
                                switchState = isIgnoringOptimizations,
                                onSwitchChange = { newState ->
                                    if (newState && !isIgnoringOptimizations) {
                                        val intent = Intent().apply {
                                            action = Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
                                            data = "package:${context.packageName}".toUri()
                                        }
                                        context.startActivity(intent)
                                    } else if (!newState && isIgnoringOptimizations) {
                                        val intent = Intent().apply {
                                            action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                                            data = "package:${context.packageName}".toUri()
                                        }
                                        context.startActivity(intent)
                                    }
                                }
                            )
                        },
                        { shape ->
                            SettingsItem(
                                shape = shape,
                                title = stringResource(R.string.settings_power_auto_dim),
                                subtitle = when (currentDimDelay) {
                                    SettingsManager.DIM_DELAY_45S -> stringResource(R.string.settings_power_auto_dim_45s)
                                    SettingsManager.DIM_DELAY_1M -> stringResource(R.string.settings_power_auto_dim_1m)
                                    SettingsManager.DIM_DELAY_90S -> stringResource(R.string.settings_power_auto_dim_90s)
                                    SettingsManager.DIM_DELAY_2M -> stringResource(R.string.settings_power_auto_dim_2m)
                                    SettingsManager.DIM_DELAY_3M -> stringResource(R.string.settings_power_auto_dim_3m)
                                    SettingsManager.DIM_DELAY_5M -> stringResource(R.string.settings_power_auto_dim_5m)
                                    else -> stringResource(R.string.settings_power_auto_dim_off)
                                },
                                icon = Icons.Rounded.BrightnessLow,
                                onClick = { showAutoDimDialog = true }
                            )
                        },
                        { shape ->
                            SettingsItem(
                                shape = shape,
                                title = stringResource(R.string.settings_power_lock_input),
                                subtitle = stringResource(R.string.settings_power_lock_input_desc),
                                icon = Icons.Rounded.Lock,
                                hasSwitch = true,
                                switchState = lockInput,
                                onSwitchChange = {
                                    lockInput = it
                                    SettingsManager.saveLockInputOnDim(context, it)
                                }
                            )
                        }
                    )
                )
            }

            item {
                SettingsGroup(
                    title = stringResource(R.string.settings_behavior_title),
                    items = listOf(
                        { shape ->
                            SettingsItem(
                                shape = shape,
                                title = stringResource(R.string.settings_power_background_streaming),
                                subtitle = stringResource(R.string.settings_power_background_streaming_desc),
                                icon = Icons.Rounded.PictureInPicture,
                                hasSwitch = true,
                                switchState = backgroundStreaming,
                                onSwitchChange = {
                                    backgroundStreaming = it
                                    SettingsManager.saveBackgroundStreaming(context, it)
                                }
                            )
                        },
                        { shape ->
                            SettingsItem(
                                shape = shape,
                                title = stringResource(R.string.settings_power_allow_reconnects),
                                subtitle = stringResource(R.string.settings_power_allow_reconnects_desc),
                                icon = Icons.Rounded.Sync,
                                hasSwitch = true,
                                switchState = allowReconnects,
                                onSwitchChange = {
                                    allowReconnects = it
                                    SettingsManager.saveAllowReconnects(context, it)
                                }
                            )
                        }
                    )
                )
            }

            item {
                SettingsGroup(
                    title = stringResource(R.string.settings_network_title),
                    items = listOf(
                        { shape ->
                            SettingsItem(
                                shape = shape,
                                title = stringResource(R.string.settings_port_title),
                                subtitle = currentPort.toString(),
                                icon = Icons.Rounded.SettingsEthernet,
                                onClick = { showPortDialog = true }
                            )
                        }
                    )
                )
            }
        }
    }
}