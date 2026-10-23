package com.sueldo.app

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ExportCsv {

    fun diasCsv(dias: List<DiaUi>): String {
        val sb = StringBuilder()
        sb.appendLine("fecha,dia_semana,presupuesto,gasto,sobrante,cerrado")
        dias.forEach { d ->
            sb.appendLine(
                "${d.fecha},${esc(d.diaSemana)},${d.presupuesto},${d.gastoReal},${d.sobrante},${d.cerrado}"
            )
        }
        return sb.toString()
    }

    fun gastosCsv(gastos: List<Gasto>): String {
        val sb = StringBuilder()
        sb.appendLine("fecha,monto,detalle,categoria")
        gastos.forEach { g ->
            sb.appendLine("${g.fecha},${g.monto},${esc(g.detalle)},${esc(g.categoria)}")
        }
        return sb.toString()
    }

    fun importantesCsv(list: List<GastoImportante>): String {
        val sb = StringBuilder()
        sb.appendLine("concepto,monto,pagado,repetir,variable,cuota_actual,cuota_total")
        list.forEach { g ->
            sb.appendLine(
                "${esc(g.concepto)},${g.monto},${g.pagado},${g.repetirMesSiguiente},${g.montoVariable},${g.cuotaActual},${g.cuotaTotal}"
            )
        }
        return sb.toString()
    }

    fun share(ctx: Context, fileName: String, content: String) {
        val dir = File(ctx.cacheDir, "exports").apply { mkdirs() }
        val stamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(Date())
        val file = File(dir, "${fileName}_$stamp.csv")
        file.writeText(content, Charsets.UTF_8)
        val uri = FileProvider.getUriForFile(ctx, "${ctx.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Mi Plata — $fileName")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        ctx.startActivity(Intent.createChooser(intent, "Exportar CSV"))
    }

    private fun esc(s: String): String {
        val t = s.replace("\"", "\"\"")
        return if (t.contains(',') || t.contains('"') || t.contains('\n')) "\"$t\"" else t
    }
}
