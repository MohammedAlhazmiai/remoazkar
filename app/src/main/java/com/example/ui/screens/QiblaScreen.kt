package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.hardware.GeomagneticField
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.PrayerStore
import com.example.data.PrayerTimesCalculator
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QiblaScreen(onBackClick: () -> Unit) {
    val context = LocalContext.current
    val rev = PrayerStore.revision

    val lat = remember(rev) { PrayerStore.latitude(context) }
    val lon = remember(rev) { PrayerStore.longitude(context) }
    val locationName = remember(rev) { PrayerStore.locationName(context) }
    val qiblaBearing = remember(lat, lon) { PrayerTimesCalculator.computeQiblaBearing(lat, lon) }
    val distanceKm = remember(lat, lon) { PrayerTimesCalculator.computeDistanceToKaabaKm(lat, lon) }

    var headingDeg by remember { mutableFloatStateOf(0f) }
    var sensorAvailable by remember { mutableStateOf(true) }
    var isLocating by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var showCityPicker by remember { mutableStateOf(false) }
    var wasAligned by remember { mutableStateOf(false) }

    val locationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        val granted = result[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            result[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            isLocating = true
            PrayerStore.refreshLocationFromDevice(context) { _, msg ->
                isLocating = false
                statusMessage = msg
            }
        } else {
            statusMessage = "يمكنك اختيار مدينتك يدويًا لحساب زاوية القبلة بدقة."
        }
    }

    fun refreshLocation() {
        if (PrayerStore.hasLocationPermission(context)) {
            isLocating = true
            PrayerStore.refreshLocationFromDevice(context) { _, msg ->
                isLocating = false
                statusMessage = msg
            }
        } else {
            locationLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    // استشعار البوصلة وتصحيح الانحراف المغناطيسي وفق إحداثيات المستخدم
    DisposableEffect(lat, lon) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        val rotSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        val accelSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val magSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

        if (sensorManager == null || (rotSensor == null && (accelSensor == null || magSensor == null))) {
            sensorAvailable = false
            onDispose {}
        } else {
            sensorAvailable = true
            val declination = GeomagneticField(
                lat.toFloat(),
                lon.toFloat(),
                0f,
                System.currentTimeMillis()
            ).declination

            val rotationMatrix = FloatArray(9)
            val orientation = FloatArray(3)
            val gravity = FloatArray(3)
            val geomagnetic = FloatArray(3)
            var hasGravity = false
            var hasMagnetic = false
            var smoothAzimuth = 0f
            var initialized = false

            fun pushAzimuth(rawMagneticDeg: Float) {
                val trueDeg = ((rawMagneticDeg + declination) % 360f + 360f) % 360f
                if (!initialized) {
                    smoothAzimuth = trueDeg
                    initialized = true
                } else {
                    // تنعيم الزاوية مع مراعاة العبور بين 359° و 0°
                    var delta = trueDeg - smoothAzimuth
                    if (delta > 180f) delta -= 360f
                    if (delta < -180f) delta += 360f
                    smoothAzimuth = ((smoothAzimuth + delta * 0.18f) % 360f + 360f) % 360f
                }
                headingDeg = smoothAzimuth
            }

            val listener = object : SensorEventListener {
                override fun onSensorChanged(event: SensorEvent) {
                    when (event.sensor.type) {
                        Sensor.TYPE_ROTATION_VECTOR -> {
                            SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                            SensorManager.getOrientation(rotationMatrix, orientation)
                            val deg = Math.toDegrees(orientation[0].toDouble()).toFloat()
                            pushAzimuth(deg)
                        }
                        Sensor.TYPE_ACCELEROMETER -> {
                            System.arraycopy(event.values, 0, gravity, 0, 3)
                            hasGravity = true
                        }
                        Sensor.TYPE_MAGNETIC_FIELD -> {
                            System.arraycopy(event.values, 0, geomagnetic, 0, 3)
                            hasMagnetic = true
                        }
                    }
                    if (rotSensor == null && hasGravity && hasMagnetic) {
                        if (SensorManager.getRotationMatrix(rotationMatrix, null, gravity, geomagnetic)) {
                            SensorManager.getOrientation(rotationMatrix, orientation)
                            val deg = Math.toDegrees(orientation[0].toDouble()).toFloat()
                            pushAzimuth(deg)
                        }
                    }
                }

                override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
            }

            if (rotSensor != null) {
                sensorManager.registerListener(listener, rotSensor, SensorManager.SENSOR_DELAY_UI)
            } else {
                sensorManager.registerListener(listener, accelSensor, SensorManager.SENSOR_DELAY_UI)
                sensorManager.registerListener(listener, magSensor, SensorManager.SENSOR_DELAY_UI)
            }

            onDispose {
                sensorManager.unregisterListener(listener)
            }
        }
    }

    val signedDiff = remember(qiblaBearing, headingDeg) {
        var d = (qiblaBearing.toFloat() - headingDeg) % 360f
        if (d > 180f) d -= 360f
        if (d < -180f) d += 360f
        d
    }
    val aligned = sensorAvailable && abs(signedDiff) <= 3.5f

    LaunchedEffect(aligned) {
        if (aligned && !wasAligned) {
            triggerLightVibrate(context)
        }
        wasAligned = aligned
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("اتجاه القبلة", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick, modifier = Modifier.testTag("back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // شريط الموقع وتحديثه
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.LocationOn, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(locationName, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Text(
                                String.format("%.4f° , %.4f°", lat, lon),
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { refreshLocation() },
                            enabled = !isLocating,
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                                .testTag("qibla_gps_button")
                        ) {
                            Icon(Icons.Default.MyLocation, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(if (isLocating) "جارٍ التحديث..." else "موقعي الحالي")
                        }
                        OutlinedButton(
                            onClick = { showCityPicker = true },
                            modifier = Modifier.heightIn(min = 48.dp)
                        ) {
                            Icon(Icons.Default.EditLocationAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("اختيار مدينة")
                        }
                    }
                    statusMessage?.let {
                        Spacer(Modifier.height(8.dp))
                        Text(it, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }

            // شارة التوافق مع القبلة
            Surface(
                shape = RoundedCornerShape(999.dp),
                color = if (aligned) Color(0xFF1B6B46) else MaterialTheme.colorScheme.surfaceVariant
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (aligned) Icons.Default.CheckCircle else Icons.Default.Explore,
                        contentDescription = null,
                        tint = if (aligned) Color(0xFFFFE082) else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = when {
                            !sensorAvailable -> "زاوية القبلة من الشمال الحقيقي: ${qiblaBearing.roundToInt()}°"
                            aligned -> "أنت متجه نحو القبلة الآن ✓"
                            signedDiff > 0f -> "أدِر الهاتف قليلًا نحو اليمين (${signedDiff.roundToInt()}°)"
                            else -> "أدِر الهاتف قليلًا نحو اليسار (${abs(signedDiff.roundToInt())}°)"
                        },
                        color = if (aligned) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }

            // البوصلة الدائرية
            QiblaCompassDial(
                headingDeg = if (sensorAvailable) headingDeg else 0f,
                qiblaBearingDeg = qiblaBearing.toFloat(),
                aligned = aligned
            )

            // إحصاءات الزاوية والمسافة إلى مكة المكرمة
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                QiblaStatCard(
                    title = "زاوية القبلة",
                    value = "${String.format("%.1f", qiblaBearing)}°",
                    subtitle = "من الشمال الجغرافي",
                    modifier = Modifier.weight(1f)
                )
                QiblaStatCard(
                    title = "المسافة للكعبة",
                    value = "${distanceKm.roundToInt()} كم",
                    subtitle = "خط مستقيم تقريبًا",
                    modifier = Modifier.weight(1f)
                )
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    }
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = if (sensorAvailable) {
                            "ضع الهاتف أفقيًا بعيدًا عن المعادن، وإذا لاحظت تذبذبًا حرّك الهاتف على شكل رقم 8 لمعايرة البوصلة."
                        } else {
                            "هذا الجهاز لا يحتوي على مستشعر بوصلة مغناطيسية؛ يمكنك استخدام زاوية القبلة أعلاه (${qiblaBearing.roundToInt()}° شرق الشمال) للتحديد الدقيق."
                        },
                        fontSize = 12.sp,
                        lineHeight = 19.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(64.dp))
        }
    }

    if (showCityPicker) {
        CityLocationPickerDialog(
            onDismiss = { showCityPicker = false },
            onSelectCity = { name, newLat, newLon ->
                PrayerStore.setLocation(context, newLat, newLon, name, isAuto = false)
                showCityPicker = false
                statusMessage = "تم تحديث موقع القبلة إلى: $name"
            }
        )
    }
}

@Composable
private fun QiblaCompassDial(
    headingDeg: Float,
    qiblaBearingDeg: Float,
    aligned: Boolean
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface
    val goldColor = Color(0xFFD4AF37)
    val emeraldGlow = Color(0xFF22A06B)

    Box(
        modifier = Modifier
            .size(290.dp)
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val radius = size.minDimension / 2f - 10.dp.toPx()

            // الحلقة الخارجية
            drawCircle(
                color = if (aligned) emeraldGlow else primaryColor.copy(alpha = 0.35f),
                radius = radius,
                style = Stroke(width = if (aligned) 6.dp.toPx() else 3.dp.toPx())
            )
            drawCircle(
                color = (if (aligned) emeraldGlow else primaryColor).copy(alpha = 0.08f),
                radius = radius - 6.dp.toPx()
            )

            // قرص الدرجات يدور بعكس اتجاه الهاتف ليظل الشمال ثابتًا نحو الشمال الحقيقي
            rotate(degrees = -headingDeg, pivot = center) {
                for (deg in 0 until 360 step 10) {
                    val isMajor = deg % 90 == 0
                    val isMedium = deg % 30 == 0
                    val tickLen = when {
                        isMajor -> 18.dp.toPx()
                        isMedium -> 12.dp.toPx()
                        else -> 6.dp.toPx()
                    }
                    val rad = Math.toRadians(deg.toDouble() - 90.0)
                    val start = Offset(
                        center.x + ((radius - tickLen) * cos(rad)).toFloat(),
                        center.y + ((radius - tickLen) * sin(rad)).toFloat()
                    )
                    val end = Offset(
                        center.x + (radius * cos(rad)).toFloat(),
                        center.y + (radius * sin(rad)).toFloat()
                    )
                    drawLine(
                        color = if (deg == 0) Color(0xFFE53935) else onSurfaceColor.copy(alpha = if (isMajor) 0.7f else 0.3f),
                        start = start,
                        end = end,
                        strokeWidth = if (isMajor) 3.dp.toPx() else 1.5.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }

                // سهم القبلة الذهبي المتجه نحو الكعبة المشرفة
                rotate(degrees = qiblaBearingDeg, pivot = center) {
                    val tipY = center.y - (radius - 26.dp.toPx())
                    val tailY = center.y + (radius * 0.32f)
                    val arrowWidth = 14.dp.toPx()

                    val pointerPath = Path().apply {
                        moveTo(center.x, tipY)
                        lineTo(center.x - arrowWidth, center.y)
                        lineTo(center.x, tailY)
                        lineTo(center.x + arrowWidth, center.y)
                        close()
                    }
                    drawPath(
                        path = pointerPath,
                        color = if (aligned) emeraldGlow else goldColor
                    )

                    // نقطة كعبة رمزية عند رأس السهم
                    drawCircle(
                        color = Color(0xFF141414),
                        radius = 10.dp.toPx(),
                        center = Offset(center.x, tipY + 12.dp.toPx())
                    )
                    drawCircle(
                        color = goldColor,
                        radius = 10.dp.toPx(),
                        center = Offset(center.x, tipY + 12.dp.toPx()),
                        style = Stroke(width = 2.dp.toPx())
                    )
                }
            }

            // خط مرجعي ثابت في أعلى الهاتف
            drawLine(
                color = if (aligned) emeraldGlow else primaryColor,
                start = Offset(center.x, center.y - radius - 8.dp.toPx()),
                end = Offset(center.x, center.y - radius + 14.dp.toPx()),
                strokeWidth = 4.dp.toPx(),
                cap = StrokeCap.Round
            )

            // مركز البوصلة
            drawCircle(
                color = primaryColor,
                radius = 7.dp.toPx(),
                center = center
            )
        }
    }
}

@Composable
private fun QiblaStatCard(
    title: String,
    value: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(title, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(4.dp))
            Text(
                value,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                subtitle,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

private fun triggerLightVibrate(context: Context) {
    runCatching {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vm.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(45, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(45)
        }
    }
}
