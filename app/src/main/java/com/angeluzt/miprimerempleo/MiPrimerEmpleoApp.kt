package com.angeluzt.miprimerempleo

import android.app.Application
import com.angeluzt.miprimerempleo.billing.GestorCompras
import com.angeluzt.miprimerempleo.cv.ClienteCv
import com.angeluzt.miprimerempleo.data.Anuncios
import com.angeluzt.miprimerempleo.data.GestorAnuncios
import com.angeluzt.miprimerempleo.data.Progreso
import com.angeluzt.miprimerempleo.data.RepositorioContenido
import com.google.android.gms.ads.MobileAds

class MiPrimerEmpleoApp : Application() {

    lateinit var contenido: RepositorioContenido
        private set
    lateinit var progreso: Progreso
        private set
    lateinit var compras: GestorCompras
        private set
    lateinit var clienteCv: ClienteCv
        private set
    lateinit var anuncios: Anuncios
        private set
    lateinit var gestorAnuncios: GestorAnuncios
        private set

    override fun onCreate() {
        super.onCreate()
        contenido = RepositorioContenido(this)
        progreso = Progreso(this)
        clienteCv = ClienteCv(this)
        anuncios = Anuncios(this)
        gestorAnuncios = GestorAnuncios(this)
        compras = GestorCompras(this).also { it.conectar() }

        // Arrancar el SDK de anuncios bloquea el hilo principal cerca de un segundo,
        // así que va aparte. No se pide ningún anuncio aquí: eso pasa solo cuando
        // alguien se topa con un capítulo cerrado.
        Thread { MobileAds.initialize(this) }.start()
    }
}
