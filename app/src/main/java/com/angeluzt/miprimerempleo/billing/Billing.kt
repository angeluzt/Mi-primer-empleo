package com.angeluzt.miprimerempleo.billing

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ConsumeParams
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
    /** Pago único que desbloquea los 9 módulos y todas las herramientas. No consumible. */
    const val PASE_COMPLETO = "pase_completo"

    /** Consumible: 10 generaciones más de CV para quien agote las incluidas. */
    const val RECARGA_CV = "recarga_cv_10"

    /** Consumible: paquete de plantillas extra de CV. */
    const val PLANTILLAS_EXTRA = "plantillas_extra"

    val todos = listOf(PASE_COMPLETO, RECARGA_CV, PLANTILLAS_EXTRA)
    val consumibles = setOf(RECARGA_CV, PLANTILLAS_EXTRA)

    /** Generaciones de CV incluidas en el pase, suficientes para un proceso de búsqueda normal. */
    const val CVS_INCLUIDOS_EN_PASE = 15
    const val CVS_POR_RECARGA = 10
}

data class EstadoCompras(
    val conectado: Boolean = false,
    val tienePase: Boolean = false,
    val recargasCompradas: Int = 0,
    val precios: Map<String, String> = emptyMap(),
    val error: String? = null,
) {
    fun creditosCv(cvsGenerados: Int): Int {
        if (!tienePase) return 0
        val total = Productos.CVS_INCLUIDOS_EN_PASE + recargasCompradas * Productos.CVS_POR_RECARGA
        return (total - cvsGenerados).coerceAtLeast(0)
    }
}

class GestorCompras(context: Context) {

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

            else ->
                _estado.update { it.copy(error = "No se pudo completar la compra.") }
        }
    }

    private val cliente = BillingClient.newBuilder(context)
        .setListener(escucha)
        .enablePendingPurchases()
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
        _estado.update { estado ->
            estado.copy(tienePase = activas.any { Productos.PASE_COMPLETO in it.products })
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
        if (compra.purchaseState != Purchase.PurchaseState.PURCHASED) return

        val esConsumible = compra.products.any { it in Productos.consumibles }
        if (esConsumible) {
            cliente.consumePurchase(
                ConsumeParams.newBuilder().setPurchaseToken(compra.purchaseToken).build()
            )
            if (Productos.RECARGA_CV in compra.products) {
                _estado.update { it.copy(recargasCompradas = it.recargasCompradas + 1) }
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
