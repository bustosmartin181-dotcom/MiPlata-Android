# Mi Plata 10.1 — versión completa (Kotlin / Compose)

## Funciones
- Hoy: saludo, semáforo, traspaso, anotar gasto, categorías, voz, racha, cuentas, recalibrar, compartir
- Días: objetivo por día, escenarios, candado
- Plan: ingresos, pagar al cobrar (fijo/variable/cuotas), metas, resumen
- Ajustes: PIN, tema, ocultar montos, recordatorio, export CSV, backup, nuevo periodo, demo

## GitHub Actions
El archivo `.github/workflows/build.yml` compila el APK solo.
Al terminar: Actions → Artifacts → MiPlata-APK

## Estructura
app/src/main/java/com/sueldo/app/
  Data.kt, AppUi.kt, MainActivity.kt, Notifications.kt, ExportCsv.kt
