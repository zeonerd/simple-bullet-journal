package com.simple.bulletjournal

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.simple.bulletjournal.ui.MainScreen
import com.simple.bulletjournal.ui.theme.BulletJournalTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BulletJournalTheme {
                MainScreen()
            }
        }
    }
}
