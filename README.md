# Cardroid

Launcher para usar una tablet Android como pantalla del auto, pensado para la Redmi Pad SE en horizontal.

## Qué hace (versión 0.2)

- Mapa integrado (MapLibre con mapas de OpenStreetMap vía OpenFreeMap, gratis y sin clave) que sigue tu posición, con botones de acercar, alejar y centrar, y velocidad en km/h.
- Tarjeta de clima de tu ubicación (Open-Meteo, sin clave).
- Tarjeta de música con portada de fondo y controles (Spotify, YouTube Music, etc.).
- Barra inferior con asistente de voz, accesos a Spotify, Waze, Google Maps y YouTube Music si están instaladas, Ajustes, hora y batería.
- Panel derecho con velocímetro por GPS, distancia del viaje y rumbo.
- Mantiene la pantalla encendida y oculta las barras del sistema.

## Instalar en la tablet

1. En la tablet, abre la página de [Releases](../../releases) y descarga `Cardroid.apk` (la versión de prueba está en la etiqueta `dev`).
2. Ábrelo y permite instalar apps desde el navegador si Android lo pide.
3. Acepta el permiso de ubicación (lo usan el mapa, el velocímetro y el clima).
4. Para ponerlo como pantalla de inicio: Ajustes > Apps > Apps predeterminadas > Pantalla de inicio > Cardroid. También puedes abrirlo como una app normal.

### Activar la tarjeta de música

Android 15 bloquea este permiso en apps instaladas desde un APK, así que hay que desbloquearlo una vez:

1. Ajustes > Apps > Cardroid > menú ⋮ (arriba a la derecha) > **Permitir ajustes restringidos**.
2. Vuelve a Cardroid y toca **Activar acceso a notificaciones**, luego activa Cardroid.

Sin este permiso los botones de música igual funcionan, pero no se ve qué canción suena.

## Compilar

GitHub Actions compila el APK en cada push (`.github/workflows/build.yml`). Localmente: `./gradlew assembleDebug` con el SDK de Android instalado.
