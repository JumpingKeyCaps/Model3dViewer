package com.lebaillyapp.model3dviewer.composition

import android.annotation.SuppressLint
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.view.View
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.collectLatest
import kotlin.math.*


@SuppressLint("SetJavaScriptEnabled")
@Composable
fun GyroscopeControlledViewer(modifier: Modifier = Modifier) {
    var offset by remember { mutableStateOf(Offset.Zero) }
    var webView by remember { mutableStateOf<WebView?>(null) }
    var filteredValues by remember { mutableStateOf(Pair(0f, 0f)) } // Pour stocker les valeurs X et Y filtrées

    Column(modifier = Modifier.fillMaxSize()) {
        // Ton visualiseur three.js ou autre truc principal
        Box(
            modifier = modifier
                .weight(1f)
                .fillMaxWidth()
                .background(Color(0xFF1E1B1B))
        ) {
            AndroidView(
                factory = { ctx ->
                    WebView(ctx).apply {
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.allowFileAccess = true
                        settings.allowContentAccess = true
                        settings.allowFileAccessFromFileURLs = true
                        settings.allowUniversalAccessFromFileURLs = true
                        webViewClient = WebViewClient()
                        setBackgroundColor(android.graphics.Color.TRANSPARENT)
                        overScrollMode = View.OVER_SCROLL_NEVER
                        loadUrl("file:///android_asset/gyroscope_viewer.html")
                        webView = this
                    }
                },
                update = { webView = it },
                modifier = Modifier.fillMaxSize()
            )
            // Le niveau à bulle (rendu visuel + lecture capteur)
            GyroscopeControlledBubbleLevel(
                modifier = Modifier
                    .height(100.dp)
                    .padding(bottom = 16.dp)
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter),
                onBubblePositionChanged = {
                    offset = it
                },
                onSensorValuesChanged = { x, y ->
                    filteredValues = Pair(x, y)

                    // Conversion des valeurs filtrées en degrés
                    // Multipliez par un facteur pour amplifier l'effet si nécessaire
                    val rotationFactorX = 10f // Ajustez selon vos besoins
                    val rotationFactorY = 10f // Ajustez selon vos besoins

                    val rotationX = x * rotationFactorX
                    val rotationY = y * rotationFactorY


                    // Envoyer les valeurs à la WebView
                    webView?.let { webView ->
                        val jsCode = "javascript:applyDeviceOrientation($rotationX, $rotationY);"
                        webView.evaluateJavascript(jsCode, null)
                    }
                }
            )
        }
    }
}

@Composable
fun GyroscopeControlledBubbleLevel(
    modifier: Modifier = Modifier,
    sensitivity: Float = 0.06f,
    bubbleMovementRate: Float = 0.05f,
    onBubblePositionChanged: (Offset) -> Unit = {},
    onSensorValuesChanged: (Float, Float) -> Unit = { _, _ -> } // Nouveau callback pour les valeurs du capteur
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var filteredX by remember { mutableStateOf(30f) }
    var filteredY by remember { mutableStateOf(30f) }
    var bubblePosition by remember { mutableStateOf(Offset.Zero) }

    val sensorManager = remember { context.getSystemService(Context.SENSOR_SERVICE) as SensorManager }
    val accelerometer = remember { sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) }

    val sensorListener = remember {
        object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
                    val x = event.values[0]
                    val y = event.values[1]

                    filteredX = filteredX * (1 - sensitivity) + x * sensitivity
                    filteredY = filteredY * (1 - sensitivity) + y * sensitivity

                    val normalizedX = -filteredX
                    val normalizedY = filteredY

                    val adjustedX = normalizedX * bubbleMovementRate
                    val adjustedY = normalizedY * bubbleMovementRate

                    val magnitude = sqrt(adjustedX * adjustedX + adjustedY * adjustedY)
                    val maxMagnitude = 1.0f

                    bubblePosition = if (magnitude > maxMagnitude) {
                        val scale = maxMagnitude / magnitude
                        Offset(adjustedX * scale, adjustedY * scale)
                    } else {
                        Offset(adjustedX, adjustedY)
                    }

                    onBubblePositionChanged(bubblePosition)

                    // Envoyer les valeurs filtrées normalisées (pas les valeurs de la bulle)
                    onSensorValuesChanged(normalizedX, normalizedY)
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> sensorManager.registerListener(sensorListener, accelerometer, SensorManager.SENSOR_DELAY_UI)
                Lifecycle.Event.ON_PAUSE -> sensorManager.unregisterListener(sensorListener)
                else -> {}
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            sensorManager.unregisterListener(sensorListener)
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // UI Bubble
    Box(modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            // [Le reste du code de dessin de la bulle reste inchangé]
            val center = Offset(size.width / 2, size.height / 2)
            val outerCircleRadius = minOf(size.width, size.height) * 0.4f
            val innerCircleRadius = outerCircleRadius * 0.05f // Bulle plus petite

            // Cercle extérieur
            drawCircle(
                color = Color.Gray, // Lignes du cercle grises
                center = center,
                radius = outerCircleRadius,
                style = Stroke(width = 2f)
            )

            // Cercle central (cible)
            drawCircle(
                color = Color.LightGray,
                center = center,
                radius = innerCircleRadius * 1.5f,
                style = Stroke(width = 1f)
            )

            // Ajouter des cercles internes supplémentaires
            val circleCount = 5
            for (i in 1..circleCount) {
                val radius = outerCircleRadius * (i / (circleCount + 1).toFloat())
                drawCircle(
                    color = Color.Gray.copy(alpha = 0.3f), // Couleur plus claire pour les cercles internes
                    center = center,
                    radius = radius,
                    style = Stroke(width = 1f)
                )
            }

            // Cercle de graduation avec plus de marques
            val graduationRadius = outerCircleRadius * 0.9f
            val gradationCount = 24 // Augmenté à 24 pour plus de graduations
            for (i in 0 until gradationCount) {
                val angle = Math.PI * 2 * i / gradationCount
                val startX = center.x + cos(angle) * graduationRadius
                val startY = center.y + sin(angle) * graduationRadius
                val endX = center.x + cos(angle) * (graduationRadius + 15)
                val endY = center.y + sin(angle) * (graduationRadius + 15)

                drawLine(
                    color = Color.Gray,
                    start = Offset(startX.toFloat(), startY.toFloat()),
                    end = Offset(endX.toFloat(), endY.toFloat()),
                    strokeWidth = 1f
                )
            }

            // Ligne horizontale de référence
            drawLine(
                color = Color.Gray, // Lignes de la croix grises
                start = Offset(center.x - outerCircleRadius, center.y),
                end = Offset(center.x + outerCircleRadius, center.y),
                strokeWidth = 1f
            )

            // Ligne verticale de référence
            drawLine(
                color = Color.Gray, // Lignes de la croix grises
                start = Offset(center.x, center.y - outerCircleRadius),
                end = Offset(center.x, center.y + outerCircleRadius),
                strokeWidth = 1f
            )

            // Calcul de la position de la bulle en fonction de l'inclinaison
            val bubbleX = center.x + (bubblePosition.x * outerCircleRadius * 0.8f)
            val bubbleY = center.y + (bubblePosition.y * outerCircleRadius * 0.8f)
            val bubbleCenter = Offset(bubbleX, bubbleY)

            // Déterminer la couleur de la bulle selon sa distance par rapport au centre
            val distanceFromCenter = sqrt(
                (bubbleCenter.x - center.x) * (bubbleCenter.x - center.x) +
                        (bubbleCenter.y - center.y) * (bubbleCenter.y - center.y)
            )

            // Bulle rouge vin si dans la zone centrale
            val isCentered = distanceFromCenter < innerCircleRadius
            val bubbleColor = if (isCentered) Color(0xFF8B0000) else Color.White // Bulle rouge vin si centrée

            // Dessin de la bulle (cercle intérieur)
            drawCircle(
                color = bubbleColor,
                center = bubbleCenter,
                radius = innerCircleRadius,
                alpha = 0.9f
            )

            // Bordure de la bulle
            drawCircle(
                color = bubbleColor,
                center = bubbleCenter,
                radius = innerCircleRadius,
                style = Stroke(width = 1f)
            )
        }
    }
}



