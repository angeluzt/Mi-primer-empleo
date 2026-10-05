package com.angeluzt.miprimerempleo.billing

import android.app.Activity
import android.content.Context
import com.angeluzt.miprimerempleo.BuildConfig
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ConsumeParams
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.android.billingclient.api.acknowledgePurchase
import com.android.billingclient.api.consumePurchase
import com.android.billingclient.api.queryProductDetails
import com.android.billingclient.api.queryPurchasesAsync
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Catálogo de la app. TODOS son productos de pago único (ProductType.INAPP).
 *
 * Decisión de producto: esta app nunca vende suscripciones. El público son estudiantes
 * y recién egresados que usan la guía unos meses; cobrarles cada mes genera cancelaciones,
 * reembolsos y malas reseñas. Se paga una vez y se queda para siempre.
 */
object Productos {
    /** Barato: abre los 10 módulos de lectura, sin el generador de CV. No consumible. */
    const val PASE_LECTURA = "pase_lectura"

    /** Pago único que desbloquea todo, incluido el generador de CV. No consumible. */
    const val PASE_COMPLETO = "pase_completo"

    /**
     * Consumible: 10 generaciones más, para generar o adaptar el CV a vacantes.
     * Es la única compra que se repite, y la única que puede repetirse sin suscripción.
     */
    const val RECARGA_CV = "recarga_cv_10"

    // Antes existía "plantillas_extra". Se quitó: los formatos van todos incluidos y
    // vender un paquete que no entrega nada es tomarle el dinero a alguien.
    val todos = listOf(PASE_LECTURA, PASE_COMPLETO, RECARGA_CV)

    /** Generaciones de CV incluidas en el pase, suficientes para un proceso de búsqueda normal. */
    const val CVS_INCLUIDOS_EN_PASE = 15
    const val CVS_POR_RECARGA = 10
}

data class EstadoCompras(
    val conectado: Boolean = false,
    val tienePase: Boolean = BuildConfig.DESBLOQUEO_PRUEBA,
    val tieneLectura: Boolean = BuildConfig.DESBLOQUEO_PRUEBA,
    val precios: Map<String, String> = emptyMap(),
    val error: String? = null,
    /**
     * Pagó con un método que se confirma después: efectivo en OXXO u otra tienda, que es
     * como paga mucha gente sin tarjeta. El pase se desbloquea solo cuando Google lo
     * confirma; mientras, hay que decirlo para que no crea que perdió su dinero.
     */
    val pagoPendiente: Boolean = false,
) {
    /** El pase completo incluye la lectura, así que quien lo tiene no necesita el otro. */
    val puedeLeerTodo: Boolean get() = tienePase || tieneLectura
}

/**
 * @param acreditarRecarga Avisa al backend de una recarga comprada y la cuenta en el
 * teléfono. Devuelve true solo si quedó acreditada; mientras no, la compra no se consume.
 */
class GestorCompras(
    context: Context,
    private val acreditarRecarga: suspend (tokenRecarga: String) -> Boolean,
) {

    private val alcance = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _estado = MutableStateFlow(EstadoCompras())
    val estado: StateFlow<EstadoCompras> = _estado.asStateFlow()

    private var detalles: Map<String, ProductDetails> = emptyMap()

    private val escucha = PurchasesUpdatedListener { resultado, compras ->
        when {
            resultado.responseCode == BillingClient.BillingResponseCode.OK && compras != null ->
                alcance.launch { compras.forEach { procesar(it) } }

            resultado.responseCode == BillingClient.BillingResponseCode.USER_CANCELED ->
                _estado.update { it.copy(error = null) }

            // Ya lo tenía (otro teléfono, reinstaló): no es un error, es devolvérselo.
            resultado.responseCode == BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED ->
                alcance.launch { restaurarCompras() }

            else ->
                _estado.update { it.copy(error = "No se pudo completar la compra.") }
        }
    }

    private val cliente = BillingClient.newBuilder(context)
        .setListener(escucha)
        .enablePendingPurchases(
            PendingPurchasesParams.newBuilder().enableOneTimeProducts().build()
        )
        .build()

    fun conectar() {
        if (cliente.isReady) return
        cliente.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(resultado: BillingResult) {
                if (resultado.responseCode == BillingClient.BillingResponseCode.OK) {
                    _estado.update { it.copy(conectado = true) }
                    alcance.launch {
                        cargarPrecios()
                        restaurarCompras()
                    }
                }
            }

            override fun onBillingServiceDisconnected() {
                _estado.update { it.copy(conectado = false) }
            }
        })
    }

    private suspend fun cargarPrecios() {
        val productos = Productos.todos.map { id ->
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(id)
                // INAPP siempre. Esta app no consulta ni vende ProductType.SUBS.
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
        }
        val resultado = cliente.queryProductDetails(
            QueryProductDetailsParams.newBuilder().setProductList(productos).build()
        )
        val lista = resultado.productDetailsList ?: return
        detalles = lista.associateBy { it.productId }
        _estado.update { estado ->
            estado.copy(
                precios = lista.mapNotNull { detalle ->
                    detalle.oneTimePurchaseOfferDetails?.formattedPrice?.let { detalle.productId to it }
                }.toMap()
            )
        }
    }

    /** Se llama al abrir la app: devuelve el pase a quien reinstaló o cambió de teléfono. */
    suspend fun restaurarCompras() {
        val resultado = cliente.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
        )
        val activas = resultado.purchasesList.filter {
            it.purchaseState == Purchase.PurchaseState.PURCHASED
        }
        val pendientes = resultado.purchasesList.any {
            it.purchaseState == Purchase.PurchaseState.PENDING
        }
        _estado.update { estado ->
            estado.copy(
                pagoPendiente = pendientes,
                tienePase = BuildConfig.DESBLOQUEO_PRUEBA ||
                    activas.any { Productos.PASE_COMPLETO in it.products },
                tieneLectura = BuildConfig.DESBLOQUEO_PRUEBA ||
                    activas.any { Productos.PASE_LECTURA in it.products },
            )
        }
        activas.forEach { procesar(it) }
    }

    fun comprar(activity: Activity, productoId: String) {
        val detalle = detalles[productoId] ?: run {
            _estado.update { it.copy(error = "Producto no disponible ahora mismo.") }
            return
        }
        val params = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(
                listOf(
                    BillingFlowParams.ProductDetailsParams.newBuilder()
                        .setProductDetails(detalle)
                        .build()
                )
            )
            .build()
        cliente.launchBillingFlow(activity, params)
    }

    private suspend fun procesar(compra: Purchase) {
        if (compra.purchaseState == Purchase.PurchaseState.PENDING) {
            _estado.update { it.copy(pagoPendiente = true, error = null) }
            return
        }
        if (compra.purchaseState != Purchase.PurchaseState.PURCHASED) return
        _estado.update { it.copy(pagoPendiente = false) }

        if (Productos.RECARGA_CV in compra.products) {
            // Primero se acredita y DESPUÉS se consume. Antes era al revés y además
            // el backend nunca se enteraba: quien compraba una recarga la perdía al
            // reiniciar la app y el servidor le seguía diciendo que no tenía saldo.
            // Mientras no se acredite (sin red, backend caído), la compra queda sin
            // consumir y restaurarCompras() la vuelve a intentar en la siguiente apertura.
            if (acreditarRecarga(compra.purchaseToken)) {
                cliente.consumePurchase(
                    ConsumeParams.newBuilder().setPurchaseToken(compra.purchaseToken).build()
                )
            }
            return
        }

        if (!compra.isAcknowledged) {
            cliente.acknowledgePurchase(
                AcknowledgePurchaseParams.newBuilder()
                    .setPurchaseToken(compra.purchaseToken)
                    .build()
            )
        }
        if (Productos.PASE_COMPLETO in compra.products) {
            _estado.update { it.copy(tienePase = true) }
        }
        if (Productos.PASE_LECTURA in compra.products) {
            _estado.update { it.copy(tieneLectura = true) }
        }
    }

    /** El token se manda al backend, que lo verifica contra Google antes de gastar tokens de IA. */
    suspend fun tokenDelPase(): String? {
        val resultado = cliente.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
        )
        return resultado.purchasesList
            .firstOrNull { Productos.PASE_COMPLETO in it.products }
            ?.purchaseToken
    }
}
