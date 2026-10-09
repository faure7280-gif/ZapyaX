# ZapyaX - Wireless High-Speed P2P File Transfer (Android)

**ZapyaX** es una aplicación nativa para Android construida en Kotlin y Jetpack Compose para transferencia inalámbrica, directa y ultrarrápida de archivos entre dispositivos cercanos sin intermediarios en la nube.

---

## Características Principales

- **Transferencia Real Sin Servidores (P2P Local):** Conexión socket directa sobre red Wi-Fi o Punto de Acceso (Hotspot) local.
- **Canal Dual Concurrente (2 en simultáneo):** Los archivos en cola se transmiten en paralelo de dos en dos a través de trabajadores independientes de socket, maximizando el ancho de banda sin latencia de espera.
- **Emparejamiento Rápido por Código QR:** El receptor genera un QR con IP, puerto y clave criptográfica efímera. El emisor escanea el código con CameraX para enlace instantáneo.
- **Cifrado de Extremo a Extremo (E2EE):** Cifrado autenticado **AES-256-GCM** por bloques con vectores de inicialización (IV) de 12 bytes y verificación de integridad AEAD. La clave no pasa por internet.
- **Transferencia en Segundo Plano:** Servicio de primer plano (`TransferForegroundService`) con notificación persistente, barra de progreso y `WakeLock` para evitar suspensión de CPU.
- **Diseño Acrílico (Glassmorphism) y Modo Oscuro Nativo:** Interfaz moderna con paneles de vidrio esmerilado, bordes neón iluminados y uso exclusivo de iconos vectoriales modernos (cero emojis).

---

## Compilación y Ejecución (Local y GitHub)

### Requisitos
- **JDK 17** o superior
- **Android SDK** (API 24 mínimo, target API 36)
- **Gradle 8.x** (incluido mediante Gradle Wrapper)

### Comandos de Compilación

```bash
# Otorgar permisos de ejecución al wrapper
chmod +x gradlew

# Ejecutar pruebas unitarias y criptográficas
./gradlew testDebugUnitTest

# Compilar el APK de depuración
./gradlew assembleDebug

# El APK generado se encuentra en:
# app/build/outputs/apk/debug/app-debug.apk
```

### Integración Continua (GitHub Actions)

El repositorio incluye el flujo de trabajo en `.github/workflows/build.yml` que:
1. Configura el entorno JDK 17 (Temurin).
2. Valida los tests unitarios y criptográficos con Robolectric.
3. Compila el APK de depuración.
4. Publica el APK como artefacto de descarga en cada push a `main` / `master`.
