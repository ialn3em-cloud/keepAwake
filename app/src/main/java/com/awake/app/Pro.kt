package com.awake.app

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams

/**
 * The Pro upgrade: a one-time Google Play purchase that unlocks automatic apps.
 * The product "pro_unlock" must exist in Play Console (Monetize ← In-app products).
 */
object Pro {
    const val PRODUCT_ID = "pro_unlock"
    private const val PREFS = "pro"
    private const val KEY_UNLOCKED = "unlocked"

    private var client: BillingClient? = null
    private var details: ProductDetails? = null
    private val main = Handler(Looper.getMainLooper())
    private val listeners = mutableSetOf<() -> Unit>()

    /** Localized price from Google Play, or null until it is known (or outside Google Play). */
    var price: String? = null
        private set

    fun isUnlocked(context: Context): Boolean =
        BuildConfig.PRO_ALWAYS || prefs(context).getBoolean(KEY_UNLOCKED, false)

    fun addListener(listener: () -> Unit) { listeners.add(listener) }
    fun removeListener(listener: () -> Unit) { listeners.remove(listener) }

    /** Connect to Google Play, restore an earlier purchase and load the price. Safe to call often. */
    fun start(context: Context) {
        if (BuildConfig.PRO_ALWAYS) return
        val app = context.applicationContext
        val c = client ?: BillingClient.newBuilder(app)
            .setListener { result, purchases ->
                if (result.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
                    handle(app, purchases, complete = false)
                }
            }
            .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
            .build()
            .also { client = it }

        if (c.isReady) {
            restore(app)
            return
        }
        if (c.connectionState == BillingClient.ConnectionState.CONNECTING) return
        c.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    restore(app)
                    loadPrice()
                }
            }

            // reconnects on the next start(), which every screen calls when it opens
            override fun onBillingServiceDisconnected() {}
        })
    }

    /** Opens Google Play's purchase sheet. False when the store is not reachable (e.g. not installed from Google Play). */
    fun buy(activity: Activity): Boolean {
        val c = client ?: return false
        val d = details ?: return false
        val params = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(
                listOf(BillingFlowParams.ProductDetailsParams.newBuilder().setProductDetails(d).build())
            )
            .build()
        return c.launchBillingFlow(activity, params).responseCode == BillingClient.BillingResponseCode.OK
    }

    private fun restore(app: Context) {
        val c = client ?: return
        val params = QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.INAPP).build()
        c.queryPurchasesAsync(params) { result, purchases ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) handle(app, purchases, complete = true)
        }
    }

    private fun loadPrice() {
        val c = client ?: return
        val product = QueryProductDetailsParams.Product.newBuilder()
            .setProductId(PRODUCT_ID)
            .setProductType(BillingClient.ProductType.INAPP)
            .build()
        val params = QueryProductDetailsParams.newBuilder().setProductList(listOf(product)).build()
        c.queryProductDetailsAsync(params) { result, found ->
            if (result.responseCode != BillingClient.BillingResponseCode.OK) return@queryProductDetailsAsync
            details = found.productDetailsList.firstOrNull()
            price = details?.oneTimePurchaseOfferDetails?.formattedPrice
            notifyListeners()
        }
    }

    /** complete = this is the full list of what the user owns, so a missing purchase means a refund. */
    private fun handle(app: Context, purchases: List<Purchase>, complete: Boolean) {
        val owned = purchases.filter {
            PRODUCT_ID in it.products && it.purchaseState == Purchase.PurchaseState.PURCHASED
        }
        // Google refunds purchases that are not acknowledged within 3 days
        owned.filter { !it.isAcknowledged }.forEach { purchase ->
            val params = AcknowledgePurchaseParams.newBuilder().setPurchaseToken(purchase.purchaseToken).build()
            client?.acknowledgePurchase(params) {}
        }
        if (owned.isNotEmpty()) setUnlocked(app, true) else if (complete) setUnlocked(app, false)
    }

    private fun setUnlocked(app: Context, unlocked: Boolean) {
        if (prefs(app).getBoolean(KEY_UNLOCKED, false) == unlocked) return
        prefs(app).edit().putBoolean(KEY_UNLOCKED, unlocked).apply()
        main.post {
            WatcherService.sync(app)
            notifyListeners()
        }
    }

    private fun notifyListeners() {
        main.post { listeners.toList().forEach { it() } }
    }

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
