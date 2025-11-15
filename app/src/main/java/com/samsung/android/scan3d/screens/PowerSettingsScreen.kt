package com.samsung.android.scan3d.screens

import android.content.Context
import android.content.Intent
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.samsung.android.scan3d.R
import com.samsung.android.scan3d.fragments.AnimatedSystemSwitch
import com.samsung.android.scan3d.fragments.AutoDimDialog
import com.samsung.android.scan3d.fragments.SettingsClickableRow
import com.samsung.android.scan3d.fragments.SettingsGroup
import com.samsung.android.scan3d.serv.Cam
import com.samsung.android.scan3d.util.SettingsManager
import androidx.compose.material.icons.rounded.SettingsEthernet
import com.samsung.android.scan3d.fragments.PortSettingDialog
import androidx.core.net.toUri
import androidx.compose.ui.text.font.FontWeight


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

    @Composable
    fun getDimDelaySummary(delayMs: Int): String {
        return when (delayMs) {
            SettingsManager.DIM_DELAY_45S -> stringResource(R.string.settings_power_auto_dim_45s)
            SettingsManager.DIM_DELAY_1M -> stringResource(R.string.settings_power_auto_dim_1m)
            SettingsManager.DIM_DELAY_90S -> stringResource(R.string.settings_power_auto_dim_90s)
            SettingsManager.DIM_DELAY_2M -> stringResource(R.string.settings_power_auto_dim_2m)
            SettingsManager.DIM_DELAY_3M -> stringResource(R.string.settings_power_auto_dim_3m)
            SettingsManager.DIM_DELAY_5M -> stringResource(R.string.settings_power_auto_dim_5m)
            else -> stringResource(R.string.settings_power_auto_dim_off)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_power_title), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    FilledTonalIconButton(
                        onClick = onBackClicked,
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainer,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.settings_back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SettingsGroup(title = stringResource(R.string.settings_power_title)) {

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
                    onDispose {
                        lifecycleOwner.lifecycle.removeObserver(observer)
                    }
                }

                SettingsToggleRow(
                    text = stringResource(R.string.settings_power_ignore_optimizations),
                    summary = stringResource(R.string.settings_power_ignore_optimizations_desc),
                    icon = Icons.Rounded.BatteryChargingFull,
                    checked = isIgnoringOptimizations,
                    onCheckedChange = { newState ->
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

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                SettingsClickableRow(
                    text = stringResource(R.string.settings_power_auto_dim),
                    icon = Icons.Rounded.BrightnessLow,
                    summary = getDimDelaySummary(currentDimDelay),
                    onClick = { showAutoDimDialog = true }
                )

                SettingsToggleRow(
                    text = stringResource(R.string.settings_power_lock_input),
                    summary = stringResource(R.string.settings_power_lock_input_desc),
                    icon = Icons.Rounded.Lock,
                    checked = lockInput,
                    onCheckedChange = {
                        lockInput = it
                        SettingsManager.saveLockInputOnDim(context, it)
                    }
                )
            }

            SettingsGroup(title = stringResource(R.string.settings_behavior_title)) {
                SettingsToggleRow(
                    text = stringResource(R.string.settings_power_background_streaming),
                    summary = stringResource(R.string.settings_power_background_streaming_desc),
                    icon = Icons.Rounded.PictureInPicture,
                    checked = backgroundStreaming,
                    onCheckedChange = {
                        backgroundStreaming = it
                        SettingsManager.saveBackgroundStreaming(context, it)
                    }
                )

                SettingsToggleRow(
                    text = stringResource(R.string.settings_power_allow_reconnects),
                    summary = stringResource(R.string.settings_power_allow_reconnects_desc),
                    icon = Icons.Rounded.Sync,
                    checked = allowReconnects,
                    onCheckedChange = {
                        allowReconnects = it
                        SettingsManager.saveAllowReconnects(context, it)
                    }
                )

                SettingsGroup(title = stringResource(R.string.settings_network_title)) {
                    SettingsClickableRow(
                        text = stringResource(R.string.settings_port_title),
                        summary = stringResource(R.string.settings_port_summary),
                        icon = Icons.Rounded.SettingsEthernet,
                        subSummary = currentPort.toString(),
                        onClick = { showPortDialog = true }
                    )
                }
            }
        }
    }

    if (showAutoDimDialog) {
        AutoDimDialog(
            currentDelay = currentDimDelay,
            onDismiss = { showAutoDimDialog = false },
            onDelaySelected = { newDelay ->
                currentDimDelay = newDelay
                SettingsManager.saveAutoDimDelay(context, newDelay)
            }
        )
    }
    if (showPortDialog) {
        PortSettingDialog(
            currentPort = currentPort,
            onDismiss = { showPortDialog = false },
            onSave = { newPort ->
                currentPort = newPort
                SettingsManager.savePort(context, newPort)

                val intent = Intent(context, Cam::class.java).apply {
                    action = "set_http_port"
                    putExtra("port", newPort)
                }
                context.startService(intent)
            }
        )
    }
}

@Composable
fun SettingsToggleRow(
    text: String,
    summary: String,
    icon: ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .clip(RoundedCornerShape(8.dp))
            .padding(vertical = 8.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = text, modifier = Modifier.padding(end = 16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Column(modifier = Modifier.weight(1f)) {
            Text(text, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = summary,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        AnimatedSystemSwitch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}
