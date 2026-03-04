#!/bin/bash
#
# Script de integração automática do Robo Dirosky
# Copia arquivos para a estrutura existente do projeto
#

set -e

PROJECT_ROOT="/workspaces/apk_previsao"
SOURCE="/workspaces/apk_previsao/ASIAN_ODDS_APK"

echo "╔══════════════════════════════════════════════════════════════╗"
echo "║   INTEGRAÇÃO ROBO DIROSKY → APK ANDROID                      ║"
echo "╚══════════════════════════════════════════════════════════════╝"
echo ""

# Verificar se estamos no diretório correto
if [ ! -d "$PROJECT_ROOT" ]; then
    echo "❌ Erro: Diretório do projeto não encontrado: $PROJECT_ROOT"
    exit 1
fi

echo "📁 Projeto: $PROJECT_ROOT"
echo ""

# ══════════════════════════════════════════════════════════════════
# 1. BACKUP DO BUILD.GRADLE ATUAL
# ══════════════════════════════════════════════════════════════════
echo "🔄 Fazendo backup do build.gradle atual..."
if [ -f "$PROJECT_ROOT/app/build.gradle" ]; then
    cp "$PROJECT_ROOT/app/build.gradle" "$PROJECT_ROOT/app/build.gradle.backup"
    echo "✅ Backup criado: app/build.gradle.backup"
fi

# ══════════════════════════════════════════════════════════════════
# 2. CRIAR ESTRUTURA DE DIRETÓRIOS
# ══════════════════════════════════════════════════════════════════
echo ""
echo "📂 Criando estrutura de diretórios..."

JAVA_BASE="$PROJECT_ROOT/app/src/main/java/com/dirosky/asianbets"

mkdir -p "$JAVA_BASE/data/models"
mkdir -p "$JAVA_BASE/data/db"
mkdir -p "$JAVA_BASE/data/api"
mkdir -p "$JAVA_BASE/domain/filters"
mkdir -p "$JAVA_BASE/services"
mkdir -p "$JAVA_BASE/utils"
mkdir -p "$JAVA_BASE/presentation/main"
mkdir -p "$PROJECT_ROOT/app/src/main/res/layout"
mkdir -p "$PROJECT_ROOT/app/src/main/res/values"
mkdir -p "$PROJECT_ROOT/app/src/main/res/drawable"

echo "✅ Estrutura criada"

# ══════════════════════════════════════════════════════════════════
# 3. COPIAR ARQUIVOS KOTLIN
# ══════════════════════════════════════════════════════════════════
echo ""
echo "📝 Copiando arquivos Kotlin..."

cp "$SOURCE/app/src/main/java/com/dirosky/asianbets/domain/filters/BeVixTntFilter.kt" \
   "$JAVA_BASE/domain/filters/"
echo "✅ BeVixTntFilter.kt"

cp "$SOURCE/app/src/main/java/com/dirosky/asianbets/data/models/Models.kt" \
   "$JAVA_BASE/data/models/"
echo "✅ Models.kt"

cp "$SOURCE/app/src/main/java/com/dirosky/asianbets/utils/NotificationHelper.kt" \
   "$JAVA_BASE/utils/"
echo "✅ NotificationHelper.kt"

cp "$SOURCE/app/src/main/java/com/dirosky/asianbets/services/MonitorService.kt" \
   "$JAVA_BASE/services/"
echo "✅ MonitorService.kt"

# ══════════════════════════════════════════════════════════════════
# 4. BACKUP E ATUALIZAR ANDROIDMANIFEST.XML
# ══════════════════════════════════════════════════════════════════
echo ""
echo "📄 Atualizando AndroidManifest.xml..."

if [ -f "$PROJECT_ROOT/app/src/main/AndroidManifest.xml" ]; then
    cp "$PROJECT_ROOT/app/src/main/AndroidManifest.xml" \
       "$PROJECT_ROOT/app/src/main/AndroidManifest.xml.backup"
    echo "✅ Backup criado: AndroidManifest.xml.backup"
fi

# Copiar novo manifest (você pode mesclar manualmente depois)
cp "$SOURCE/app/src/main/AndroidManifest.xml" \
   "$PROJECT_ROOT/app/src/main/AndroidManifest_asian_odds.xml"
echo "⚠️  Novo manifest salvo como: AndroidManifest_asian_odds.xml"
echo "    Revise e mescle as permissões manualmente!"

# ══════════════════════════════════════════════════════════════════
# 5. COPIAR DOCUMENTAÇÃO
# ══════════════════════════════════════════════════════════════════
echo ""
echo "📚 Copiando documentação..."

cp "$SOURCE/README.md" "$PROJECT_ROOT/README_ASIAN_ODDS.md"
echo "✅ README_ASIAN_ODDS.md"

cp "$SOURCE/MIGRATION_PLAN.md" "$PROJECT_ROOT/MIGRATION_PLAN.md"
echo "✅ MIGRATION_PLAN.md"

# ══════════════════════════════════════════════════════════════════
# 6. CRIAR ARQUIVOS AUXILIARES
# ══════════════════════════════════════════════════════════════════
echo ""
echo "🔧 Criando arquivos auxiliares..."

# AsianBetsDatabase.kt
cat > "$JAVA_BASE/data/db/AsianBetsDatabase.kt" << 'DBEOF'
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
DBEOF
echo "✅ AsianBetsDatabase.kt"

# ScoreTrendApi.kt
cat > "$JAVA_BASE/data/api/ScoreTrendApi.kt" << 'APIEOF'
package com.dirosky.asianbets.data.api

import com.dirosky.asianbets.data.models.EventData
import com.dirosky.asianbets.data.models.LiveOddsResponse
import okhttp3.MultipartBody
import retrofit2.http.*

interface ScoreTrendApi {
    
    @Multipart
    @POST("get_asian_odds_full_data")
    suspend fun getOddsData(
        @Part("date") date: String
    ): List<EventData>
    
    @Multipart
    @POST("get_asian_odds_full_live")
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
APIEOF
echo "✅ ScoreTrendApi.kt"

# AsianBetsApplication.kt
cat > "$JAVA_BASE/AsianBetsApplication.kt" << 'APPEOF'
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
APPEOF
echo "✅ AsianBetsApplication.kt"

# ══════════════════════════════════════════════════════════════════
# 7. CRIAR DEPENDÊNCIAS GRADLE
# ══════════════════════════════════════════════════════════════════
echo ""
echo "📦 Criando arquivo de dependências..."

cat > "$PROJECT_ROOT/app/dependencies_asian_odds.gradle" << 'GRADLEEOF'
// ═══════════════════════════════════════════════════════════════════
// DEPENDÊNCIAS PARA ASIAN ODDS / ROBO DIROSKY
// Adicione estas linhas ao seu app/build.gradle existente
// ═══════════════════════════════════════════════════════════════════

dependencies {
    // ROOM DATABASE
    def room_version = '2.6.1'
    implementation "androidx.room:room-runtime:$room_version"
    implementation "androidx.room:room-ktx:$room_version"
    kapt "androidx.room:room-compiler:$room_version"
    
    // RETROFIT (HTTP Client)
    def retrofit_version = '2.9.0'
    implementation "com.squareup.retrofit2:retrofit:$retrofit_version"
    implementation "com.squareup.retrofit2:converter-gson:$retrofit_version"
    implementation 'com.squareup.okhttp3:okhttp:4.12.0'
    implementation 'com.squareup.okhttp3:logging-interceptor:4.12.0'
    
    // GSON (JSON)
    implementation 'com.google.code.gson:gson:2.10.1'
    
    // COROUTINES
    def coroutines_version = '1.7.3'
    implementation "org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutines_version"
    implementation "org.jetbrains.kotlinx:kotlinx-coroutines-android:$coroutines_version"
    
    // LIFECYCLE
    def lifecycle_version = '2.7.0'
    implementation "androidx.lifecycle:lifecycle-service:$lifecycle_version"
    implementation "androidx.lifecycle:lifecycle-runtime-ktx:$lifecycle_version"
    
    // WORKMANAGER (opcional)
    implementation "androidx.work:work-runtime-ktx:2.9.0"
}
GRADLEEOF
echo "✅ dependencies_asian_odds.gradle"

# ══════════════════════════════════════════════════════════════════
# 8. CRIAR GUIA DE PRÓXIMOS PASSOS
# ══════════════════════════════════════════════════════════════════
cat > "$PROJECT_ROOT/NEXT_STEPS_ASIAN_ODDS.md" << 'STEPSEOF'
# 🚀 Próximos Passos - Integração Asian Odds

## ✅ Concluído

- [x] Estrutura de diretórios criada
- [x] Arquivos Kotlin copiados
- [x] Database DAO criado
- [x] API interface criada
- [x] Application class criada
- [x] Documentação copiada

## 📋 A Fazer AGORA

### 1. Atualizar build.gradle

Abra `app/build.gradle` e adicione as dependências de `dependencies_asian_odds.gradle`:

```gradle
plugins {
    id 'kotlin-kapt'  // ← Adicionar se não existir
}

// ... resto do arquivo ...

// Copiar dependências de dependencies_asian_odds.gradle
```

### 2. Atualizar AndroidManifest.xml

Abra `app/src/main/AndroidManifest.xml` e adicione:

#### a) Permissões (antes de `<application>`):
```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_DATA_SYNC" />
<uses-permission android:name="android.permission.WAKE_LOCK" />
<uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />
```

#### b) Application name:
```xml
<application
    android:name="com.dirosky.asianbets.AsianBetsApplication"
    ...
```

#### c) Service (dentro de `<application>`):
```xml
<service
    android:name="com.dirosky.asianbets.services.MonitorService"
    android:enabled="true"
    android:exported="false"
    android:foregroundServiceType="dataSync"
    android:stopWithTask="false" />
```

### 3. Criar Recursos (res/)

#### a) Ícones de notificação

Criar em `app/src/main/res/drawable/`:
- `ic_bet_notification.xml`
- `ic_bet365.xml`
- `ic_details.xml`
- `ic_win.xml`
- `ic_loss.xml`
- `ic_monitor.xml`

**Exemplo** (`ic_bet_notification.xml`):
```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp"
    android:height="24dp"
    android:viewportWidth="24"
    android:viewportHeight="24"
    android:tint="?attr/colorControlNormal">
    <path
        android:fillColor="@android:color/white"
        android:pathData="M12,2C6.48,2 2,6.48 2,12s4.48,10 10,10 10,-4.48 10,-10S17.52,2 12,2zM13,17h-2v-2h2v2zM13,13h-2L11,7h2v6z"/>
</vector>
```

#### b) Strings

Adicionar em `app/src/main/res/values/strings.xml`:
```xml
<string name="notification_channel_alerts">Alertas de Aposta</string>
<string name="notification_channel_results">Resultados</string>
<string name="notification_channel_service">Monitoramento</string>
```

### 4. Iniciar Serviço

Na sua MainActivity (ou onde preferir), adicionar:

```kotlin
import android.content.Intent
import android.os.Build
import com.dirosky.asianbets.services.MonitorService

private fun startMonitorService() {
    val intent = Intent(this, MonitorService::class.java)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        startForegroundService(intent)
    } else {
        startService(intent)
    }
}

override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    // ... seu código ...
    
    startMonitorService()
}
```

### 5. Build e Teste

```bash
# Limpar build
./gradlew clean

# Build debug
./gradlew assembleDebug

# Instalar no dispositivo
adb install app/build/outputs/apk/debug/app-debug.apk
```

### 6. Verificar Logs

```bash
# Filtrar logs do serviço
adb logcat | grep MonitorService

# Filtrar logs de notificações
adb logcat | grep NotificationHelper
```

## 🐛 Troubleshooting

### Erro de compilação "kapt not found"

Adicionar no `build.gradle` (topo):
```gradle
plugins {
    id 'kotlin-kapt'
}
```

### Notificações não aparecem

1. Verificar permissões: Settings > Apps > [Seu App] > Notifications
2. Android 13+: solicitar permissão POST_NOTIFICATIONS

### Serviço para após um tempo

Settings > Battery > Battery optimization > [Seu App] > Don't optimize

## 📚 Documentação

- `README_ASIAN_ODDS.md` - Guia completo
- `MIGRATION_PLAN.md` - Plano de migração
- Código original Python: `ASIAN_ODDS_com_modelos_v1_1_ipynb.txt`

## 🎯 Roadmap

- [ ] Build e teste inicial
- [ ] UI para lista de jogos
- [ ] Tela de detalhes
- [ ] Configurações (odds min/max, intervalos)
- [ ] Gráfico de odds (opcional)
- [ ] Testes unitários dos filtros
- [ ] Build release + assinatura
STEPSEOF

echo ""
echo "╔══════════════════════════════════════════════════════════════╗"
echo "║   ✅ INTEGRAÇÃO CONCLUÍDA!                                   ║"
echo "╚══════════════════════════════════════════════════════════════╝"
echo ""
echo "📁 Arquivos criados:"
echo "   - Kotlin: domain/filters, data/models, services, utils"
echo "   - Database: AsianBetsDatabase.kt"
echo "   - API: ScoreTrendApi.kt"
echo "   - App: AsianBetsApplication.kt"
echo ""
echo "📚 Documentação:"
echo "   - README_ASIAN_ODDS.md"
echo "   - MIGRATION_PLAN.md"
echo "   - NEXT_STEPS_ASIAN_ODDS.md  ← LEIA ESTE!"
echo ""
echo "⚠️  IMPORTANTE:"
echo "   1. Revise NEXT_STEPS_ASIAN_ODDS.md"
echo "   2. Adicione dependências ao build.gradle"
echo "   3. Atualize AndroidManifest.xml"
echo "   4. Crie ícones de notificação"
echo ""
echo "🚀 Depois execute: ./gradlew clean assembleDebug"
echo ""

