package com.lebaillyapp.model3dviewer.composition

import android.annotation.SuppressLint
import android.net.Uri
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.Locale


/**
 * Un Composable qui affiche une WebView configurée pour charger
 * notre scène Three.js locale depuis les assets et une sheet latérale pour les réglages.
 */
@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun ThreeDViewer(modifier: Modifier = Modifier) {
    val mContext = LocalContext.current
    var webViewInstance: WebView? by remember { mutableStateOf(null) }

    // --- États pour la position de la lumière directionnelle ---
    var lightX by remember { mutableFloatStateOf(-5f) }
    var lightY by remember { mutableFloatStateOf(30f) }
    var lightZ by remember { mutableFloatStateOf(-17f) }

    // --- État pour l'intensité de la lumière ambiante ---
    var ambientIntensity by remember { mutableFloatStateOf(7.0f) }

    // --- État pour le damping factor des contrôles ---
    var dampingFactor by remember { mutableFloatStateOf(0.05f) }

    // --- État pour le nom du modèle à charger depuis assets ---
    var assetModelName by remember { mutableStateOf("sumoo.gltf") }

    // --- État pour contrôler l'affichage de la bottom sheet ---
    var isBottomSheetOpen by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()
    val scrollState = rememberScrollState()

    // --- État pour stocker le chemin du fichier sélectionné ---
    var selectedFilePath by remember { mutableStateOf<String?>(null) }

    fun getFileNameFromUri(uri: Uri): String? {
        val cursor = mContext.contentResolver.query(uri, null, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val displayNameColumnIndex =
                    it.getColumnIndexOrThrow(android.provider.OpenableColumns.DISPLAY_NAME)
                return it.getString(displayNameColumnIndex)
            }
        }
        return null
    }

    val filePickerLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
            uri?.let {
                val inputStream: InputStream? = mContext.contentResolver.openInputStream(it)
                inputStream?.let { input ->
                    val fileName = getFileNameFromUri(it) ?: "selected_model.glb"
                    val outputFile = File(mContext.filesDir, fileName)
                    try {
                        FileOutputStream(outputFile).use { output ->
                            input.copyTo(output)
                        }
                        selectedFilePath = outputFile.absolutePath
                        webViewInstance?.evaluateJavascript(
                            "javascript:loadLocalModel('${outputFile.absolutePath}');",
                            null
                        )
                    } catch (e: Exception) {
                        e.printStackTrace()
                    } finally {
                        input.close()
                    }
                }
            }
        }

    Box(modifier = modifier) {
        AndroidView(
            modifier = Modifier.fillMaxSize()
                .padding(10.dp)
                .clip(RoundedCornerShape(10.dp)),
            factory = { context ->
                WebView(context).apply {
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.allowFileAccess = true
                    settings.allowContentAccess = true
                    settings.allowFileAccessFromFileURLs = true
                    settings.allowUniversalAccessFromFileURLs = true
                    webViewClient = object : WebViewClient() {
                        override fun onPageFinished(view: WebView?, url: String?) {
                            super.onPageFinished(view, url)
                            // Une fois la page chargée, on tente de charger le fichier sélectionné ou le modèle par défaut
                            if (selectedFilePath != null) {
                                webViewInstance?.evaluateJavascript(
                                    "javascript:loadLocalModel('${selectedFilePath}');",
                                    null
                                )
                            } else {
                                webViewInstance?.evaluateJavascript(
                                    "javascript:loadModel('${assetModelName}');",
                                    null
                                )
                            }
                            // Initialisation des sliders après le chargement de la page
                            webViewInstance?.evaluateJavascript(
                                "javascript:setLightPosition(${
                                    lightX.format(
                                        2
                                    )
                                }, ${lightY.format(2)}, ${lightZ.format(2)});", null
                            )
                            webViewInstance?.evaluateJavascript(
                                "javascript:setAmbientLightIntensity(${
                                    ambientIntensity.format(
                                        2
                                    )
                                });", null
                            )
                            webViewInstance?.evaluateJavascript(
                                "javascript:setDampingFactor(${
                                    dampingFactor.format(
                                        2
                                    )
                                });", null
                            )
                        }
                    }
                    loadUrl("file:///android_asset/viewer.html")
                }.also {
                    webViewInstance = it
                }
            },
            update = { webView ->
                webViewInstance = webView
                // Appeler les fonctions JavaScript pour mettre à jour les réglages
                webView.evaluateJavascript(
                    "javascript:setLightPosition(${lightX.format(2)}, ${
                        lightY.format(
                            2
                        )
                    }, ${lightZ.format(2)});", null
                )
                webView.evaluateJavascript(
                    "javascript:setAmbientLightIntensity(${
                        ambientIntensity.format(
                            2
                        )
                    });", null
                )
                webView.evaluateJavascript(
                    "javascript:setDampingFactor(${dampingFactor.format(2)});",
                    null
                )
            }
        )

        FloatingActionButton(
            onClick = { isBottomSheetOpen = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            Icon(Icons.Filled.Settings, "Ouvrir les réglages")
        }

        if (isBottomSheetOpen) {
            ModalBottomSheet(
                onDismissRequest = { isBottomSheetOpen = false },
                sheetState = sheetState,
                modifier = Modifier.heightIn(max = 650.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(30.dp)
                        .fillMaxWidth()
                        .verticalScroll(scrollState)
                ) {
                    Text(
                        modifier = modifier.padding(start = 0.dp),
                        text = "Réglages 3D",
                        style = androidx.compose.material3.MaterialTheme.typography.headlineSmall
                    )
                    Text(
                        text = "Lumière Directionnelle",
                        style = androidx.compose.material3.MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(top = 16.dp)
                    )
                    Text(text = "Position X: ${lightX.format(2)}")
                    Slider(
                        value = lightX,
                        onValueChange = {
                            lightX = it
                            webViewInstance?.evaluateJavascript(
                                "javascript:setLightX(${it.format(2)});",
                                null
                            )
                        },
                        valueRange = -30f..30f
                    )
                    Text(text = "Position Y: ${lightY.format(2)}")
                    Slider(
                        value = lightY,
                        onValueChange = {
                            lightY = it
                            webViewInstance?.evaluateJavascript(
                                "javascript:setLightY(${it.format(2)});",
                                null
                            )
                        },
                        valueRange = -30f..30f
                    )
                    Text(text = "Position Z: ${lightZ.format(2)}")
                    Slider(
                        value = lightZ,
                        onValueChange = {
                            lightZ = it
                            webViewInstance?.evaluateJavascript(
                                "javascript:setLightZ(${it.format(2)});",
                                null
                            )
                        },
                        valueRange = -30f..30f
                    )

                    Text(
                        text = "Lumière Ambiante",
                        style = androidx.compose.material3.MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(top = 16.dp)
                    )
                    Text(text = "Intensité: ${ambientIntensity.format(2)}")
                    Slider(
                        value = ambientIntensity,
                        onValueChange = {
                            ambientIntensity = it
                            webViewInstance?.evaluateJavascript(
                                "javascript:setAmbientLightIntensity(${
                                    it.format(
                                        2
                                    )
                                });", null
                            )
                        },
                        valueRange = 0f..30f
                    )

                    Text(
                        text = "Contrôles Orbitaux",
                        style = androidx.compose.material3.MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(top = 16.dp)
                    )
                    Text(text = "Damping Factor: ${dampingFactor.format(2)}")
                    Slider(
                        value = dampingFactor,
                        onValueChange = {
                            dampingFactor = it
                            webViewInstance?.evaluateJavascript(
                                "javascript:setDampingFactor(${
                                    it.format(
                                        2
                                    )
                                });", null
                            )
                        },
                        valueRange = 0f..0.15f
                    )

                    Spacer(modifier = Modifier.heightIn(36.dp))
                    Text(
                        text = "Charger un modèle",
                        style = androidx.compose.material3.MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(top = 8.dp, bottom = 12.dp)
                    )
                    Button(
                        onClick = {
                            filePickerLauncher.launch(
                                arrayOf(
                                    "application/octet-stream",
                                    "model/gltf+json"
                                )
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            Icons.Filled.AddCircle,
                            contentDescription = "Ouvrir le sélecteur de fichiers"
                        )
                        Text("Sélectionner un fichier GLTF/GLB")
                    }
                }
            }
        }
    } // Fin de la Box
}

// Helper format (si pas déjà là)
fun Float.format(digits: Int) = String.format(Locale.US, "%.${digits}f", this)