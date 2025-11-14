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
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.samsung.android.scan3d.R
import com.samsung.android.scan3d.ViewState
import com.samsung.android.scan3d.fragments.AnimatedSystemSwitch
import com.samsung.android.scan3d.serv.CamEngine
import com.samsung.android.scan3d.serv.CamEngine.Companion.ParcelableSize
import com.samsung.android.scan3d.ui.theme.RemoteCamM3Theme
import com.samsung.android.scan3d.util.Selector
import androidx.compose.material3.ExperimentalMaterial3Api

val QUALITIES = listOf(1, 10, 20, 30, 40, 50, 60, 70, 80, 90, 100)

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
    onSensorSelected: (Int) -> Unit,
    onResolutionSelected: (Int) -> Unit,
    onQualitySelected: (Int) -> Unit,
    onIpClicked: (String) -> Unit,
    onZoomScaleChanged: (Float) -> Unit,
    onZoomRatioChanged: (Float) -> Unit,
    onSettingsClicked: () -> Unit,
    onDoubleTapped: () -> Unit
) {

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name), fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                actions = {
                    Text(
                        text = "${quickData?.ms ?: 0}ms / ${quickData?.rateKbs ?: 0}kB/s",
                        modifier = Modifier.padding(end = 8.dp),
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    IconButton(onClick = onSettingsClicked) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = stringResource(R.string.cam_settings),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onStopClicked,
                text = { Text(stringResource(R.string.cam_stop)) },
                icon = { Icon(Icons.Default.Videocam, contentDescription = stringResource(R.string.cam_stop)) },
                containerColor = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                AndroidView(
                    factory = { context ->

                        val scaleGestureDetector = ScaleGestureDetector(context,
                            object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
                                override fun onScale(detector: ScaleGestureDetector): Boolean {
                                    onZoomScaleChanged(detector.scaleFactor)
                                    return true
                                }
                            }
                        )

                        val gestureDetector = GestureDetector(
                            context,
                            object : GestureDetector.SimpleOnGestureListener() {
                                override fun onDoubleTap(e: MotionEvent): Boolean {
                                    onDoubleTapped()
                                    return true
                                }

                                override fun onSingleTapUp(e: MotionEvent): Boolean {
                                    textureView.performClick()
                                    return true
                                }
                            }
                        )

                        textureView.apply {
                            surfaceTextureListener = object : TextureView.SurfaceTextureListener {

                                private fun configureTransform(viewWidth: Int, viewHeight: Int) {
                                    val orientation = camData?.sensorOrientation ?: 90
                                    if (camData == null || viewWidth == 0 || viewHeight == 0) {
                                        return
                                    }

                                    val matrix = Matrix()
                                    val viewRect = RectF(0f, 0f, viewWidth.toFloat(), viewHeight.toFloat())
                                    val centerX = viewRect.centerX()
                                    val centerY = viewRect.centerY()

                                    val bufferWidth: Int
                                    val bufferHeight: Int
                                    camData.resolutions.getOrNull(viewState.resolutionIndex ?: 0)?.let {
                                        bufferWidth = it.width
                                        bufferHeight = it.height
                                    } ?: return

                                    val logicalBufferWidth: Int
                                    val logicalBufferHeight: Int
                                    if (orientation == 90 || orientation == 270) {
                                        logicalBufferWidth = bufferHeight // 1080
                                        logicalBufferHeight = bufferWidth  // 1920
                                    } else {
                                        logicalBufferWidth = bufferWidth
                                        logicalBufferHeight = bufferHeight
                                    }

                                    val scale: Float
                                    val scaleX = viewWidth.toFloat() / logicalBufferWidth.toFloat()
                                    val scaleY = viewHeight.toFloat() / logicalBufferHeight.toFloat()

                                    scale = maxOf(scaleX, scaleY)

                                    matrix.postScale(scale, scale, centerX, centerY)

                                    if (orientation == 90 || orientation == 270) {
                                        matrix.postRotate((orientation - 90).toFloat(), centerX, centerY)
                                    } else if (orientation == 180) {
                                        matrix.postRotate(180f, centerX, centerY)
                                    }

                                    textureView.setTransform(matrix)
                                }


                                override fun onSurfaceTextureAvailable(st: SurfaceTexture, width: Int, height: Int) {
                                    configureTransform(width, height)
                                    val surface = Surface(st)
                                    onSurfaceAvailable(surface)
                                }

                                override fun onSurfaceTextureSizeChanged(st: SurfaceTexture, width: Int, height: Int) {
                                    configureTransform(width, height)
                                }

                                override fun onSurfaceTextureDestroyed(st: SurfaceTexture): Boolean {
                                    onSurfaceDestroyed()
                                    return true
                                }

                                override fun onSurfaceTextureUpdated(st: SurfaceTexture) {}
                            }

                            setOnClickListener { }
                            setOnTouchListener { _, event ->
                                scaleGestureDetector.onTouchEvent(event)
                                gestureDetector.onTouchEvent(event)
                                true
                            }
                        }
                    },
                )
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ControlGroup(title = stringResource(R.string.cam_controls)) {
                        ControlRow(label = stringResource(R.string.cam_local_preview)) {
                            AnimatedSystemSwitch(checked = viewState.preview, onCheckedChange = onPreviewToggled)
                        }
                        ControlRow(label = stringResource(R.string.cam_mjpeg_stream)) {
                            AnimatedSystemSwitch(checked = viewState.stream, onCheckedChange = onStreamToggled)
                        }
                        if (camData?.hasFlash == true) {
                            ControlRow(label = stringResource(R.string.cam_flash)) {
                                AnimatedSystemSwitch(
                                    checked = viewState.flash,
                                    onCheckedChange = onFlashToggled,
                                )
                            }
                        }
                    }

                    if (camData != null && viewState.resolutionIndex != null) {
                        ControlGroup(title = stringResource(R.string.cam_parameters)) {
                            DropdownControl(
                                label = stringResource(R.string.cam_sensor),
                                options = camData.sensors.map { it.title },
                                selectedIndex = camData.sensors.indexOfFirst { it.cameraId == viewState.cameraId },
                                onSelected = onSensorSelected
                            )

                            DropdownControl(
                                label = stringResource(R.string.cam_resolution),
                                options = camData.resolutions.map { it.toString() },
                                selectedIndex = viewState.resolutionIndex!!,
                                onSelected = onResolutionSelected
                            )

                            DropdownControl(
                                label = stringResource(R.string.cam_quality),
                                options = QUALITIES.map { "$it%" },
                                selectedIndex = QUALITIES.indexOf(viewState.quality),
                                onSelected = { index -> onQualitySelected(QUALITIES[index]) }
                            )

                            if (camData.maxZoom > 1.0f) {
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    text = "Zoom: ${"%.1f".format(camData.currentZoom)}x",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.ZoomOut, null, modifier = Modifier.padding(end = 8.dp))
                                    Slider(
                                        value = zoomSliderPosition,
                                        onValueChange = onZoomRatioChanged,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Icon(Icons.Default.ZoomIn, null, modifier = Modifier.padding(start = 8.dp))
                                }
                            }
                        }
                    } else {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                    }

                    ControlGroup(title = stringResource(R.string.cam_information)) {
                        Text(
                            text = localIp,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onIpClicked(localIp) }
                                .padding(vertical = 8.dp)
                        )

                        val uriHandler = LocalUriHandler.current
                        val githubUrl = "https://github.com/alan7383/RemoteCam-Enhanced"
                        Row(
                            modifier = Modifier.clickable { uriHandler.openUri(githubUrl) },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Link, contentDescription = "Lien", modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = githubUrl,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}


@Composable
fun ControlGroup(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            HorizontalDivider(modifier = Modifier.padding(bottom = 12.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                content()
            }
        }
    }
}

@Composable
fun ControlRow(label: String, content: @Composable () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyLarge)
        content()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DropdownControl(
    label: String,
    options: List<String>,
    selectedIndex: Int,
    onSelected: (Int) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))

        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded },
            modifier = Modifier.weight(1f)
        ) {
            TextField(
                value = options.getOrNull(selectedIndex) ?: "",
                onValueChange = {},
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                colors = TextFieldDefaults.colors(
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, enabled = true)
            )

            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                options.forEachIndexed { index, text ->
                    DropdownMenuItem(
                        text = { Text(text) },
                        onClick = {
                            onSelected(index)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true)
@Composable
fun CameraScreenPreview() {
    val fakeSensor1 = Selector.SensorDesc(cameraId = "0", title = "Caméra Arrière", format = 256)
    val fakeSensor2 = Selector.SensorDesc(cameraId = "1", title = "Caméra Avant", format = 256)

    val previewData = CamEngine.Companion.Data(
        sensors = listOf(fakeSensor1, fakeSensor2),
        sensorSelected = fakeSensor1,
        resolutions = listOf(
            ParcelableSize(1280, 720),
            ParcelableSize(1920, 1080)
        ),
        resolutionSelected = 0,
        currentZoom = 2.5f,
        maxZoom = 8.0f,
        hasFlash = true,
        quality = 80,
        flashState = true,
        sensorOrientation = 90
    )

    RemoteCamM3Theme {
        CameraScreenUI(
            textureView = TextureView(LocalContext.current),
            camData = previewData,
            quickData = CamEngine.Companion.DataQuick(16, 1200),
            viewState = ViewState(
                preview = true,
                stream = false,
                cameraId = "0",
                resolutionIndex = 0,
                quality = 80,
                flash = true
            ),
            localIp = "192.168.1.10:8080/cam.mjpeg",
            zoomSliderPosition = 0.2f,
            isInputLocked = true,
            onSurfaceAvailable = {},
            onSurfaceDestroyed = {},
            onStopClicked = {},
            onPreviewToggled = {},
            onStreamToggled = {},
            onFlashToggled = {},
            onSensorSelected = {},
            onResolutionSelected = {},
            onQualitySelected = {},
            onIpClicked = {},
            onZoomScaleChanged = {},
            onZoomRatioChanged = {},
            onSettingsClicked = {},
            onDoubleTapped = {}
        )
    }
}
