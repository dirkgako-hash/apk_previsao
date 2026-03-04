#!/bin/bash

BUILD_GRADLE="app/build.gradle"

echo "🔧 Adicionando dependências ao build.gradle..."

# Verificar se kotlin-kapt já existe
if ! grep -q "id 'kotlin-kapt'" "$BUILD_GRADLE"; then
    # Adicionar kotlin-kapt nos plugins
    sed -i "/id 'kotlin-android'/a\    id 'kotlin-kapt'" "$BUILD_GRADLE"
    echo "✅ Plugin kotlin-kapt adicionado"
else
    echo "ℹ️  Plugin kotlin-kapt já existe"
fi

# Adicionar dependências no final do bloco dependencies
cat >> "$BUILD_GRADLE" << 'DEPS'

    // ══════════════════════════════════════════════════════════════
    // ASIAN ODDS / ROBO DIROSKY
    // ══════════════════════════════════════════════════════════════
    
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
    implementation "org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3"
    implementation "org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3"
    
    // LIFECYCLE
    implementation "androidx.lifecycle:lifecycle-service:2.7.0"
    implementation "androidx.lifecycle:lifecycle-runtime-ktx:2.7.0"
    
    // WORKMANAGER (opcional)
    implementation "androidx.work:work-runtime-ktx:2.9.0"
DEPS

echo "✅ Dependências adicionadas!"
echo ""
echo "⚠️  VERIFIQUE o arquivo manualmente:"
echo "    nano app/build.gradle"
