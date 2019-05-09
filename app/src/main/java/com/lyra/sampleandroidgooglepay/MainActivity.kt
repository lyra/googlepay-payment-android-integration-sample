package com.lyra.sampleandroidgooglepay

import android.os.Bundle
import android.view.View
import android.widget.Toast
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.wallet.PaymentsClient
import com.lyra.sampleandroidgooglepay.payment.AbstractLyraActivity
import com.lyra.sampleandroidgooglepay.payment.LyraPayment
import com.lyra.sampleandroidgooglepay.payment.PaymentData
import com.lyra.sampleandroidgooglepay.payment.PaymentResult
import kotlinx.android.synthetic.main.activity_main.*


// Merchant server url
// FIXME: change by the right payment server
private const val SERVER_URL = "<REPLACE_ME>"

// Gateway merchant ID
// FIXME: change by the right identifier
private const val GATEWAY_MERCHANT_ID = "<REPLACE_ME>"

// One or more card networks you support also supported by the Google Pay API
// FIXME: change by what you can supported
private const val SUPPORTED_NETWORKS = "AMEX, VISA, MASTERCARD, DISCOVER, JCB"

// Environment TEST or PRODUCTION, refer to documentation for more information
// FIXME: change by your targeted environment
private const val PAYMENT_MODE = "TEST"

/**
 * Main activity
 *
 * This main activity allows to user to fill payment data (amount, order id, so on)
 * Before retrieving these payment data:
 * <li>Initialize payment context with LyraPayment.init(activity: Activity, mode: String, supportedNetworks: String) method</li>
 * <li>Check payment possibility with LyraPayment.isPaymentPossible(paymentsClient: PaymentsClient) method</li>
 * After retrieving these payment data:
 * <li>LyraPayment.execute(payload: JSONObject, serverUrl: String, gatewayMerchantId: String, activity: Activity, paymentsClient: PaymentsClient) is executed</li>.
 * <li>The payment result is handled by handlePaymentResult(result: PaymentResult) method</li>
 *
 * For readability purposes in this example, we do not use logs
 * @author Lyra Network
 */
class MainActivity : AbstractLyraActivity() {

    private lateinit var paymentsClient: PaymentsClient

    /**
     * onCreate method
     * Activity creation
     *
     * @param savedInstanceState Bundle?
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Environment TEST or PRODUCTION
        paymentsClient = LyraPayment.init(this, PAYMENT_MODE, SUPPORTED_NETWORKS)

        LyraPayment.isPaymentPossible(paymentsClient).addOnCompleteListener { task ->
            try {
                val result = task.getResult(ApiException::class.java)
                if (result) {
                    // show Google Pay as a payment option
                    payBtn.visibility = View.VISIBLE
                } else {
                    Toast.makeText(this, "isPaymentPossible return false", Toast.LENGTH_LONG).show()
                }
            } catch (e: ApiException) {
                Toast.makeText(this, "isPaymentPossible exception catched", Toast.LENGTH_LONG).show()
            }

        }
    }

    /**
     * Create PaymentData object used as payload of HTTP request to Merchant server
     *
     * @return PaymentData
     */
    private fun createPaymentPayload(): PaymentData {

        val paymentData = PaymentData()
        paymentData.setOrderId(orderText.text.toString())
        paymentData.setAmount(amountText.text.toString())
        paymentData.setEmail(emailText.text.toString())

        // Specify the currency code
        // For example, for Euro currency, use "978" value
        // See: https://en.wikipedia.org/wiki/ISO_4217
        paymentData.setCurrency("978")

        return paymentData
    }

    /**
     * onPayClick method
     * Payment execution
     *
     * @param view View Pay button
     */
    fun onPayClick(view: View) {
        progressBar.visibility = View.VISIBLE

        LyraPayment.execute(createPaymentPayload(), SERVER_URL, GATEWAY_MERCHANT_ID, this, paymentsClient)
    }

    /**
     * Handle payment result
     *
     * @param result PaymentResult
     */
    override fun handlePaymentResult(result: PaymentResult) {
        progressBar.visibility = View.GONE
        if (result.isSuccess()) {
            Toast.makeText(this, "Payment successful", Toast.LENGTH_LONG).show()
        } else {
            Toast.makeText(this, "Payment failed. errorCode = " + result.getErrorCode() + " and cause = " + result.getCause(), Toast.LENGTH_LONG).show()
        }
    }
}