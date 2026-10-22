# Mi Plata

App Android nativa para controlar tu sueldo y gastos diarios.  
Diseñada con **Jetpack Compose + Material 3**, sin aspecto de tabla Excel.

## Características

- **Resumen visual** con hero card, progreso del período y estadísticas
- **Gastos importantes** con toggle de pagado/pendiente
- **Diario** con selector de días, presupuesto + carry-over automático y registro de gastos
- **Ajustes** para sueldo, saldo previo y monto diario manual
- Datos locales con Room (sin internet)
- Tema claro/oscuro automático

## Cómo abrir el proyecto

1. Instalá [Android Studio](https://developer.android.com/studio) (Ladybug o más reciente)
2. Abrí la carpeta `SueldoApp` como proyecto
3. Dejá que Gradle sincronice (puede tardar la primera vez)
4. Conectá un celular o usá un emulador
5. Presioná **Run** ▶

## Generar APK

```
./gradlew assembleRelease
```

El APK queda en:
`app/build/outputs/apk/release/app-release-unsigned.apk`

Para firmarlo usá el wizard de Android Studio → Build → Generate Signed Bundle / APK.

## Datos iniciales

La app viene precargada con los gastos de tu planilla de octubre/noviembre 2026.  
Podés editar todo desde las pantallas.

## Estructura

```
app/src/main/java/com/sueldo/app/
├── data/          # Room + Repository + modelos
├── navigation/    # Bottom navigation
├── ui/
│   ├── resumen/   # Dashboard principal
│   ├── gastos/    # Lista de gastos fijos
│   ├── diario/    # Control día a día
│   ├── ajustes/   # Configuración
│   ├── components/# Componentes reutilizables
│   └── theme/     # Colores y tipografía
└── MainActivity.kt
```

## Próximas mejoras posibles (comercializables)

- Agregar gasto importante desde diálogo
- Exportar PDF / compartir resumen
- Notificaciones de recordatorio
- Widgets de Android
- Múltiples períodos / historial
- Backup en la nube (opcional)
