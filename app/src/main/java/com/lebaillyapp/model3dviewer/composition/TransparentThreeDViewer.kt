package com.lebaillyapp.model3dviewer.composition

import android.annotation.SuppressLint
import android.graphics.Color
import android.view.View
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.lebaillyapp.model3dviewer.R

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun TransparentThreeDViewer(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize()) {
        // Image de fond
        Image(
            painter = painterResource(id = R.drawable.designedbck),
            contentScale = ContentScale.Crop,
            contentDescription = "Background Image",
            modifier = Modifier.fillMaxSize()
        )
        // WebView avec fond transparent
        AndroidView(
            factory = { context ->
                WebView(context).apply {
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.allowFileAccess = true
                    settings.allowContentAccess = true
                    settings.allowFileAccessFromFileURLs = true
                    settings.allowUniversalAccessFromFileURLs = true
                    webViewClient = WebViewClient()
                    setBackgroundColor(Color.TRANSPARENT) // Rendre le fond de la WebView transparent
                    overScrollMode = View.OVER_SCROLL_NEVER
                    loadUrl("file:///android_asset/transparent_viewer.html")
                }
            },
            update = { /* Aucune mise à jour spécifique nécessaire ici pour la transparence */ },
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp)
                .clip(RoundedCornerShape(10.dp))
        )





    }
}