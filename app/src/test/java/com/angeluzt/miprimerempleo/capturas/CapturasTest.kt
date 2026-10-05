package com.angeluzt.miprimerempleo.capturas

import android.app.Application
import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import com.angeluzt.miprimerempleo.R
import com.angeluzt.miprimerempleo.cv.cvDeMuestra
import com.angeluzt.miprimerempleo.ui.EstadoApp
import com.angeluzt.miprimerempleo.ui.EstadoCv
import com.angeluzt.miprimerempleo.ui.EstadoVacante
import com.angeluzt.miprimerempleo.ui.screens.ContenidoCv
import com.angeluzt.miprimerempleo.ui.screens.ContenidoVacante
import com.angeluzt.miprimerempleo.ui.screens.PantallaAjustes
import com.angeluzt.miprimerempleo.ui.screens.PantallaBienvenida
import com.angeluzt.miprimerempleo.ui.screens.PantallaBitacora
import com.angeluzt.miprimerempleo.ui.screens.PantallaEditorCv
import com.angeluzt.miprimerempleo.ui.screens.PantallaEntrevista
import com.angeluzt.miprimerempleo.ui.screens.PantallaLector
import com.angeluzt.miprimerempleo.ui.screens.PantallaModulo
import com.angeluzt.miprimerempleo.ui.screens.PantallaPaywall
import com.angeluzt.miprimerempleo.ui.screens.PantallaPlantillas
import com.angeluzt.miprimerempleo.ui.screens.PantallaRuta
import com.angeluzt.miprimerempleo.ui.theme.MiPrimerEmpleoTheme
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Dibuja las pantallas reales de la app, con el contenido real, en la JVM.
 *
 * No comparan nada ni fallan por diferencias de píxeles: existen para poder VER la app
 * sin emulador. CI sube los PNG a la rama `capturas-ci`. Cada cambio de diseño se
 * revisa contra estas imágenes, no contra una maqueta en HTML que se parece a la app.
 *
 * `application = Application::class` evita arrancar la app de verdad: así no se conecta
 * a Play Billing ni inicializa AdMob, que en la JVM no tienen nada con qué hablar.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], application = Application::class, qualifiers = TELEFONO)
class CapturasTest {

    private val contexto: Context get() = ApplicationProvider.getApplicationContext()

    private fun capturar(nombre: String, oscuro: Boolean = false, pantalla: @Composable () -> Unit) =
        captureRoboImage("build/capturas/$nombre.png") {
            MiPrimerEmpleoTheme(oscuro = oscuro) { pantalla() }
        }

    // ---------- Ícono ----------

    /**
     * El ícono adaptable como lo recortan los launchers: cada capa mide 108 dp y solo se ven
     * los 72 del centro, con la forma que elija el teléfono (círculo, gota, cuadro redondeado).
     */
    @Test
    @Config(qualifiers = "w393dp-h200dp-xhdpi")
    fun icono() = capturar("00-icono") {
        Row(
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(24.dp),
            horizontalArrangement = Arrangement.spacedBy(28.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icono(CircleShape, 96.dp)
            Icono(RoundedCornerShape(28), 72.dp)
            Icono(CircleShape, 48.dp)
            Image(
                painter = painterResource(R.drawable.ic_launcher_respaldo),
                contentDescription = "Android 7",
                modifier = Modifier.size(48.dp),
            )
        }
    }

    @Composable
    private fun Icono(forma: Shape, lado: Dp) {
        Box(Modifier.size(lado).clip(forma), contentAlignment = Alignment.Center) {
            listOf(R.drawable.ic_launcher_background, R.drawable.ic_launcher_foreground).forEach { capa ->
                Image(painterResource(capa), contentDescription = null, modifier = Modifier.requiredSize(lado * 1.5f))
            }
        }
    }

    // ---------- Bienvenida e inicio ----------

    @Test
    fun bienvenida() = capturar("01-bienvenida") {
        PantallaBienvenida(Escenarios.primeraVez(contexto), {}, {})
    }

    @Test
    fun inicio() = capturar("02-inicio") { Inicio(Escenarios.enCamino(contexto)) }

    @Test
    fun inicioOscuro() = capturar("02o-inicio-oscuro", oscuro = true) { Inicio(Escenarios.enCamino(contexto)) }

    @Test
    @Config(qualifiers = MUY_LARGO)
    fun inicioCompleto() = capturar("02b-inicio-completo") { Inicio(Escenarios.enCamino(contexto)) }

    @Test
    fun inicioConPase() = capturar("02c-inicio-con-pase") { Inicio(Escenarios.buscando(contexto)) }

    @Composable
    private fun Inicio(estado: EstadoApp) =
        PantallaRuta(estado, {}, { _, _ -> }, {}, {}, {}, {}, {})

    // ---------- Lectura ----------

    @Test
    fun modulo() = capturar("03-modulo") {
        PantallaModulo(Escenarios.enCamino(contexto), "estafas", { _, _ -> }, {}, {})
    }

    @Test
    @Config(qualifiers = LARGO)
    fun lectorAbierto() = capturar("04-lector") { Lector(Escenarios.enCamino(contexto), "es_regla") }

    @Test
    @Config(qualifiers = LARGO)
    fun lectorCerrado() = capturar("05-capitulo-cerrado") { Lector(Escenarios.enCamino(contexto), "es_catalogo") }

    @Test
    fun lectorCerradoOscuro() = capturar("05o-capitulo-cerrado-oscuro", oscuro = true) {
        Lector(Escenarios.enCamino(contexto), "es_catalogo")
    }

    @Composable
    private fun Lector(estado: EstadoApp, capitulo: String) = PantallaLector(
        estado = estado,
        moduloId = "estafas",
        capituloId = capitulo,
        onLeido = {}, onAccion = { _, _ -> }, onEnlace = {}, onPaywall = {},
        onPrepararAnuncio = {}, onVerAnuncio = {}, onAvisoVisto = {},
        onSiguiente = { _, _ -> }, onAtras = {},
    )

    @Test
    @Config(qualifiers = LARGO)
    fun paywall() = capturar("06-paywall") {
        PantallaPaywall(Escenarios.enCamino(contexto), {}, {}, {})
    }

    // ---------- CV ----------

    @Test
    fun cvEntrevista() = capturar("07-cv-entrevista") { Cv(Escenarios.entrevistando) }

    @Test
    @Config(qualifiers = LARGO)
    fun cvListo() = capturar("07b-cv-revisado") { Cv(Escenarios.cvListo) }

    @Test
    @Config(qualifiers = LARGO)
    fun cvListoOscuro() = capturar("07o-cv-revisado-oscuro", oscuro = true) { Cv(Escenarios.cvListo) }

    @Composable
    private fun Cv(cv: EstadoCv) = ContenidoCv(
        cv = cv,
        tienePase = true,
        entrada = "",
        listState = rememberLazyListState(),
        avisos = remember { SnackbarHostState() },
        onEntrada = {}, onEnviar = {}, onSugerencia = {}, onSaltar = {}, onGenerar = {},
        onVolverA = {}, onRevisarConIa = {}, onExportar = { _, _ -> }, onDescartarError = {},
        onPlantillas = {}, onEditor = {}, onVacante = {}, onPaywall = {}, onAtras = {},
    )

    @Test
    @Config(qualifiers = MUY_LARGO)
    fun editor() = capturar("08-editor") { PantallaEditorCv(Escenarios.par, {}, {}) }

    @Test
    fun formatos() = capturar("09-formatos") {
        PantallaPlantillas(
            cv = cvDeMuestra(),
            plantillaElegida = "",
            fotoRuta = "",
            onElegir = {}, onElegirFoto = {}, onAtras = {},
        )
    }

    // ---------- Vacantes ----------

    @Test
    @Config(qualifiers = LARGO)
    fun vacanteEstafa() = capturar("10-vacante-estafa") {
        Vacante(Escenarios.vacanteEstafa, Escenarios.buscando(contexto))
    }

    @Test
    @Config(qualifiers = LARGO)
    fun vacanteAnalizada() = capturar("10b-vacante-analizada") {
        Vacante(Escenarios.vacanteAnalizada, Escenarios.buscando(contexto))
    }

    @Composable
    private fun Vacante(vacante: EstadoVacante, estado: EstadoApp) = ContenidoVacante(
        vacante = vacante,
        estado = estado,
        avisos = remember { SnackbarHostState() },
        onTexto = {}, onAdaptar = {}, onAbrir = {}, onNueva = {}, onExportar = {},
        onDescartarError = {}, onAtras = {},
    )

    // ---------- Bitácora ----------

    @Test
    @Config(qualifiers = MUY_LARGO)
    fun bitacora() = capturar("11-bitacora") {
        PantallaBitacora(Escenarios.entrevistas, {}, {}, { _, _ -> }, {})
    }

    @Test
    fun bitacoraVacia() = capturar("11b-bitacora-vacia") {
        PantallaBitacora(emptyList(), {}, {}, { _, _ -> }, {})
    }

    @Test
    @Config(qualifiers = MUY_LARGO)
    fun entrevista() = capturar("12-entrevista") {
        PantallaEntrevista(Escenarios.entrevistas.last(), {}, {}, {})
    }

    @Test
    @Config(qualifiers = LARGO)
    fun ajustes() = capturar("13-ajustes") {
        PantallaAjustes(Escenarios.enCamino(contexto), {}, {}, {}, {}, {}, {})
    }
}

/** Un teléfono común: 393×852 dp, como un Pixel 7 o un Galaxy A54. */
const val TELEFONO = "w393dp-h852dp-xhdpi"

/** Para ver una pantalla entera de un vistazo, sin el corte del primer scroll. */
const val LARGO = "w393dp-h1900dp-xhdpi"

/** Para las pantallas que no caben ni en [LARGO]: el inicio completo, el editor, la bitácora. */
const val MUY_LARGO = "w393dp-h2600dp-xhdpi"
