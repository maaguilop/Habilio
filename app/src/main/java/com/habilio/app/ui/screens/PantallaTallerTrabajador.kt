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

@Composable
fun PantallaTallerTrabajador(
    trabajador: PerfilUsuario?,
    evidencias: List<EvidenciaItem>,
    solicitudes: List<SolicitudItem>,
    escalaTexto: Float,
    alCambiarEscalaTexto: (Float) -> Unit,
    apariencia: String,
    alCambiarApariencia: (String) -> Unit,
    alEditarPerfil: () -> Unit,
    alPublicarTrabajo: (NuevoTrabajo, (Int, Int) -> Unit, (Boolean) -> Unit) -> Unit,
    alClickFotoEvidencia: (EvidenciaItem) -> Unit,
    alIrASolicitudes: () -> Unit,
    alCambiarRol: () -> Unit,
    resenas: List<ResenaItem> = emptyList()
) {
    val contextoTaller = androidx.compose.ui.platform.LocalContext.current
    val scopeTaller = rememberCoroutineScope()
    var tituloNuevaEvidencia by remember { mutableStateOf("") }
    // Acta #8: datos del trabajo y consentimiento
    var descripcionTrabajo by remember { mutableStateOf("") }
    var tipoServicioTrabajo by remember { mutableStateOf("") }
    val hoyTexto = remember { SimpleDateFormat("dd/MM/yyyy", Locale.US).format(java.util.Date()) }
    var fechaTrabajo by remember { mutableStateOf(hoyTexto) }
    var consentimientoImagenes by remember { mutableStateOf(false) }
    // Acta #9: informe de cada foto y progreso de subida
    val informesImagen = remember { mutableStateMapOf<Uri, InformeImagen>() }
    var progresoSubida by remember { mutableStateOf<Pair<Int, Int>?>(null) }

    // RF-02: el trabajador elige entre una foto individual o un conjunto Antes / Después
    var modoAntesDespues by remember { mutableStateOf(false) }
    var fotoUnica by remember { mutableStateOf<Uri?>(null) }
    val fotosAntes = remember { mutableStateListOf<Uri>() }
    val fotosDespues = remember { mutableStateListOf<Uri>() }
    var ladoPendiente by remember { mutableStateOf(EtiquetaFoto.UNICA) }
    var publicando by remember { mutableStateOf(false) }
    var mensajePublicacion by remember { mutableStateOf<String?>(null) }
    var publicacionConError by remember { mutableStateOf(false) }

    val trabajos = remember(evidencias) { agruparEvidencias(evidencias) }

    val ultimoTrabajoConcluido = solicitudes.firstOrNull { it.estado == "completado" }

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            when (ladoPendiente) {
                EtiquetaFoto.ANTES -> if (fotosAntes.size < MAX_FOTOS_POR_LADO) fotosAntes.add(uri)
                EtiquetaFoto.DESPUES -> if (fotosDespues.size < MAX_FOTOS_POR_LADO) fotosDespues.add(uri)
                else -> fotoUnica = uri
            }
            scopeTaller.launch { SupabaseService.analizarImagen(contextoTaller, uri)?.let { informesImagen[uri] = it } }
        }
    }
    fun elegirFoto(lado: String) {
        ladoPendiente = lado
        mensajePublicacion = null
        imagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }

    val totalFotosElegidas = if (modoAntesDespues) fotosAntes.size + fotosDespues.size else (if (fotoUnica != null) 1 else 0)
    val fechaValida = remember(fechaTrabajo) {
        try {
            val f = SimpleDateFormat("dd/MM/yyyy", Locale.US).apply { isLenient = false }
            val d = f.parse(fechaTrabajo.trim())
            d != null && !d.after(java.util.Date())
        } catch (_: Exception) { false }
    }
    val datosTrabajoValidos = tituloNuevaEvidencia.isNotBlank() && descripcionTrabajo.trim().length >= 15 &&
        tipoServicioTrabajo.isNotBlank() && fechaValida && consentimientoImagenes
    val listoParaPublicar = datosTrabajoValidos && !publicando && (
            if (modoAntesDespues) fotosAntes.isNotEmpty() && fotosDespues.isNotEmpty() else fotoUnica != null
            )

    Scaffold(
        topBar = {
            HabilioTopBar(
                subtituloUbicacion = trabajador?.zona ?: "Pénjamo, Gto.",
                modoTrabajador = true,
                alAccionDerecha = alCambiarRol
            )
        },
        bottomBar = {
            HabilioBarraInferior(
                primerTabSeleccionado = false,
                etiquetaPrimero = "Solicitudes",
                iconoPrimero = Icons.Default.FormatListBulleted,
                alClickPrimero = alIrASolicitudes,
                etiquetaSegundo = "Mi Taller",
                iconoSegundo = Icons.Default.Build,
                alClickSegundo = {}
            )
        },
        containerColor = HabilioTheme.FondoPrincipal
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            if (evidencias.isEmpty()) {
                HabilioTarjeta(
                    colorFondo = HabilioTheme.FondoAzulSuave,
                    colorBorde = HabilioTheme.AzulIcono.copy(alpha = 0.5f)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Verified,
                            contentDescription = null,
                            tint = HabilioTheme.AzulIcono,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "¡Te damos la bienvenida a tu taller! Publica trabajos y gana insignias",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 14.sp,
                                color = HabilioTheme.TextoPrimario
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Sube tu primera fotografía de un trabajo real terminado para mostrar tu experiencia. Las insignias se ganan con requisitos claros que cualquiera puede comprobar.",
                                fontSize = 12.sp,
                                color = HabilioTheme.TextoSecundario,
                                lineHeight = 17.sp
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            if (ultimoTrabajoConcluido != null) {
                HabilioTarjeta(
                    colorFondo = HabilioTheme.FondoVerdeSuave,
                    colorBorde = HabilioTheme.VerdeExito.copy(alpha = 0.6f)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        HabilioBadge("SERVICIO CONCLUIDO", HabilioTheme.VerdeExito, HabilioTheme.FondoVerdeSuave)
                        Text("Completado", fontSize = 12.sp, color = HabilioTheme.TextoSecundario)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "${ultimoTrabajoConcluido.descripcion} • Cliente ${ultimoTrabajoConcluido.nombreCliente}",
                        fontSize = 14.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "$${String.format("%.2f", ultimoTrabajoConcluido.monto)} MXN (Acuerdo directo)",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = HabilioTheme.VerdeExito
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            HabilioTarjeta {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        AvatarUsuario(
                            tamano = 56.dp,
                            icono = Icons.Default.Person,
                            colorIcono = HabilioTheme.TextoPrimario,
                            fotoUrl = trabajador?.fotoUrl
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(trabajador?.nombre ?: "Especialista Habilio", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                            Text(
                                text = trabajador?.oficio ?: "Oficio",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = HabilioTheme.AmarilloAcentoTexto
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = alEditarPerfil,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, HabilioTheme.AmarilloPrimario),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Icon(Icons.Outlined.Edit, contentDescription = "Editar perfil", tint = HabilioTheme.AmarilloAcentoTexto, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Editar", color = HabilioTheme.AmarilloAcentoTexto, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                HabilioTarjeta(colorFondo = HabilioTheme.SuperficieInput, radioEsquina = 8.dp, paddingInterno = 10.dp) {
                    Text(
                        text = "Especialista en ${trabajador?.oficio ?: "Oficio"} • ${trabajador?.modalidad ?: "Trabajo a domicilio"}" +
                                if (!trabajador?.especialidad.isNullOrBlank()) "\n${trabajador?.especialidad}" else "",
                        fontSize = 13.sp,
                        color = HabilioTheme.TextoSecundario
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Sección de accesibilidad: Tamaño del texto (Modo Trabajador)
            SeccionTamanoTexto(
                escalaActual = escalaTexto,
                alCambiarEscala = alCambiarEscalaTexto
            )

            Spacer(modifier = Modifier.height(16.dp))

            SeccionApariencia(
                aparienciaActual = apariencia,
                alCambiarApariencia = alCambiarApariencia
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (trabajador?.autorizacionEstado == "pendiente") {
                HabilioTarjeta(colorFondo = HabilioTheme.FondoAmarilloSuave, radioEsquina = 12.dp, paddingInterno = 12.dp) {
                    Text("Autorización pendiente de verificar", fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = HabilioTheme.TextoPrimario)
                    Text(
                        "Habilio se comunicará con tu madre, padre o persona tutora para confirmar tu autorización. Mientras tanto puedes preparar tu portafolio, pero tu perfil no aparece en búsquedas ni recibe solicitudes.",
                        fontSize = 12.sp, lineHeight = 17.sp, color = HabilioTheme.TextoSecundario
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
            if (trabajador != null) {
                PanelInsignias(trabajador, evidencias, resenas)
                Spacer(modifier = Modifier.height(16.dp))
            }

            HabilioTarjeta(colorBorde = HabilioTheme.AmarilloPrimario.copy(alpha = 0.5f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Publicar un trabajo", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text("+ Evidencia", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = HabilioTheme.AmarilloAcentoTexto)
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Muestra un trabajo terminado. Puedes subir una sola foto o enseñar el cambio con fotos de antes y después.",
                    fontSize = 13.sp,
                    color = HabilioTheme.TextoSecundario,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = tituloNuevaEvidencia,
                    onValueChange = { tituloNuevaEvidencia = it; mensajePublicacion = null },
                    placeholder = {
                        Text("Título (ej. Impermeabilización de azotea)", color = HabilioTheme.TextoPlaceholder, fontSize = 13.sp)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    colors = coloresInputHabilio()
                )

                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = descripcionTrabajo,
                    onValueChange = { descripcionTrabajo = it; mensajePublicacion = null },
                    label = { Text("¿Qué hiciste? (mín. 15 caracteres)") },
                    minLines = 2, modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp), colors = coloresInputHabilio(),
                    supportingText = { Text("Materiales, problema que resolviste, tiempo que tomó.") }
                )

                Spacer(modifier = Modifier.height(8.dp))
                Text("Tipo de servicio", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                val opcionesServicio = remember(trabajador?.oficio) {
                    (EJEMPLOS_ESPECIALIDAD[trabajador?.oficio].orEmpty() + "Otro").distinct()
                }
                Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    opcionesServicio.forEach { o ->
                        FilterChip(
                            selected = tipoServicioTrabajo == o, onClick = { tipoServicioTrabajo = o },
                            label = { Text(o, fontSize = 13.sp) },
                            modifier = Modifier.heightIn(min = HabilioTheme.alturaMinima(48.dp))
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = fechaTrabajo, onValueChange = { fechaTrabajo = it.take(10) },
                    label = { Text("Fecha del trabajo (dd/mm/aaaa)") }, singleLine = true,
                    isError = !fechaValida, modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp), colors = coloresInputHabilio(),
                    supportingText = { if (!fechaValida) Text("Usa el formato dd/mm/aaaa y una fecha que no sea futura") }
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text("¿Cómo quieres mostrarlo?", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(8.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OpcionTipoTrabajo(
                        titulo = "Una sola foto",
                        descripcion = "El trabajo terminado",
                        icono = Icons.Outlined.Image,
                        seleccionado = !modoAntesDespues,
                        alClick = { modoAntesDespues = false; mensajePublicacion = null },
                        modifier = Modifier.weight(1f)
                    )
                    OpcionTipoTrabajo(
                        titulo = "Antes y después",
                        descripcion = "Enseña el cambio",
                        icono = Icons.Outlined.Compare,
                        seleccionado = modoAntesDespues,
                        alClick = { modoAntesDespues = true; mensajePublicacion = null },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                if (!modoAntesDespues) {
                    val foto = fotoUnica
                    if (foto != null) {
                        FotoElegida(
                            uri = foto,
                            descripcion = "Foto elegida para publicar",
                            alto = 170.dp,
                            alQuitar = { fotoUnica = null }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        InformeFotoTexto(informesImagen[foto])
                    } else {
                        BotonAgregarFoto("Elegir fotografía", { elegirFoto(EtiquetaFoto.UNICA) })
                    }
                } else {
                    val puedeAgregarMas = totalFotosElegidas < MAX_FOTOS_POR_TRABAJO
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        listOf(
                            Triple(EtiquetaFoto.ANTES, fotosAntes, "Agregar antes"),
                            Triple(EtiquetaFoto.DESPUES, fotosDespues, "Agregar después")
                        ).forEach { (lado, lista, textoBoton) ->
                            Column(modifier = Modifier.weight(1f)) {
                                EtiquetaAntesDespues(lado)
                                Spacer(modifier = Modifier.height(6.dp))
                                lista.toList().forEachIndexed { indice, uri ->
                                    FotoElegida(
                                        uri = uri,
                                        descripcion = if (lado == EtiquetaFoto.ANTES) "Foto de antes" else "Foto de después",
                                        alto = if (lista.size > 1) 96.dp else 130.dp,
                                        alQuitar = { lista.remove(uri) }
                                    )
                                    InformeFotoTexto(informesImagen[uri])
                                    Spacer(modifier = Modifier.height(6.dp))
                                }
                                if (lista.size < MAX_FOTOS_POR_LADO && puedeAgregarMas) {
                                    BotonAgregarFoto(textoBoton, { elegirFoto(lado) })
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Necesitas al menos una foto de antes y una de después. Máximo $MAX_FOTOS_POR_TRABAJO fotos por trabajo.",
                        fontSize = 12.sp,
                        color = HabilioTheme.TextoSecundario,
                        lineHeight = 17.sp
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    verticalAlignment = Alignment.Top,
                    modifier = Modifier.fillMaxWidth().clickable(enabled = !publicando) { consentimientoImagenes = !consentimientoImagenes }
                ) {
                    Checkbox(
                        checked = consentimientoImagenes, onCheckedChange = { consentimientoImagenes = it }, enabled = !publicando,
                        colors = CheckboxDefaults.colors(checkedColor = HabilioTheme.AmarilloPrimario, checkmarkColor = HabilioTheme.TextoSobreAmarillo)
                    )
                    Text(
                        "Confirmo que tengo permiso de las personas que aparecen y de quien es dueña o dueño del lugar para publicar estas fotos, y que no se ven documentos, placas ni menores de edad.",
                        fontSize = 12.sp, lineHeight = 17.sp, color = HabilioTheme.TextoPrimario,
                        modifier = Modifier.padding(top = 12.dp)
                    )
                }

                progresoSubida?.let { (listas, total) ->
                    if (publicando) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Subiendo fotos: $listas de $total", fontSize = 12.sp, color = HabilioTheme.TextoSecundario)
                        LinearProgressIndicator(
                            progress = { if (total == 0) 0f else listas.toFloat() / total },
                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                            color = HabilioTheme.AmarilloPrimario, trackColor = HabilioTheme.BordeTarjeta
                        )
                    }
                }

                if (mensajePublicacion != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = mensajePublicacion!!,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (publicacionConError) HabilioTheme.RojoUrgente else HabilioTheme.VerdeExito
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = {
                        val fotos = if (modoAntesDespues) {
                            fotosAntes.map { FotoNueva(EtiquetaFoto.ANTES, it) } +
                                    fotosDespues.map { FotoNueva(EtiquetaFoto.DESPUES, it) }
                        } else {
                            listOfNotNull(fotoUnica?.let { FotoNueva(EtiquetaFoto.UNICA, it) })
                        }
                        publicando = true
                        mensajePublicacion = null
                        progresoSubida = 0 to fotos.size
                        val nuevo = NuevoTrabajo(
                            titulo = tituloNuevaEvidencia, descripcion = descripcionTrabajo, tipoServicio = tipoServicioTrabajo,
                            fechaTrabajo = fechaTrabajo, consentimientoImagenes = consentimientoImagenes, fotos = fotos
                        )
                        alPublicarTrabajo(nuevo, { listas, total -> progresoSubida = listas to total }) { exito ->
                            publicando = false
                            publicacionConError = !exito
                            if (exito) {
                                tituloNuevaEvidencia = ""; descripcionTrabajo = ""; tipoServicioTrabajo = ""
                                fechaTrabajo = hoyTexto; consentimientoImagenes = false
                                fotoUnica = null
                                fotosAntes.clear()
                                fotosDespues.clear()
                                informesImagen.clear()
                                progresoSubida = null
                                mensajePublicacion = "¡Listo! Tu trabajo ya aparece en tu portafolio."
                            } else {
                                // Los datos y las fotos se conservan: basta con volver a presionar el botón
                                mensajePublicacion = "No se pudo publicar (se reintentó cada foto 3 veces). Tus datos y fotos siguen aquí: revisa tu conexión y presiona \"Reintentar\"."
                            }
                        }
                    },
                    enabled = listoParaPublicar,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = HabilioTheme.alturaMinima(52.dp)),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = HabilioTheme.AmarilloPrimario,
                        contentColor = HabilioTheme.TextoSobreAmarillo
                    )
                ) {
                    Icon(Icons.Default.Upload, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (publicando) "Publicando..." else if (publicacionConError) "Reintentar publicación" else "Publicar trabajo",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 14.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text("Mis trabajos publicados (${trabajos.size})", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(12.dp))

            EstadoCargaOVacio(
                cargando = false,
                estaVacio = trabajos.isEmpty(),
                mensajeVacio = "Aún no tienes trabajos publicados. Escribe un título arriba, elige tus fotos y presiona 'Publicar trabajo'."
            )

            trabajos.forEach { trabajo ->
                TarjetaTrabajoPortafolio(
                    trabajo = trabajo,
                    alClickFoto = alClickFotoEvidencia,
                    altoFoto = 130.dp,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
            }
        }
    }
}
