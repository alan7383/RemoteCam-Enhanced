package com.samsung.android.scan3d.screens

import android.annotation.SuppressLint
import android.graphics.Matrix
import android.graphics.RectF
import android.graphics.SurfaceTexture
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.Surface
import android.view.TextureView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.samsung.android.scan3d.R
import com.samsung.android.scan3d.ViewState
import com.samsung.android.scan3d.fragments.SettingsGroup
import com.samsung.android.scan3d.fragments.SettingsGroupTitle
import com.samsung.android.scan3d.fragments.SettingsItem
import com.samsung.android.scan3d.serv.CamEngine

val QUALITIES = listOf(1, 10, 20, 30, 40, 50, 60, 70, 80, 90, 100)

@Composable
fun SettingsIntegerSliderItem(
    shape: Shape,
    title: String,
    icon: ImageVector,
    sliderValue: Int,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int,
    onValueChange: (Int) -> Unit,
    defaultValue: Int? = null
) {
    Card(
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(20.dp))
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
                Spacer(modifier = Modifier.weight(1f))

                if (defaultValue != null && sliderValue != defaultValue) {
                    IconButton(
                        onClick = { onValueChange(defaultValue) },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.RestartAlt,
                            contentDescription = "Reset",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                }

                Text(
                    text = "$sliderValue",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Remove, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                Slider(
                    value = sliderValue.toFloat(),
                    onValueChange = { onValueChange(it.toInt()) },
                    valueRange = valueRange,
                    steps = steps,
                    modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
                )
                Icon(Icons.Rounded.Add, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
            }
        }
    }
}

@SuppressLint("ClickableViewAccessibility")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CameraScreenUI(
    textureView: TextureView,
    camData: CamEngine.Companion.Data?,
    quickData: CamEngine.Companion.DataQuick?,
    viewState: ViewState,
    localIp: String,
    zoomSliderPosition: Float,
    isInputLocked: Boolean,
    onSurfaceAvailable: (Surface) -> Unit,
    onSurfaceDestroyed: () -> Unit,
    onStopClicked: () -> Unit,
    onPreviewToggled: (Boolean) -> Unit,
    onStreamToggled: (Boolean) -> Unit,
    onFlashToggled: (Boolean) -> Unit,
    onFlashLevelChanged: (Int) -> Unit,
    onSensorSelected: (Int) -> Unit,
    onResolutionSelected: (Int) -> Unit,
    onQualitySelected: (Int) -> Unit,
    onIpClicked: (String) -> Unit,
    onZoomScaleChanged: (Float) -> Unit,
    onZoomRatioChanged: (Float) -> Unit,
    onSettingsClicked: () -> Unit,
    onDoubleTapped: () -> Unit,
    onH264BitrateChanged: (Int) -> Unit,
    onH264ModeChanged: (Int) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name), fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                ),
                actions = {
                    val statsText = if (viewState.streamFormat == 1) {
                        "${quickData?.rateKbs ?: 0} kB/s"
                    } else {
                        "${quickData?.ms ?: 0}ms / ${quickData?.rateKbs ?: 0} kB/s"
                    }

                    Text(
                        text = statsText,
                        modifier = Modifier.padding(end = 12.dp),
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                    FilledTonalIconButton(onClick = onSettingsClicked) {
                        Icon(Icons.Rounded.Settings, null)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onStopClicked,
                text = { Text(stringResource(R.string.cam_stop), fontWeight = FontWeight.Bold) },
                icon = { Icon(Icons.Rounded.Videocam, null) },
                containerColor = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer,
                shape = RoundedCornerShape(16.dp)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier.fillMaxSize().padding(paddingValues)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                AndroidView(
                    factory = { context ->
                        val scaleGestureDetector = ScaleGestureDetector(context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
                            override fun onScale(d: ScaleGestureDetector): Boolean { onZoomScaleChanged(d.scaleFactor); return true }
                        })
                        val gestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
                            override fun onDoubleTap(e: MotionEvent): Boolean { onDoubleTapped(); return true }
                            override fun onSingleTapUp(e: MotionEvent): Boolean { textureView.performClick(); return true }
                        })
                        textureView.apply {
                            surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                                private fun configureTransform(viewWidth: Int, viewHeight: Int) {
                                    val data = camData ?: return
                                    val bufferSize = data.resolutions.getOrNull(viewState.resolutionIndex ?: 0) ?: return
                                    val orientation = data.sensorOrientation

                                    val matrix = Matrix()
                                    val viewRect = RectF(0f, 0f, viewWidth.toFloat(), viewHeight.toFloat())
                                    val bufferRect = RectF(0f, 0f, bufferSize.height.toFloat(), bufferSize.width.toFloat())
                                    bufferRect.offset(viewWidth/2f - bufferRect.centerX(), viewHeight/2f - bufferRect.centerY())
                                    matrix.setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL)
                                    val scale = maxOf(viewHeight.toFloat() / bufferSize.width, viewWidth.toFloat() / bufferSize.height)
                                    matrix.postScale(scale, scale, viewWidth/2f, viewHeight/2f)
                                    matrix.postRotate((orientation - 90).toFloat(), viewWidth/2f, viewHeight/2f)
                                    textureView.setTransform(matrix)
                                }
                                override fun onSurfaceTextureAvailable(st: SurfaceTexture, w: Int, h: Int) {
                                    configureTransform(w, h)
                                    onSurfaceAvailable(Surface(st))
                                }
                                override fun onSurfaceTextureSizeChanged(st: SurfaceTexture, w: Int, h: Int) { configureTransform(w, h) }
                                override fun onSurfaceTextureDestroyed(st: SurfaceTexture): Boolean { onSurfaceDestroyed(); return true }
                                override fun onSurfaceTextureUpdated(st: SurfaceTexture) {}
                            }
                            setOnTouchListener { _, event -> scaleGestureDetector.onTouchEvent(event); gestureDetector.onTouchEvent(event); true }
                        }
                        textureView
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }

            Column(
                modifier = Modifier.weight(1.2f).fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(top = 16.dp, bottom = 100.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    val maxFlashLevel = camData?.maxFlashLevel ?: 1
                    val hasFlash = camData?.hasFlash == true
                    val showIntensitySlider = hasFlash && viewState.flash && maxFlashLevel > 1
                    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                        SettingsGroupTitle(stringResource(R.string.cam_controls))
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            SettingsItem(
                                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp, bottomStart = 4.dp, bottomEnd = 4.dp),
                                title = stringResource(R.string.cam_local_preview),
                                icon = Icons.Rounded.Visibility,
                                hasSwitch = true,
                                switchState = viewState.preview,
                                onSwitchChange = onPreviewToggled
                            )
                            SettingsItem(
                                shape = RoundedCornerShape(4.dp),
                                title = stringResource(R.string.cam_mjpeg_stream),
                                icon = Icons.Rounded.CellTower,
                                hasSwitch = true,
                                switchState = viewState.stream,
                                onSwitchChange = onStreamToggled
                            )
                            if (hasFlash) {
                                val flashToggleBottomRadius by animateDpAsState(
                                    targetValue = if (showIntensitySlider) 4.dp else 24.dp,
                                    label = "FlashToggleCornerAnimation"
                                )

                                SettingsItem(
                                    shape = RoundedCornerShape(
                                        topStart = 4.dp,
                                        topEnd = 4.dp,
                                        bottomStart = flashToggleBottomRadius,
                                        bottomEnd = flashToggleBottomRadius
                                    ),
                                    title = stringResource(R.string.cam_flash),
                                    icon = Icons.Rounded.FlashOn,
                                    hasSwitch = true,
                                    switchState = viewState.flash,
                                    onSwitchChange = onFlashToggled
                                )
                            }

                            AnimatedVisibility(
                                visible = showIntensitySlider,
                                enter = expandVertically() + fadeIn(),
                                exit = shrinkVertically() + fadeOut()
                            ) {
                                val currentLevel = if (viewState.flashLevel > 0) viewState.flashLevel else maxFlashLevel
                                SettingsIntegerSliderItem(
                                    shape = RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp, bottomStart = 24.dp, bottomEnd = 24.dp),
                                    title = stringResource(R.string.cam_flash_level),
                                    icon = Icons.Rounded.Tungsten,
                                    sliderValue = currentLevel,
                                    valueRange = 1f..(maxFlashLevel.toFloat()),
                                    steps = if (maxFlashLevel > 2) maxFlashLevel - 2 else 0,
                                    onValueChange = onFlashLevelChanged
                                )
                            }
                        }
                    }

                    if (camData != null && viewState.resolutionIndex != null) {
                        val parameterItems = mutableListOf<@Composable (Shape) -> Unit>()
                        parameterItems.add { shape -> SettingsDropdownItem(shape, stringResource(R.string.cam_sensor), Icons.Rounded.CameraAlt, camData.sensors.map { it.title }, camData.sensors.indexOfFirst { it.cameraId == viewState.cameraId }, onSensorSelected) }
                        parameterItems.add { shape -> SettingsDropdownItem(shape, stringResource(R.string.cam_resolution), Icons.Rounded.PhotoSizeSelectActual, camData.resolutions.map { it.toString() }, viewState.resolutionIndex, onResolutionSelected) }

                        if (viewState.streamFormat == 1) {
                            parameterItems.add { shape ->
                                SettingsIntegerSliderItem(
                                    shape = shape,
                                    title = stringResource(R.string.h264_bitrate),
                                    icon = Icons.Rounded.Speed,
                                    sliderValue = viewState.h264Bitrate,
                                    valueRange = 1f..100f,
                                    steps = 98,
                                    onValueChange = onH264BitrateChanged,
                                    defaultValue = 10
                                )
                            }
                            val modeOptions = listOf(stringResource(R.string.h264_mode_cbr), stringResource(R.string.h264_mode_vbr))
                            parameterItems.add { shape -> SettingsDropdownItem(shape, stringResource(R.string.h264_mode), Icons.Rounded.Tune, modeOptions, viewState.h264Mode, onH264ModeChanged) }
                        } else {
                            parameterItems.add { shape -> SettingsDropdownItem(shape, stringResource(R.string.cam_quality), Icons.Rounded.HighQuality, QUALITIES.map { "$it%" }, QUALITIES.indexOf(viewState.quality), { onQualitySelected(QUALITIES[it]) }) }
                        }

                        if (camData.maxZoom > camData.minZoom) {
                            parameterItems.add { shape -> SettingsSliderItem(shape, "Zoom", Icons.Rounded.ZoomIn, zoomSliderPosition, camData.currentZoom, onZoomRatioChanged) }
                        }

                        SettingsGroup(title = stringResource(R.string.cam_parameters), items = parameterItems)
                    }

                    val uriHandler = LocalUriHandler.current
                    SettingsGroup(
                        title = stringResource(R.string.cam_information),
                        items = listOf(
                            { shape ->
                                SettingsItem(
                                    shape = shape,
                                    title = stringResource(R.string.cam_mjpeg_stream),
                                    subtitle = localIp,
                                    icon = Icons.Rounded.Link,
                                    onClick = { onIpClicked(localIp) }
                                )
                            },
    // -----------------------
                            { shape ->
                                SettingsItem(
                                    shape = shape,
                                    title = "GitHub",
                                    subtitle = "github.com/alan7383/RemoteCam-Enhanced",
                                    icon = Icons.Rounded.Code,
                                    onClick = { uriHandler.openUri("https://github.com/alan7383/RemoteCam-Enhanced") }
                                )
                            }
                        )
                    )
                }
            }
        }
    }
}

@Composable
fun SettingsDropdownItem(shape: Shape, title: String, icon: ImageVector, options: List<String>, selectedIndex: Int, onSelected: (Int) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Card(shape = shape, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer), modifier = Modifier.fillMaxWidth()) {
        Box {
            Row(modifier = Modifier.fillMaxWidth().heightIn(min = 72.dp).clickable { expanded = true }.padding(horizontal = 20.dp, vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null, tint = MaterialTheme.colorScheme.primary); Spacer(Modifier.width(20.dp))
                Column(Modifier.weight(1f)) {
                    Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
                    Text(options.getOrNull(selectedIndex) ?: "", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Icon(Icons.Rounded.ArrowDropDown, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }, modifier = Modifier.background(MaterialTheme.colorScheme.surfaceContainerHigh)) {
                options.forEachIndexed { idx, txt -> DropdownMenuItem(text = { Text(txt) }, onClick = { onSelected(idx); expanded = false }) }
            }
        }
    }
}

@Composable
fun SettingsSliderItem(shape: Shape, title: String, icon: ImageVector, sliderValue: Float, displayValue: Float, onValueChange: (Float) -> Unit) {
    Card(shape = shape, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer), modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null, tint = MaterialTheme.colorScheme.primary); Spacer(Modifier.width(20.dp))
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
                Spacer(Modifier.weight(1f))
                Text("${"%.1f".format(displayValue)}x", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.ZoomOut, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                Slider(value = sliderValue, onValueChange = onValueChange, modifier = Modifier.weight(1f).padding(horizontal = 8.dp))
                Icon(Icons.Rounded.ZoomIn, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
            }
        }
    }
}