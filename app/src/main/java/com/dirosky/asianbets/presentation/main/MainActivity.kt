package com.dirosky.asianbets.presentation.main

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate

class MainActivity : AppCompatActivity() {
    
    companion object {
        private const val REQUEST_NOTIFICATION_PERMISSION = 100
    }
    
    private lateinit var database: AsianBetsDatabase
    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyView: TextView
    private lateinit var adapter: JogosAdapter
    private var currentFilter: String? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        setSupportActionBar(findViewById(R.id.toolbar))
        
        // Inicializar database
        database = Room.databaseBuilder(
            applicationContext,
            AsianBetsDatabase::class.java,
            "asian_bets.db"
        ).build()
        
        // Setup RecyclerView
        recyclerView = findViewById(R.id.recyclerViewJogos)
        emptyView = findViewById(R.id.emptyView)
        adapter = JogosAdapter()
        
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter
        
        // FAB refresh
        findViewById<FloatingActionButton>(R.id.fabRefresh).setOnClickListener {
            loadJogos()
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
        startMonitorService()
        
        // Carregar jogos
        loadJogos()
        
        // Auto-refresh a cada 30 segundos
        startAutoRefresh()
    }
    
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
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
                Toast.makeText(this, "Mostrando todos os níveis", Toast.LENGTH_SHORT).show()
                true
            }
            R.id.filter_a -> {
                currentFilter = "A"
                loadJogos()
                Toast.makeText(this, "🔥 Filtrando: Nível A (FORTE)", Toast.LENGTH_SHORT).show()
                true
            }
            R.id.filter_b -> {
                currentFilter = "B"
                loadJogos()
                Toast.makeText(this, "🔵 Filtrando: Nível B (BOM)", Toast.LENGTH_SHORT).show()
                true
            }
            R.id.filter_c -> {
                currentFilter = "C"
                loadJogos()
                Toast.makeText(this, "📊 Filtrando: Nível C (BASE)", Toast.LENGTH_SHORT).show()
                true
            }
            R.id.action_stats -> {
                showStats()
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
                val hoje = LocalDate.now().toString()
                val jogos = database.jogoDao().getJogosByData(hoje)
                
                val filtrados = if (currentFilter != null) {
                    jogos.filter { it.nivel == currentFilter }
                } else {
                    jogos
                }
                
                runOnUiThread {
                    if (filtrados.isEmpty()) {
                        recyclerView.visibility = View.GONE
                        emptyView.visibility = View.VISIBLE
                    } else {
                        recyclerView.visibility = View.VISIBLE
                        emptyView.visibility = View.GONE
                        adapter.updateJogos(filtrados)
                    }
                }
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(
                        this@MainActivity,
                        "Erro ao carregar: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }
    
    private fun showStats() {
        lifecycleScope.launch {
            try {
                val hoje = LocalDate.now().toString()
                val total = database.jogoDao().countJogos(hoje)
                val apostas = database.jogoDao().countApostas(hoje)
                
                runOnUiThread {
                    Toast.makeText(
                        this@MainActivity,
                        "📊 Total: $total jogos | 🎯 Apostas: $apostas",
                        Toast.LENGTH_LONG
                    ).show()
                }
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(
                        this@MainActivity,
                        "Erro: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }
    
    private fun startAutoRefresh() {
        lifecycleScope.launch {
            while (true) {
                delay(30000) // 30 segundos
                loadJogos()
            }
        }
    }
    
    private fun startMonitorService() {
        val intent = Intent(this, MonitorService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }
}
