package com.dirosky.asianbets.services

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.room.Room
import com.dirosky.asianbets.data.db.AsianBetsDatabase
import com.dirosky.asianbets.data.api.ScoreTrendApi
import com.dirosky.asianbets.data.models.*
import com.dirosky.asianbets.domain.filters.BeVixTntFilter
import com.dirosky.asianbets.utils.NotificationHelper
import kotlinx.coroutines.*
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit

/**
 * Serviço de monitoramento contínuo de jogos
 * 
 * Equivalente ao loop principal (Célula 9) do Python:
 * - Re-fetch de odds a cada 3 minutos
 * - Poll de jogos live a cada 30 segundos
 * - Emissão de alertas via notificação
 * - Persistência em Room database
 */
class MonitorService : Service() {
    
    companion object {
        private const val TAG = "MonitorService"
        private const val NOTIFICATION_ID = 1000
        
        // Configurações do loop (equivalente a LOOP_CONFIG)
        const val INTERVALO_ODDS_MS = 3 * 60 * 1000L      // 3 minutos
        const val INTERVALO_LIVE_MS = 30 * 1000L          // 30 segundos
        const val MAX_MINUTO = 25
        const val ODDS_MIN = 1.65
        const val ODDS_MAX = 2.10
    }
    
    private val serviceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private lateinit var database: AsianBetsDatabase
    private lateinit var api: ScoreTrendApi
    
    private var lastOddsFetch = 0L
    private val jogosEnviados = mutableSetOf<String>() // Controle de jogos já processados
    
    override fun onCreate() {
        super.onCreate()
        
        // Inicializar database (Room)
        database = Room.databaseBuilder(
            applicationContext,
            AsianBetsDatabase::class.java,
            "asian_bets.db"
        ).build()
        
        // Inicializar API (Retrofit)
        val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
        
        val retrofit = Retrofit.Builder()
            .baseUrl("https://api_v2.scoretrend.net/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        
        api = retrofit.create(ScoreTrendApi::class.java)
        
        Log.d(TAG, "Service criado")
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "Service iniciado")
        
        // Criar canais de notificação
        NotificationHelper.createNotificationChannels(this)
        
        // Iniciar como Foreground Service (notificação persistente)
        val notification = NotificationHelper.createServiceNotification(this, 0, 0)
        startForeground(NOTIFICATION_ID, notification)
        
        // Iniciar loop de monitoramento
        serviceScope.launch {
            try {
                monitorLoop()
            } catch (e: Exception) {
                Log.e(TAG, "Erro no loop de monitoramento", e)
            }
        }
        
        return START_STICKY // Reinicia se o sistema matar o serviço
    }
    
    /**
     * Loop principal de monitoramento
     * Equivalente a loop_continuo() do Python
     */
    private suspend fun monitorLoop() {
        val hoje = LocalDate.now()
        val hojeIso = hoje.toString() // "2026-03-03"
        val hojeComp = hoje.format(DateTimeFormatter.ofPattern("yyyyMMdd")) // "20260303"
        
        Log.d(TAG, "Loop iniciado para $hojeIso")
        
        while (true) {
            val agora = System.currentTimeMillis()
            
            // ══════════════════════════════════════════════════════════════════
            // BLOCO A: RE-FETCH ODDS (a cada 3 minutos)
            // ══════════════════════════════════════════════════════════════════
            if (agora - lastOddsFetch >= INTERVALO_ODDS_MS) {
                lastOddsFetch = agora
                
                Log.d(TAG, "Re-fetch de odds...")
                refetchOdds(hojeComp, hojeIso)
            }
            
            // ══════════════════════════════════════════════════════════════════
            // BLOCO B: POLL LIVE (a cada 30 segundos)
            // ══════════════════════════════════════════════════════════════════
            val jogosAtivos = database.jogoDao().getJogosAtivos(hojeIso)
            
            Log.d(TAG, "Polling ${jogosAtivos.size} jogos ativos...")
            
            for (jogo in jogosAtivos) {
                processarJogoLive(jogo)
            }
            
            // Atualizar notificação do serviço
            val totalJogos = database.jogoDao().countJogos(hojeIso)
            val totalApostas = database.jogoDao().countApostas(hojeIso)
            NotificationHelper.updateServiceNotification(this, totalJogos, totalApostas)
            
            // Aguardar próximo ciclo
            delay(INTERVALO_LIVE_MS)
        }
    }
    
    /**
     * Re-fetch de odds da API
     * Equivalente a refetch_odds() + bloco de filtro do Python
     */
    private suspend fun refetchOdds(hojeComp: String, hojeIso: String) {
        try {
            // Buscar odds do dia
            val eventos = api.getOddsData(hojeComp)
            
            Log.d(TAG, "Recebidos ${eventos.size} eventos da API")
            
            var novosInseridos = 0
            
            for (evento in eventos) {
                val eventId = evento.event_id
                
                // Pular se já processado
                if (eventId in jogosEnviados) continue
                
                // Verificar se tem dados Bet365
                val bet365Data = evento.data?.get("Bet365") ?: continue
                
                // Aplicar filtro BeVIX-1 ∩ TNT-A
                val filterResult = BeVixTntFilter.apply(bet365Data) ?: continue
                
                // Status inicial
                val statusInicial = mapearStatus(evento.status)
                
                // Extrair informações do evento
                val home = evento.event_data?.home?.name ?: "?"
                val away = evento.event_data?.away?.name ?: "?"
                val league = evento.event_data?.league?.name ?: "?"
                val eventTime = extrairEventTime(evento)
                val eventTs = extrairEventTs(evento)
                
                // Criar entity
                val jogoEntity = JogoEntity(
                    eventId = eventId,
                    home = home,
                    away = away,
                    league = league,
                    delta = filterResult.delta,
                    lineClose = filterResult.lineClose,
                    hcOpen = filterResult.hcOpen,
                    nivel = filterResult.toNivelString(),
                    prob = filterResult.toProbString(),
                    status = statusInicial,
                    data = hojeIso,
                    eventTime = eventTime,
                    eventTs = eventTs
                )
                
                // Inserir no banco (se não existe)
                val inserted = database.jogoDao().insertIfNotExists(jogoEntity)
                
                if (inserted > 0) {
                    novosInseridos++
                    jogosEnviados.add(eventId)
                    
                    Log.d(TAG, "Novo jogo: ${filterResult.nivel.emoji} $home vs $away (δ=${filterResult.delta})")
                }
            }
            
            Log.d(TAG, "Re-fetch concluído: $novosInseridos novos jogos")
            
        } catch (e: Exception) {
            Log.e(TAG, "Erro no re-fetch de odds", e)
        }
    }
    
    /**
     * Processar jogo live
     * Equivalente a processar_jogo() do Python
     */
    private suspend fun processarJogoLive(jogo: JogoEntity) {
        try {
            // Buscar dados live
            val liveData = api.getLiveOdds(jogo.eventId) ?: return
            
            // Parse do live
            val parsedLive = parseLive(liveData)
            val minuto = parsedLive.minuto ?: 0
            val score = parsedLive.score ?: ""
            val odd = parsedLive.htOverOdd
            
            // Status
            val status = mapearStatus(liveData.status)
            
            // Atualizar status e placar
            database.jogoDao().updateLiveStatus(
                eventId = jogo.eventId,
                status = status,
                minutoLive = minuto,
                scoreLive = score
            )
            
            // Guardar kick_ts (primeira vez que detecta Live)
            if (status == "Live" && minuto >= 1 && jogo.kickTs.isEmpty()) {
                val kickTs = LocalDateTime.now().toString()
                database.jogoDao().updateKickTs(jogo.eventId, kickTs)
            }
            
            // ══════════════════════════════════════════════════════════════════
            // ALERTA LIVE (condições para apostar)
            // ══════════════════════════════════════════════════════════════════
            if (!jogo.alertaLive &&
                status == "Live" &&
                score == "0-0" &&
                minuto in 1..MAX_MINUTO &&
                odd != null &&
                odd in ODDS_MIN..ODDS_MAX
            ) {
                // Criar alerta
                val alerta = AlertaEntity(
                    ts = LocalDateTime.now().toString(),
                    eventId = jogo.eventId,
                    tipo = "LIVE",
                    minuto = minuto,
                    score = score,
                    odd = odd,
                    notificationSent = true
                )
                
                val alertaId = database.alertaDao().insert(alerta)
                
                // Atualizar jogo
                database.jogoDao().updateAlerta(
                    eventId = jogo.eventId,
                    alertaLive = true,
                    oddLive = odd,
                    logId = alertaId
                )
                
                // ENVIAR NOTIFICAÇÃO
                NotificationHelper.alertaAposta(
                    context = this,
                    jogo = jogo.copy(alertaLive = true, oddLive = odd),
                    minuto = minuto,
                    score = score,
                    odd = odd
                )
                
                Log.i(TAG, "🎯 ALERTA: ${jogo.home} vs ${jogo.away} | min=$minuto odd=$odd")
            }
            
            // ══════════════════════════════════════════════════════════════════
            // RESULTADO HT (calcular GREEN/RED)
            // ══════════════════════════════════════════════════════════════════
            if ((status == "HT" || status == "Ended") &&
                jogo.alertaLive &&
                jogo.logId != null
            ) {
                val (golsCasa, golsFora) = parseScore(score)
                
                if (golsCasa != null && golsFora != null) {
                    val totalGolos = golsCasa + golsFora
                    val resultado = if (totalGolos >= 1) "🟢 GREEN" else "🔴 RED"
                    
                    // Atualizar alerta
                    database.alertaDao().updateResultado(
                        id = jogo.logId,
                        htScore = score,
                        resultado = resultado
                    )
                    
                    // Atualizar jogo
                    database.jogoDao().updateHtScore(jogo.eventId, score)
                    
                    // ENVIAR NOTIFICAÇÃO DE RESULTADO
                    if (status == "HT") {
                        NotificationHelper.resultado(
                            context = this,
                            jogo = jogo.copy(htScore = score),
                            resultado = resultado,
                            htScore = score,
                            ftScore = null,
                            odd = jogo.oddLive ?: 0.0
                        )
                        
                        Log.i(TAG, "${if (resultado.contains("GREEN")) "✅" else "❌"} ${jogo.home} vs ${jogo.away} | $resultado HT=$score")
                    }
                }
            }
            
            // ══════════════════════════════════════════════════════════════════
            // RESULTADO FT
            // ══════════════════════════════════════════════════════════════════
            if (status == "Ended") {
                val ftScore = liveData.result?.let { "${it["home"]}-${it["away"]}" }
                    ?: liveData.scores?.let { "${it["home"]}-${it["away"]}" }
                    ?: score
                
                if (ftScore.isNotEmpty()) {
                    database.jogoDao().updateFtScore(jogo.eventId, ftScore)
                    
                    if (jogo.logId != null) {
                        database.alertaDao().updateFtScore(jogo.logId, ftScore)
                    }
                }
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao processar jogo ${jogo.eventId}", e)
        }
    }
    
    // ══════════════════════════════════════════════════════════════════════
    // FUNÇÕES AUXILIARES (equivalentes às do Python)
    // ══════════════════════════════════════════════════════════════════════
    
    private fun mapearStatus(raw: Any?): String {
        return when {
            raw is Int -> when (raw) {
                0 -> "Not Started"
                1 -> "Live"
                2 -> "Ended"
                3 -> "HT"
                else -> "Not Started"
            }
            raw is String -> when (raw.lowercase()) {
                "not started", "notstarted", "scheduled" -> "Not Started"
                "live", "inprogress", "in_progress" -> "Live"
                "halftime", "ht", "interval" -> "HT"
                "ended", "finished", "fulltime", "ft" -> "Ended"
                else -> raw
            }
            else -> "Not Started"
        }
    }
    
    private fun parseLive(data: LiveOddsResponse): ParsedLive {
        val bet365 = data.odds?.get("Bet365") ?: return ParsedLive()
        
        val market11 = bet365.`1_1`?.firstOrNull() ?: return ParsedLive()
        
        val score = market11.ss
        val minuto = market11.time_str?.toIntOrNull()
        
        val market16 = bet365.`1_6`?.firstOrNull()
        val htOverOdd = market16?.over_od?.toDoubleOrNull()
        
        return ParsedLive(
            minuto = minuto,
            score = score,
            htOverOdd = htOverOdd
        )
    }
    
    private data class ParsedLive(
        val minuto: Int? = null,
        val score: String? = null,
        val htOverOdd: Double? = null
    )
    
    private fun parseScore(score: String): Pair<Int?, Int?> {
        return try {
            val parts = score.split("-")
            Pair(parts[0].toInt(), parts[1].toInt())
        } catch (e: Exception) {
            Pair(null, null)
        }
    }
    
    private fun extrairEventTime(evento: EventData): String? {
        // Tentar extrair hora do evento
        val time = evento.time ?: evento.event_data?.time ?: evento.event_data?.start_time
        return time?.take(5) // "14:30"
    }
    
    private fun extrairEventTs(evento: EventData): Long? {
        // Tentar extrair timestamp Unix
        val time = evento.time ?: evento.event_data?.time
        return time?.toLongOrNull()
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        Log.d(TAG, "Service destruído")
    }
}
