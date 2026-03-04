package com.dirosky.asianbets.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.dirosky.asianbets.R
import com.dirosky.asianbets.data.models.JogoEntity
import com.dirosky.asianbets.domain.filters.BeVixTntFilter
import com.dirosky.asianbets.presentation.main.MainActivity

/**
 * Helper para gerenciar notificações do sistema Android
 * Substitui o envio de mensagens via Telegram
 */
object NotificationHelper {
    
    // IDs dos canais de notificação
    private const val CHANNEL_ALERTS = "asian_bets_alerts"
    private const val CHANNEL_RESULTS = "asian_bets_results"
    private const val CHANNEL_SERVICE = "asian_bets_service"
    
    // IDs de notificação
    private const val NOTIFICATION_SERVICE = 1000
    
    /**
     * Cria os canais de notificação (necessário para Android 8.0+)
     * Deve ser chamado ao iniciar o app
     */
    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            
            // Canal de alertas de aposta (prioridade alta, com som)
            val alertsChannel = NotificationChannel(
                CHANNEL_ALERTS,
                "Alertas de Aposta",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notificações quando surge oportunidade de apostar"
                enableVibration(true)
                setShowBadge(true)
            }
            
            // Canal de resultados (prioridade normal)
            val resultsChannel = NotificationChannel(
                CHANNEL_RESULTS,
                "Resultados",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Resultados de apostas (GREEN/RED)"
                setShowBadge(true)
            }
            
            // Canal do serviço de monitoramento (prioridade baixa, sem som)
            val serviceChannel = NotificationChannel(
                CHANNEL_SERVICE,
                "Monitoramento",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Notificação persistente do serviço de monitoramento"
                setShowBadge(false)
            }
            
            notificationManager.createNotificationChannels(
                listOf(alertsChannel, resultsChannel, serviceChannel)
            )
        }
    }
    
    /**
     * Notificação de alerta de aposta (quando jogo atinge condições ideais)
     * 
     * Equivalente a: tg_send(CH_PUBLICO, msg_alerta_live(...))
     */
    fun alertaAposta(
        context: Context,
        jogo: JogoEntity,
        minuto: Int,
        score: String,
        odd: Double
    ) {
        val nivelInfo = BeVixTntFilter.Nivel.valueOf(jogo.nivel)
        
        // Intent para abrir Bet365 (deep link)
        val bet365Intent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse(createBet365Link(jogo.home))
        }
        val bet365PendingIntent = PendingIntent.getActivity(
            context, 
            jogo.eventId.hashCode(), 
            bet365Intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        
        // Intent para abrir detalhes do jogo no app
        val detailsIntent = Intent(context, MainActivity::class.java).apply {
            putExtra("eventId", jogo.eventId)
            putExtra("openDetails", true)
        }
        val detailsPendingIntent = PendingIntent.getActivity(
            context,
            jogo.eventId.hashCode() + 1,
            detailsIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        
        // Texto expandido (BigTextStyle)
        val bigText = """
            ⏱️ Minuto: $minuto'
            📊 Score: $score
            💰 Odd HT Over: $odd
            📉 δ=${String.format("%+.2f", jogo.delta)} | linha=${String.format("%.2f", jogo.lineClose)}
            🧠 Prob Over 0.5 HT: ${jogo.prob}
            📈 win=${(nivelInfo.winHist * 100).toInt()}% | ROI@1.70=${String.format("%+.3f", nivelInfo.roi)}
        """.trimIndent()
        
        val notification = NotificationCompat.Builder(context, CHANNEL_ALERTS)
            .setSmallIcon(R.drawable.ic_bet_notification)
            .setContentTitle("${nivelInfo.emoji} APOSTAR AGORA - ${nivelInfo.label}")
            .setContentText("${jogo.home} vs ${jogo.away}")
            .setStyle(NotificationCompat.BigTextStyle()
                .setBigContentTitle("${nivelInfo.emoji} APOSTAR AGORA - ${nivelInfo.label}")
                .bigText("⚽ ${jogo.home} vs ${jogo.away}\n🏆 ${jogo.league}\n\n$bigText"))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(detailsPendingIntent)
            .addAction(
                R.drawable.ic_bet365,
                "Bet365",
                bet365PendingIntent
            )
            .addAction(
                R.drawable.ic_details,
                "Detalhes",
                detailsPendingIntent
            )
            .build()
        
        NotificationManagerCompat.from(context).notify(
            jogo.eventId.hashCode(),
            notification
        )
    }
    
    /**
     * Notificação de resultado (GREEN ou RED)
     * 
     * Equivalente a: tg_send(CH_ANALISE, msg_resultado(...))
     */
    fun resultado(
        context: Context,
        jogo: JogoEntity,
        resultado: String, // "🟢 GREEN" ou "🔴 RED"
        htScore: String,
        ftScore: String?,
        odd: Double
    ) {
        val nivelInfo = BeVixTntFilter.Nivel.valueOf(jogo.nivel)
        val isGreen = resultado.contains("GREEN")
        
        val emoji = if (isGreen) "✅✅✅" else "❌❌❌"
        val title = if (isGreen) "$emoji OVER - Aposta ganha!" else "$emoji Sem Over - Aposta perdida"
        
        val detailsIntent = Intent(context, MainActivity::class.java).apply {
            putExtra("eventId", jogo.eventId)
            putExtra("openDetails", true)
        }
        val detailsPendingIntent = PendingIntent.getActivity(
            context,
            jogo.eventId.hashCode() + 2,
            detailsIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        
        val ftLine = if (!ftScore.isNullOrEmpty()) "\n🏁 FT: $ftScore" else ""
        val bigText = """
            ${jogo.home} vs ${jogo.away}
            🏆 ${jogo.league}
            
            📊 HT: $htScore$ftLine
            💰 Odd: $odd | 🧠 Prob: ${jogo.prob}
            📈 ${nivelInfo.emoji} ${nivelInfo.label} | win=${(nivelInfo.winHist * 100).toInt()}%
        """.trimIndent()
        
        val notification = NotificationCompat.Builder(context, CHANNEL_RESULTS)
            .setSmallIcon(if (isGreen) R.drawable.ic_win else R.drawable.ic_loss)
            .setContentTitle(title)
            .setContentText("${jogo.home} vs ${jogo.away} | HT: $htScore")
            .setStyle(NotificationCompat.BigTextStyle()
                .setBigContentTitle(title)
                .bigText(bigText))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(detailsPendingIntent)
            .build()
        
        NotificationManagerCompat.from(context).notify(
            jogo.eventId.hashCode() + 100,
            notification
        )
    }
    
    /**
     * Notificação persistente do serviço (Foreground Service)
     * Mostra status de monitoramento
     */
    fun createServiceNotification(
        context: Context,
        jogosMonitorados: Int,
        apostasAtivas: Int
    ): android.app.Notification {
        val intent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE
        )
        
        return NotificationCompat.Builder(context, CHANNEL_SERVICE)
            .setSmallIcon(R.drawable.ic_monitor)
            .setContentTitle("Robo Dirosky • Monitorando")
            .setContentText("$jogosMonitorados jogos | $apostasAtivas apostas")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .build()
    }
    
    /**
     * Atualiza notificação do serviço
     */
    fun updateServiceNotification(
        context: Context,
        jogosMonitorados: Int,
        apostasAtivas: Int
    ) {
        val notification = createServiceNotification(context, jogosMonitorados, apostasAtivas)
        NotificationManagerCompat.from(context).notify(NOTIFICATION_SERVICE, notification)
    }
    
    /**
     * Cancela todas as notificações de um jogo específico
     */
    fun cancelJogoNotifications(context: Context, eventId: String) {
        val notificationManager = NotificationManagerCompat.from(context)
        val baseId = eventId.hashCode()
        notificationManager.cancel(baseId)       // Alerta
        notificationManager.cancel(baseId + 100) // Resultado
    }
    
    /**
     * Cancela todas as notificações do app
     */
    fun cancelAllNotifications(context: Context) {
        NotificationManagerCompat.from(context).cancelAll()
    }
    
    /**
     * Cria link para Bet365 (mesmo algoritmo do Python)
     */
    private fun createBet365Link(teamName: String): String {
        val cleanName = teamName
            .replace(Regex("\\(.*?\\)"), "")
            .replace(Regex("[^\\w\\s-]"), "")
            .trim()
        
        val encoded = Uri.encode(cleanName)
        return "https://www.bet365.com/#/AX/K%5E$encoded"
    }
}
