package com.habilio.app

import androidx.compose.foundation.selection.toggleable
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
fun ModalSolicitarServicio(
    trabajador: PerfilUsuario,
    direccionInicial: String,
    claveBorrador: String = "",
    enviando: Boolean,
    mensajeEnvio: String,
    alCerrar: () -> Unit,
    alConfirmarSolicitud: (descripcion: String, ubicacion: String, presupuesto: Double, esUrgente: Boolean, fotos: List<Uri>) -> Unit
) {
    // Acta #10: datos mínimos obligatorios + confirmación explícita antes de enviar + fotos del problema.
    var paso by remember { mutableIntStateOf(0) }
    // Acta #24: borrador local. Lo escrito se conserva si falla la conexión o se cierra la app (las fotos no).
    val contextoModal = androidx.compose.ui.platform.LocalContext.current
    val borrador = remember(claveBorrador) {
        if (claveBorrador.isBlank()) null else BorradorLocal(contextoModal, "solicitud.$claveBorrador")
    }
    val hayBorradorRecuperado = remember(claveBorrador) { borrador?.existe() == true }
    var borradorDescartado by remember { mutableStateOf(false) }
    var descripcion by remember { mutableStateOf(borrador?.leer("descripcion") ?: "") }
    // Privacidad: la zona es visible para la persona especialista; la dirección exacta es opcional y solo se
    // comparte después de que acepte el trabajo.
    var ubicacion by remember { mutableStateOf(borrador?.leer("ubicacion") ?: ubicacionZona(direccionInicial)) }
    var direccionExacta by remember { mutableStateOf(borrador?.leer("direccion") ?: ubicacionExacta(direccionInicial)) }
    var compartirDireccionExacta by remember { mutableStateOf(borrador?.leerBool("compartir", true) ?: true) }
    var presupuestoTexto by remember { mutableStateOf(borrador?.leer("presupuesto") ?: "") }
    var esUrgente by remember { mutableStateOf(borrador?.leerBool("urgente", false) ?: false) }
    var confirmo by remember { mutableStateOf(false) }
    var intentoAvanzar by remember { mutableStateOf(false) }
    val fotos = remember { mutableStateListOf<Uri>() }

    val selectorFotos = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(3)
    ) { uris ->
        uris.forEach { if (fotos.size < 3 && it !in fotos) fotos.add(it) }
    }

    LaunchedEffect(descripcion, ubicacion, direccionExacta, compartirDireccionExacta, presupuestoTexto, esUrgente) {
        if (borrador != null) {
            if (descripcion.isNotBlank()) {
                borrador.guardar(
                    "descripcion" to descripcion, "ubicacion" to ubicacion, "direccion" to direccionExacta,
                    "compartir" to compartirDireccionExacta, "presupuesto" to presupuestoTexto, "urgente" to esUrgente
                )
            } else borrador.borrar()
        }
    }

    val descripcionOk = descripcion.trim().length >= 15
    val ubicacionOk = ubicacion.trim().length >= 3
    val hayErrorEnvio = mensajeEnvio.isNotBlank() && !enviando &&
        !mensajeEnvio.startsWith("Subiendo") && !mensajeEnvio.startsWith("Enviando")
    val montoIngresado = presupuestoTexto.replace(",", "").toDoubleOrNull()
    val montoOk = presupuestoTexto.isBlank() || (montoIngresado != null && montoIngresado > 0)
    val formularioValido = descripcionOk && ubicacionOk && montoOk
    val montoFinal = if (presupuestoTexto.isBlank()) 0.0 else (montoIngresado ?: 0.0)

    Dialog(
        onDismissRequest = { if (!enviando) alCerrar() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        HabilioTarjeta(
            radioEsquina = 24.dp,
            paddingInterno = 20.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp)
                .verticalScroll(rememberScrollState())
        ) {
            EncabezadoModal(
                titulo = if (paso == 0) "Solicitar servicio" else "Revisa tu solicitud",
                icono = Icons.Default.Handshake,
                alCerrar = { if (!enviando) alCerrar() }
            )

            // Indicador de pasos (1 de 2 / 2 de 2)
            Text(
                "Paso ${paso + 1} de 2",
                fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = HabilioTheme.AmarilloAcentoTexto
            )
            LinearProgressIndicator(
                progress = { (paso + 1) / 2f },
                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                color = HabilioTheme.AmarilloPrimario,
                trackColor = HabilioTheme.BordeTarjeta
            )

            HabilioTarjeta(colorFondo = HabilioTheme.SuperficieInput, radioEsquina = 14.dp) {
                Text("Persona especialista", fontSize = 12.sp, color = HabilioTheme.TextoSecundario)
                Text(trabajador.nombre, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = HabilioTheme.TextoPrimario)
                Text(trabajador.oficio, fontSize = 13.sp, color = HabilioTheme.TextoSecundario)
            }

            Spacer(Modifier.height(16.dp))

            if (paso == 0) {
                if (hayBorradorRecuperado && !borradorDescartado) {
                    HabilioTarjeta(colorFondo = HabilioTheme.FondoAzulSuave, radioEsquina = 12.dp, paddingInterno = 10.dp) {
                        Text("Recuperamos tu borrador. Las fotos hay que agregarlas de nuevo.", fontSize = 12.sp, color = HabilioTheme.TextoPrimario)
                        TextButton(onClick = {
                            borradorDescartado = true
                            descripcion = ""; presupuestoTexto = ""; esUrgente = false
                            ubicacion = ubicacionZona(direccionInicial); direccionExacta = ubicacionExacta(direccionInicial)
                            borrador?.borrar()
                        }) { Text("Descartar borrador", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = HabilioTheme.AzulVerificado) }
                    }
                    Spacer(Modifier.height(10.dp))
                }
                Text("Describe qué necesitas *", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Text("Ejemplo: \"Gotea la llave de la cocina y el piso se está mojando\"", fontSize = 12.sp, color = HabilioTheme.TextoSecundario)
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(
                    value = descripcion, onValueChange = { descripcion = it },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 96.dp),
                    maxLines = 4, shape = RoundedCornerShape(12.dp), colors = coloresInputHabilio(),
                    isError = intentoAvanzar && !descripcionOk,
                    supportingText = {
                        Text(
                            if (intentoAvanzar && !descripcionOk) "Escribe al menos 15 caracteres (${descripcion.trim().length}/15)"
                            else "${descripcion.trim().length} caracteres"
                        )
                    }
                )

                AvisoDatosDeContacto(descripcion)
                Spacer(Modifier.height(8.dp))
                Text("¿En qué zona se hará el trabajo? *", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Text("Solo esta zona general es visible para la persona especialista antes de que acepte.", fontSize = 12.sp, color = HabilioTheme.TextoSecundario)
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(
                    value = ubicacion, onValueChange = { ubicacion = it.replace(SEPARADOR_UBICACION, "") },
                    modifier = Modifier.fillMaxWidth(), singleLine = true,
                    shape = RoundedCornerShape(12.dp), colors = coloresInputHabilio(),
                    placeholder = { Text("Colonia o zona (sin calle ni número)") },
                    isError = intentoAvanzar && !ubicacionOk,
                    supportingText = { if (intentoAvanzar && !ubicacionOk) Text("Indica la colonia o zona") }
                )

                Spacer(Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().heightIn(min = HabilioTheme.alturaMinima(48.dp)).clickable { compartirDireccionExacta = !compartirDireccionExacta }
                ) {
                    Switch(checked = compartirDireccionExacta, onCheckedChange = { compartirDireccionExacta = it })
                    Spacer(Modifier.width(10.dp))
                    Text("Compartir mi dirección exacta, solo cuando la persona especialista acepte el trabajo", fontSize = 13.sp, modifier = Modifier.weight(1f))
                }
                if (compartirDireccionExacta) {
                    OutlinedTextField(
                        value = direccionExacta, onValueChange = { direccionExacta = it.replace(SEPARADOR_UBICACION, "") },
                        modifier = Modifier.fillMaxWidth(), singleLine = true,
                        shape = RoundedCornerShape(12.dp), colors = coloresInputHabilio(),
                        placeholder = { Text("Calle y número (opcional)") }
                    )
                } else {
                    Text("No se compartirá tu dirección exacta; podrán acordarla por otro medio si lo decides.", fontSize = 12.sp, color = HabilioTheme.TextoSecundario)
                }

                Spacer(Modifier.height(8.dp))
                Text("Presupuesto orientativo (opcional)", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Text("Es solo una referencia, no es un cobro. Habilio no procesa pagos: el monto final y la forma de pago se acuerdan directamente con la persona especialista.", fontSize = 12.sp, color = HabilioTheme.TextoSecundario)
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(
                    value = presupuestoTexto, onValueChange = { presupuestoTexto = it },
                    prefix = { Text("$", color = HabilioTheme.AmarilloAcentoTexto, fontWeight = FontWeight.ExtraBold) },
                    suffix = { Text("MXN", color = HabilioTheme.AmarilloAcentoTexto, fontWeight = FontWeight.Bold) },
                    placeholder = { Text("Por cotizar") },
                    singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = coloresInputHabilio(),
                    isError = !montoOk,
                    supportingText = { if (!montoOk) Text("Escribe un monto mayor a cero o déjalo vacío") }
                )

                Spacer(Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().heightIn(min = HabilioTheme.alturaMinima(48.dp))
                        .toggleable(value = esUrgente, onValueChange = { esUrgente = it }, role = Role.Checkbox)
                ) {
                    Checkbox(checked = esUrgente, onCheckedChange = null,
                        colors = CheckboxDefaults.colors(checkedColor = HabilioTheme.AmarilloPrimario, checkmarkColor = HabilioTheme.TextoSobreAmarillo))
                    Text("Es urgente (hoy o mañana)", fontSize = 14.sp, color = HabilioTheme.TextoPrimario)
                }

                Spacer(Modifier.height(8.dp))
                Text("Fotos del problema (opcional, máx. 3)", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Text("Ayudan a que la persona especialista entienda el trabajo antes de aceptar.", fontSize = 12.sp, color = HabilioTheme.TextoSecundario)
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
                    fotos.forEachIndexed { i, uri ->
                        Box {
                            AsyncImage(
                                model = uri, contentDescription = "Foto ${i + 1} del problema",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.size(84.dp).clip(RoundedCornerShape(12.dp))
                            )
                            IconButton(
                                onClick = { fotos.remove(uri) },
                                modifier = Modifier.align(Alignment.TopEnd).size(28.dp)
                                    .background(HabilioTheme.SuperficieTarjeta, CircleShape)
                            ) { Icon(Icons.Default.Close, contentDescription = "Quitar foto ${i + 1}", modifier = Modifier.size(16.dp)) }
                        }
                    }
                    if (fotos.size < 3) {
                        OutlinedButton(
                            onClick = { selectorFotos.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                            modifier = Modifier.size(84.dp), shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.AddAPhoto, contentDescription = null, tint = HabilioTheme.AmarilloAcentoTexto)
                                Text("Agregar", fontSize = 11.sp, color = HabilioTheme.TextoPrimario)
                            }
                        }
                    }
                }

                Spacer(Modifier.height(20.dp))
                BotonPrimarioHabilio(
                    texto = "Revisar solicitud",
                    mostrarFlecha = true,
                    alClick = { intentoAvanzar = true; if (formularioValido) paso = 1 }
                )
            } else {
                // ---------- Paso 2: resumen y confirmación explícita ----------
                @Composable
                fun Dato(titulo: String, valor: String) {
                    Column(modifier = Modifier.padding(vertical = 4.dp)) {
                        Text(titulo, fontSize = 12.sp, color = HabilioTheme.TextoSecundario)
                        Text(valor, fontSize = 14.sp, color = HabilioTheme.TextoPrimario, lineHeight = 19.sp)
                    }
                }
                Dato("Trabajo solicitado", descripcion.trim())
                Dato("Zona", ubicacion.trim())
                if (compartirDireccionExacta && direccionExacta.isNotBlank()) Dato("Dirección exacta", "${direccionExacta.trim()} (se comparte al aceptar el trabajo)")
                Dato("Presupuesto orientativo", if (montoFinal > 0) "$${"%.2f".format(montoFinal)} MXN" else "Por cotizar")
                Dato("Urgencia", if (esUrgente) "Urgente" else "Sin urgencia")
                Dato("Fotos adjuntas", if (fotos.isEmpty()) "Ninguna" else "${fotos.size}")
                Text(
                    "Seguridad: avisa a una persona de confianza que recibirás a alguien en tu domicilio y, si puedes, procura no estar sola. Puedes reportar o bloquear desde el servicio.",
                    fontSize = 12.sp, lineHeight = 16.sp, color = HabilioTheme.TextoSecundario,
                    modifier = Modifier.padding(top = 6.dp)
                )

                Spacer(Modifier.height(10.dp))
                Row(
                    verticalAlignment = Alignment.Top,
                    modifier = Modifier.fillMaxWidth().heightIn(min = HabilioTheme.alturaMinima(48.dp))
                        .toggleable(value = confirmo, enabled = !enviando, onValueChange = { confirmo = it }, role = Role.Checkbox)
                ) {
                    Checkbox(checked = confirmo, onCheckedChange = null, enabled = !enviando,
                        colors = CheckboxDefaults.colors(checkedColor = HabilioTheme.AmarilloPrimario, checkmarkColor = HabilioTheme.TextoSobreAmarillo))
                    Text(
                        "Confirmo que los datos son correctos y acepto compartir mi dirección y teléfono con ${trabajador.nombre} para coordinar el servicio.",
                        fontSize = 13.sp, color = HabilioTheme.TextoPrimario, lineHeight = 18.sp,
                        modifier = Modifier.padding(top = 12.dp)
                    )
                }

                if (!EstadoRed.hayRed) {
                    Spacer(Modifier.height(8.dp))
                    Text("Sin conexión: tu solicitud queda guardada en este teléfono. Podrás enviarla cuando vuelva la señal.",
                        fontSize = 13.sp, color = HabilioTheme.RojoUrgente)
                }
                if (mensajeEnvio.isNotBlank()) {
                    Spacer(Modifier.height(8.dp))
                    Text(mensajeEnvio, fontSize = 13.sp,
                        color = if (hayErrorEnvio) HabilioTheme.RojoUrgente else HabilioTheme.TextoSecundario)
                }

                Spacer(Modifier.height(14.dp))
                BotonPrimarioHabilio(
                    texto = if (enviando) "Enviando…" else if (hayErrorEnvio) "Reintentar envío" else "Enviar solicitud",
                    habilitado = confirmo && !enviando,
                    alClick = { alConfirmarSolicitud(descripcion.trim(), armarUbicacionSolicitud(ubicacion, direccionExacta, compartirDireccionExacta), montoFinal, esUrgente, fotos.toList()) }
                )
                TextButton(onClick = { paso = 0 }, enabled = !enviando, modifier = Modifier.fillMaxWidth()) {
                    Text("Corregir datos", color = HabilioTheme.TextoSecundario)
                }
            }
        }
    }
}

@Composable
fun ModalSolicitudConfirmada(
    solicitud: SolicitudItem,
    alAceptarYVerEstatus: () -> Unit,
    alVolverAlInicio: () -> Unit
) {
    val folioCorto = remember(solicitud.id) {
        val digitos = solicitud.id.filter { it.isDigit() }.take(4).padEnd(4, '8')
        "#HAB-$digitos"
    }

    Dialog(
        onDismissRequest = alVolverAlInicio,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        HabilioTarjeta(
            radioEsquina = 24.dp,
            paddingInterno = 22.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    IconButton(
                        onClick = alVolverAlInicio,
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(HabilioTheme.SuperficieElevada)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cerrar",
                            tint = HabilioTheme.TextoSecundario,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .size(78.dp)
                        .clip(CircleShape)
                        .background(HabilioTheme.FondoVerdeSuave)
                        .border(2.dp, HabilioTheme.VerdeExito.copy(alpha = 0.5f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(HabilioTheme.VerdeExito),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Confirmado",
                            tint = HabilioTheme.TextoSobreVerde,
                            modifier = Modifier.size(30.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                Text(
                    text = "¡Solicitud Registrada!",
                    fontSize = 23.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = HabilioTheme.TextoPrimario,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Tu solicitud ha sido enviada a ${solicitud.nombreTrabajador}. El seguimiento del trabajo y tu calificación quedan registrados en Habilio.",
                    fontSize = 14.sp,
                    color = HabilioTheme.TextoSecundario,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                )

                Spacer(modifier = Modifier.height(20.dp))

                HabilioTarjeta(colorFondo = HabilioTheme.SuperficieInput) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            AvatarUsuario(
                                tamano = 42.dp,
                                esCircular = true,
                                icono = Icons.Default.Handyman,
                                colorIcono = HabilioTheme.AmarilloAcentoTexto
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(solicitud.nombreTrabajador, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                Text(solicitud.oficioTrabajador, fontSize = 12.sp, color = HabilioTheme.TextoSecundario)
                            }
                        }
                        HabilioBadge(
                            texto = "Activo",
                            colorTexto = HabilioTheme.VerdeExito,
                            colorFondo = HabilioTheme.FondoVerdeSuave
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = HabilioTheme.BordeTarjeta)
                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("FOLIO", fontSize = 11.sp, color = HabilioTheme.TextoSecundario, fontWeight = FontWeight.Bold)
                            Text(
                                text = folioCorto,
                                fontSize = 15.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("MONTO ORIENTATIVO", fontSize = 11.sp, color = HabilioTheme.TextoSecundario, fontWeight = FontWeight.Bold)
                            Text(
                                text = if (solicitud.monto > 0) "$${String.format("%.2f", solicitud.monto)} MXN" else "Por cotizar",
                                maxLines = 1,
                                softWrap = false,
                                fontSize = 16.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.ExtraBold,
                                color = HabilioTheme.AmarilloAcentoTexto
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = HabilioTheme.BordeTarjeta)
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Handshake,
                            contentDescription = null,
                            tint = HabilioTheme.VerdeExito,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Habilio no procesa pagos. El pago se acuerda y se realiza directamente con la persona especialista; el monto mostrado es solo orientativo.",
                            fontSize = 12.sp,
                            color = HabilioTheme.TextoSecundario
                        )
                    }
                }

                Spacer(modifier = Modifier.height(22.dp))

                BotonPrimarioHabilio(
                    texto = "Aceptar y ver estatus",
                    alClick = alAceptarYVerEstatus,
                    mostrarFlecha = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    onClick = alVolverAlInicio,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = HabilioTheme.alturaMinima(48.dp)),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = HabilioTheme.SuperficieElevada,
                        contentColor = HabilioTheme.TextoPrimario
                    )
                ) {
                    Text("Volver al Inicio", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                }
            }
        }
    }
}

@Composable
fun ModalEvaluarServicio(
    nombreClienteInicial: String,
    alCerrar: () -> Unit,
    alPublicarResena: (estrellas: Int, autor: String, comentario: String) -> Unit
) {
    var estrellasSeleccionadas by remember { mutableStateOf(5) }
    var autor by remember { mutableStateOf(nombreClienteInicial) }
    var comentario by remember { mutableStateOf("") }
    var publicando by remember { mutableStateOf(false) }

    val etiquetaEstrellas = when (estrellasSeleccionadas) {
        5 -> "5 Estrellas · Excelente trabajo comprobado"
        4 -> "4 Estrellas · Muy buen trabajo"
        3 -> "3 Estrellas · Trabajo regular"
        2 -> "2 Estrellas · Tuvo detalles por mejorar"
        else -> "1 Estrella · Mal servicio"
    }

    Dialog(
        onDismissRequest = alCerrar,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        HabilioTarjeta(
            radioEsquina = 24.dp,
            paddingInterno = 20.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp)
                .verticalScroll(rememberScrollState())
        ) {
            EncabezadoModal(
                titulo = "Evaluar Servicio",
                icono = Icons.Default.Star,
                alCerrar = alCerrar
            )

            Text(
                text = "Selecciona las estrellas para calificar el trabajo:",
                fontSize = 13.sp,
                color = HabilioTheme.TextoSecundario,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                for (i in 1..5) {
                    IconButton(onClick = { estrellasSeleccionadas = i }, modifier = Modifier.size(48.dp)) {
                        Icon(
                            imageVector = if (i <= estrellasSeleccionadas) Icons.Default.Star else Icons.Outlined.StarBorder,
                            contentDescription = "$i estrellas",
                            tint = HabilioTheme.AmarilloAcentoTexto,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = etiquetaEstrellas,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = HabilioTheme.AmarilloAcentoTexto,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(18.dp))

            Text("Tu nombre:", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
                value = autor,
                onValueChange = { autor = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = coloresInputHabilio()
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text("Recomendación o comentario del servicio:", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
                value = comentario,
                onValueChange = { comentario = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 100.dp),
                maxLines = 4,
                shape = RoundedCornerShape(12.dp),
                colors = coloresInputHabilio()
            )

            Spacer(modifier = Modifier.height(22.dp))

            Button(
                onClick = {
                    publicando = true
                    alPublicarResena(estrellasSeleccionadas, autor, comentario)
                },
                enabled = !publicando && comentario.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = HabilioTheme.alturaMinima(52.dp)),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = HabilioTheme.VerdeExito,
                    contentColor = HabilioTheme.TextoSobreVerde
                )
            ) {
                Icon(Icons.Default.CheckCircleOutline, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (publicando) "Publicando..." else "Publicar reseña",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }
    }
}

/** Ventana con la sección anterior, para que quien presta el servicio la abra desde su lista de solicitudes. */
@Composable
fun DialogoAcuerdos(solicitud: SolicitudItem, rol: String, alCerrar: () -> Unit) {
    Dialog(onDismissRequest = alCerrar, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        HabilioTarjeta(
            radioEsquina = 24.dp, paddingInterno = 18.dp,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp).heightIn(max = 640.dp).verticalScroll(rememberScrollState())
        ) {
            EncabezadoModal(titulo = "Presupuesto e historial", icono = Icons.Default.Timeline, alCerrar = alCerrar)
            SeccionAcuerdosYHistorial(solicitud, rol)
        }
    }
}

/**
 * Acta #13: cancelación con motivo obligatorio, aviso claro del plazo y notificación a la otra parte.
 * Fuera del plazo libre se pide un comentario y se recomienda reportar un desacuerdo.
 */
@Composable
fun DialogoCancelacion(
    solicitud: SolicitudItem,
    rol: String,
    alCerrar: () -> Unit,
    alConfirmar: (motivo: String, comentario: String) -> Unit
) {
    val scope = rememberCoroutineScope()
    val motivos = if (rol == "cliente") SupabaseService.MOTIVOS_CANCELACION_CLIENTE else SupabaseService.MOTIVOS_CANCELACION_TRABAJADOR
    var motivo by remember { mutableStateOf("") }
    var comentario by remember { mutableStateOf("") }
    var dentroDePlazo by remember { mutableStateOf(true) }
    var enviando by remember { mutableStateOf(false) }
    LaunchedEffect(solicitud.id) {
        dentroDePlazo = SupabaseService.dentroDePlazoCancelacion(solicitud, SupabaseService.obtenerEventos(solicitud.id))
    }
    val comentarioObligatorio = !dentroDePlazo || motivo == "Otro motivo"
    val valido = motivo.isNotBlank() && (!comentarioObligatorio || comentario.trim().length >= 10)

    Dialog(onDismissRequest = alCerrar, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        HabilioTarjeta(
            radioEsquina = 24.dp, paddingInterno = 20.dp,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).verticalScroll(rememberScrollState())
        ) {
            EncabezadoModal(titulo = "Cancelar servicio", icono = Icons.Default.Cancel, alCerrar = alCerrar)
            HabilioTarjeta(
                colorFondo = if (dentroDePlazo) HabilioTheme.FondoAzulSuave else HabilioTheme.FondoRojoSuave,
                radioEsquina = 12.dp, paddingInterno = 12.dp
            ) {
                Text(
                    if (dentroDePlazo) "Puedes cancelar sin problema. La otra persona recibirá un aviso con tu motivo."
                    else "Pasaron más de ${SupabaseService.PLAZO_CANCELACION_LIBRE_HORAS} horas desde que se aceptó el servicio. Puedes cancelar, pero quedará registrado en el historial y debes explicar el motivo. Si el problema es con el trabajo, mejor reporta un desacuerdo.",
                    fontSize = 13.sp, lineHeight = 18.sp, color = HabilioTheme.TextoPrimario
                )
            }
            Spacer(Modifier.height(12.dp))
            Text("¿Por qué cancelas?", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = HabilioTheme.TextoPrimario)
            motivos.forEach { m -> OpcionRadio(m, motivo == m) { motivo = m } }
            OutlinedTextField(
                comentario, { comentario = it },
                label = { Text(if (comentarioObligatorio) "Explica brevemente (obligatorio)" else "Comentario (opcional)") },
                minLines = 2, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = coloresInputHabilio()
            )
            Spacer(Modifier.height(14.dp))
            BotonPrimarioHabilio(
                texto = if (enviando) "Cancelando…" else "Confirmar cancelación",
                habilitado = valido && !enviando,
                alClick = { enviando = true; alConfirmar(motivo, comentario) }
            )
            TextButton(onClick = alCerrar, modifier = Modifier.fillMaxWidth()) {
                Text("No cancelar", color = HabilioTheme.TextoSecundario)
            }
        }
    }
}

/** Acta #14: ventana para reportar una reseña falsa o abusiva, con los criterios de moderación a la vista. */
@Composable
fun DialogoReportarResena(alCerrar: () -> Unit, alEnviar: (criterio: String, detalle: String) -> Unit) {
    var criterio by remember { mutableStateOf("") }
    var detalle by remember { mutableStateOf("") }
    Dialog(onDismissRequest = alCerrar, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        HabilioTarjeta(
            radioEsquina = 24.dp, paddingInterno = 20.dp,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).verticalScroll(rememberScrollState())
        ) {
            EncabezadoModal(titulo = "Reportar reseña", icono = Icons.Default.Flag, alCerrar = alCerrar)
            Text(
                "Revisamos cada reporte con estos criterios. Si tres personas distintas reportan la misma reseña, se oculta mientras el equipo la revisa.",
                fontSize = 12.sp, color = HabilioTheme.TextoSecundario, lineHeight = 17.sp
            )
            Spacer(Modifier.height(8.dp))
            SupabaseService.CRITERIOS_MODERACION.forEach { c -> OpcionRadio(c, criterio == c) { criterio = c } }
            OutlinedTextField(detalle, { detalle = it }, label = { Text("Detalle (opcional)") }, minLines = 2,
                modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = coloresInputHabilio())
            Spacer(Modifier.height(12.dp))
            BotonPrimarioHabilio("Enviar reporte", habilitado = criterio.isNotBlank(), alClick = { alEnviar(criterio, detalle) })
        }
    }
}

/** Avisos internos (Acta #13): lista de movimientos nuevos dirigidos a la persona que usa la app. */
@Composable
fun DialogoAvisos(avisos: List<EventoSolicitud>, alCerrar: () -> Unit) {
    Dialog(onDismissRequest = alCerrar) {
        HabilioTarjeta(radioEsquina = 20.dp, paddingInterno = 18.dp, modifier = Modifier.heightIn(max = 560.dp).verticalScroll(rememberScrollState())) {
            EncabezadoModal(titulo = "Avisos", icono = Icons.Default.Notifications, alCerrar = alCerrar)
            if (avisos.isEmpty()) Text("No tienes avisos nuevos.", fontSize = 14.sp, color = HabilioTheme.TextoSecundario)
            avisos.forEach {
                Column(modifier = Modifier.padding(vertical = 6.dp)) {
                    Text(textoEvento(it), fontSize = 14.sp, color = HabilioTheme.TextoPrimario)
                    Text(formatearFechaEvento(it.creadoEn), fontSize = 11.sp, color = HabilioTheme.TextoSecundario)
                }
            }
        }
    }
}
