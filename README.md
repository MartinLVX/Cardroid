# Cardroid

Launcher para usar una tablet Android como pantalla del auto, pensado para la Redmi Pad SE en horizontal.

## Qué hace (versión 0.1)

- Reloj y fecha grandes.
- Tarjeta de música: muestra la canción, el artista y la portada de lo que suene (Spotify, YouTube Music, etc.) con botones de anterior, reproducir/pausar y siguiente.
- Botones grandes para CarPlay (abre [DiPlay](https://github.com/shihabal3amri/DiPlay)), Google Maps, Waze y Spotify. Si una app no está instalada, el botón lleva a descargarla.
- Lista de todas las apps y acceso a Ajustes.
- Mantiene la pantalla encendida y oculta las barras del sistema.

## Instalar en la tablet

1. En la tablet, abre la página de [Releases](../../releases) y descarga `Cardroid.apk` (la versión de prueba está en la etiqueta `dev`).
2. Ábrelo y permite instalar apps desde el navegador si Android lo pide.
3. Para ponerlo como pantalla de inicio: Ajustes > Apps > Apps predeterminadas > Pantalla de inicio > Cardroid. También puedes abrirlo como una app normal.

### Activar la tarjeta de música

Android 15 bloquea este permiso en apps instaladas desde un APK, así que hay que desbloquearlo una vez:

1. Ajustes > Apps > Cardroid > menú ⋮ (arriba a la derecha) > **Permitir ajustes restringidos**.
2. Vuelve a Cardroid y toca **Activar acceso a notificaciones**, luego activa Cardroid.

Sin este permiso los botones de música igual funcionan, pero no se ve qué canción suena.

## Compilar

GitHub Actions compila el APK en cada push (`.github/workflows/build.yml`). Localmente: `./gradlew assembleDebug` con el SDK de Android instalado.
