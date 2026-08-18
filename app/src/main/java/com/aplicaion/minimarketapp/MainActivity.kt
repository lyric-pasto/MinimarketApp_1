package com.aplicaion.minimarketapp

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.aplicaion.minimarketapp.api.JsonDatabaseManager
import com.aplicaion.minimarketapp.db.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lifecycleScope.launch(Dispatchers.IO) {
            val db = AppDatabase.getInstance(applicationContext)
            JsonDatabaseManager.seedDatabaseFromAssets(applicationContext, db)
        }
        val intent = Intent(this, inicio_sesion::class.java)
        startActivity(intent)
        finish()
    }
}
