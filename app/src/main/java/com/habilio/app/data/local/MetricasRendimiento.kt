package com.habilio.app

import android.app.Activity
import android.os.Build
import android.os.Debug
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.FrameMetrics
import android.view.Window

/**
 * ACTA #23 (y #9): MEDICIÓN DE RENDIMIENTO EN EL TELÉFONO.
 *
 * Mide sin librerías externas y sin enviar nada a internet:
 *  - Arranque: desde que Android crea el proceso hasta el primer cuadro dibujado.
 *  - Memoria: memoria real usada por la app (PSS) y montón de Java usado / máximo.
 *  - Fluidez: cuadros dibujados y cuadros lentos (más de 32 ms, es decir, que se notan como "tirones").
 *  - Red: número de consultas, tiempo promedio, percentil 95, la más lenta, errores y datos recibidos.
 *  - Subida de fotos: cantidad, peso total, tiempo y velocidad promedio en KB/s.
 *
 * Los resultados se ven en la tarjeta "Diagnóstico de rendimiento" (Mi perfil / Mi taller) y se escriben en
 * Logcat con la etiqueta "HabilioMetricas" para copiarlos al informe de pruebas.
 */
object MetricasRendimiento {
    private const val ETIQUETA = "HabilioMetricas"
    private const val MAX_MUESTRAS = 200
    private const val CUADRO_LENTO_MS = 32.0

    @Volatile var arranqueMs: Long = -1
        private set

    private val tiemposRed = ArrayDeque<Long>()
    private var peticiones = 0
    private var erroresRed = 0
    private var bytesRecibidos = 0L
    private var peticionMasLenta = ""
    private var tiempoMasLento = 0L

    private var subidas = 0
    private var subidasFallidas = 0
    private var bytesSubidos = 0L
    private var msSubida = 0L

    private var cuadros = 0L
    private var cuadrosLentos = 0L
    private var peorCuadroMs = 0.0
    private var escuchaCuadros: Any? = null

    fun registrarArranque(ms: Long) {
        if (arranqueMs >= 0) return // solo el arranque en frío, no al girar la pantalla
        arranqueMs = ms
        Log.i(ETIQUETA, "Arranque en frío hasta el primer cuadro: $ms ms")
    }

    @Synchronized
    fun registrarPeticion(url: String, metodo: String, ms: Long, bytes: Int, codigo: Int) {
        peticiones++
        if (codigo >= 400) erroresRed++
        bytesRecibidos += bytes
        tiemposRed.addLast(ms)
        if (tiemposRed.size > MAX_MUESTRAS) tiemposRed.removeFirst()
        if (ms > tiempoMasLento) {
            tiempoMasLento = ms
            // Solo la tabla o función, sin filtros (que pueden llevar ids de personas)
            peticionMasLenta = "$metodo " + url.substringAfter("/v1/").substringBefore("?")
        }
    }

    @Synchronized
    fun registrarSubida(bytes: Long, ms: Long, exito: Boolean) {
        if (!exito) { subidasFallidas++; return }
        subidas++
        bytesSubidos += bytes
        msSubida += ms
        val kbps = if (ms > 0) bytes / 1024.0 / (ms / 1000.0) else 0.0
        Log.i(ETIQUETA, "Foto subida: ${bytes / 1024} KB en $ms ms (${"%.0f".format(kbps)} KB/s)")
    }

    /**
     * Fluidez: escucha los tiempos de cada cuadro que Android realmente dibuja (API 24+). No obliga a dibujar
     * cuadros extra, así que no gasta batería cuando la pantalla está quieta.
     */
    fun iniciarMonitorFluidez(activity: Activity) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N || escuchaCuadros != null) return
        val escucha = Window.OnFrameMetricsAvailableListener { _, metricas, _ ->
            val ms = metricas.getMetric(FrameMetrics.TOTAL_DURATION) / 1_000_000.0
            synchronized(this) {
                cuadros++
                if (ms > CUADRO_LENTO_MS) cuadrosLentos++
                if (ms > peorCuadroMs) peorCuadroMs = ms
            }
        }
        try {
            activity.window.addOnFrameMetricsAvailableListener(escucha, Handler(Looper.getMainLooper()))
            escuchaCuadros = escucha
        } catch (_: Exception) { }
    }

    fun detenerMonitorFluidez(activity: Activity) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return
        val escucha = escuchaCuadros as? Window.OnFrameMetricsAvailableListener ?: return
        try { activity.window.removeOnFrameMetricsAvailableListener(escucha) } catch (_: Exception) { }
        escuchaCuadros = null
    }

    /** Resumen en pares (indicador, valor) para mostrar en pantalla y escribir en Logcat. */
    @Synchronized
    fun resumen(): List<Pair<String, String>> {
        val rt = Runtime.getRuntime()
        val usadoMb = (rt.totalMemory() - rt.freeMemory()) / 1_048_576.0
        val maxMb = rt.maxMemory() / 1_048_576.0
        val pssMb = Debug.getPss() / 1024.0
        val ordenados = tiemposRed.sorted()
        val promedio = if (ordenados.isEmpty()) 0L else ordenados.sum() / ordenados.size
        val p95 = if (ordenados.isEmpty()) 0L else ordenados[((ordenados.size - 1) * 95) / 100]
        val kbpsSubida = if (msSubida > 0) bytesSubidos / 1024.0 / (msSubida / 1000.0) else 0.0
        val porcLentos = if (cuadros > 0) cuadrosLentos * 100.0 / cuadros else 0.0
        val filas = listOf(
            "Arranque (hasta 1.er cuadro)" to if (arranqueMs >= 0) "$arranqueMs ms" else "Sin medir",
            "Memoria real (PSS)" to "%.1f MB".format(pssMb),
            "Memoria Java usada / máx." to "%.1f / %.0f MB".format(usadoMb, maxMb),
            "Cuadros dibujados" to "$cuadros",
            "Cuadros lentos (> 32 ms)" to "$cuadrosLentos (%.1f %%)".format(porcLentos),
            "Cuadro más lento" to "%.0f ms".format(peorCuadroMs),
            "Consultas de red" to "$peticiones ($erroresRed con error)",
            "Respuesta promedio / p95" to "$promedio ms / $p95 ms",
            "Consulta más lenta" to if (tiempoMasLento > 0) "$tiempoMasLento ms · $peticionMasLenta" else "—",
            "Datos recibidos" to "%.1f KB".format(bytesRecibidos / 1024.0),
            "Fotos subidas" to "$subidas (${subidasFallidas} fallidas) · %.0f KB".format(bytesSubidos / 1024.0),
            "Velocidad de subida" to if (subidas > 0) "%.0f KB/s".format(kbpsSubida) else "—"
        )
        filas.forEach { (k, v) -> Log.i(ETIQUETA, "$k: $v") }
        return filas
    }
}
