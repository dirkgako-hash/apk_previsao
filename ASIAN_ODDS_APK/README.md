# 🎰 Asian Odds Android APK - Robo Dirosky

Migração do robô de apostas Python/Colab para aplicação Android nativa com notificações push.

## 📱 Funcionalidades

### ✅ Implementadas (nos arquivos fornecidos)
- Filtro BeVIX-1 ∩ TNT-A completo (100% compatível com Python)
- Modelos de dados Room (SQLite)
- Sistema de notificações Android
- Foreground Service para monitoramento contínuo
- API client Retrofit (ScoreTrendApi)

### 🚧 Pendentes (a implementar)
- Interface gráfica (UI)
- Room Database DAO interfaces
- API Retrofit interface completa
- WorkManager para tarefas periódicas
- Telas de configurações
- Testes unitários

---

## 🚀 Guia de Implementação

### Passo 1: Clonar o Repositório Base

```bash
# Clonar seu repositório MultiStreamViewer (já configurado para compilar APK)
git clone https://github.com/dirkgako-hash/MultiStreamViewer.git
cd MultiStreamViewer

# Criar branch para desenvolvimento
git checkout -b asian-odds-migration
```

### Passo 2: Copiar Arquivos Fornecidos

Copie os arquivos criados para a estrutura do projeto:

```
MultiStreamViewer/
├── app/
│   ├── build.gradle                    ← COPIAR build.gradle fornecido
│   └── src/main/
│       ├── AndroidManifest.xml         ← COPIAR AndroidManifest.xml fornecido
│       └── java/com/dirosky/asianbets/
│           ├── data/
│           │   └── models/
│           │       └── Models.kt       ← COPIAR Models.kt fornecido
│           ├── domain/
│           │   └── filters/
│           │       └── BeVixTntFilter.kt  ← COPIAR BeVixTntFilter.kt fornecido
│           ├── services/
│           │   └── MonitorService.kt   ← COPIAR MonitorService.kt fornecido
│           └── utils/
│               └── NotificationHelper.kt  ← COPIAR NotificationHelper.kt fornecido
```

### Passo 3: Implementar Componentes Faltantes

#### A) Room Database DAO

Criar `app/src/main/java/com/dirosky/asianbets/data/db/AsianBetsDatabase.kt`:

```kotlin
package com.dirosky.asianbets.data.db

import androidx.room.*
import com.dirosky.asianbets.data.models.*

@Dao
interface JogoDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIfNotExists(jogo: JogoEntity): Long
    
    @Query("SELECT * FROM jogos WHERE data = :data")
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
    
    @Query("SELECT * FROM alertas WHERE eventId = :eventId")
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
```

#### B) Retrofit API Interface

Criar `app/src/main/java/com/dirosky/asianbets/data/api/ScoreTrendApi.kt`:

```kotlin
package com.dirosky.asianbets.data.api

import com.dirosky.asianbets.data.models.EventData
import com.dirosky.asianbets.data.models.LiveOddsResponse
import retrofit2.http.*

interface ScoreTrendApi {
    
    @POST("get_asian_odds_full_data")
    @Multipart
    suspend fun getOddsData(
        @Part("date") date: String
    ): List<EventData>
    
    @POST("get_asian_odds_full_live")
    @Multipart
    suspend fun getLiveOdds(
        @Part("game_id") gameId: String
    ): LiveOddsResponse?
    
    @GET("event/{eventId}")
    suspend fun getEventDetails(
        @Path("eventId") eventId: String
    ): Map<String, Any>
    
    @GET("graph/public/{eventId}")
    suspend fun getGraphData(
        @Path("eventId") eventId: String
    ): Map<String, Any>
}
```

#### C) Application Class

Criar `app/src/main/java/com/dirosky/asianbets/AsianBetsApplication.kt`:

```kotlin
package com.dirosky.asianbets

import android.app.Application
import com.dirosky.asianbets.utils.NotificationHelper

class AsianBetsApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        
        // Criar canais de notificação
        NotificationHelper.createNotificationChannels(this)
    }
}
```

#### D) MainActivity Básica

Criar `app/src/main/java/com/dirosky/asianbets/presentation/main/MainActivity.kt`:

```kotlin
package com.dirosky.asianbets.presentation.main

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.dirosky.asianbets.R
import com.dirosky.asianbets.services.MonitorService

class MainActivity : AppCompatActivity() {
    
    companion object {
        private const val REQUEST_NOTIFICATION_PERMISSION = 100
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
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
```

### Passo 4: Recursos (res/)

#### Layouts

Criar `app/src/main/res/layout/activity_main.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<androidx.coordinatorlayout.widget.CoordinatorLayout 
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="match_parent">

    <com.google.android.material.appbar.AppBarLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content">

        <com.google.android.material.appbar.MaterialToolbar
            android:id="@+id/toolbar"
            android:layout_width="match_parent"
            android:layout_height="?attr/actionBarSize"
            app:title="Robo Dirosky" />

    </com.google.android.material.appbar.AppBarLayout>

    <androidx.recyclerview.widget.RecyclerView
        android:id="@+id/recyclerViewJogos"
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        app:layout_behavior="@string/appbar_scrolling_view_behavior" />

    <com.google.android.material.floatingactionbutton.FloatingActionButton
        android:id="@+id/fabSettings"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:layout_gravity="bottom|end"
        android:layout_margin="16dp"
        app:srcCompat="@drawable/ic_settings" />

</androidx.coordinatorlayout.widget.CoordinatorLayout>
```

#### Strings

Criar `app/src/main/res/values/strings.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="app_name">Robo Dirosky</string>
    <string name="settings">Configurações</string>
    <string name="notification_channel_alerts">Alertas de Aposta</string>
    <string name="notification_channel_results">Resultados</string>
    <string name="notification_channel_service">Monitoramento</string>
</resources>
```

#### Ícones (criar ou usar placeholders)

- `res/drawable/ic_bet_notification.xml`
- `res/drawable/ic_bet365.xml`
- `res/drawable/ic_details.xml`
- `res/drawable/ic_win.xml`
- `res/drawable/ic_loss.xml`
- `res/drawable/ic_monitor.xml`
- `res/drawable/ic_settings.xml`

### Passo 5: Build e Teste

```bash
# No Android Studio:
# 1. Build > Make Project
# 2. Run > Run 'app'

# Ou via linha de comando:
./gradlew assembleDebug

# APK estará em:
# app/build/outputs/apk/debug/app-debug.apk
```

### Passo 6: Instalação no Dispositivo

```bash
# Via ADB
adb install app/build/outputs/apk/debug/app-debug.apk

# Ou copiar APK para o dispositivo e instalar manualmente
```

---

## 🔧 Configuração do Projeto

### build.gradle (raiz do projeto)

```gradle
buildscript {
    ext.kotlin_version = '1.9.20'
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        classpath 'com.android.tools.build:gradle:8.2.0'
        classpath "org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlin_version"
    }
}

allprojects {
    repositories {
        google()
        mavenCentral()
        maven { url 'https://jitpack.io' } // Para MPAndroidChart
    }
}
```

### settings.gradle

```gradle
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url 'https://jitpack.io' }
    }
}

rootProject.name = "AsianBets"
include ':app'
```

---

## 📊 Arquitetura

```
┌─────────────────────────────────────────────────────┐
│                    UI LAYER                         │
│  (MainActivity, RecyclerView, Notifications)        │
└────────────────┬────────────────────────────────────┘
                 │
┌────────────────▼────────────────────────────────────┐
│                 DOMAIN LAYER                        │
│  (BeVixTntFilter, Business Logic)                   │
└────────────────┬────────────────────────────────────┘
                 │
┌────────────────▼────────────────────────────────────┐
│                  DATA LAYER                         │
│  ┌──────────────┐    ┌─────────────────┐           │
│  │ Room DB      │    │ Retrofit API    │           │
│  │ (SQLite)     │    │ (ScoreTrend)    │           │
│  └──────────────┘    └─────────────────┘           │
└─────────────────────────────────────────────────────┘
                 │
┌────────────────▼────────────────────────────────────┐
│             BACKGROUND SERVICES                     │
│  MonitorService (Foreground) + WorkManager          │
└─────────────────────────────────────────────────────┘
```

---

## 📝 Comparação Python ↔ Kotlin

| Função Python | Equivalente Kotlin | Arquivo |
|---------------|-------------------|---------|
| `parse_condicao()` | `BeVixTntFilter.apply()` | `BeVixTntFilter.kt` |
| `tg_send()` | `NotificationHelper.alertaAposta()` | `NotificationHelper.kt` |
| `loop_continuo()` | `MonitorService.monitorLoop()` | `MonitorService.kt` |
| `processar_jogo()` | `MonitorService.processarJogoLive()` | `MonitorService.kt` |
| `init_soccer_db()` | `Room.databaseBuilder()` | `AsianBetsDatabase.kt` |
| `fetch_live_post()` | `ScoreTrendApi.getLiveOdds()` | `ScoreTrendApi.kt` |

---

## 🎯 Próximos Passos

1. ✅ **Copiar arquivos fornecidos** para o projeto MultiStreamViewer
2. ⬜ **Implementar Room DAOs** (AsianBetsDatabase.kt)
3. ⬜ **Implementar Retrofit API** (ScoreTrendApi.kt)
4. ⬜ **Criar UI básica** (RecyclerView de jogos)
5. ⬜ **Adicionar ícones e recursos**
6. ⬜ **Testar em dispositivo real**
7. ⬜ **Build release + assinatura**
8. ⬜ **Publicar APK**

---

## 🐛 Troubleshooting

### Erro de compilação

```bash
./gradlew clean build
```

### Notificações não aparecem

1. Verificar permissões em Settings > Apps > Robo Dirosky > Notifications
2. Verificar logs: `adb logcat | grep NotificationHelper`

### Serviço para após um tempo

Adicionar bateria sem otimização:
Settings > Battery > Battery optimization > All apps > Robo Dirosky > Don't optimize

---

## 📄 Licença

MIT License

---

## 👥 Autoria

Migração de Python/Colab para Android por Claude (Anthropic)  
Baseado no código original do Robo Dirosky
