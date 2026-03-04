package com.dirosky.asianbets.presentation.main

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
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
import kotlinx.coroutines.launch
import java.time.LocalDate

class MainActivity : AppCompatActivity() {
    
    companion object {
        private const val REQUEST_NOTIFICATION_PERMISSION = 100
    }
    
    private lateinit var database: AsianBetsDatabase
    private lateinit var recyclerView: RecyclerView
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
        recyclerView.layoutManager = LinearLayoutManager(this)
        
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
                true
            }
            R.id.filter_a -> {
                currentFilter = "A"
                loadJogos()
                true
            }
            R.id.filter_b -> {
                currentFilter = "B"
                loadJogos()
                true
            }
            R.id.filter_c -> {
                currentFilter = "C"
                loadJogos()
                true
            }
            R.id.action_stats -> {
                showStats()
                true
            }
            R.id.action_settings -> {
                // TODO: Abrir tela de configurações
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
                    // TODO: Atualizar adapter do RecyclerView
                    Toast.makeText(
                        this@MainActivity,
                        "${filtrados.size} jogos encontrados",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(
                        this@MainActivity,
                        "Erro ao carregar jogos: ${e.message}",
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
    
    private fun startMonitorService() {
        val intent = Intent(this, MonitorService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }
}
