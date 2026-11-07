package com.vibra.wanderai.presentation.vision

import android.text.format.DateFormat
import androidx.activity.compose.LocalActivity
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.FlashOff
import androidx.compose.material.icons.outlined.FlashOn
import androidx.compose.material.icons.outlined.HistoryEdu
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.TravelExplore
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vibra.wanderai.R
import com.vibra.wanderai.domain.model.LandmarkMatch
import com.vibra.wanderai.presentation.common.formatCoordinates
import com.vibra.wanderai.presentation.common.formatDistance
import com.vibra.wanderai.presentation.common.localized
import com.vibra.wanderai.presentation.common.wanderViewModel
import com.vibra.wanderai.presentation.components.AppPermissions
import com.vibra.wanderai.presentation.components.rememberPermissionState
import com.vibra.wanderai.ui.theme.NavyInk
import com.vibra.wanderai.ui.theme.WanderTheme
import kotlinx.coroutines.delay
import java.util.Date

@Composable
fun VisionScreen(onOpenMap: () -> Unit) {
    val viewModel = wanderViewModel { VisionViewModel(it.observeTravelerLocation, it.identifyLandmark) }
    val state by viewModel.state.collectAsStateWithLifecycle()

    val cameraPermission = rememberPermissionState(AppPermissions.camera)
    val locationPermission = rememberPermissionState(AppPermissions.location, requireAll = false)
    var torch by rememberSaveable { mutableStateOf(false) }
    var cameraFailed by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (!cameraPermission.isGranted) cameraPermission.request()
    }
    LaunchedEffect(cameraPermission.isGranted) {
        if (cameraPermission.isGranted && !locationPermission.isGranted) locationPermission.request()
    }
    LaunchedEffect(locationPermission.isGranted) {
        if (locationPermission.isGranted) viewModel.refreshLocation()
    }

    LightStatusBarIcons()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(NavyInk, WanderTheme.colors.heroStart))),
    ) {
        if (cameraPermission.isGranted && !cameraFailed) {
            CameraPreview(
                torchEnabled = torch,
                onCameraUnavailable = { cameraFailed = true },
                modifier = Modifier.fillMaxSize(),
            )
        }

        // Legibility scrims.
        Box(
            Modifier
                .fillMaxWidth()
                .height(180.dp)
                .background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.65f), Color.Transparent)))
        )
        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(320.dp)
                .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.75f))))
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            VisionTopRow(
                isLive = state.location.isLive,
                torchOn = torch,
                torchAvailable = cameraPermission.isGranted && !cameraFailed,
                onToggleTorch = { torch = !torch },
            )

            BoxWithConstraints(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                val finder = minOf(maxWidth * 0.78f, maxHeight * 0.8f, 300.dp)
                if (!cameraPermission.isGranted || cameraFailed) {
                    CameraPermissionPrompt(
                        failed = cameraFailed,
                        onGrant = cameraPermission::request,
                    )
                } else {
                    Viewfinder(size = finder, locked = state.match != null)
                }
            }

            DetectionBanner(match = state.match, analyzing = state.isAnalyzing)
            Spacer(Modifier.height(10.dp))
            InfoPanel(
                coordinates = formatCoordinates(state.location.point),
                isLive = state.location.isLive,
                match = state.match,
                onOpenMap = onOpenMap,
            )
            Spacer(Modifier.height(8.dp))
        }
    }
}

/** Camera UI is always dark: force light status-bar icons while visible. */
@Composable
private fun LightStatusBarIcons() {
    val activity = LocalActivity.current ?: return
    val view = LocalView.current
    DisposableEffect(activity, view) {
        val controller = WindowCompat.getInsetsController(activity.window, view)
        val previous = controller.isAppearanceLightStatusBars
        controller.isAppearanceLightStatusBars = false
        onDispose { controller.isAppearanceLightStatusBars = previous }
    }
}

@Composable
private fun VisionTopRow(
    isLive: Boolean,
    torchOn: Boolean,
    torchAvailable: Boolean,
    onToggleTorch: () -> Unit,
) {
    val context = LocalContext.current
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = System.currentTimeMillis()
            delay(15_000)
        }
    }
    val pulse = rememberInfiniteTransition(label = "live")
    val alpha by pulse.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "liveAlpha",
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(9.dp)
                .clip(CircleShape)
                .background(WanderTheme.colors.brandTeal.copy(alpha = alpha))
        )
        Spacer(Modifier.width(8.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.vision_title),
                color = Color.White,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
            )
            Text(
                text = stringResource(R.string.vision_subtitle),
                color = Color.White.copy(alpha = 0.7f),
                style = MaterialTheme.typography.labelMedium,
            )
        }
        GlassChip(
            text = (if (isLive) "GPS · " else "") + DateFormat.getTimeFormat(context).format(Date(now)),
            dotColor = if (isLive) WanderTheme.colors.success else WanderTheme.colors.brandGold,
        )
        if (torchAvailable) {
            Spacer(Modifier.width(6.dp))
            IconButton(
                onClick = onToggleTorch,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.14f)),
            ) {
                Icon(
                    if (torchOn) Icons.Outlined.FlashOn else Icons.Outlined.FlashOff,
                    contentDescription = stringResource(R.string.vision_flash),
                    tint = if (torchOn) WanderTheme.colors.brandGold else Color.White,
                )
            }
        }
    }
}

@Composable
private fun GlassChip(text: String, dotColor: Color) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(Color.Black.copy(alpha = 0.45f))
            .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(dotColor)
        )
        Spacer(Modifier.width(6.dp))
        Text(text, color = Color.White, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun Viewfinder(size: Dp, locked: Boolean) {
    val transition = rememberInfiniteTransition(label = "scan")
    val scanY by transition.animateFloat(
        initialValue = 0.04f,
        targetValue = 0.96f,
        animationSpec = infiniteRepeatable(tween(2_200, easing = LinearEasing), RepeatMode.Reverse),
        label = "scanLine",
    )
    val bracket = if (locked) WanderTheme.colors.accentWarm else Color.White
    val line = WanderTheme.colors.brandTeal

    Box(
        modifier = Modifier
            .size(size)
            .drawWithContent {
                drawContent()
                val w = this.size.width
                val h = this.size.height
                val arm = w * 0.14f
                val stroke = 4.dp.toPx()
                val corners = listOf(
                    Offset(0f, 0f) to Offset(1f, 1f),
                    Offset(w, 0f) to Offset(-1f, 1f),
                    Offset(0f, h) to Offset(1f, -1f),
                    Offset(w, h) to Offset(-1f, -1f),
                )
                corners.forEach { (corner, dir) ->
                    drawLine(bracket, corner, corner + Offset(arm * dir.x, 0f), stroke, StrokeCap.Round)
                    drawLine(bracket, corner, corner + Offset(0f, arm * dir.y), stroke, StrokeCap.Round)
                }
                if (!locked) {
                    val y = h * scanY
                    drawLine(
                        brush = Brush.horizontalGradient(
                            listOf(Color.Transparent, line, line, Color.Transparent)
                        ),
                        start = Offset(w * 0.06f, y),
                        end = Offset(w * 0.94f, y),
                        strokeWidth = 2.5.dp.toPx(),
                        cap = StrokeCap.Round,
                    )
                }
            },
    )
}

@Composable
private fun CameraPermissionPrompt(failed: Boolean, onGrant: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
        Icon(Icons.Outlined.CameraAlt, contentDescription = null, tint = Color.White.copy(alpha = 0.6f), modifier = Modifier.size(56.dp))
        Spacer(Modifier.height(12.dp))
        Text(
            text = stringResource(if (failed) R.string.vision_camera_unavailable else R.string.vision_camera_permission),
            color = Color.White,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
        )
        if (!failed) {
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = onGrant,
                colors = ButtonDefaults.buttonColors(containerColor = WanderTheme.colors.brandTeal, contentColor = Color.White),
                shape = RoundedCornerShape(14.dp),
            ) {
                Text(stringResource(R.string.vision_camera_grant), fontWeight = FontWeight.Bold)
            }
        }
    }
}

/** Amber banner — "WANDERAI VISION: SAN FELIPE DETECTADO (96%)". */
@Composable
private fun DetectionBanner(match: LandmarkMatch?, analyzing: Boolean) {
    AnimatedContent(targetState = match, label = "detection") { current ->
        val (text, container) = when {
            current != null -> stringResource(
                R.string.vision_detected,
                current.place.name.uppercase(),
                current.confidencePercent,
            ) to WanderTheme.colors.accentWarm
            analyzing -> stringResource(R.string.vision_analyzing) to Color.Black.copy(alpha = 0.5f)
            else -> stringResource(R.string.vision_no_landmark) to Color.Black.copy(alpha = 0.5f)
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(container)
                .padding(horizontal = 14.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                if (current != null) Icons.Outlined.AutoAwesome else Icons.Outlined.TravelExplore,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(10.dp))
            Text(
                text = text,
                color = Color.White,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 13.sp,
                letterSpacing = 0.4.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun InfoPanel(
    coordinates: String,
    isLive: Boolean,
    match: LandmarkMatch?,
    onOpenMap: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(NavyInk.copy(alpha = 0.78f))
            .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(20.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        InfoRow(
            icon = Icons.Outlined.LocationOn,
            tint = WanderTheme.colors.brandTeal,
            label = stringResource(R.string.vision_current_location),
            value = stringResource(R.string.vision_city),
            detail = coordinates + if (isLive) "" else " · " + stringResource(R.string.gps_reference),
        )
        if (match != null) {
            InfoRow(
                icon = Icons.Outlined.HistoryEdu,
                tint = WanderTheme.colors.brandGold,
                label = stringResource(R.string.vision_history),
                value = match.place.history?.localized() ?: match.place.name,
                detail = match.place.summary.localized() + " · " + formatDistance(match.distanceMeters),
            )
            TextButton(onClick = onOpenMap, modifier = Modifier.align(Alignment.End)) {
                Icon(Icons.Outlined.Map, contentDescription = null, tint = WanderTheme.colors.brandTeal, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.vision_open_map), color = WanderTheme.colors.brandTeal, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun InfoRow(icon: ImageVector, tint: Color, label: String, value: String, detail: String) {
    Row(verticalAlignment = Alignment.Top) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(tint.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column {
            Text(label.uppercase(), color = tint, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.8.sp)
            Text(value, color = Color.White, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Text(detail, color = Color.White.copy(alpha = 0.7f), style = MaterialTheme.typography.bodySmall)
        }
    }
}
