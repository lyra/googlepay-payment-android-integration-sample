package com.lyra.sampleandroidgooglepay.payment

import android.app.Activity
import android.content.Intent
import android.support.v7.app.AppCompatActivity
import com.google.android.gms.wallet.AutoResolveHelper
import com.google.android.gms.wallet.PaymentData

/**
 * AbstractLyraActivity
 */
abstract class AbstractLyraActivity: AppCompatActivity() {
    /**
     * Handle payment result
     *
     * @param result PaymentResult
     */
    abstract fun handlePaymentResult(result: PaymentResult)

    /**
     * Allow to retrieve to payment status
     *
     * @param requestCode Int
     * @param resultCode Int
     * @param data Intent?
     */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        // Manage Google Pay result
        if (requestCode == GooglePayManagement.GOOGLE_PAYMENT_CODE_RESULT) {
            when (resultCode) {
                Activity.RESULT_OK -> {
                    if (data != null) {
                        val paymentData = PaymentData.getFromIntent(data)
                        val googlePayData = paymentData!!.toJson()
                        // Execute payment
                        LyraPayment.executeTransaction(googlePayData, this)
                    } else {
                        LyraPayment.returnsResult(false, LyraPaymentErrorCode.UNKNOWN_ERROR, "Unknown error", this)
                    }
                }
                Activity.RESULT_CANCELED -> {
                    LyraPayment.returnsResult(false, LyraPaymentErrorCode.PAYMENT_CANCELLED_ERROR, "Payment cancelled by user", this)
                }
                AutoResolveHelper.RESULT_ERROR -> {
                    LyraPayment.returnsResult(false, LyraPaymentErrorCode.UNKNOWN_ERROR, "Unknown error", this)
                }
            }
        }
    }
}