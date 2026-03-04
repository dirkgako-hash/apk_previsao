package com.dirosky.asianbets.data.db

import androidx.room.*
import com.dirosky.asianbets.data.models.*

@Dao
interface JogoDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIfNotExists(jogo: JogoEntity): Long
    
    @Query("SELECT * FROM jogos WHERE data = :data ORDER BY eventTime, delta DESC")
    suspend fun getJogosByData(data: String): List<JogoEntity>
    
    @Query("SELECT * FROM jogos WHERE data = :data AND status NOT IN ('Ended', 'Canceled', 'Postponed')")
    suspend fun getJogosAtivos(data: String): List<JogoEntity>
    
    @Query("UPDATE jogos SET status = :status, minutoLive = :minuto, scoreLive = :score WHERE eventId = :eventId")
    suspend fun updateLiveStatus(eventId: String, status: String, minuto: Int, score: String)
    
    @Query("UPDATE jogos SET kickTs = :kickTs WHERE eventId = :eventId")
    suspend fun updateKickTs(eventId: String, kickTs: String)
    
    @Query("UPDATE jogos SET alertaLive = :alerta, oddLive = :odd, logId = :logId WHERE eventId = :eventId")
    suspend fun updateAlerta(eventId: String, alerta: Boolean, odd: Double, logId: Long)
    
    @Query("UPDATE jogos SET htScore = :htScore WHERE eventId = :eventId")
    suspend fun updateHtScore(eventId: String, htScore: String)
    
    @Query("UPDATE jogos SET ftScore = :ftScore WHERE eventId = :eventId")
    suspend fun updateFtScore(eventId: String, ftScore: String)
    
    @Query("SELECT COUNT(*) FROM jogos WHERE data = :data")
    suspend fun countJogos(data: String): Int
    
    @Query("SELECT COUNT(*) FROM jogos WHERE data = :data AND alertaLive = 1")
    suspend fun countApostas(data: String): Int
}

@Dao
interface AlertaDao {
    @Insert
    suspend fun insert(alerta: AlertaEntity): Long
    
    @Query("UPDATE alertas SET htScore = :htScore, resultado = :resultado WHERE id = :id")
    suspend fun updateResultado(id: Long, htScore: String, resultado: String)
    
    @Query("UPDATE alertas SET ftScore = :ftScore WHERE id = :id")
    suspend fun updateFtScore(id: Long, ftScore: String)
    
    @Query("SELECT * FROM alertas WHERE eventId = :eventId ORDER BY ts DESC")
    suspend fun getAlertasByEvent(eventId: String): List<AlertaEntity>
}

@Database(
    entities = [JogoEntity::class, AlertaEntity::class, IntervaloEntity::class],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AsianBetsDatabase : RoomDatabase() {
    abstract fun jogoDao(): JogoDao
    abstract fun alertaDao(): AlertaDao
}
