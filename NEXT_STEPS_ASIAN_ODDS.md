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
