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
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
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
fun LogoGlobalHabilio(
    modifier: Modifier = Modifier,
    colorIcono: Color = HabilioTheme.AmarilloPrimario
) {
    when (val logo = AppConfig.logoGlobal) {
        is ImageVector -> {
            Icon(
                imageVector = logo,
                contentDescription = "Logo Habilio",
                tint = colorIcono,
                modifier = modifier
            )
        }
        else -> {
            AsyncImage(
                model = logo,
                contentDescription = "Logo Habilio",
                contentScale = ContentScale.FillBounds,
                modifier = modifier
            )
        }
    }
}

@Composable
fun coloresInputHabilio() = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = HabilioTheme.SuperficieInput,
    unfocusedContainerColor = HabilioTheme.SuperficieInput,
    focusedBorderColor = HabilioTheme.AmarilloPrimario,
    unfocusedBorderColor = HabilioTheme.BordeInput,
    focusedTextColor = HabilioTheme.TextoPrimario,
    unfocusedTextColor = HabilioTheme.TextoPrimario,
    cursorColor = HabilioTheme.AmarilloAcentoTexto
)

@Composable
fun HabilioTarjeta(
    modifier: Modifier = Modifier,
    colorFondo: Color = HabilioTheme.SuperficieTarjeta,
    colorBorde: Color = HabilioTheme.BordeTarjeta,
    grosorBorde: Dp = 1.dp,
    radioEsquina: Dp = 16.dp,
    paddingInterno: Dp = 16.dp,
    alClick: (() -> Unit)? = null,
    contenido: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(radioEsquina)
    val border = BorderStroke(grosorBorde, colorBorde)
    if (alClick != null) {
        Surface(
            onClick = alClick,
            shape = shape,
            color = colorFondo,
            border = border,
            shadowElevation = HabilioTheme.ElevacionTarjeta,
            modifier = modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(paddingInterno), content = contenido)
        }
    } else {
        Surface(
            shape = shape,
            color = colorFondo,
            border = border,
            shadowElevation = HabilioTheme.ElevacionTarjeta,
            modifier = modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(paddingInterno), content = contenido)
        }
    }
}

/**
 * Sección de accesibilidad compartida entre Modo Cliente y Modo Trabajador
 * para ajustar el tamaño global del texto en tiempo real.
 */
@Composable
fun SeccionTamanoTexto(
    escalaActual: Float,
    alCambiarEscala: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val nivelActivo = NIVELES_TEXTO.firstOrNull { abs(escalaActual - it.third) < 0.02f } ?: NIVELES_TEXTO.first()

    HabilioTarjeta(modifier = modifier) {
        FilaAdaptable(
            inicio = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(HabilioTheme.FondoAmarilloSuave),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.FormatSize,
                            contentDescription = null,
                            tint = HabilioTheme.AmarilloAcentoTexto,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Tamaño del texto",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = HabilioTheme.TextoPrimario
                        )
                        Text(
                            text = "Texto y botones de toda la app",
                            fontSize = 12.sp,
                            color = HabilioTheme.TextoSecundario
                        )
                    }
                }
            },
            fin = {
                HabilioBadge(
                    texto = "${nivelActivo.first} (${nivelActivo.second})",
                    colorTexto = HabilioTheme.AmarilloAcentoTexto,
                    colorFondo = HabilioTheme.FondoAmarilloSuave
                )
            }
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            NIVELES_TEXTO.forEach { (nombre, porcentaje, valorEscala) ->
                val seleccionado = abs(escalaActual - valorEscala) < 0.02f
                Surface(
                    onClick = { alCambiarEscala(valorEscala) },
                    shape = RoundedCornerShape(10.dp),
                    color = if (seleccionado) HabilioTheme.AmarilloPrimario else HabilioTheme.SuperficieInput,
                    border = BorderStroke(
                        width = 1.dp,
                        color = if (seleccionado) HabilioTheme.AmarilloPrimario else HabilioTheme.BordeInput
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = HabilioTheme.alturaMinima(56.dp))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp, horizontal = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = nombre,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (seleccionado) HabilioTheme.TextoSobreAmarillo else HabilioTheme.TextoPrimario,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = porcentaje,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (seleccionado) HabilioTheme.TextoSobreAmarillo.copy(alpha = 0.85f) else HabilioTheme.TextoSecundario,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

/** Selector de apariencia: Sistema / Claro / Oscuro. */
@Composable
fun SeccionApariencia(
    aparienciaActual: String,
    alCambiarApariencia: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val opciones = listOf(
        Triple("sistema", "Sistema", Icons.Outlined.PhoneAndroid),
        Triple("claro", "Claro", Icons.Outlined.LightMode),
        Triple("oscuro", "Oscuro", Icons.Outlined.DarkMode)
    )

    HabilioTarjeta(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(HabilioTheme.FondoAmarilloSuave),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Palette,
                    contentDescription = null,
                    tint = HabilioTheme.AmarilloAcentoTexto,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text("Apariencia", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = HabilioTheme.TextoPrimario)
                Text("Modo claro u oscuro", fontSize = 12.sp, color = HabilioTheme.TextoSecundario)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            opciones.forEach { (clave, nombre, icono) ->
                val seleccionado = aparienciaActual == clave
                Surface(
                    onClick = { alCambiarApariencia(clave) },
                    shape = RoundedCornerShape(10.dp),
                    color = if (seleccionado) HabilioTheme.AmarilloPrimario else HabilioTheme.SuperficieInput,
                    border = BorderStroke(1.dp, if (seleccionado) HabilioTheme.AmarilloPrimario else HabilioTheme.BordeInput),
                    modifier = Modifier.weight(1f).heightIn(min = HabilioTheme.alturaMinima(56.dp))
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp, horizontal = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = icono,
                            contentDescription = null,
                            tint = if (seleccionado) HabilioTheme.TextoSobreAmarillo else HabilioTheme.TextoSecundario,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = nombre,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (seleccionado) HabilioTheme.TextoSobreAmarillo else HabilioTheme.TextoPrimario,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ModalVerFotoCompleta(
    evidencia: EvidenciaItem,
    alCerrar: () -> Unit
) {
    DialogoHabilio(
        onDismissRequest = alCerrar,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.92f))
                .clickable(onClick = alCerrar)
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = HabilioTheme.SuperficieTarjeta,
                border = BorderStroke(1.dp, HabilioTheme.BordeTarjeta),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = false) {}
            ) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            if (evidencia.etiqueta != EtiquetaFoto.UNICA) {
                                EtiquetaAntesDespues(evidencia.etiqueta)
                                Spacer(modifier = Modifier.height(4.dp))
                            }
                            Text(
                                text = evidencia.titulo,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 16.sp,
                                color = HabilioTheme.TextoPrimario
                            )
                            Text(
                                text = "✓ ${evidencia.fechaPublicacion}",
                                fontSize = 12.sp,
                                color = HabilioTheme.VerdeExito
                            )
                        }
                        IconButton(
                            onClick = alCerrar,
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(HabilioTheme.SuperficieElevada)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Cerrar vista completa",
                                tint = HabilioTheme.TextoPrimario
                            )
                        }
                    }

                    AsyncImage(
                        model = evidencia.fotoUrl,
                        contentDescription = evidencia.titulo,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 260.dp, max = 480.dp)
                            .background(Color.Black)
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "📍 ${evidencia.ubicacion}",
                            fontSize = 13.sp,
                            color = HabilioTheme.TextoSecundario,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = alCerrar) {
                            Text(
                                text = "Cerrar",
                                color = HabilioTheme.AmarilloAcentoTexto,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HabilioTopBar(
    subtituloUbicacion: String? = "Pénjamo, Gto.",
    modoTrabajador: Boolean = false,
    alAccionDerecha: (() -> Unit)? = null,
    iconoDerecha: ImageVector = Icons.Default.SwapHoriz
) {
    Surface(
        color = HabilioTheme.FondoPrincipal,
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(HabilioTheme.SuperficieTarjeta)
                            .border(1.dp, HabilioTheme.BordeTarjeta, RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        LogoGlobalHabilio(modifier = Modifier.fillMaxSize())
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        FilaAdaptable(
                            inicio = {
                                Text(
                                    text = "Habilio",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = HabilioTheme.TextoPrimario,
                                    maxLines = 1
                                )
                            },
                            fin = {
                                if (modoTrabajador) {
                                    HabilioBadge(
                                        texto = "● Modo especialista",
                                        colorTexto = HabilioTheme.VerdeExito,
                                        colorFondo = HabilioTheme.FondoVerdeSuave
                                    )
                                }
                            }
                        )
                        if (subtituloUbicacion != null) {
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = subtituloUbicacion,
                                fontSize = 13.sp,
                                color = HabilioTheme.TextoSecundario,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                if (alAccionDerecha != null) {
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = alAccionDerecha,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(HabilioTheme.SuperficieTarjeta)
                            .border(1.dp, HabilioTheme.BordeTarjeta, RoundedCornerShape(10.dp))
                    ) {
                        Icon(
                            imageVector = iconoDerecha,
                            contentDescription = "Cerrar sesión o cambiar rol",
                            tint = HabilioTheme.TextoSecundario
                        )
                    }
                }
            }
            HorizontalDivider(color = HabilioTheme.BordeTarjeta, thickness = 1.dp)
        }
    }
}

/**
 * Acta #22: ventana emergente que respeta el "Tamaño del texto" elegido en Habilio.
 * Un Dialog de Android se dibuja en una ventana aparte que vuelve a tomar la escala de letra del sistema, así que
 * el texto se veía chico aunque la persona hubiera elegido "Muy grande". Aquí se le pasa la misma escala de la app.
 */
@Composable
fun DialogoHabilio(
    onDismissRequest: () -> Unit,
    properties: DialogProperties = DialogProperties(),
    content: @Composable () -> Unit
) {
    val densidadApp = LocalDensity.current
    Dialog(onDismissRequest = onDismissRequest, properties = properties) {
        CompositionLocalProvider(LocalDensity provides densidadApp, content = content)
    }
}

/** Alto máximo de una ventana: 90 % de la pantalla (antes había topes fijos que la hacían ver chica). */
@Composable
fun altoMaximoVentana(): Dp = (androidx.compose.ui.platform.LocalConfiguration.current.screenHeightDp * 0.9f).dp

/**
 * Acta #22: fila que se adapta al tamaño de letra. Pone [inicio] y [fin] lado a lado cuando [fin] ocupa como máximo
 * [proporcionMaximaFin] del ancho; si no cabe (letra grande o textos largos), pone [fin] debajo de [inicio].
 * Evita que una insignia o un botón le quite todo el espacio al texto de al lado y este se parta letra por letra.
 */
@Composable
fun FilaAdaptable(
    modifier: Modifier = Modifier,
    separacion: Dp = 8.dp,
    proporcionMaximaFin: Float = 0.5f,
    inicio: @Composable () -> Unit,
    fin: @Composable () -> Unit
) {
    androidx.compose.ui.layout.Layout(
        content = {
            Box { inicio() }
            Box { fin() }
        },
        modifier = modifier.fillMaxWidth()
    ) { medibles, restricciones ->
        val sueltas = restricciones.copy(minWidth = 0, minHeight = 0)
        val espacio = separacion.roundToPx()
        if (!restricciones.hasBoundedWidth) {
            // Dentro de un desplazamiento horizontal: siempre lado a lado
            val pIni = medibles[0].measure(sueltas)
            val pFin = medibles[1].measure(sueltas)
            val alto = maxOf(pIni.height, pFin.height)
            return@Layout layout(pIni.width + espacio + pFin.width, alto) {
                pIni.placeRelative(0, (alto - pIni.height) / 2)
                pFin.placeRelative(pIni.width + espacio, (alto - pFin.height) / 2)
            }
        }
        val ancho = restricciones.maxWidth
        val pFin = medibles[1].measure(sueltas)
        if (pFin.width == 0) {
            // [fin] vacío (por ejemplo, una insignia que no aplica)
            val pIni = medibles[0].measure(sueltas)
            return@Layout layout(ancho, pIni.height) { pIni.placeRelative(0, 0) }
        }
        if (pFin.width <= ancho * proporcionMaximaFin) {
            val pIni = medibles[0].measure(sueltas.copy(maxWidth = (ancho - pFin.width - espacio).coerceAtLeast(0)))
            val alto = maxOf(pIni.height, pFin.height)
            layout(ancho, alto) {
                pIni.placeRelative(0, (alto - pIni.height) / 2)
                pFin.placeRelative(ancho - pFin.width, (alto - pFin.height) / 2)
            }
        } else {
            val pIni = medibles[0].measure(sueltas)
            val espacioVertical = (espacio * 3) / 4
            layout(ancho, pIni.height + espacioVertical + pFin.height) {
                pIni.placeRelative(0, 0)
                pFin.placeRelative(0, pIni.height + espacioVertical)
            }
        }
    }
}

@Composable
fun HabilioBadge(
    texto: String,
    colorTexto: Color,
    colorFondo: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color = colorFondo,
        shape = RoundedCornerShape(6.dp),
        modifier = modifier
    ) {
        Text(
            text = texto,
            color = colorTexto,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun AvatarUsuario(
    tamano: Dp = 54.dp,
    esCircular: Boolean = false,
    mostrarPuntoDisponible: Boolean = false,
    icono: ImageVector = Icons.Default.Person,
    colorIcono: Color = HabilioTheme.TextoSecundario,
    colorFondo: Color = HabilioTheme.SuperficieElevada,
    fotoUrl: String? = null
) {
    val shape = if (esCircular) CircleShape else RoundedCornerShape(12.dp)
    Box(contentAlignment = Alignment.BottomEnd) {
        if (!fotoUrl.isNullOrBlank()) {
            AsyncImage(
                model = fotoUrl,
                contentDescription = "Avatar",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(tamano)
                    .clip(shape)
                    .background(colorFondo)
            )
        } else {
            Box(
                modifier = Modifier
                    .size(tamano)
                    .clip(shape)
                    .background(colorFondo),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icono,
                    contentDescription = null,
                    tint = colorIcono,
                    modifier = Modifier.size(tamano * 0.6f)
                )
            }
        }
        if (mostrarPuntoDisponible) {
            Box(
                modifier = Modifier
                    .size(14.dp)
                    .clip(CircleShape)
                    .background(HabilioTheme.VerdeExito)
                    .border(2.dp, HabilioTheme.SuperficieTarjeta, CircleShape)
            )
        }
    }
}

@Composable
fun EstadoCargaOVacio(
    cargando: Boolean,
    estaVacio: Boolean,
    mensajeVacio: String,
    subtituloVacio: String? = null,
    iconoVacio: ImageVector? = null
) {
    if (cargando) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 40.dp),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = HabilioTheme.AmarilloPrimario)
        }
    } else if (estaVacio) {
        HabilioTarjeta(paddingInterno = 22.dp) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (iconoVacio != null) {
                    Icon(
                        imageVector = iconoVacio,
                        contentDescription = null,
                        tint = HabilioTheme.AmarilloAcentoTexto,
                        modifier = Modifier.size(40.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                }
                Text(
                    text = mensajeVacio,
                    fontWeight = if (subtituloVacio != null) FontWeight.Bold else FontWeight.Normal,
                    fontSize = 14.sp,
                    color = if (subtituloVacio != null) HabilioTheme.TextoPrimario else HabilioTheme.TextoSecundario,
                    textAlign = TextAlign.Center
                )
                if (subtituloVacio != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = subtituloVacio,
                        fontSize = 13.sp,
                        color = HabilioTheme.TextoSecundario,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
fun HabilioBarraInferior(
    primerTabSeleccionado: Boolean,
    etiquetaPrimero: String,
    iconoPrimero: ImageVector,
    alClickPrimero: () -> Unit,
    etiquetaSegundo: String,
    iconoSegundo: ImageVector,
    alClickSegundo: () -> Unit
) {
    val coloresItem = NavigationBarItemDefaults.colors(
        selectedIconColor = HabilioTheme.AmarilloAcentoTexto,
        selectedTextColor = HabilioTheme.AmarilloAcentoTexto,
        unselectedIconColor = HabilioTheme.TextoSecundario,
        unselectedTextColor = HabilioTheme.TextoSecundario,
        indicatorColor = HabilioTheme.IndicadorNavegacion
    )
    NavigationBar(
        containerColor = HabilioTheme.FondoPrincipal,
        tonalElevation = 8.dp
    ) {
        NavigationBarItem(
            selected = primerTabSeleccionado,
            onClick = alClickPrimero,
            icon = { Icon(iconoPrimero, contentDescription = etiquetaPrimero) },
            label = { Text(etiquetaPrimero, fontWeight = FontWeight.SemiBold) },
            colors = coloresItem
        )
        NavigationBarItem(
            selected = !primerTabSeleccionado,
            onClick = alClickSegundo,
            icon = { Icon(iconoSegundo, contentDescription = etiquetaSegundo) },
            label = { Text(etiquetaSegundo, fontWeight = FontWeight.SemiBold) },
            colors = coloresItem
        )
    }
}

@Composable
fun EncabezadoModal(
    titulo: String,
    icono: ImageVector,
    alCerrar: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icono,
                contentDescription = null,
                tint = HabilioTheme.AmarilloAcentoTexto,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = titulo,
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                color = HabilioTheme.TextoPrimario,
                // Acta #22: los lectores de pantalla lo anuncian como encabezado y permiten saltar entre secciones
                modifier = Modifier.semantics { heading() }
            )
        }
        IconButton(onClick = alCerrar, modifier = Modifier.size(HabilioTheme.alturaMinima(48.dp))) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Cerrar",
                tint = HabilioTheme.TextoSecundario
            )
        }
    }
    Spacer(modifier = Modifier.height(12.dp))
    HorizontalDivider(color = HabilioTheme.BordeTarjeta)
    Spacer(modifier = Modifier.height(16.dp))
}

@Composable
fun EncabezadoAutenticacion(
    titulo: String,
    subtitulo: String,
    textoVolver: String = "Cambiar rol",
    alVolver: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start
        ) {
            TextButton(onClick = alVolver) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = textoVolver,
                    tint = HabilioTheme.TextoSecundario,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(textoVolver, color = HabilioTheme.TextoSecundario, fontSize = 13.sp)
            }
        }

        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(HabilioTheme.SuperficieTarjeta)
                .border(1.dp, HabilioTheme.BordeTarjeta, RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center
        ) {
            LogoGlobalHabilio(modifier = Modifier.fillMaxSize())
        }

        Spacer(modifier = Modifier.height(12.dp))

        HabilioBadge(
            texto = "📍 PÉNJAMO, GTO. Y REGIÓN",
            colorTexto = HabilioTheme.VerdeExito,
            colorFondo = HabilioTheme.FondoVerdeSuave
        )

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = titulo,
            fontSize = 24.sp,
            fontWeight = FontWeight.ExtraBold,
            color = HabilioTheme.TextoPrimario,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = subtitulo,
            fontSize = 14.sp,
            color = HabilioTheme.TextoSecundario,
            textAlign = TextAlign.Center,
            lineHeight = 20.sp,
            modifier = Modifier.padding(horizontal = 12.dp)
        )
    }
}

@Composable
fun SelectorModoAutenticacion(
    esLoginSeleccionado: Boolean,
    alSeleccionarLogin: () -> Unit,
    alSeleccionarRegistro: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(HabilioTheme.SuperficieInput)
            .border(1.dp, HabilioTheme.BordeTarjeta, RoundedCornerShape(12.dp))
            .padding(4.dp)
    ) {
        listOf(
            Triple("Iniciar Sesión", esLoginSeleccionado, alSeleccionarLogin),
            Triple("Crear Cuenta", !esLoginSeleccionado, alSeleccionarRegistro)
        ).forEach { (texto, activo, accion) ->
            Button(
                onClick = accion,
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = HabilioTheme.alturaMinima(48.dp)),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (activo) HabilioTheme.AmarilloPrimario else Color.Transparent,
                    contentColor = if (activo) HabilioTheme.TextoSobreAmarillo else HabilioTheme.TextoSecundario
                ),
                elevation = null
            ) {
                Text(texto, fontWeight = FontWeight.Bold, fontSize = 14.sp, textAlign = TextAlign.Center)
            }
        }
    }
}

@Composable
fun HabilioCampoTexto(
    etiqueta: String,
    etiquetaDerecha: String? = null,
    colorEtiquetaDerecha: Color = HabilioTheme.TextoSecundario,
    accionEtiquetaDerecha: (() -> Unit)? = null,
    valor: String,
    alCambiarValor: (String) -> Unit,
    placeholder: String,
    iconoInicial: ImageVector? = null,
    prefijoTexto: String? = null,
    esContrasena: Boolean = false
) {
    var mostrarContrasena by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth()) {
        FilaAdaptable(
            inicio = {
                Text(
                    text = etiqueta,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = HabilioTheme.TextoPrimario
                )
            },
            fin = {
                if (etiquetaDerecha != null) {
                    Text(
                        text = etiquetaDerecha,
                        fontSize = 12.sp,
                        color = colorEtiquetaDerecha,
                        fontWeight = FontWeight.Medium,
                        modifier = if (accionEtiquetaDerecha != null) {
                            Modifier
                                .clickable(role = Role.Button, onClick = accionEtiquetaDerecha)
                                .padding(vertical = 4.dp)
                        } else Modifier
                    )
                }
            }
        )

        Spacer(modifier = Modifier.height(6.dp))

        OutlinedTextField(
            value = valor,
            onValueChange = alCambiarValor,
            modifier = Modifier.fillMaxWidth(),
            placeholder = {
                Text(
                    text = placeholder,
                    color = HabilioTheme.TextoPlaceholder,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            },
            leadingIcon = if (prefijoTexto != null) {
                {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(start = 12.dp, end = 8.dp)
                    ) {
                        Text("🇲🇽", fontSize = 16.sp)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = prefijoTexto,
                            color = HabilioTheme.TextoPrimario,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(20.dp)
                                .background(HabilioTheme.BordeTarjeta)
                        )
                    }
                }
            } else if (iconoInicial != null) {
                {
                    Icon(
                        imageVector = iconoInicial,
                        contentDescription = null,
                        tint = HabilioTheme.TextoSecundario
                    )
                }
            } else null,
            trailingIcon = if (esContrasena) {
                {
                    IconButton(onClick = { mostrarContrasena = !mostrarContrasena }) {
                        Icon(
                            imageVector = if (mostrarContrasena) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                            contentDescription = if (mostrarContrasena) "Ocultar contraseña" else "Mostrar contraseña",
                            tint = HabilioTheme.TextoSecundario
                        )
                    }
                }
            } else null,
            visualTransformation = if (esContrasena && !mostrarContrasena) {
                PasswordVisualTransformation()
            } else {
                VisualTransformation.None
            },
            singleLine = true,
            shape = RoundedCornerShape(10.dp),
            colors = coloresInputHabilio()
        )
    }
}

@Composable
fun BotonPrimarioHabilio(
    texto: String,
    alClick: () -> Unit,
    modifier: Modifier = Modifier,
    habilitado: Boolean = true,
    mostrarFlecha: Boolean = false
) {
    Button(
        onClick = alClick,
        enabled = habilitado,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = HabilioTheme.alturaMinima(52.dp)),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = HabilioTheme.AmarilloPrimario,
            contentColor = HabilioTheme.TextoSobreAmarillo
        )
    ) {
        Text(
            text = texto,
            fontSize = 15.sp,
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center
        )
        if (mostrarFlecha) {
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
