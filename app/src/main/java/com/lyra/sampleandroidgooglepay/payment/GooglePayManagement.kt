package com.lyra.sampleandroidgooglepay.payment

import android.app.Activity
import com.google.android.gms.tasks.Task
import com.google.android.gms.wallet.*
import org.json.JSONArray
import org.json.JSONObject
import java.util.*

/**
 * Google Pay Management
 */
object GooglePayManagement {

    // Technical code used for specifying the result of WebView activity
    const val GOOGLE_PAYMENT_CODE_RESULT = 985

    private val SUPPORTED_METHODS = Arrays.asList(
            "PAN_ONLY"
    )!!

    // The name of your payment processor / gateway. Please refer to their documentation for
    // more information.
    private const val GATEWAY_TOKENIZATION_NAME = "lyra"

    private lateinit var supportedNetworks: Array<String>


    /**
     * Get allowed card networks
     */
    private fun getAllowedCardNetworks(): JSONArray {
        val allowedCardNetworks = JSONArray()

        for (network in supportedNetworks) {
            allowedCardNetworks.put(network)
        }

        return allowedCardNetworks
    }


    /**
     * Get allow card auth methods
     */
    private fun getAllowedCardAuthMethods(): JSONArray {
        val allowedCardAuthMethods = JSONArray()

        for (method in SUPPORTED_METHODS) {
            allowedCardAuthMethods.put(method)
        }

        return allowedCardAuthMethods
    }

    /**
     * Get card payment method
     * That uses getAllowedCardAuthMethods, getAllowedCardNetworks methods and maybe some additional parameters
     */
    private fun getCardPaymentMethod(additionalParams: JSONObject?, tokenizationSpecification: JSONObject?): JSONObject {
        val params = JSONObject()
                .put("allowedAuthMethods", getAllowedCardAuthMethods())
                .put("allowedCardNetworks", getAllowedCardNetworks())

        // Additional parameters provided?
        if (additionalParams != null && additionalParams.length() > 0) {
            val keys = additionalParams.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                params.put(key, additionalParams.get(key))
            }
        }

        val cardPaymentMethod = JSONObject()
        cardPaymentMethod.put("type", "CARD")
        cardPaymentMethod.put(
                "parameters", params)

        if (tokenizationSpecification != null) {
            cardPaymentMethod.put(
                    "tokenizationSpecification", tokenizationSpecification)
        }

        return cardPaymentMethod
    }

    /**
     * Create base request by specifying api version
     */
    private fun getBaseRequest(): JSONObject {
        return JSONObject()
                .put("apiVersion", 2)
                .put("apiVersionMinor", 0)
    }

    /**
     * Initializes PaymentsClient object
     *
     * @param activity Activity
     * @param mode String
     * @return PaymentsClient
     */
    private fun initPaymentsClient(activity: Activity, mode: String): PaymentsClient {
        val builder = Wallet.WalletOptions.Builder()
        if (mode == "TEST") {
            builder.setEnvironment(WalletConstants.ENVIRONMENT_TEST)
        } else {
            builder.setEnvironment(WalletConstants.ENVIRONMENT_PRODUCTION)
        }

        return Wallet.getPaymentsClient(activity, builder.build())
    }

    /**
     * Prepare payment data Request by creating a PaymentDataRequest object
     *
     * @param price String
     * @param currency String
     * @param gatewayMerchantId String
     * @return PaymentDataRequest
     */
    private fun preparePaymentDataRequest(price: String, currency: String, gatewayMerchantId: String): PaymentDataRequest {
        val paymentDataRequestJson = getBaseRequest()
        val additionalParams = JSONObject()

        val transactionJson = JSONObject()
        transactionJson
                .put("totalPriceStatus", "FINAL")
                .put("totalPrice", price)
                .put("currencyCode", currency)

        additionalParams.put("billingAddressRequired", true)

        additionalParams
                .put("billingAddressParameters", JSONObject()
                .put("format", "FULL").put("phoneNumberRequired", false))

        paymentDataRequestJson
                .put("allowedPaymentMethods", JSONArray()
                .put(getCardPaymentMethod(additionalParams, getTokenizationSpecification(gatewayMerchantId))))

        paymentDataRequestJson.put("shippingAddressRequired", true)
        paymentDataRequestJson.put("emailRequired", true)

        paymentDataRequestJson.put("transactionInfo", transactionJson)

        return PaymentDataRequest.fromJson(paymentDataRequestJson.toString())
    }

    /**
     * Get tokenization specification
     */
    private fun getTokenizationSpecification(gatewayMerchantId: String): JSONObject {
        val params = JSONObject().put("gateway", GATEWAY_TOKENIZATION_NAME)
        params.put("gatewayMerchantId", gatewayMerchantId)

        return JSONObject().put("type", "PAYMENT_GATEWAY").put("parameters", params)
    }

    /**
     * Prepare ready Request by creating an IsReadyToPayRequest object
     *
     * @return IsReadyToPayRequest
     */
    private fun prepareIsReadyToPayRequest(): IsReadyToPayRequest {
        val isReadyToPayRequest = getBaseRequest()
        isReadyToPayRequest.put(
                "allowedPaymentMethods", JSONArray()
                .put(getCardPaymentMethod(null, null)))
        return IsReadyToPayRequest.fromJson(isReadyToPayRequest.toString())
    }

    /**
     * Initializes payments client
     *
     * @param activity Activity
     * @param mode String
     * @param supportedNetworks String
     */
    internal fun init(activity: Activity, mode: String, supportedNetworks: String): PaymentsClient {
        this.supportedNetworks = supportedNetworks.split(Regex(",[ ]*")).toTypedArray()
        return initPaymentsClient(activity, mode)
    }

    /**
     * Determines if Google Pay is available
     *
     * @param paymentsClient PaymentsClient
     * @return Task<Boolean>
     */
    internal fun isPossible(paymentsClient: PaymentsClient): Task<Boolean> {
        val isReadyToPayRequest = prepareIsReadyToPayRequest()
        return paymentsClient.isReadyToPay(isReadyToPayRequest)
    }

    /**
     * Executes Google Pay
     *
     * @param amount String
     * @param currency String
     * @param mode String
     * @param gatewayMerchantId String
     * @param activity Activity
     * @param paymentsClient PaymentsClient
     */
    internal fun execute(amount: String?, currency: String?, mode: String?, gatewayMerchantId: String, activity: Activity, paymentsClient: PaymentsClient) {
        if (amount == null) {
            LyraPayment.returnsResult(false, LyraPaymentErrorCode.PAYMENT_DATA_ERROR, "amount is required", activity)
            return
        }

        if (mode == null) {
            LyraPayment.returnsResult(false, LyraPaymentErrorCode.PAYMENT_DATA_ERROR, "mode is required", activity)
            return
        }

        if (currency == null) {
            LyraPayment.returnsResult(false, LyraPaymentErrorCode.PAYMENT_DATA_ERROR, "currency is required", activity)
            return
        }

        val paymentDataRequest = preparePaymentDataRequest(amount, currency, gatewayMerchantId)
        AutoResolveHelper.resolveTask(paymentsClient.loadPaymentData(paymentDataRequest), activity, GOOGLE_PAYMENT_CODE_RESULT)
    }
}