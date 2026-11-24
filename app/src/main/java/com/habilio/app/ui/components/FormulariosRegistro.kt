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

/** Selector de zona predefinida (sin GPS, RF-03 / RF-06). */
@Composable
fun SelectorZonaHabilio(
    zonaActual: String,
    alElegirZona: (String) -> Unit
) {
    ZONAS_HABILIO.chunked(2).forEach { fila ->
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            fila.forEach { zona ->
                val seleccionada = zonaActual.equals(zona, ignoreCase = true)
                Surface(
                    onClick = { alElegirZona(zona) },
                    shape = RoundedCornerShape(12.dp),
                    color = if (seleccionada) HabilioTheme.FondoAzulSuave else HabilioTheme.SuperficieInput,
                    border = BorderStroke(
                        if (seleccionada) 2.dp else 1.dp,
                        if (seleccionada) HabilioTheme.AzulIcono else HabilioTheme.BordeInput
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .padding(vertical = 4.dp)
                        .heightIn(min = HabilioTheme.alturaMinima(52.dp))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (seleccionada) Icons.Default.CheckCircle else Icons.Outlined.LocationOn,
                            contentDescription = null,
                            tint = if (seleccionada) HabilioTheme.AzulIcono else HabilioTheme.TextoSecundario,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = zona,
                            fontSize = 13.sp,
                            fontWeight = if (seleccionada) FontWeight.Bold else FontWeight.Medium,
                            color = if (seleccionada) HabilioTheme.AzulVerificado else HabilioTheme.TextoPrimario
                        )
                    }
                }
            }
            if (fila.size == 1) Spacer(modifier = Modifier.weight(1f))
        }
    }
}

/** RF-05: declaración de edad del trabajador (primer paso del registro guiado). */
@Composable
fun SelectorEdadTrabajador(
    rangoEdad: String,
    alCambiarRango: (String) -> Unit,
    autorizacionTutor: Boolean,
    alCambiarAutorizacion: (Boolean) -> Unit
) {
    val opciones = listOf(
        Triple(RangoEdad.MAYOR, "Tengo 18 años o más", "Puedes elegir cualquier oficio."),
        Triple(RangoEdad.MENOR_15_17, "Tengo entre 15 y 17 años", "Solo oficios sin riesgo y con autorización de tu madre, padre o tutor."),
        Triple(RangoEdad.MENOR_15, "Tengo menos de 15 años", "Por ley todavía no puedes trabajar ni registrarte.")
    )

    Text("¿Qué edad tienes?", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    Spacer(modifier = Modifier.height(8.dp))

    opciones.forEach { (clave, titulo, descripcion) ->
        val activo = rangoEdad == clave
        HabilioTarjeta(
            radioEsquina = 12.dp,
            paddingInterno = 12.dp,
            colorFondo = HabilioTheme.SuperficieInput,
            colorBorde = if (activo) HabilioTheme.AmarilloPrimario else HabilioTheme.BordeInput,
            alClick = { alCambiarRango(clave) },
            modifier = Modifier.padding(vertical = 4.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(
                    selected = activo,
                    onClick = { alCambiarRango(clave) },
                    colors = RadioButtonDefaults.colors(
                        selectedColor = HabilioTheme.AmarilloAcentoTexto,
                        unselectedColor = HabilioTheme.TextoSecundario
                    )
                )
                Spacer(modifier = Modifier.width(6.dp))
                Column {
                    Text(titulo, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = HabilioTheme.TextoPrimario)
                    Text(descripcion, fontSize = 12.sp, color = HabilioTheme.TextoSecundario, lineHeight = 16.sp)
                }
            }
        }
    }

    when (rangoEdad) {
        RangoEdad.MENOR_15 -> {
            Spacer(modifier = Modifier.height(8.dp))
            HabilioTarjeta(
                colorFondo = HabilioTheme.FondoRojoSuave,
                colorBorde = HabilioTheme.RojoUrgente.copy(alpha = 0.5f),
                radioEsquina = 12.dp,
                paddingInterno = 12.dp
            ) {
                Text(
                    text = "Habilio no permite crear cuentas a menores de 15 años. Cuando cumplas 15 podrás registrarte con la autorización de tu madre, padre o tutor.",
                    fontSize = 13.sp,
                    color = HabilioTheme.RojoUrgente,
                    fontWeight = FontWeight.Medium,
                    lineHeight = 18.sp
                )
            }
        }
        RangoEdad.MENOR_15_17 -> {
            Spacer(modifier = Modifier.height(8.dp))
            HabilioTarjeta(
                colorFondo = HabilioTheme.FondoAmarilloSuave,
                colorBorde = if (autorizacionTutor) HabilioTheme.AmarilloPrimario else HabilioTheme.BordeInput,
                radioEsquina = 12.dp,
                paddingInterno = 12.dp,
                alClick = { alCambiarAutorizacion(!autorizacionTutor) }
            ) {
                Row(verticalAlignment = Alignment.Top) {
                    Checkbox(
                        checked = autorizacionTutor,
                        onCheckedChange = null,
                        colors = CheckboxDefaults.colors(
                            checkedColor = HabilioTheme.AmarilloPrimario,
                            uncheckedColor = HabilioTheme.TextoSecundario,
                            checkmarkColor = HabilioTheme.TextoSobreAmarillo
                        )
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Mi madre, padre o tutor autoriza que trabaje",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = HabilioTheme.TextoPrimario
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Albañilería, Herrería y Electricidad quedan bloqueadas hasta los 18 años porque son oficios de riesgo.",
                            fontSize = 12.sp,
                            color = HabilioTheme.TextoSecundario,
                            lineHeight = 17.sp
                        )
                    }
                }
            }
        }
    }
    Spacer(modifier = Modifier.height(16.dp))
}

@Composable
fun SelectorFotoPerfil(
    imagenUriLocal: Uri?,
    fotoUrlActual: String?,
    titulo: String,
    subtitulo: String,
    iconoPorDefecto: ImageVector = Icons.Default.Person,
    alClickElegirFoto: () -> Unit
) {
    HabilioTarjeta(paddingInterno = 18.dp, alClick = alClickElegirFoto) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(contentAlignment = Alignment.BottomEnd) {
                when {
                    imagenUriLocal != null -> {
                        AsyncImage(
                            model = imagenUriLocal,
                            contentDescription = "Nueva foto de perfil",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(78.dp)
                                .clip(CircleShape)
                                .background(HabilioTheme.SuperficieElevada)
                        )
                    }
                    !fotoUrlActual.isNullOrBlank() -> {
                        AsyncImage(
                            model = fotoUrlActual,
                            contentDescription = "Foto de perfil actual",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(78.dp)
                                .clip(CircleShape)
                                .background(HabilioTheme.SuperficieElevada)
                        )
                    }
                    else -> {
                        AvatarUsuario(
                            tamano = 78.dp,
                            esCircular = true,
                            icono = iconoPorDefecto,
                            colorIcono = HabilioTheme.AmarilloAcentoTexto
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(HabilioTheme.AmarilloPrimario)
                        .border(2.dp, HabilioTheme.SuperficieTarjeta, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PhotoCamera,
                        contentDescription = "Cambiar fotografía",
                        tint = HabilioTheme.TextoSobreAmarillo,
                        modifier = Modifier.size(15.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(titulo, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitulo,
                fontSize = 13.sp,
                color = HabilioTheme.TextoSecundario,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun CheckDeclaracionEdadYPrivacidad(
    aceptado: Boolean,
    alCambiarAceptado: (Boolean) -> Unit,
    esTrabajador: Boolean
) {
    var mostrarAvisoCompleto by remember { mutableStateOf(false) }

    val detalleRol = if (esTrabajador) {
        "Tus datos de contacto, ubicación de taller y fotografías de portafolio serán visibles para clientes dentro de Habilio."
    } else {
        "Tus datos de contacto se utilizarán únicamente dentro de Habilio para coordinar solicitudes de servicio en tu comunidad."
    }

    HabilioTarjeta(
        colorFondo = HabilioTheme.SuperficieInput,
        colorBorde = if (aceptado) HabilioTheme.AmarilloPrimario else HabilioTheme.BordeInput,
        radioEsquina = 12.dp,
        paddingInterno = 12.dp,
        alClick = { alCambiarAceptado(!aceptado) }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            Checkbox(
                checked = aceptado,
                onCheckedChange = null,
                colors = CheckboxDefaults.colors(
                    checkedColor = HabilioTheme.AmarilloPrimario,
                    uncheckedColor = HabilioTheme.TextoSecundario,
                    checkmarkColor = HabilioTheme.TextoSobreAmarillo
                )
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (esTrabajador) "Aviso de privacidad y veracidad de tu edad" else "Declaración de edad y aceptación del aviso de privacidad",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = HabilioTheme.TextoPrimario,
                    lineHeight = 18.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (esTrabajador) {
                        "Acepto el tratamiento de mis datos personales y declaro, bajo protesta de decir verdad, que la edad que indiqué al inicio del registro es correcta. $detalleRol"
                    } else {
                        "Declaro ser mayor de 18 años y acepto el tratamiento de mis datos personales. $detalleRol"
                    },
                    fontSize = 11.sp,
                    color = HabilioTheme.TextoSecundario,
                    lineHeight = 16.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Leer Aviso de Privacidad completo >",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = HabilioTheme.AmarilloAcentoTexto,
                    modifier = Modifier
                        .clickable(role = Role.Button) { mostrarAvisoCompleto = true }
                        .padding(vertical = 2.dp)
                )
            }
        }
    }

    if (mostrarAvisoCompleto) {
        ModalAvisoPrivacidad(
            esTrabajador = esTrabajador,
            alAceptar = {
                alCambiarAceptado(true)
                mostrarAvisoCompleto = false
            },
            alCerrar = { mostrarAvisoCompleto = false }
        )
    }
}

@Composable
fun ModalAvisoPrivacidad(
    esTrabajador: Boolean,
    alAceptar: () -> Unit,
    alCerrar: () -> Unit
) {
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
                titulo = "Aviso de Privacidad y Edad",
                icono = Icons.Outlined.VerifiedUser,
                alCerrar = alCerrar
            )

            Text(
                text = "1. Declaración de Mayoría de Edad",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = HabilioTheme.AmarilloAcentoTexto
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = if (esTrabajador) {
                    "Al crear una cuenta de trabajador declaras bajo protesta de decir verdad la edad que indicaste. Con 18 años o más puedes ejercer cualquier oficio. De 15 a 17 años solo puedes ejercer oficios que no sean de riesgo (no Albañilería, Herrería ni Electricidad) y con autorización de tu madre, padre o tutor. Menores de 15 años no pueden registrarse. Guardamos la fecha y hora en que aceptas este aviso."
                } else {
                    "Al crear una cuenta de cliente declaras bajo protesta de decir verdad que tienes al menos 18 años cumplidos y cuentas con capacidad legal para contratar servicios de oficio. Guardamos la fecha y hora en que aceptas este aviso."
                },
                fontSize = 13.sp,
                color = HabilioTheme.TextoSecundario,
                lineHeight = 19.sp
            )

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "2. Uso y Protección de Datos Personales",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = HabilioTheme.AmarilloAcentoTexto
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = if (esTrabajador) {
                    "Recabamos tu nombre, teléfono, correo, zona de cobertura y fotografías de evidencia para mostrar tu perfil profesional a clientes de tu comunidad y gestionar solicitudes de trabajo."
                } else {
                    "Recabamos tu nombre, teléfono, correo y ubicación de servicio únicamente para vincular tus solicitudes con especialistas locales y dar seguimiento a tus trabajos."
                },
                fontSize = 13.sp,
                color = HabilioTheme.TextoSecundario,
                lineHeight = 19.sp
            )

            Spacer(modifier = Modifier.height(20.dp))

            BotonPrimarioHabilio(
                texto = "Aceptar y Continuar",
                alClick = alAceptar
            )
        }
    }
}

@Composable
fun AvisoBorradorRecuperado(alEmpezarDeNuevo: () -> Unit) {
    HabilioTarjeta(colorFondo = HabilioTheme.FondoAzulSuave, radioEsquina = 12.dp, paddingInterno = 12.dp) {
        Text("Recuperamos lo que ya habías escrito.", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = HabilioTheme.TextoPrimario)
        Text("Por seguridad, la contraseña y las casillas de aceptación se vuelven a llenar.", fontSize = 12.sp, color = HabilioTheme.TextoSecundario)
        TextButton(onClick = alEmpezarDeNuevo) { Text("Empezar de nuevo", color = HabilioTheme.AzulVerificado, fontWeight = FontWeight.Bold) }
    }
}

/** Acta #4: datos de la persona adulta responsable. La autorización queda PENDIENTE hasta que Habilio la confirme. */
@Composable
fun CamposAutorizacionTutor(
    nombreTutor: String, alCambiarNombre: (String) -> Unit,
    telefonoTutor: String, alCambiarTelefono: (String) -> Unit,
    parentesco: String, alCambiarParentesco: (String) -> Unit
) {
    Spacer(Modifier.height(10.dp))
    HabilioTarjeta(radioEsquina = 12.dp, paddingInterno = 12.dp, colorFondo = HabilioTheme.SuperficieInput) {
        Text("Datos de tu madre, padre o persona tutora", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = HabilioTheme.TextoPrimario)
        Text(
            "Habilio se comunicará con esa persona para confirmar la autorización. Mientras no esté confirmada, tu perfil no aparecerá en las búsquedas ni recibirá solicitudes.",
            fontSize = 12.sp, color = HabilioTheme.TextoSecundario, lineHeight = 17.sp
        )
        Spacer(Modifier.height(10.dp))
        HabilioCampoTexto(
            etiqueta = "Nombre completo", valor = nombreTutor, alCambiarValor = alCambiarNombre,
            placeholder = "Ej. María Hernández"
        )
        Spacer(Modifier.height(8.dp))
        HabilioCampoTexto(
            etiqueta = "Teléfono (10 dígitos)", valor = telefonoTutor, alCambiarValor = alCambiarTelefono,
            placeholder = "469 123 4567", prefijoTexto = "+52"
        )
        Spacer(Modifier.height(8.dp))
        Text("Parentesco", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("Madre", "Padre", "Persona tutora").forEach { p ->
                FilterChip(
                    selected = parentesco == p, onClick = { alCambiarParentesco(p) },
                    label = { Text(p, fontSize = 13.sp) },
                    modifier = Modifier.heightIn(min = HabilioTheme.alturaMinima(48.dp))
                )
            }
        }
    }
}

/** Acta #6/#7: años de experiencia, disponibilidad y precio orientativo, con ayuda contextual en cada campo. */
@Composable
fun SeccionDatosProfesionales(
    anios: String, alCambiarAnios: (String) -> Unit,
    disponibilidad: Set<String>, alCambiarDisponibilidad: (Set<String>) -> Unit,
    precioDesde: String, alCambiarPrecio: (String) -> Unit
) {
    Spacer(Modifier.height(16.dp))
    Text("¿Cuántos años llevas en este oficio?", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    Text("Cuenta también el tiempo que ayudaste a alguien más. Puedes dejar 0 si apenas empiezas.", fontSize = 12.sp, color = HabilioTheme.TextoSecundario)
    Spacer(Modifier.height(4.dp))
    OutlinedTextField(
        value = anios, onValueChange = { alCambiarAnios(it.filter(Char::isDigit).take(2)) },
        singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        suffix = { Text("años") }, modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp), colors = coloresInputHabilio()
    )

    Spacer(Modifier.height(14.dp))
    Text("¿Cuándo puedes trabajar?", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    Text("Elige todas las que apliquen. Quienes buscan servicio podrán filtrar por esto.", fontSize = 12.sp, color = HabilioTheme.TextoSecundario)
    Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        DISPONIBILIDADES_HABILIO.forEach { d ->
            val sel = d in disponibilidad
            FilterChip(
                selected = sel,
                onClick = { alCambiarDisponibilidad(if (sel) disponibilidad - d else disponibilidad + d) },
                label = { Text(d, fontSize = 13.sp) },
                modifier = Modifier.heightIn(min = HabilioTheme.alturaMinima(48.dp))
            )
        }
    }

    Spacer(Modifier.height(14.dp))
    Text("Precio orientativo: ¿desde cuánto cobras?", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    Text("Es solo una referencia (por ejemplo, tu visita o tu trabajo más sencillo). El precio final siempre se acuerda en cada solicitud.", fontSize = 12.sp, color = HabilioTheme.TextoSecundario, lineHeight = 17.sp)
    Spacer(Modifier.height(4.dp))
    OutlinedTextField(
        value = precioDesde, onValueChange = { alCambiarPrecio(it.filter(Char::isDigit).take(5)) },
        singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        prefix = { Text("$") }, suffix = { Text("MXN") }, placeholder = { Text("Opcional") },
        modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = coloresInputHabilio()
    )
}

/** Acta #7: preguntas guiadas con ejemplos tocables para armar la presentación sin escribir desde cero. */
@Composable
fun GuiaPresentacionProfesional(
    oficio: String,
    queHaces: String, alCambiarQueHaces: (String) -> Unit,
    comoAprendiste: String, alCambiarComoAprendiste: (String) -> Unit,
    queDebenSaber: String, alCambiarQueDebenSaber: (String) -> Unit
) {
    val ejemplosHaces = EJEMPLOS_ESPECIALIDAD[oficio].orEmpty().take(3)
    @Composable
    fun Pregunta(
        titulo: String, ayuda: String, valor: String, alCambiar: (String) -> Unit,
        ejemplos: List<String>, placeholder: String
    ) {
        Text(titulo, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        Text(ayuda, fontSize = 12.sp, color = HabilioTheme.TextoSecundario, lineHeight = 16.sp)
        Spacer(Modifier.height(4.dp))
        OutlinedTextField(
            value = valor, onValueChange = { alCambiar(it.take(160)) },
            placeholder = { Text(placeholder, color = HabilioTheme.TextoPlaceholder, fontSize = 13.sp) },
            minLines = 2, modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp), colors = coloresInputHabilio(),
            supportingText = { Text("${valor.length}/160") }
        )
        if (ejemplos.isNotEmpty()) {
            Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ejemplos.forEach { e ->
                    AssistChip(
                        onClick = { alCambiar(if (valor.isBlank()) e else "$valor. $e") },
                        label = { Text(e, fontSize = 12.sp) },
                        modifier = Modifier.heightIn(min = HabilioTheme.alturaMinima(40.dp))
                    )
                }
            }
        }
        Spacer(Modifier.height(12.dp))
    }

    HabilioTarjeta(colorFondo = HabilioTheme.FondoAzulSuave, radioEsquina = 12.dp, paddingInterno = 12.dp) {
        Text("Una buena presentación responde tres preguntas. No tiene que ser perfecta; toca un ejemplo si no sabes por dónde empezar.",
            fontSize = 12.sp, color = HabilioTheme.TextoPrimario, lineHeight = 17.sp)
    }
    Spacer(Modifier.height(12.dp))
    Pregunta("1. ¿Qué trabajos haces mejor?", "Menciona los trabajos que más te piden.", queHaces, alCambiarQueHaces,
        ejemplosHaces, "Ej. Instalo y reparo llaves, boiler y tinacos")
    Pregunta("2. ¿Cómo aprendiste y cuánto tiempo llevas?", "Experiencia propia, escuela, taller o con familiares.", comoAprendiste, alCambiarComoAprendiste,
        listOf("Aprendí en un taller", "Estudié el oficio", "Trabajo por mi cuenta"), "Ej. Llevo 8 años trabajando por mi cuenta")
    Pregunta("3. ¿Qué debe saber quien te contrata?", "Materiales, tiempos, garantía o cómo cobras.", queDebenSaber, alCambiarQueDebenSaber,
        listOf("Llevo mis herramientas", "Doy presupuesto sin costo", "Aviso antes de comprar materiales"), "Ej. Doy presupuesto antes de empezar")
}

/**
 * Acta #1: opción VOLUNTARIA. Quien la activa aparece también en la sección "Mujeres en oficios".
 * Habilio no pregunta ni guarda el género de nadie, y quien no la active se muestra igual en todas las búsquedas.
 */
@Composable
fun OpcionMujeresEnOficios(activa: Boolean, alCambiar: (Boolean) -> Unit) {
    HabilioTarjeta(
        colorFondo = HabilioTheme.FondoAmarilloSuave, radioEsquina = 12.dp, paddingInterno = 12.dp,
        colorBorde = if (activa) HabilioTheme.AmarilloPrimario else HabilioTheme.BordeInput,
        alClick = { alCambiar(!activa) }
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Checkbox(
                checked = activa, onCheckedChange = null,
                colors = CheckboxDefaults.colors(checkedColor = HabilioTheme.AmarilloPrimario, checkmarkColor = HabilioTheme.TextoSobreAmarillo)
            )
            Spacer(Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Quiero aparecer en \"Mujeres en oficios\" (opcional)", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = HabilioTheme.TextoPrimario)
                Text(
                    "Es una sección para que más gente conozca el trabajo de mujeres en oficios. Habilio no te pide ni guarda tu género; tú decides si apareces ahí. Tu perfil se ve igual en todas las búsquedas.",
                    fontSize = 12.sp, color = HabilioTheme.TextoSecundario, lineHeight = 17.sp
                )
            }
        }
    }
}
