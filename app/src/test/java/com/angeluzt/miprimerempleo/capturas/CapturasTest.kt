package com.angeluzt.miprimerempleo.capturas

import android.app.Application
import android.content.Context
import androidx.compose.runtime.Composable
import androidx.test.core.app.ApplicationProvider
import com.angeluzt.miprimerempleo.cv.cvDeMuestra
import com.angeluzt.miprimerempleo.ui.screens.PantallaAjustes
import com.angeluzt.miprimerempleo.ui.screens.PantallaBienvenida
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

    @Test
    fun bienvenida() = capturar("01-bienvenida") {
        PantallaBienvenida(Escenarios.primeraVez(contexto), {}, {})
    }

    @Test
    @Config(qualifiers = LARGO)
    fun bienvenidaCompleta() = capturar("01b-bienvenida-completa") {
        PantallaBienvenida(Escenarios.primeraVez(contexto), {}, {})
    }

    @Test
    fun inicio() = capturar("02-inicio") {
        PantallaRuta(Escenarios.enCamino(contexto), {}, {}, {}, {})
    }

    @Test
    fun inicioOscuro() = capturar("02o-inicio-oscuro", oscuro = true) {
        PantallaRuta(Escenarios.enCamino(contexto), {}, {}, {}, {})
    }

    @Test
    @Config(qualifiers = LARGO)
    fun inicioCompleto() = capturar("02b-inicio-completo") {
        PantallaRuta(Escenarios.enCamino(contexto), {}, {}, {}, {})
    }

    @Test
    fun modulo() = capturar("03-modulo") {
        PantallaModulo(Escenarios.enCamino(contexto), "estafas", { _, _ -> }, {}, {})
    }

    @Test
    @Config(qualifiers = LARGO)
    fun lectorAbierto() = capturar("04-lector") {
        PantallaLector(
            estado = Escenarios.enCamino(contexto),
            moduloId = "estafas",
            capituloId = "es_regla",
            onLeido = {}, onAccion = { _, _ -> }, onEnlace = {}, onPaywall = {},
            onPrepararAnuncio = {}, onVerAnuncio = {}, onAvisoVisto = {}, onAtras = {},
        )
    }

    @Test
    fun lectorCerrado() = capturar("05-capitulo-cerrado") {
        PantallaLector(
            estado = Escenarios.enCamino(contexto),
            moduloId = "estafas",
            capituloId = "es_catalogo",
            onLeido = {}, onAccion = { _, _ -> }, onEnlace = {}, onPaywall = {},
            onPrepararAnuncio = {}, onVerAnuncio = {}, onAvisoVisto = {}, onAtras = {},
        )
    }

    @Test
    @Config(qualifiers = LARGO)
    fun paywall() = capturar("06-paywall") {
        PantallaPaywall(Escenarios.enCamino(contexto), {}, {}, {})
    }

    @Test
    fun plantillas() = capturar("07-formatos") {
        PantallaPlantillas(
            cv = cvDeMuestra(),
            plantillaElegida = "",
            fotoRuta = "",
            onElegir = {}, onElegirFoto = {}, onAtras = {},
        )
    }

    @Test
    @Config(qualifiers = LARGO)
    fun ajustes() = capturar("08-ajustes") {
        PantallaAjustes(Escenarios.enCamino(contexto), {}, {}, {}, {}, {})
    }
}

/** Un teléfono común: 393×852 dp, como un Pixel 7 o un Galaxy A54. */
const val TELEFONO = "w393dp-h852dp-xhdpi"

/** Para ver una pantalla entera de un vistazo, sin el corte del primer scroll. */
const val LARGO = "w393dp-h1900dp-xhdpi"
