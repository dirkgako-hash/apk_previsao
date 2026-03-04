# Plano de Migração: Robo Dirosky → Android APK

## 📋 Visão Geral
Migração do robô de apostas de Python/Colab para aplicação Android nativa com notificações push.

## 🎯 Objetivos
1. ✅ Manter toda a lógica de negócio (filtros BeVIX-1 ∩ TNT-A)
2. ✅ Substituir Telegram por notificações Android locais
3. ✅ Interface nativa para visualização de jogos
4. ✅ Background service para monitoramento contínuo
5. ✅ Banco de dados local SQLite

## 🏗️ Arquitetura Proposta

### Stack Tecnológica
- **Linguagem**: Kotlin (ou Java)
- **Framework**: Android SDK nativo
- **Background**: WorkManager + Foreground Service
- **Notificações**: NotificationManager
- **HTTP**: Retrofit2 + OkHttp
- **DB**: Room (wrapper do SQLite)
- **UI**: Jetpack Compose ou XML tradicional

### Estrutura de Pastas
```
app/src/main/
├── java/com/dirosky/asianbets/
│   ├── data/
│   │   ├── api/          # Retrofit interfaces
│   │   ├── db/           # Room database
│   │   └── models/       # Data classes
│   ├── domain/
│   │   ├── filters/      # BeVIX-1 + TNT-A logic
│   │   └── usecases/     # Business logic
│   ├── presentation/
│   │   ├── main/         # Lista de jogos
│   │   ├── details/      # Detalhes do jogo
│   │   └── settings/     # Configurações
│   ├── services/
│   │   ├── MonitorService.kt      # Foreground service
│   │   └── OddsWorker.kt          # WorkManager periodic
│   └── utils/
│       ├── NotificationHelper.kt
│       └── TimeUtils.kt
├── res/
│   ├── layout/
│   ├── drawable/
│   └── values/
└── AndroidManifest.xml
```

## 📦 Componentes Principais

### 1. API Client (Retrofit)
```kotlin
interface ScoreTrendApi {
    @POST("get_asian_odds_full_data")
    suspend fun getOddsData(@Body request: OddsRequest): List<EventData>
    
    @POST("get_asian_odds_full_live")
    suspend fun getLiveOdds(@Body gameId: String): LiveOddsResponse
}
```

### 2. Database (Room)
```kotlin
@Entity(tableName = "jogos")
data class JogoEntity(
    @PrimaryKey val eventId: String,
    val home: String,
    val away: String,
    val league: String,
    val delta: Double,
    val lineClose: Double,
    val nivel: String, // A, B, C
    val prob: String,
    val status: String,
    val alertaLive: Boolean,
    val htScore: String?,
    val ftScore: String?
)
```

### 3. Foreground Service
```kotlin
class MonitorService : Service() {
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Notificação persistente "Monitorando..."
        startForeground(NOTIFICATION_ID, createNotification())
        
        // Loop de monitoramento
        startMonitoring()
        
        return START_STICKY
    }
}
```

### 4. Notificações
```kotlin
object NotificationHelper {
    fun alertaAposta(context: Context, jogo: Jogo) {
        val notification = NotificationCompat.Builder(context, CHANNEL_ALERTS)
            .setSmallIcon(R.drawable.ic_bet)
            .setContentTitle("🔥 APOSTAR AGORA - ${jogo.nivel}")
            .setContentText("${jogo.home} vs ${jogo.away}")
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("""
                    ⏱️ ${jogo.minuto}' | 📊 0-0
                    💰 Odd: ${jogo.odd}
                    🧠 Prob: ${jogo.prob}
                    📈 ${jogo.nivel} | δ=${jogo.delta}
                """.trimIndent()))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()
            
        NotificationManagerCompat.from(context).notify(jogo.eventId.hashCode(), notification)
    }
}
```

## 🔄 Mapeamento Python → Kotlin

| Python (Colab) | Kotlin (Android) |
|----------------|------------------|
| `asyncio` + `aiohttp` | `coroutines` + `Retrofit` |
| `sqlite3` | `Room` |
| `Telegram` | `NotificationManager` |
| `pandas` | `List` + `Flow` |
| Loop infinito | `WorkManager` (periodic) + `Service` |
| `tqdm` | `ProgressBar` |

## 📱 Funcionalidades do APK

### Tela Principal
- ✅ Lista de jogos filtrados (RecyclerView)
- ✅ Filtro por nível (A/B/C)
- ✅ Status live (🔴/⏸️/✅/❌)
- ✅ Pull-to-refresh

### Tela de Detalhes
- ✅ Timeline do jogo
- ✅ Gráfico de odds
- ✅ Link para Bet365
- ✅ Histórico de alertas

### Configurações
- ✅ Intervalo de re-fetch (3min padrão)
- ✅ Intervalo de poll (30s padrão)
- ✅ Max minuto para alerta (25 padrão)
- ✅ Range de odds (1.65-2.10)
- ✅ Ativar/desativar níveis

### Notificações
1. **Alertas de Aposta** (prioridade alta, som)
   - Disparado quando: 0-0, minuto ≤25, odd OK
   - Ação: Abrir Bet365 / Ver detalhes
   
2. **Resultados** (prioridade normal)
   - HT: GREEN (✅✅✅) ou RED (❌❌❌)
   - FT: Score final
   
3. **Status Persistente** (foreground service)
   - "Monitorando X jogos | Y apostas"

## 🔐 Permissões Necessárias

```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.WAKE_LOCK" />
<uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />
```

## 📝 Tarefas de Implementação

### Fase 1: Core (Semana 1)
- [ ] Setup projeto Android
- [ ] Configurar Retrofit + API endpoints
- [ ] Criar modelos de dados (Room entities)
- [ ] Implementar filtros BeVIX-1 + TNT-A
- [ ] Testes unitários dos filtros

### Fase 2: Background (Semana 2)
- [ ] WorkManager para re-fetch periódico
- [ ] Foreground Service para polling live
- [ ] Sistema de notificações
- [ ] Persistência de estado (SharedPreferences)

### Fase 3: UI (Semana 3)
- [ ] Tela principal (lista de jogos)
- [ ] Tela de detalhes
- [ ] Tela de configurações
- [ ] Tela de estatísticas
- [ ] Navigation component

### Fase 4: Polish (Semana 4)
- [ ] Otimização de bateria
- [ ] Testes em diferentes dispositivos
- [ ] Ícone e splash screen
- [ ] Documentação
- [ ] Build release + assinatura

## 🎨 Design de Notificações

### Alerta de Aposta
```
┌────────────────────────────────────┐
│ 🔥 APOSTAR AGORA - FORTE          │
│ ─────────────────────────────────  │
│ ⚽ Sporting vs Porto                │
│ ⏱️ 12' | 📊 0-0                    │
│ 💰 Odd: 1.85 | 🧠 Prob: 91%       │
│ 📈 δ=+0.75 | linha=3.75            │
│                                     │
│ [Bet365]  [Detalhes]               │
└────────────────────────────────────┘
```

### Resultado
```
┌────────────────────────────────────┐
│ ✅✅✅ OVER - Aposta ganha!        │
│ ─────────────────────────────────  │
│ Sporting vs Porto                   │
│ 📊 HT: 1-0 | 🏁 FT: 2-1            │
│ 💰 Odd: 1.85 | 🔥 FORTE            │
└────────────────────────────────────┘
```

## ⚡ Otimizações

1. **Bateria**
   - Usar `WorkManager` em vez de `AlarmManager`
   - Doze mode compliance
   - Background restrictions handling

2. **Rede**
   - Cache de respostas HTTP (1min)
   - Exponential backoff em erros
   - Compress responses (gzip)

3. **DB**
   - Índices em `eventId`, `data`, `status`
   - Cleanup automático de jogos antigos (>7 dias)
   - Transactions para batch inserts

4. **UI**
   - RecyclerView com DiffUtil
   - Paginação (20 jogos por página)
   - Image loading: Coil ou Glide

## 🚀 Deploy

### Build Types
```gradle
buildTypes {
    debug {
        applicationIdSuffix ".debug"
        debuggable true
    }
    release {
        minifyEnabled true
        proguardFiles getDefaultProguardFile('proguard-android-optimize.txt')
        signingConfig signingConfigs.release
    }
}
```

### GitHub Actions
```yaml
- name: Build APK
  run: ./gradlew assembleRelease
  
- name: Sign APK
  uses: r0adkll/sign-android-release@v1
  
- name: Upload to Releases
  uses: actions/upload-artifact@v3
```

## 📊 Métricas de Sucesso

- ✅ 100% dos filtros Python replicados
- ✅ Latência de notificação < 5s após condição
- ✅ Bateria: < 5% em 8h de monitoramento
- ✅ Crash rate < 1%
- ✅ APK size < 10MB

## 🔗 Recursos

- [Android Developer Guide](https://developer.android.com)
- [WorkManager Best Practices](https://developer.android.com/topic/libraries/architecture/workmanager/advanced)
- [Room Database](https://developer.android.com/training/data-storage/room)
- [Retrofit](https://square.github.io/retrofit/)

---

**Próximos Passos:**
1. Clonar repositório MultiStreamViewer
2. Criar branch `asian-odds-migration`
3. Adaptar `build.gradle` com dependências
4. Implementar `ScoreTrendApi.kt`
5. Criar `BeVixTntFilter.kt` com lógica dos filtros
