package com.dirosky.asianbets.data.models

import androidx.room.*
import com.dirosky.asianbets.domain.filters.BeVixTntFilter

// ═══════════════════════════════════════════════════════════════════════════
// ENTITIES (Tabelas SQLite)
// ═══════════════════════════════════════════════════════════════════════════

/**
 * Tabela principal de jogos filtrados
 * Equivalente à tabela "jogos" do Python
 */
@Entity(
    tableName = "jogos",
    indices = [
        Index(value = ["eventId"], unique = true),
        Index(value = ["data", "status"]),
        Index(value = ["nivel"])
    ]
)
data class JogoEntity(
    @PrimaryKey
    val eventId: String,
    
    val home: String,
    val away: String,
    val league: String,
    
    // Métricas do filtro
    val delta: Double,
    val lineClose: Double,
    val hcOpen: Double?,
    
    @ColumnInfo(defaultValue = "C")
    val nivel: String, // A, B, C
    
    val prob: String, // "89%"
    
    // Estado do jogo
    @ColumnInfo(defaultValue = "Not Started")
    val status: String, // Not Started, Live, HT, Ended
    
    @ColumnInfo(defaultValue = "0")
    val alertaLive: Boolean = false,
    
    val oddLive: Double? = null,
    val logId: Long? = null, // FK para AlertaEntity
    
    // Timestamps e scores
    val data: String, // "2026-03-03"
    val eventTime: String?, // "14:30"
    val eventTs: Long? = null, // Unix timestamp
    
    @ColumnInfo(defaultValue = "0")
    val minutoLive: Int = 0,
    
    @ColumnInfo(defaultValue = "")
    val scoreLive: String = "",
    
    @ColumnInfo(defaultValue = "")
    val htScore: String = "",
    
    @ColumnInfo(defaultValue = "")
    val ftScore: String = "",
    
    @ColumnInfo(defaultValue = "")
    val kickTs: String = "" // ISO timestamp do 1º minuto detectado
)

/**
 * Tabela de alertas emitidos
 * Equivalente à tabela "alertas" do Python
 */
@Entity(
    tableName = "alertas",
    foreignKeys = [
        ForeignKey(
            entity = JogoEntity::class,
            parentColumns = ["eventId"],
            childColumns = ["eventId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["eventId"])]
)
data class AlertaEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    val ts: String, // ISO timestamp
    val eventId: String,
    val tipo: String, // "LIVE"
    
    val minuto: Int,
    val score: String, // "0-0"
    val odd: Double,
    
    val htScore: String? = null,
    val ftScore: String? = null,
    val resultado: String? = null, // "🟢 GREEN" ou "🔴 RED"
    
    @ColumnInfo(defaultValue = "0")
    val tgPub: Boolean = false,
    
    @ColumnInfo(defaultValue = "0")
    val tgAnal: Boolean = false,
    
    @ColumnInfo(defaultValue = "0")
    val notificationSent: Boolean = false // NOVO: controla se notificação foi enviada
)

/**
 * Tabela de intervalos (pós-processamento)
 * Para análise de golos por período
 */
@Entity(
    tableName = "intervalos",
    primaryKeys = ["eventId", "periodo"]
)
data class IntervaloEntity(
    val eventId: String,
    val periodo: String, // "0-15", "15-30", etc.
    val golsCasa: Int,
    val golsFora: Int,
    val minuto: Int
)

// ═══════════════════════════════════════════════════════════════════════════
// DATA CLASSES (para API e lógica de negócio)
// ═══════════════════════════════════════════════════════════════════════════

/**
 * Resultado do filtro BeVIX-1 ∩ TNT-A
 */
data class FilterResult(
    val delta: Double,
    val lineClose: Double,
    val hcOpen: Double?,
    val nivel: BeVixTntFilter.Nivel,
    val prob: Double
) {
    fun toNivelString(): String = nivel.name
    fun toProbString(): String = BeVixTntFilter.formatProb(prob)
}

/**
 * Dados de odds da Bet365 (da API)
 */
data class Bet365Data(
    val handicap1_3start: Handicap? = null, // Over/Under abertura
    val handicap1_3end: Handicap? = null,   // Over/Under fecho
    val handicap1_2start: Handicap? = null, // Handicap abertura
    val handicapEnd1_2: Handicap? = null,   // Handicap fecho
    val spread1_2: Handicap? = null         // Spread
)

data class Handicap(
    val over: String? = null,
    val under: String? = null,
    val home: String? = null,
    val away: String? = null
)

/**
 * Evento da API (odds pré-jogo)
 */
data class EventData(
    val event_id: String,
    val date: String?,
    val time: String?,
    val status: Any?, // String ou Int
    val event_data: EventDetails? = null,
    val data: Map<String, Bet365Data>? = null,
    val risultato: String? = null, // "1:0"
    val minuto: String? = null
)

data class EventDetails(
    val home: TeamInfo? = null,
    val away: TeamInfo? = null,
    val league: LeagueInfo? = null,
    val time: String? = null,
    val start_time: String? = null,
    val hour: String? = null
)

data class TeamInfo(
    val name: String? = null,
    val id: String? = null
)

data class LeagueInfo(
    val name: String? = null,
    val id: String? = null
)

/**
 * Dados live de um jogo específico
 */
data class LiveOddsResponse(
    val status: Any?, // String ou Int
    val odds: Map<String, BookOdds>? = null, // "Bet365" -> BookOdds
    val result: Map<String, String>? = null, // "home" -> "1", "away" -> "0"
    val scores: Map<String, String>? = null
)

data class BookOdds(
    val `1_1`: List<LiveMarket>? = null, // Status e minuto
    val `1_3`: List<LiveMarket>? = null, // Over/Under FT
    val `1_6`: List<LiveMarket>? = null  // Over/Under HT
)

data class LiveMarket(
    val ss: String? = null,      // Score "0-0"
    val time_str: String? = null, // Minuto "12"
    val over_c: String? = null,   // Linha Over/Under
    val handicap: String? = null, // Alternativa para over_c
    val over_od: String? = null   // Odd do Over
)

/**
 * Modelo de UI combinado
 */
data class JogoUi(
    val jogo: JogoEntity,
    val alerta: AlertaEntity? = null,
    val nivelInfo: BeVixTntFilter.Nivel
) {
    val statusEmoji: String
        get() = when {
            jogo.status.lowercase() in listOf("ended", "fulltime", "finished", "ft") -> {
                if (jogo.alertaLive) "✅✅✅" else "❌❌❌"
            }
            jogo.status.lowercase() in listOf("live", "inprogress", "in progress") -> "🔴"
            jogo.status.lowercase() in listOf("halftime", "ht", "interval") -> "⏸️"
            jogo.status.lowercase() in listOf("canceled", "postponed") -> "❌"
            else -> "⏳"
        }
    
    val displayStatus: String
        get() = when (jogo.status.lowercase()) {
            "live" -> if (jogo.minutoLive > 0) "🔴 ${jogo.minutoLive}'" else "🔴 AO VIVO"
            "ht", "halftime" -> "⏸️ INTERVALO"
            "ended", "finished" -> if (jogo.alertaLive) "✅ GANHOU" else "❌ PERDEU"
            else -> "⏳ Aguarda"
        }
}

// ═══════════════════════════════════════════════════════════════════════════
// TYPE CONVERTERS (para Room)
// ═══════════════════════════════════════════════════════════════════════════

class Converters {
    @TypeConverter
    fun fromTimestamp(value: Long?): java.util.Date? {
        return value?.let { java.util.Date(it) }
    }

    @TypeConverter
    fun dateToTimestamp(date: java.util.Date?): Long? {
        return date?.time
    }
}
