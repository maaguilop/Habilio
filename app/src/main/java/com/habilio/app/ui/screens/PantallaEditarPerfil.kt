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
fun PantallaEditarPerfil(
    usuarioActual: PerfilUsuario,
    alGuardarExitoso: (PerfilUsuario) -> Unit,
    alCancelar: () -> Unit
) {
    val contexto = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    val esTrabajador = usuarioActual.rol == "trabajador"
    // Acta #24: borrador local. Si falla el guardado o se cierra la app, lo escrito no se pierde.
    val borrador = remember(usuarioActual.id) { BorradorLocal(contexto, "perfil.${usuarioActual.id}") }

    var nombre by remember(usuarioActual) { mutableStateOf(borrador.leer("nombre") ?: usuarioActual.nombre) }
    var telefono by remember(usuarioActual) { mutableStateOf(borrador.leer("telefono") ?: usuarioActual.telefono) }
    var zona by remember(usuarioActual) { mutableStateOf(borrador.leer("zona") ?: usuarioActual.zona) }
    var direccion by remember(usuarioActual) { mutableStateOf(borrador.leer("direccion") ?: usuarioActual.direccion) }
    var nombreTaller by remember(usuarioActual) { mutableStateOf(borrador.leer("taller") ?: usuarioActual.nombreTaller) }
    var oficioSeleccionado by remember(usuarioActual) {
        mutableStateOf(usuarioActual.oficio.ifBlank { "Albañilería" })
    }
    var especialidad by remember(usuarioActual) { mutableStateOf(borrador.leer("especialidad") ?: usuarioActual.especialidad) }
    var modalidadSeleccionada by remember(usuarioActual.id, usuarioActual.modalidad) {
        val modGuardada = usuarioActual.modalidad.trim()
        val idx = when {
            modGuardada.contains("Ambos", ignoreCase = true) -> 2
            modGuardada.contains("Taller", ignoreCase = true) ||
                    modGuardada.contains("Local", ignoreCase = true) -> 1
            modGuardada.contains("Domicilio", ignoreCase = true) -> 0
            else -> MODALIDADES_HABILIO.indexOfFirst {
                it.first.equals(modGuardada, ignoreCase = true)
            }.coerceAtLeast(0)
        }
        mutableStateOf(idx)
    }

    var nuevaImagenUri by remember { mutableStateOf<Uri?>(null) }
    var guardando by remember { mutableStateOf(false) }
    var mensajeError by remember { mutableStateOf<String?>(null) }

    // Acta #6/#7/#1: datos profesionales editables
    var aniosExp by remember(usuarioActual) { mutableStateOf(borrador.leer("anios") ?: if (usuarioActual.aniosExperiencia > 0) usuarioActual.aniosExperiencia.toString() else "") }
    var dispSel by remember(usuarioActual) {
        mutableStateOf(usuarioActual.disponibilidad.split(",").map { it.trim() }.filter { it.isNotBlank() }.toSet())
    }
    var precioTxt by remember(usuarioActual) { mutableStateOf(borrador.leer("precio") ?: if (usuarioActual.precioDesde > 0) usuarioActual.precioDesde.toInt().toString() else "") }
    var mujeresOpt by remember(usuarioActual) { mutableStateOf(usuarioActual.visibleTalentoMujeres) }

    LaunchedEffect(nombre, telefono, zona, direccion, nombreTaller, especialidad, aniosExp, precioTxt) {
        val cambio = nombre != usuarioActual.nombre || telefono != usuarioActual.telefono || zona != usuarioActual.zona ||
            direccion != usuarioActual.direccion || nombreTaller != usuarioActual.nombreTaller ||
            especialidad != usuarioActual.especialidad
        if (cambio || aniosExp.isNotBlank() || precioTxt.isNotBlank()) {
            borrador.guardar("nombre" to nombre, "telefono" to telefono, "zona" to zona, "direccion" to direccion,
                "taller" to nombreTaller, "especialidad" to especialidad, "anios" to aniosExp, "precio" to precioTxt)
        }
        if (!cambio && aniosExp.isBlank() && precioTxt.isBlank()) borrador.borrar()
    }

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) nuevaImagenUri = uri
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        EncabezadoAutenticacion(
            titulo = "Editar Mi Perfil",
            subtitulo = "Actualiza tu fotografía o modifica tus datos de contacto y cobertura.",
            textoVolver = "Volver a mi perfil",
            alVolver = alCancelar
        )

        Spacer(modifier = Modifier.height(20.dp))

        SelectorFotoPerfil(
            imagenUriLocal = nuevaImagenUri,
            fotoUrlActual = usuarioActual.fotoUrl,
            titulo = "Fotografía de Perfil Actual",
            subtitulo = "Toca la imagen para subir una foto nueva desde tu dispositivo.",
            iconoPorDefecto = if (esTrabajador) Icons.Default.Handyman else Icons.Default.Person,
            alClickElegirFoto = {
                imagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            }
        )

        Spacer(modifier = Modifier.height(22.dp))

        HabilioTarjeta(paddingInterno = 18.dp) {
            Text(
                text = "👤 DATOS GENERALES",
                fontSize = 13.sp,
                fontWeight = FontWeight.ExtraBold,
                color = HabilioTheme.AmarilloAcentoTexto
            )
            Spacer(modifier = Modifier.height(12.dp))

            HabilioCampoTexto(
                etiqueta = "Nombre Completo",
                valor = nombre,
                alCambiarValor = { nombre = it },
                placeholder = "Tu nombre completo",
                iconoInicial = Icons.Outlined.Person
            )

            Spacer(modifier = Modifier.height(14.dp))

            HabilioCampoTexto(
                etiqueta = "Teléfono móvil (10 dígitos)",
                valor = telefono,
                alCambiarValor = { telefono = it },
                placeholder = "469 123 4567",
                prefijoTexto = "+52"
            )

            Spacer(modifier = Modifier.height(14.dp))

            if (esTrabajador) {
                Text("Zona o Colonia donde trabajas", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(6.dp))
                SelectorZonaHabilio(zonaActual = zona, alElegirZona = { zona = it })
            } else {
                HabilioCampoTexto(
                    etiqueta = "Zona o Colonia",
                    valor = zona,
                    alCambiarValor = { zona = it },
                    placeholder = "Ej. Pénjamo Centro, Santa Ana Pacueco",
                    iconoInicial = Icons.Outlined.Explore
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            HabilioCampoTexto(
                etiqueta = if (esTrabajador) "Dirección o Referencia del Taller" else "Dirección de Domicilio",
                valor = direccion,
                alCambiarValor = { direccion = it },
                placeholder = "Ej. Calle Hidalgo #114, Zona Centro",
                iconoInicial = Icons.Outlined.LocationOn
            )
        }

        if (esTrabajador) {
            Spacer(modifier = Modifier.height(20.dp))

            HabilioTarjeta(paddingInterno = 18.dp) {
                Text(
                    text = "🛠️ DATOS DE TU TALLER Y OFICIO",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = HabilioTheme.AmarilloAcentoTexto
                )
                Spacer(modifier = Modifier.height(12.dp))

                HabilioCampoTexto(
                    etiqueta = "Nombre del Taller / Negocio",
                    etiquetaDerecha = "(Opcional)",
                    valor = nombreTaller,
                    alCambiarValor = { nombreTaller = it },
                    placeholder = "Ej. Taller Méndez",
                    iconoInicial = Icons.Outlined.Storefront
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text("Oficio Principal", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(8.dp))

                // Tarjetas parejas por fila y una sola columna con letra grande (igual que en el registro)
                val columnasOficio = if (LocalDensity.current.fontScale > 1.3f) 1 else 2
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OFICIOS_HABILIO.chunked(columnasOficio).forEach { fila ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(IntrinsicSize.Min),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        fila.forEach { (tituloOficio, subOficio, iconoOficio) ->
                            val seleccionado = oficioSeleccionado == tituloOficio
                            val bloqueado = usuarioActual.rangoEdad == RangoEdad.MENOR_15_17 &&
                                    tituloOficio in OFICIOS_DE_RIESGO
                            Surface(
                                onClick = { oficioSeleccionado = tituloOficio },
                                enabled = !bloqueado,
                                shape = RoundedCornerShape(12.dp),
                                color = if (seleccionado) HabilioTheme.AmarilloPrimario else HabilioTheme.SuperficieInput,
                                border = BorderStroke(
                                    1.dp,
                                    if (seleccionado) HabilioTheme.AmarilloPrimario else HabilioTheme.BordeInput
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .heightIn(min = HabilioTheme.alturaMinima(52.dp))
                                    .alpha(if (bloqueado) 0.5f else 1f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = iconoOficio,
                                        contentDescription = null,
                                        tint = if (seleccionado) HabilioTheme.TextoSobreAmarillo else HabilioTheme.AmarilloAcentoTexto,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = tituloOficio,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = if (seleccionado) HabilioTheme.TextoSobreAmarillo else HabilioTheme.TextoPrimario
                                        )
                                        Text(
                                            text = if (bloqueado) "Desde los 18 años" else subOficio,
                                            fontSize = 10.sp,
                                            color = if (seleccionado) HabilioTheme.TextoSobreAmarillo.copy(alpha = 0.8f) else HabilioTheme.TextoSecundario
                                        )
                                    }
                                }
                            }
                        }
                        if (columnasOficio == 2 && fila.size == 1) Spacer(modifier = Modifier.weight(1f))
                    }
                }
                }

                Spacer(modifier = Modifier.height(14.dp))

                HabilioCampoTexto(
                    etiqueta = "Especialidad o Servicios Específicos",
                    valor = especialidad,
                    alCambiarValor = { especialidad = it },
                    placeholder = "Ej. Instalación de vitropiso, boiler solar..."
                )

                Spacer(modifier = Modifier.height(14.dp))
                Text("Modalidad de Atención", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(8.dp))

                MODALIDADES_HABILIO.forEachIndexed { indice, (tituloMod, descMod) ->
                    val activo = modalidadSeleccionada == indice
                    HabilioTarjeta(
                        radioEsquina = 12.dp,
                        paddingInterno = 12.dp,
                        colorFondo = HabilioTheme.SuperficieInput,
                        colorBorde = if (activo) HabilioTheme.AmarilloPrimario else HabilioTheme.BordeInput,
                        alClick = { modalidadSeleccionada = indice },
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = activo,
                                onClick = { modalidadSeleccionada = indice },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = HabilioTheme.AmarilloAcentoTexto,
                                    unselectedColor = HabilioTheme.TextoSecundario
                                )
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                Text(tituloMod, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text(descMod, fontSize = 11.sp, color = HabilioTheme.TextoSecundario)
                            }
                        }
                    }
                }

                SeccionDatosProfesionales(
                    anios = aniosExp, alCambiarAnios = { aniosExp = it },
                    disponibilidad = dispSel, alCambiarDisponibilidad = { dispSel = it },
                    precioDesde = precioTxt, alCambiarPrecio = { precioTxt = it }
                )
                Spacer(modifier = Modifier.height(14.dp))
                OpcionMujeresEnOficios(activa = mujeresOpt, alCambiar = { mujeresOpt = it })
            }
        }

        if (mensajeError != null) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(mensajeError!!, color = HabilioTheme.RojoUrgente, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        }

        Spacer(modifier = Modifier.height(22.dp))

        BotonPrimarioHabilio(
            texto = if (guardando) "Guardando cambios..." else "Guardar Cambios",
            habilitado = !guardando && nombre.isNotBlank() && telefono.filter { it.isDigit() }.length >= 10,
            alClick = {
                guardando = true
                mensajeError = null
                scope.launch {
                    var urlFotoFinal: String? = usuarioActual.fotoUrl.ifBlank { null }
                    if (nuevaImagenUri != null) {
                        val subida = SupabaseService.subirFotoStorage(contexto, nuevaImagenUri!!)
                        if (subida != null) {
                            urlFotoFinal = subida
                        } else {
                            // Antes la foto se ignoraba en silencio; ahora se avisa y se conserva todo para reintentar
                            guardando = false
                            mensajeError = (EstadoRed.ultimoMensaje ?: "No se pudo subir tu nueva foto.") +
                                " Tus cambios siguen aquí: vuelve a intentarlo."
                            return@launch
                        }
                    }

                    val res = SupabaseService.actualizarPerfil(
                        id = usuarioActual.id,
                        rol = usuarioActual.rol,
                        nombre = nombre,
                        telefono = telefono,
                        zona = zona,
                        direccion = direccion,
                        nombreTaller = nombreTaller,
                        oficio = oficioSeleccionado,
                        especialidad = especialidad,
                        modalidad = MODALIDADES_HABILIO[modalidadSeleccionada].first,
                        fotoUrl = urlFotoFinal,
                        rangoEdadActual = usuarioActual.rangoEdad
                    )
                    res.onSuccess { perfilGuardado ->
                        var perfilFinal = perfilGuardado
                        if (esTrabajador) {
                            val anios = aniosExp.toIntOrNull() ?: 0
                            val disp = dispSel.joinToString(",")
                            val precio = precioTxt.toDoubleOrNull() ?: 0.0
                            val datosOk = SupabaseService.actualizarDatosProfesionales(perfilGuardado.id, anios, disp, precio, mujeresOpt)
                            if (!datosOk) {
                                guardando = false
                                mensajeError = "Se guardaron tus datos generales, pero no los datos profesionales (experiencia, disponibilidad, precio). " +
                                    (EstadoRed.ultimoMensaje ?: "Revisa tu conexión") + " y presiona Guardar de nuevo."
                                return@onSuccess
                            }
                            perfilFinal = perfilGuardado.copy(
                                aniosExperiencia = anios, disponibilidad = disp,
                                precioDesde = precio, visibleTalentoMujeres = mujeresOpt,
                                autorizacionEstado = usuarioActual.autorizacionEstado
                            )
                        }
                        guardando = false
                        borrador.borrar()
                        alGuardarExitoso(perfilFinal)
                    }.onFailure {
                        guardando = false
                        mensajeError = it.message ?: "Error al actualizar el perfil"
                    }
                }
            }
        )

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedButton(
            onClick = { borrador.borrar(); alCancelar() },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = HabilioTheme.alturaMinima(48.dp)),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, HabilioTheme.BordeTarjeta)
        ) {
            Text("Cancelar", color = HabilioTheme.TextoSecundario, fontWeight = FontWeight.SemiBold)
        }
    }
}
