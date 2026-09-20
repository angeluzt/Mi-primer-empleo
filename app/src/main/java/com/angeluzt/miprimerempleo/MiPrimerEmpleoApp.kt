package com.angeluzt.miprimerempleo

import android.app.Application
import com.angeluzt.miprimerempleo.billing.GestorCompras
import com.angeluzt.miprimerempleo.cv.ClienteCv
import com.angeluzt.miprimerempleo.data.Progreso
import com.angeluzt.miprimerempleo.data.RepositorioContenido

class MiPrimerEmpleoApp : Application() {

    lateinit var contenido: RepositorioContenido
        private set
    lateinit var progreso: Progreso
        private set
    lateinit var compras: GestorCompras
        private set
    lateinit var clienteCv: ClienteCv
        private set

    override fun onCreate() {
        super.onCreate()
        contenido = RepositorioContenido(this)
        progreso = Progreso(this)
        clienteCv = ClienteCv()
        compras = GestorCompras(this).also { it.conectar() }
    }
}
