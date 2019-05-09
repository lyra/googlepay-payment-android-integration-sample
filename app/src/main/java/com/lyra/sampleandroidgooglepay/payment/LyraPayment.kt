package com.lyra.sampleandroidgooglepay.payment

import android.annotation.TargetApi
import android.app.Activity
import android.os.AsyncTask
import android.os.Build
import android.system.ErrnoException
import com.google.android.gms.tasks.Task
import com.google.android.gms.wallet.PaymentsClient
import com.lyra.sampleandroidgooglepay.BuildConfig
import org.json.JSONException
import org.json.JSONObject
import java.io.*
import java.net.*
import java.util.*


/**
 * Lyra payment error codes
 */
object LyraPaymentErrorCode {
    // Unknown error
    const val UNKNOWN_ERROR = 1

    // Timeout error
    const val TIMEOUT_ERROR = 2

    // No connection error
    const val NO_CONNECTION_ERROR = 3

    // Server error
    const val SERVER_ERROR = 4

    // Payment cancelled error
    const val PAYMENT_CANCELLED_ERROR = 5

    // Payment refused error
    const val PAYMENT_REFUSED_ERROR = 6

    // Payment data error
    const val PAYMENT_DATA_ERROR = 7
}

/**
 * PaymentResult to store payment result
 */
class PaymentResult : JSONObject(){
    fun getErrorCode(): Int {
        return this.getInt("errorCode")
    }
    fun setErrorCode(errorCode: Int?){
        this.put("errorCode", errorCode)
    }

    fun getCause(): String {
        return this.getString("cause")
    }
    fun setCause(cause: String?){
        this.put("cause", cause)
    }

    fun isSuccess(): Boolean {
        return this.getBoolean("success")
    }
    fun setSuccess(success: Boolean){
        this.put("success", success)
    }
}

/**
 * PaymentData to store payment data
 */
class PaymentData : JSONObject() {

    init {
        this.put("language", Locale.getDefault().language)
    }

    fun setOrderId(orderId: String?) {
        if(!orderId.isNullOrEmpty()) this.put("orderId", orderId)
    }
    fun setAmount(amount: String?) {
        if(!amount.isNullOrEmpty()) this.put("amount", amount)
    }
    fun setEmail(email: String?) {
        if(!email.isNullOrEmpty()) this.put("email", email)
    }
    fun setCurrency(currency: String?) {
        if(!currency.isNullOrEmpty()) this.put("currency", currency)
    }

    fun getAmount(): String {
        return this.getString("amount")
    }
    fun getCurrency(): String {
        return this.getString("currency")
    }
}

//Timeout before connection to merchant server expire
private const val CONNECTION_TIMEOUT = 15000

/**
 * Util class that manage the bridge with Google pay
 * execute method calls Google Pay and then on result calls merchant server
 */
object LyraPayment {
    private lateinit var serverUrl: String
    lateinit var mode: String
    private lateinit var payload: JSONObject

    /**
     * Executes Google Pay
     *
     * @param payload PaymentData
     * @param serverUrl String
     * @param gatewayMerchantId String
     * @param activity Activity
     * @param paymentsClient PaymentsClient
     */
    fun execute(payload: PaymentData, serverUrl: String, gatewayMerchantId: String, activity: Activity, paymentsClient: PaymentsClient) {
        GooglePayManagement.execute(payload.getAmount(), payload.getCurrency(), mode, gatewayMerchantId, activity, paymentsClient)
        this.serverUrl = serverUrl
        this.payload = payload
    }

    /**
     * Initializes Google Pay context
     *
     * @param activity Activity
     * @param mode String
     * @param supportedNetworks String
     */
    fun init(activity: Activity, mode: String, supportedNetworks: String): PaymentsClient {
        this.mode = mode
        return GooglePayManagement.init(activity, mode, supportedNetworks)
    }

    /**
     * Determines if Google Pay payment is possible
     *
     * @param paymentsClient PaymentsClient
     * @return Task<Boolean>
     */
    fun isPaymentPossible(paymentsClient: PaymentsClient): Task<Boolean> {
        return GooglePayManagement.isPossible(paymentsClient)
    }

    /**
     * Executes transaction on merchant server
     *
     * @param googlePayData String
     */
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    fun executeTransaction(googlePayData: String, activity: Activity) {
        doAsync {
            try {
                // Payload payment part
                val lyraPayload = JSONObject()
                val deviceInformation = JSONObject()
                        .put("deviceName", Build.MANUFACTURER + " " + Build.MODEL)
                        .put("os", "Android")
                        .put("osVersion", Build.VERSION.RELEASE.replace(".", "").map { it })
                        .put("sdkVersion", Build.VERSION.SDK_INT.toString())
                        .put("isMobile", true)
                val appVersion = JSONObject()
                        .put("applicationId", BuildConfig.APPLICATION_ID)
                        .put("versionCode", BuildConfig.VERSION_CODE.toString())
                        .put("versionName", BuildConfig.VERSION_NAME)

                payload.put("deviceInformation", deviceInformation)
                payload.put("appVersion", appVersion)
                payload.put("mode", mode)
                lyraPayload.put("createPaymentData", payload)

                // Payload wallet part
                lyraPayload.put("walletPayload", JSONObject(googlePayData))

                val conn = URL(serverUrl).openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-type", "application/json")
                conn.setRequestProperty("Accept", "*/*")
                conn.doInput = true
                conn.doOutput = true
                conn.connectTimeout = CONNECTION_TIMEOUT

                val os = conn.outputStream
                val writer = BufferedWriter(OutputStreamWriter(os, "UTF-8"))
                writer.write(lyraPayload.toString())
                writer.flush()
                writer.close()
                os.close()

                conn.connect()

                val out = OutputStreamWriter(conn.outputStream)


                if (conn.responseCode in 200..299) {
                    // If response is OK
                    val content = BufferedReader(InputStreamReader(conn.inputStream)).use(BufferedReader::readText)
                    activity.runOnUiThread {
                        try {
                            // Check transaction status
                            val transactionStatus = JSONObject(content).get("transactionStatus")
                            if (transactionStatus == "AUTHORISED") {
                                returnsResult(true, null, null, activity)
                            } else {
                                returnsResult(false, LyraPaymentErrorCode.PAYMENT_REFUSED_ERROR, "Payment refused", activity)
                            }
                        } catch (e: JSONException) {
                            returnsResult(false, LyraPaymentErrorCode.SERVER_ERROR, conn.responseCode.toString(), activity)
                        }
                    }
                } else {
                    // If response is KO
                    val content = BufferedReader(InputStreamReader(conn.errorStream)).use(BufferedReader::readText)
                    activity.runOnUiThread {
                        try {
                            returnsResult(false, LyraPaymentErrorCode.SERVER_ERROR, conn.responseCode.toString() + ":" + JSONObject(content).get("errorMessage"), activity)
                        } catch (e: JSONException) {
                            returnsResult(false, LyraPaymentErrorCode.SERVER_ERROR, conn.responseCode.toString(), activity)
                        }
                    }
                }

                out.close()
            } catch (e: SocketTimeoutException) {
                activity.runOnUiThread {
                    returnsResult(false, LyraPaymentErrorCode.TIMEOUT_ERROR, "", activity)
                }
            } catch (e: ErrnoException) {
                activity.runOnUiThread {
                    returnsResult(false, LyraPaymentErrorCode.NO_CONNECTION_ERROR, "", activity)
                }
            } catch (e: IOException) {
                activity.runOnUiThread {
                    returnsResult(false, LyraPaymentErrorCode.UNKNOWN_ERROR, e.message, activity)
                }
            }

        }.execute()
    }

    /**
     * Returns payment result to main activity
     *
     * @param value Boolean
     * @param errorCode Int?
     * @param cause String?
     */
    fun returnsResult(value: Boolean, errorCode: Int?, cause: String?, activity: Activity) {
        (activity as AbstractLyraActivity).handlePaymentResult(constructPaymentResult(value, errorCode, cause))
    }

    /**
     * Construct JSONObject from result parts
     *
     * @param value Boolean
     * @param errorCode Int?
     * @param cause String?
     * @return PaymentResult
     */
    private fun constructPaymentResult(value: Boolean, errorCode: Int?, cause: String?): PaymentResult {
        val result = PaymentResult()
        result.setSuccess(value)
        if (!value) {
            result.setErrorCode(errorCode)
            result.setCause(cause)
        }

        return result
    }

    /**
     * Allow to execute HTTP call outside UI thread
     * @property handler Function0<Unit>
     * @constructor
     */
    class doAsync(val handler: () -> Unit) : AsyncTask<Void, Void, Void>() {
        override fun doInBackground(vararg params: Void?): Void? {
            handler()
            return null
        }
    }
}