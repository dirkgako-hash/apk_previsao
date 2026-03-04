package com.dirosky.asianbets.presentation.main

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.room.Room
import com.dirosky.asianbets.R
import com.dirosky.asianbets.data.db.AsianBetsDatabase
import com.dirosky.asianbets.services.MonitorService
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {
    
    companion object {
        private const val TAG = "MainActivity"
        private const val REQUEST_NOTIFICATION_PERMISSION = 100
    }
    
    private var database: AsianBetsDatabase? = null
    private var recyclerView: RecyclerView? = null
    private var emptyView: TextView? = null
    private var adapter: JogosAdapter? = null
    private var currentFilter: String? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        try {
            setContentView(R.layout.activity_main)
            Log.d(TAG, "Layout inflated successfully")
            
            // Toolbar
            try {
                setSupportActionBar(findViewById(R.id.toolbar))
                Log.d(TAG, "Toolbar set")
            } catch (e: Exception) {
                Log.e(TAG, "Error setting toolbar", e)
            }
            
            // Inicializar database
            try {
                database = Room.databaseBuilder(
                    applicationContext,
                    AsianBetsDatabase::class.java,
                    "asian_bets.db"
                ).fallbackToDestructiveMigration()
                 .build()
                Log.d(TAG, "Database initialized")
            } catch (e: Exception) {
                Log.e(TAG, "Error initializing database", e)
                Toast.makeText(this, "Erro ao inicializar DB: ${e.message}", Toast.LENGTH_LONG).show()
            }
            
            // Setup RecyclerView
            try {
                recyclerView = findViewById(R.id.recyclerViewJogos)
                emptyView = findViewById(R.id.emptyView)
                adapter = JogosAdapter()
                
                recyclerView?.layoutManager = LinearLayoutManager(this)
                recyclerView?.adapter = adapter
                Log.d(TAG, "RecyclerView configured")
            } catch (e: Exception) {
                Log.e(TAG, "Error setting up RecyclerView", e)
                Toast.makeText(this, "Erro ao configurar lista: ${e.message}", Toast.LENGTH_LONG).show()
            }
            
            // FAB refresh
            try {
                findViewById<FloatingActionButton>(R.id.fabRefresh)?.setOnClickListener {
                    loadJogos()
                }
                Log.d(TAG, "FAB configured")
            } catch (e: Exception) {
                Log.e(TAG, "Error setting up FAB", e)
            }
            
            // Solicitar permissão de notificações (Android 13+)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) 
                    != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(
                        this,
                        arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                        REQUEST_NOTIFICATION_PERMISSION
                    )
                }
            }
            
            // Iniciar serviço de monitoramento
            try {
                startMonitorService()
                Log.d(TAG, "Service started")
            } catch (e: Exception) {
                Log.e(TAG, "Error starting service", e)
                Toast.makeText(this, "Erro ao iniciar serviço: ${e.message}", Toast.LENGTH_LONG).show()
            }
            
            // Carregar jogos (após um delay para garantir que tudo está pronto)
            lifecycleScope.launch {
                try {
                    kotlinx.coroutines.delay(1000)
                    loadJogos()
                } catch (e: Exception) {
                    Log.e(TAG, "Error loading initial games", e)
                }
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Critical error in onCreate", e)
            Toast.makeText(this, "Erro crítico: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
    
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        return try {
            menuInflater.inflate(R.menu.main_menu, menu)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error creating menu", e)
            false
        }
    }
    
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_refresh -> {
                loadJogos()
                Toast.makeText(this, "Atualizando...", Toast.LENGTH_SHORT).show()
                true
            }
            R.id.filter_all -> {
                currentFilter = null
                loadJogos()
                Toast.makeText(this, "Todos os níveis", Toast.LENGTH_SHORT).show()
                true
            }
            R.id.filter_a -> {
                currentFilter = "A"
                loadJogos()
                Toast.makeText(this, "🔥 Nível A", Toast.LENGTH_SHORT).show()
                true
            }
            R.id.filter_b -> {
                currentFilter = "B"
                loadJogos()
                Toast.makeText(this, "🔵 Nível B", Toast.LENGTH_SHORT).show()
                true
            }
            R.id.filter_c -> {
                currentFilter = "C"
                loadJogos()
                Toast.makeText(this, "📊 Nível C", Toast.LENGTH_SHORT).show()
                true
            }
            R.id.action_stats -> {
                showStats()
                true
            }
            R.id.action_debug -> {
                try {
                    startActivity(Intent(this, Class.forName("com.dirosky.asianbets.presentation.debug.DebugActivity")))
                } catch (e: Exception) {
                    Toast.makeText(this, "Erro ao abrir debug: ${e.message}", Toast.LENGTH_LONG).show()
                }
                true
            }
            R.id.action_settings -> {
                Toast.makeText(this, "Configurações em breve", Toast.LENGTH_SHORT).show()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
    
    private fun loadJogos() {
        lifecycleScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val hoje = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        java.time.LocalDate.now().toString()
                    } else {
                        java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                            .format(java.util.Date())
                    }
                    
                    Log.d(TAG, "Loading games for: $hoje")
                    
                    val jogos = database?.jogoDao()?.getJogosByData(hoje) ?: emptyList()
                    Log.d(TAG, "Found ${jogos.size} games")
                    
                    val filtrados = if (currentFilter != null) {
                        jogos.filter { it.nivel == currentFilter }
                    } else {
                        jogos
                    }
                    
                    withContext(Dispatchers.Main) {
                        if (filtrados.isEmpty()) {
                            recyclerView?.visibility = View.GONE
                            emptyView?.visibility = View.VISIBLE
                        } else {
                            recyclerView?.visibility = View.VISIBLE
                            emptyView?.visibility = View.GONE
                            adapter?.updateJogos(filtrados)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading games", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@MainActivity,
                        "Erro: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }
    
    private fun showStats() {
        lifecycleScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val hoje = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        java.time.LocalDate.now().toString()
                    } else {
                        java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                            .format(java.util.Date())
                    }
                    
                    val total = database?.jogoDao()?.countJogos(hoje) ?: 0
                    val apostas = database?.jogoDao()?.countApostas(hoje) ?: 0
                    
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            this@MainActivity,
                            "📊 Total: $total | 🎯 Apostas: $apostas",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error showing stats", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@MainActivity,
                        "Erro: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }
    
    private fun startMonitorService() {
        try {
            val intent = Intent(this, MonitorService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }
            Log.d(TAG, "Monitor service started")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start monitor service", e)
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        database = null
    }
}

// Adicionar ao final da classe MainActivity, antes do último }
private fun openDebug() {
    startActivity(android.content.Intent(this, 
        Class.forName("com.dirosky.asianbets.presentation.debug.DebugActivity")))
}
