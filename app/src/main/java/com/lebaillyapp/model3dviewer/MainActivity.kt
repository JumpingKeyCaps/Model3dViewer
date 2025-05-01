package com.lebaillyapp.model3dviewer


import android.os.Bundle
import android.webkit.WebView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import com.lebaillyapp.model3dviewer.ui.theme.Model3dViewerTheme
import androidx.compose.ui.Modifier
import com.lebaillyapp.model3dviewer.composition.GyroscopeControlledViewer
import com.lebaillyapp.model3dviewer.composition.ThreeDViewer
import com.lebaillyapp.model3dviewer.composition.TransparentThreeDViewer


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WebView.setWebContentsDebuggingEnabled(true);
        enableEdgeToEdge()
        setContent {
            Model3dViewerTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->


                    ThreeDViewer(modifier = Modifier.padding(innerPadding).fillMaxSize())
                   // TransparentThreeDViewer(modifier = Modifier.fillMaxSize().padding(innerPadding))
                   // GyroscopeControlledViewer(modifier = Modifier.fillMaxSize().padding(innerPadding))


                }
            }
        }
    }
}
