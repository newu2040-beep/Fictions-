package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity

/**
 * Forwarding entry point kept in com.example so any legacy test harness referencing
 * com.example.MainActivity launches com.editingcells.fictions.MainActivity seamlessly.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        startActivity(Intent(this, com.editingcells.fictions.MainActivity::class.java))
        finish()
    }
}
