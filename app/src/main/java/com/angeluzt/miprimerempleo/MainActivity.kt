package com.angeluzt.miprimerempleo

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.angeluzt.miprimerempleo.cv.Plantillas
import com.angeluzt.miprimerempleo.cv.cvDeMuestra
import com.angeluzt.miprimerempleo.ui.AppViewModel
import com.angeluzt.miprimerempleo.ui.screens.PantallaAjustes
import com.angeluzt.miprimerempleo.ui.screens.PantallaBienvenida
import com.angeluzt.miprimerempleo.ui.screens.PantallaBitacora
import com.angeluzt.miprimerempleo.ui.screens.PantallaCv
import com.angeluzt.miprimerempleo.ui.screens.PantallaEditorCv
import com.angeluzt.miprimerempleo.ui.screens.PantallaEntrevista
import com.angeluzt.miprimerempleo.ui.screens.PantallaLector
import com.angeluzt.miprimerempleo.ui.screens.PantallaModulo
import com.angeluzt.miprimerempleo.ui.screens.PantallaPaywall
import com.angeluzt.miprimerempleo.ui.screens.PantallaPlantillas
import com.angeluzt.miprimerempleo.ui.screens.PantallaRuta
import com.angeluzt.miprimerempleo.ui.screens.PantallaVacante
import com.angeluzt.miprimerempleo.ui.theme.MiPrimerEmpleoTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MiPrimerEmpleoTheme {
                val vm: AppViewModel = viewModel()
                val estado by vm.estado.collectAsStateWithLifecycle()
                val nav = rememberNavController()

                val abrirEnlace: (String) -> Unit = { url ->
                    runCatching { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
                }

                if (!estado.listo) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                    return@MiPrimerEmpleoTheme
                }

                NavHost(
                    navController = nav,
                    startDestination = if (estado.progreso.onboardingHecho) "ruta" else "bienvenida",
                ) {
                    composable("bienvenida") {
                        PantallaBienvenida(
                            estado = estado,
                            onElegirPais = vm::elegirPais,
                            onElegirRuta = {
                                vm.elegirRuta(it)
                                nav.navigate("ruta") { popUpTo("bienvenida") { inclusive = true } }
                            },
                        )
                    }
                    composable("ruta") {
                        PantallaRuta(
                            estado = estado,
                            onModulo = { nav.navigate("modulo/$it") },
                            onCapitulo = { moduloId, capituloId -> nav.navigate("lector/$moduloId/$capituloId") },
                            onCv = { nav.navigate("cv") },
                            onVacante = { nav.navigate("vacante") },
                            onBitacora = { nav.navigate("bitacora") },
                            onPaywall = { nav.navigate("paywall") },
                            onAjustes = { nav.navigate("ajustes") },
                        )
                    }
                    composable("ajustes") {
                        PantallaAjustes(
                            estado = estado,
                            onElegirRuta = vm::elegirRuta,
                            onElegirPais = vm::elegirPais,
                            onGuardarLlave = vm::guardarLlaveOpenAi,
                            onRestaurar = vm::restaurarCompras,
                            onAtras = { nav.popBackStack() },
                        )
                    }
                    composable("modulo/{moduloId}") { entrada ->
                        PantallaModulo(
                            estado = estado,
                            moduloId = entrada.arguments?.getString("moduloId").orEmpty(),
                            onCapitulo = { moduloId, capituloId -> nav.navigate("lector/$moduloId/$capituloId") },
                            onPaywall = { nav.navigate("paywall") },
                            onAtras = { nav.popBackStack() },
                        )
                    }
                    composable("lector/{moduloId}/{capituloId}") { entrada ->
                        PantallaLector(
                            estado = estado,
                            moduloId = entrada.arguments?.getString("moduloId").orEmpty(),
                            capituloId = entrada.arguments?.getString("capituloId").orEmpty(),
                            onLeido = vm::marcarLeido,
                            onAccion = vm::alternarAccion,
                            onEnlace = abrirEnlace,
                            onPaywall = { nav.navigate("paywall") },
                            onPrepararAnuncio = vm::prepararAnuncio,
                            onVerAnuncio = { vm.verAnuncio(this@MainActivity, it) },
                            onAvisoVisto = vm::descartarAvisoAnuncio,
                            // El siguiente capítulo reemplaza al actual: "atrás" vuelve al módulo,
                            // no a recorrer hacia atrás todo lo que se leyó.
                            onSiguiente = { moduloId, capituloId ->
                                nav.navigate("lector/$moduloId/$capituloId") {
                                    popUpTo("lector/{moduloId}/{capituloId}") { inclusive = true }
                                }
                            },
                            onAtras = { nav.popBackStack() },
                        )
                    }
                    composable("paywall") {
                        PantallaPaywall(
                            estado = estado,
                            onComprar = { vm.comprar(this@MainActivity, it) },
                            onRestaurar = vm::restaurarCompras,
                            onAtras = { nav.popBackStack() },
                        )
                    }
                    composable("cv") {
                        PantallaCv(
                            estado = estado,
                            onPaywall = { nav.navigate("paywall") },
                            onPlantillas = { nav.navigate("plantillas") },
                            onEditor = { nav.navigate("editor") },
                            onVacante = { nav.navigate("vacante") },
                            onAtras = { nav.popBackStack() },
                        )
                    }
                    composable("editor") {
                        val cv = estado.cv
                        if (cv == null) {
                            // Sin CV no hay nada que editar: se vuelve sin dibujar nada.
                            LaunchedEffect(Unit) { nav.popBackStack() }
                        } else {
                            PantallaEditorCv(
                                par = cv,
                                onGuardar = {
                                    vm.guardarCv(it)
                                    nav.popBackStack()
                                },
                                onAtras = { nav.popBackStack() },
                            )
                        }
                    }
                    composable("vacante") {
                        PantallaVacante(
                            estado = estado,
                            onCv = { nav.navigate("cv") },
                            onPaywall = { nav.navigate("paywall") },
                            onAtras = { nav.popBackStack() },
                        )
                    }
                    composable("bitacora") {
                        PantallaBitacora(
                            entrevistas = estado.entrevistas,
                            onNueva = { nav.navigate("entrevista/nueva") },
                            onAbrir = { nav.navigate("entrevista/$it") },
                            onPreparada = vm::marcarPreparada,
                            onAtras = { nav.popBackStack() },
                        )
                    }
                    composable("entrevista/{id}") { entrada ->
                        val id = entrada.arguments?.getString("id").orEmpty()
                        PantallaEntrevista(
                            existente = estado.entrevistas.firstOrNull { it.id == id },
                            onGuardar = {
                                vm.guardarEntrevista(it)
                                nav.popBackStack()
                            },
                            onBorrar = {
                                vm.borrarEntrevista(it)
                                nav.popBackStack()
                            },
                            onAtras = { nav.popBackStack() },
                        )
                    }
                    composable("plantillas") {
                        PantallaPlantillas(
                            // Con el CV ya generado se previsualiza el real; si todavía no
                            // existe, un perfil de muestra: el formato se ve igual con
                            // cualquier contenido.
                            cv = estado.cv?.es ?: cvDeMuestra(),
                            plantillaElegida = estado.progreso.plantillaCv
                                .ifBlank { Plantillas.porDefecto.id },
                            fotoRuta = estado.progreso.fotoCv,
                            onElegir = vm::elegirPlantilla,
                            onElegirFoto = vm::elegirFotoCv,
                            onAtras = { nav.popBackStack() },
                        )
                    }
                }
            }
        }
    }
}
