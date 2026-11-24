package com.habilio.app

import android.content.Context
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import java.util.UUID
import kotlin.math.abs
import kotlin.math.max

enum class RutaHabilio {
    INICIO_ROL,
    LOGIN_CLIENTE,
    REGISTRO_CLIENTE,
    LOGIN_TRABAJADOR,
    REGISTRO_TRABAJADOR,
    BUSCADOR_CLIENTE,
    DETALLE_ESPECIALISTA,
    PERFIL_CLIENTE,
    SOLICITUDES_TRABAJADOR,
    TALLER_TRABAJADOR,
    EDITAR_PERFIL,
    SEGUIMIENTO_SOLICITUD
}

@Composable
fun HabilioApp() {
    val contexto = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()

    // Preferencia persistente del tamaño de texto para ambos modos
    val prefs = remember(contexto) {
        contexto.getSharedPreferences(AppConfig.PREFS_NAME, Context.MODE_PRIVATE)
    }
    var escalaTexto by remember {
        // Las preferencias guardadas con la escala anterior (85 %, 115 %, 130 %) se ajustan al nivel más cercano
        mutableFloatStateOf(ajustarEscalaGuardada(prefs.getFloat(AppConfig.KEY_ESCALA_TEXTO, 1.0f)))
    }
    // Botones y controles crecen en proporción al texto (RF-07)
    HabilioTheme.factorControles = 1f + (escalaTexto - 1f) * 0.5f

    fun actualizarEscalaTexto(nuevaEscala: Float) {
        escalaTexto = nuevaEscala
        prefs.edit().putFloat(AppConfig.KEY_ESCALA_TEXTO, nuevaEscala).apply()
    }

    // Apariencia: sistema / claro / oscuro (persistente)
    var apariencia by remember {
        mutableStateOf(prefs.getString(AppConfig.KEY_APARIENCIA, "sistema") ?: "sistema")
    }
    fun actualizarApariencia(nueva: String) {
        apariencia = nueva
        prefs.edit().putString(AppConfig.KEY_APARIENCIA, nueva).apply()
    }
    val sistemaOscuro = androidx.compose.foundation.isSystemInDarkTheme()
    val usarOscuro = when (apariencia) {
        "claro" -> false
        "oscuro" -> true
        else -> sistemaOscuro
    }
    // Se asigna durante la composición para que el primer frame ya use la paleta correcta
    HabilioTheme.modoOscuro = usarOscuro

    // Barras del sistema acordes al tema
    val actividad = contexto as? ComponentActivity
    SideEffect {
        actividad?.enableEdgeToEdge(
            statusBarStyle = HabilioTheme.estiloBarraSistema(usarOscuro),
            navigationBarStyle = HabilioTheme.estiloBarraSistema(usarOscuro)
        )
    }

    var pantallaActual by remember { mutableStateOf(RutaHabilio.INICIO_ROL) }

    var usuarioActual by remember { mutableStateOf<PerfilUsuario?>(null) }
    var listaTrabajadores by remember { mutableStateOf<List<PerfilUsuario>>(emptyList()) }
    var trabajadorSeleccionado by remember { mutableStateOf<PerfilUsuario?>(null) }
    var solicitudEnSeguimiento by remember { mutableStateOf<SolicitudItem?>(null) }
    var todasLasEvidencias by remember { mutableStateOf<List<EvidenciaItem>>(emptyList()) }
    var todasLasResenas by remember { mutableStateOf<List<ResenaItem>>(emptyList()) }
    var solicitudesUsuario by remember { mutableStateOf<List<SolicitudItem>>(emptyList()) }
    var cargandoDatos by remember { mutableStateOf(false) }

    var mostrarModalCompra by remember { mutableStateOf(false) }
    var solicitudRecienConfirmada by remember { mutableStateOf<SolicitudItem?>(null) }
    var mostrarModalCalificar by remember { mutableStateOf(false) }
    var evidenciaEnPantallaCompleta by remember { mutableStateOf<EvidenciaItem?>(null) }

    var solicitudAEvaluar by remember { mutableStateOf<SolicitudItem?>(null) }

    // Acta #10: estado del envío de una solicitud (progreso de fotos y errores visibles)
    var enviandoSolicitud by remember { mutableStateOf(false) }
    var mensajeEnvioSolicitud by remember { mutableStateOf("") }
    // Acta #13: solicitud que se está por cancelar (abre el diálogo con motivo)
    var solicitudACancelar by remember { mutableStateOf<SolicitudItem?>(null) }
    // Acta #13: avisos internos pendientes
    var avisosPendientes by remember { mutableStateOf<List<EventoSolicitud>>(emptyList()) }
    var mostrarAvisos by remember { mutableStateOf(false) }
    // Acta #14: reseña que se va a reportar
    var resenaAReportar by remember { mutableStateOf<ResenaItem?>(null) }

    fun recargarDatosGlobales() {
        scope.launch {
            cargandoDatos = true
            listaTrabajadores = SupabaseService.obtenerTrabajadores()
            todasLasEvidencias = SupabaseService.obtenerEvidencias()
            todasLasResenas = SupabaseService.obtenerResenas()
            usuarioActual?.let { user ->
                solicitudesUsuario = SupabaseService.obtenerSolicitudesUsuario(
                    usuarioId = user.id,
                    esTrabajador = user.rol == "trabajador"
                )
                trabajadorSeleccionado?.let { sel ->
                    trabajadorSeleccionado = listaTrabajadores.find { it.id == sel.id } ?: sel
                }
                solicitudEnSeguimiento?.let { seg ->
                    solicitudEnSeguimiento = solicitudesUsuario.find { it.id == seg.id } ?: seg
                }
                avisosPendientes = SupabaseService.obtenerAvisosPendientes(user.id)
            }
            cargandoDatos = false
        }
    }

    fun cerrarSesionYVolverInicio() {
        SupabaseService.cerrarSesionToken()
        usuarioActual = null
        pantallaActual = RutaHabilio.INICIO_ROL
    }

    BackHandler(enabled = pantallaActual != RutaHabilio.INICIO_ROL) {
        when (pantallaActual) {
            RutaHabilio.LOGIN_CLIENTE, RutaHabilio.LOGIN_TRABAJADOR -> pantallaActual = RutaHabilio.INICIO_ROL
            RutaHabilio.REGISTRO_CLIENTE -> pantallaActual = RutaHabilio.LOGIN_CLIENTE
            RutaHabilio.REGISTRO_TRABAJADOR -> pantallaActual = RutaHabilio.LOGIN_TRABAJADOR
            RutaHabilio.DETALLE_ESPECIALISTA, RutaHabilio.PERFIL_CLIENTE -> pantallaActual = RutaHabilio.BUSCADOR_CLIENTE
            RutaHabilio.SEGUIMIENTO_SOLICITUD -> pantallaActual = RutaHabilio.PERFIL_CLIENTE
            RutaHabilio.TALLER_TRABAJADOR -> pantallaActual = RutaHabilio.SOLICITUDES_TRABAJADOR
            RutaHabilio.EDITAR_PERFIL -> {
                pantallaActual = if (usuarioActual?.rol == "trabajador") {
                    RutaHabilio.TALLER_TRABAJADOR
                } else {
                    RutaHabilio.PERFIL_CLIENTE
                }
            }
            RutaHabilio.BUSCADOR_CLIENTE, RutaHabilio.SOLICITUDES_TRABAJADOR -> cerrarSesionYVolverInicio()
            RutaHabilio.INICIO_ROL -> {}
        }
    }

    // Aplica el escalado tipográfico elegido a toda la aplicación
    val densidadBase = LocalDensity.current
    CompositionLocalProvider(
        LocalDensity provides Density(
            density = densidadBase.density,
            fontScale = densidadBase.fontScale * escalaTexto
        )
    ) {
        MaterialTheme(
            colorScheme = (if (usarOscuro) darkColorScheme() else lightColorScheme()).copy(
                background = HabilioTheme.FondoPrincipal,
                surface = HabilioTheme.SuperficieTarjeta,
                surfaceVariant = HabilioTheme.SuperficieInput,
                surfaceTint = Color.Transparent,
                secondaryContainer = HabilioTheme.FondoAmarilloSuave,
                onSecondaryContainer = HabilioTheme.AmarilloAcentoTexto,
                // En claro el "primary" del sistema (cursor, botones de texto) usa el ámbar oscuro legible
                primary = if (usarOscuro) HabilioTheme.AmarilloPrimario else HabilioTheme.AmarilloAcentoTexto,
                onPrimary = if (usarOscuro) HabilioTheme.TextoSobreAmarillo else Color.White,
                outlineVariant = HabilioTheme.BordeTarjeta,
                onBackground = HabilioTheme.TextoPrimario,
                onSurface = HabilioTheme.TextoPrimario,
                onSurfaceVariant = HabilioTheme.TextoSecundario,
                outline = HabilioTheme.BordeTarjeta,
                error = HabilioTheme.RojoUrgente
            )
        ) {
            Surface(modifier = Modifier.fillMaxSize(), color = HabilioTheme.FondoPrincipal) {
                when (pantallaActual) {
                    RutaHabilio.INICIO_ROL -> PantallaInicioRol(
                        alElegirCliente = { pantallaActual = RutaHabilio.LOGIN_CLIENTE },
                        alElegirTrabajador = { pantallaActual = RutaHabilio.LOGIN_TRABAJADOR }
                    )

                    RutaHabilio.LOGIN_CLIENTE -> PantallaLoginUnificada(
                        esModoTrabajador = false,
                        alIrARegistro = { pantallaActual = RutaHabilio.REGISTRO_CLIENTE },
                        alLoginExitoso = { perfil ->
                            usuarioActual = perfil
                            recargarDatosGlobales()
                            pantallaActual = if (perfil.rol == "trabajador") {
                                RutaHabilio.SOLICITUDES_TRABAJADOR
                            } else {
                                RutaHabilio.BUSCADOR_CLIENTE
                            }
                        },
                        alCambiarRolOpuesto = { pantallaActual = RutaHabilio.LOGIN_TRABAJADOR },
                        alVolverInicio = { pantallaActual = RutaHabilio.INICIO_ROL }
                    )

                    RutaHabilio.REGISTRO_CLIENTE -> PantallaRegistroCliente(
                        alIrALogin = { pantallaActual = RutaHabilio.LOGIN_CLIENTE },
                        alRegistroExitoso = { perfil ->
                            usuarioActual = perfil
                            recargarDatosGlobales()
                            pantallaActual = RutaHabilio.BUSCADOR_CLIENTE
                        },
                        alCambiarATrabajador = { pantallaActual = RutaHabilio.LOGIN_TRABAJADOR },
                        alVolverInicio = { pantallaActual = RutaHabilio.INICIO_ROL }
                    )

                    RutaHabilio.LOGIN_TRABAJADOR -> PantallaLoginUnificada(
                        esModoTrabajador = true,
                        alIrARegistro = { pantallaActual = RutaHabilio.REGISTRO_TRABAJADOR },
                        alLoginExitoso = { perfil ->
                            usuarioActual = perfil
                            recargarDatosGlobales()
                            pantallaActual = if (perfil.rol == "cliente") {
                                RutaHabilio.BUSCADOR_CLIENTE
                            } else {
                                RutaHabilio.SOLICITUDES_TRABAJADOR
                            }
                        },
                        alCambiarRolOpuesto = { pantallaActual = RutaHabilio.LOGIN_CLIENTE },
                        alVolverInicio = { pantallaActual = RutaHabilio.INICIO_ROL }
                    )

                    RutaHabilio.REGISTRO_TRABAJADOR -> PantallaRegistroTrabajador(
                        alIrALogin = { pantallaActual = RutaHabilio.LOGIN_TRABAJADOR },
                        alRegistroExitoso = { perfil ->
                            usuarioActual = perfil
                            recargarDatosGlobales()
                            pantallaActual = RutaHabilio.TALLER_TRABAJADOR
                        },
                        alCambiarACliente = { pantallaActual = RutaHabilio.LOGIN_CLIENTE },
                        alVolverInicio = { pantallaActual = RutaHabilio.INICIO_ROL }
                    )

                    RutaHabilio.BUSCADOR_CLIENTE -> PantallaBuscadorCliente(
                        trabajadores = listaTrabajadores,
                        evidencias = todasLasEvidencias,
                        resenas = todasLasResenas,
                        cargando = cargandoDatos,
                        alRefrescar = { recargarDatosGlobales() },
                        alVerEspecialista = { especialista ->
                            trabajadorSeleccionado = especialista
                            pantallaActual = RutaHabilio.DETALLE_ESPECIALISTA
                        },
                        alClickFotoEvidencia = { ev ->
                            if (!ev.fotoUrl.isNullOrBlank()) evidenciaEnPantallaCompleta = ev
                        },
                        alIrAMiPerfil = {
                            recargarDatosGlobales()
                            pantallaActual = RutaHabilio.PERFIL_CLIENTE
                        },
                        alCambiarRol = { cerrarSesionYVolverInicio() }
                    )

                    RutaHabilio.DETALLE_ESPECIALISTA -> {
                        val esp = trabajadorSeleccionado
                        if (esp != null) {
                            val serviciosConEsteTrabajador = solicitudesUsuario.count {
                                it.trabajadorId == esp.id && it.estado == "completado"
                            }
                            val serviciosSinEvaluar = solicitudesUsuario.count {
                                it.trabajadorId == esp.id && it.estado == "completado" && !it.fueEvaluada
                            }

                            PantallaDetalleEspecialista(
                                especialista = esp,
                                evidencias = todasLasEvidencias.filter { it.trabajadorId == esp.id },
                                resenas = todasLasResenas.filter { it.trabajadorId == esp.id },
                                totalServicios = serviciosConEsteTrabajador,
                                puedeCalificar = serviciosSinEvaluar > 0,
                                alVolverAlListado = { pantallaActual = RutaHabilio.BUSCADOR_CLIENTE },
                                alAbrirModalSolicitar = { mostrarModalCompra = true },
                                alAbrirModalCalificar = {
                                    solicitudAEvaluar = solicitudesUsuario.firstOrNull {
                                        it.trabajadorId == esp.id && it.estado == "completado" && !it.fueEvaluada
                                    }
                                    mostrarModalCalificar = true
                                },
                                alClickFotoEvidencia = { ev ->
                                    if (!ev.fotoUrl.isNullOrBlank()) evidenciaEnPantallaCompleta = ev
                                },
                                alIrAExplorar = { pantallaActual = RutaHabilio.BUSCADOR_CLIENTE },
                                alIrAMiPerfil = {
                                    recargarDatosGlobales()
                                    pantallaActual = RutaHabilio.PERFIL_CLIENTE
                                },
                                alReportarResena = { resenaAReportar = it }
                            )
                        } else {
                            pantallaActual = RutaHabilio.BUSCADOR_CLIENTE
                        }
                    }

                    RutaHabilio.PERFIL_CLIENTE -> PantallaPerfilCliente(
                        cliente = usuarioActual,
                        solicitudes = solicitudesUsuario,
                        escalaTexto = escalaTexto,
                        apariencia = apariencia,
                        alCambiarApariencia = ::actualizarApariencia,
                        alCambiarEscalaTexto = ::actualizarEscalaTexto,
                        alEditarPerfil = { pantallaActual = RutaHabilio.EDITAR_PERFIL },
                        alVerSeguimientoSolicitud = { solicitud ->
                            solicitudEnSeguimiento = solicitud
                            pantallaActual = RutaHabilio.SEGUIMIENTO_SOLICITUD
                        },
                        alLiberarPago = { solicitud ->
                            solicitudesUsuario = solicitudesUsuario.map {
                                if (it.id == solicitud.id) it.copy(estado = "completado") else it
                            }
                            scope.launch {
                                SupabaseService.cambiarEstadoConEvento(solicitud, "cliente", "completado")
                                recargarDatosGlobales()
                            }
                        },
                        alCalificarTrabajador = { solicitud ->
                            listaTrabajadores.find { it.id == solicitud.trabajadorId }?.let {
                                trabajadorSeleccionado = it
                                solicitudAEvaluar = solicitud
                                mostrarModalCalificar = true
                            }
                        },
                        alIrAExplorar = { pantallaActual = RutaHabilio.BUSCADOR_CLIENTE },
                        alCambiarRol = { cerrarSesionYVolverInicio() }
                    )

                    RutaHabilio.SEGUIMIENTO_SOLICITUD -> {
                        val solActual = solicitudesUsuario.find { it.id == solicitudEnSeguimiento?.id }
                            ?: solicitudEnSeguimiento
                        if (solActual != null) {
                            val espAsociado = listaTrabajadores.find { it.id == solActual.trabajadorId }
                            PantallaSeguimientoSolicitud(
                                solicitud = solActual,
                                especialista = espAsociado,
                                alVolver = { pantallaActual = RutaHabilio.PERFIL_CLIENTE },
                                alRefrescar = { recargarDatosGlobales() },
                                alCancelarPedido = { solicitudACancelar = solActual },
                                alConfirmarCierre = {
                                    val actualizada = solActual.copy(estado = "completado")
                                    solicitudEnSeguimiento = actualizada
                                    solicitudesUsuario = solicitudesUsuario.map {
                                        if (it.id == solActual.id) actualizada else it
                                    }
                                    scope.launch {
                                        SupabaseService.cambiarEstadoConEvento(solActual, "cliente", "completado")
                                        recargarDatosGlobales()
                                    }
                                },
                                alCalificar = {
                                    if (espAsociado != null) {
                                        trabajadorSeleccionado = espAsociado
                                        solicitudAEvaluar = solActual
                                        mostrarModalCalificar = true
                                    }
                                },
                                alVerPerfilTrabajador = {
                                    if (espAsociado != null) {
                                        trabajadorSeleccionado = espAsociado
                                        pantallaActual = RutaHabilio.DETALLE_ESPECIALISTA
                                    }
                                }
                            )
                        } else {
                            pantallaActual = RutaHabilio.PERFIL_CLIENTE
                        }
                    }

                    RutaHabilio.SOLICITUDES_TRABAJADOR -> PantallaSolicitudesTrabajador(
                        trabajador = usuarioActual,
                        solicitudes = solicitudesUsuario,
                        cargando = cargandoDatos,
                        alRefrescar = { recargarDatosGlobales() },
                        alCambiarEstadoSolicitud = { solicitud, nuevoEstado ->
                            if (nuevoEstado == "cancelada" || (nuevoEstado == "rechazado" && solicitud.estado != "pendiente")) {
                                // Acta #13: cancelar un servicio ya aceptado exige motivo y deja constancia
                                solicitudACancelar = solicitud
                            } else {
                                solicitudesUsuario = solicitudesUsuario.map {
                                    if (it.id == solicitud.id) it.copy(estado = nuevoEstado) else it
                                }
                                scope.launch {
                                    SupabaseService.cambiarEstadoConEvento(solicitud, "trabajador", nuevoEstado)
                                    recargarDatosGlobales()
                                }
                            }
                        },
                        alIrAMiTaller = {
                            recargarDatosGlobales()
                            pantallaActual = RutaHabilio.TALLER_TRABAJADOR
                        },
                        alCambiarRol = { cerrarSesionYVolverInicio() }
                    )

                    RutaHabilio.TALLER_TRABAJADOR -> PantallaTallerTrabajador(
                        trabajador = usuarioActual,
                        evidencias = todasLasEvidencias.filter { it.trabajadorId == usuarioActual?.id },
                        solicitudes = solicitudesUsuario,
                        escalaTexto = escalaTexto,
                        apariencia = apariencia,
                        alCambiarApariencia = ::actualizarApariencia,
                        alCambiarEscalaTexto = ::actualizarEscalaTexto,
                        alEditarPerfil = { pantallaActual = RutaHabilio.EDITAR_PERFIL },
                        resenas = todasLasResenas.filter { it.trabajadorId == usuarioActual?.id },
                        alPublicarTrabajo = { nuevo, alProgreso, alTerminar ->
                            val t = usuarioActual
                            if (t == null) {
                                alTerminar(false)
                            } else {
                                scope.launch {
                                    val exito = SupabaseService.publicarTrabajo(
                                        context = contexto,
                                        trabajadorId = t.id,
                                        titulo = nuevo.titulo,
                                        ubicacion = t.zona,
                                        fotos = nuevo.fotos,
                                        descripcion = nuevo.descripcion,
                                        tipoServicio = nuevo.tipoServicio,
                                        fechaTrabajo = nuevo.fechaTrabajo,
                                        consentimientoImagenes = nuevo.consentimientoImagenes,
                                        onProgreso = alProgreso
                                    )
                                    if (exito) recargarDatosGlobales()
                                    alTerminar(exito)
                                }
                            }
                        },
                        alClickFotoEvidencia = { ev ->
                            if (!ev.fotoUrl.isNullOrBlank()) evidenciaEnPantallaCompleta = ev
                        },
                        alIrASolicitudes = {
                            recargarDatosGlobales()
                            pantallaActual = RutaHabilio.SOLICITUDES_TRABAJADOR
                        },
                        alCambiarRol = { cerrarSesionYVolverInicio() }
                    )

                    RutaHabilio.EDITAR_PERFIL -> {
                        val u = usuarioActual
                        if (u != null) {
                            PantallaEditarPerfil(
                                usuarioActual = u,
                                alGuardarExitoso = { perfilActualizado ->
                                    usuarioActual = perfilActualizado
                                    recargarDatosGlobales()
                                    pantallaActual = if (perfilActualizado.rol == "trabajador") {
                                        RutaHabilio.TALLER_TRABAJADOR
                                    } else {
                                        RutaHabilio.PERFIL_CLIENTE
                                    }
                                },
                                alCancelar = {
                                    pantallaActual = if (u.rol == "trabajador") {
                                        RutaHabilio.TALLER_TRABAJADOR
                                    } else {
                                        RutaHabilio.PERFIL_CLIENTE
                                    }
                                }
                            )
                        } else {
                            pantallaActual = RutaHabilio.INICIO_ROL
                        }
                    }
                }

                if (mostrarModalCompra && trabajadorSeleccionado != null) {
                    ModalSolicitarServicio(
                        trabajador = trabajadorSeleccionado!!,
                        direccionInicial = usuarioActual?.direccion ?: "",
                        enviando = enviandoSolicitud,
                        mensajeEnvio = mensajeEnvioSolicitud,
                        alCerrar = { mostrarModalCompra = false; mensajeEnvioSolicitud = "" },
                        alConfirmarSolicitud = { descripcion, ubicacion, presupuesto, esUrgente, fotos ->
                            val clienteId = usuarioActual?.id ?: return@ModalSolicitarServicio
                            val trab = trabajadorSeleccionado ?: return@ModalSolicitarServicio
                            scope.launch {
                                enviandoSolicitud = true
                                mensajeEnvioSolicitud = ""
                                var urlsFotos = emptyList<String>()
                                if (fotos.isNotEmpty()) {
                                    val subidas = SupabaseService.subirFotosProblema(contexto, fotos) { listas, total ->
                                        mensajeEnvioSolicitud = "Subiendo fotos… $listas de $total"
                                    }
                                    if (subidas == null) {
                                        enviandoSolicitud = false
                                        mensajeEnvioSolicitud = "No se pudieron subir las fotos. Revisa tu conexión e inténtalo de nuevo (tus datos no se perdieron)."
                                        return@launch
                                    }
                                    urlsFotos = subidas
                                }
                                mensajeEnvioSolicitud = "Enviando solicitud…"
                                SupabaseService.crearSolicitud(
                                    clienteId = clienteId,
                                    trabajadorId = trab.id,
                                    oficioEtiqueta = trab.oficio,
                                    descripcion = descripcion,
                                    ubicacion = ubicacion,
                                    monto = presupuesto,
                                    esUrgente = esUrgente,
                                    fotosProblema = urlsFotos
                                ).onSuccess { creada ->
                                    SupabaseService.registrarEvento(creada.id, "cliente", TipoEvento.CREADA, "", trab.id)
                                    mostrarModalCompra = false
                                    mensajeEnvioSolicitud = ""
                                    solicitudRecienConfirmada = creada.copy(
                                        nombreTrabajador = trab.nombre,
                                        oficioTrabajador = trab.oficio
                                    )
                                    recargarDatosGlobales()
                                }.onFailure {
                                    mensajeEnvioSolicitud = it.message ?: "No se pudo enviar la solicitud."
                                }
                                enviandoSolicitud = false
                            }
                        }
                    )
                }

                if (solicitudRecienConfirmada != null) {
                    ModalSolicitudConfirmada(
                        solicitud = solicitudRecienConfirmada!!,
                        alAceptarYVerEstatus = {
                            solicitudEnSeguimiento = solicitudRecienConfirmada
                            solicitudRecienConfirmada = null
                            recargarDatosGlobales()
                            pantallaActual = RutaHabilio.SEGUIMIENTO_SOLICITUD
                        },
                        alVolverAlInicio = {
                            solicitudRecienConfirmada = null
                            pantallaActual = RutaHabilio.BUSCADOR_CLIENTE
                        }
                    )
                }

                if (mostrarModalCalificar && trabajadorSeleccionado != null) {
                    ModalEvaluarServicio(
                        nombreClienteInicial = usuarioActual?.let { "${it.nombre} (${it.zona})" }
                            ?: "Cliente de Habilio",
                        alCerrar = { mostrarModalCalificar = false },
                        alPublicarResena = { estrellas, autor, comentario ->
                            val trab = trabajadorSeleccionado ?: return@ModalEvaluarServicio
                            scope.launch {
                                val solEv = solicitudAEvaluar
                                val publicada = SupabaseService.publicarResena(trab.id, autor, comentario, estrellas, solEv?.id ?: "")

                                solEv?.takeIf { publicada }?.let { sol ->
                                    SupabaseService.marcarSolicitudEvaluada(sol.id)

                                    solicitudesUsuario = solicitudesUsuario.map {
                                        if (it.id == sol.id) it.copy(fueEvaluada = true) else it
                                    }
                                    if (solicitudEnSeguimiento?.id == sol.id) {
                                        solicitudEnSeguimiento = solicitudEnSeguimiento?.copy(fueEvaluada = true)
                                    }
                                }

                                mostrarModalCalificar = false
                                solicitudAEvaluar = null

                                recargarDatosGlobales()
                            }
                        }
                    )
                }

                if (avisosPendientes.isNotEmpty() && usuarioActual != null && !mostrarAvisos) {
                    Box(
                        modifier = Modifier.fillMaxSize().statusBarsPadding().padding(top = 64.dp, end = 12.dp),
                        contentAlignment = Alignment.TopEnd
                    ) {
                        Surface(
                            onClick = { mostrarAvisos = true },
                            shape = RoundedCornerShape(20.dp),
                            color = HabilioTheme.AmarilloPrimario,
                            shadowElevation = 4.dp,
                            modifier = Modifier.heightIn(min = HabilioTheme.alturaMinima(44.dp))
                        ) {
                            Row(modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Notifications, contentDescription = null, tint = HabilioTheme.TextoSobreAmarillo, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    if (avisosPendientes.size == 1) "1 aviso nuevo" else "${avisosPendientes.size} avisos nuevos",
                                    fontSize = 13.sp, fontWeight = FontWeight.Bold, color = HabilioTheme.TextoSobreAmarillo
                                )
                            }
                        }
                    }
                }

                solicitudACancelar?.let { sol ->
                    val rolActor = usuarioActual?.rol ?: "cliente"
                    DialogoCancelacion(
                        solicitud = sol,
                        rol = rolActor,
                        alCerrar = { solicitudACancelar = null },
                        alConfirmar = { motivo, comentario ->
                            scope.launch {
                                val ok = SupabaseService.cancelarSolicitud(sol, rolActor, motivo, comentario)
                                if (ok) {
                                    solicitudesUsuario = solicitudesUsuario.map {
                                        if (it.id == sol.id) it.copy(estado = "cancelada") else it
                                    }
                                    if (solicitudEnSeguimiento?.id == sol.id) {
                                        solicitudEnSeguimiento = solicitudEnSeguimiento?.copy(estado = "cancelada")
                                    }
                                }
                                solicitudACancelar = null
                                recargarDatosGlobales()
                            }
                        }
                    )
                }

                if (mostrarAvisos) {
                    DialogoAvisos(
                        avisos = avisosPendientes,
                        alCerrar = {
                            mostrarAvisos = false
                            usuarioActual?.id?.let { id ->
                                scope.launch { SupabaseService.marcarAvisosLeidos(id); avisosPendientes = emptyList() }
                            }
                        }
                    )
                }

                resenaAReportar?.let { r ->
                    DialogoReportarResena(
                        alCerrar = { resenaAReportar = null },
                        alEnviar = { criterio, detalle ->
                            val reportante = usuarioActual?.id ?: ""
                            scope.launch { SupabaseService.reportarResena(r.id, reportante, criterio, detalle); recargarDatosGlobales() }
                            resenaAReportar = null
                        }
                    )
                }

                if (evidenciaEnPantallaCompleta != null) {
                    ModalVerFotoCompleta(
                        evidencia = evidenciaEnPantallaCompleta!!,
                        alCerrar = { evidenciaEnPantallaCompleta = null }
                    )
                }
            }
        }
    }
}
