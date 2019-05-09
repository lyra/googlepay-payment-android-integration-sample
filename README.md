# Google Pay Payment integration example

## Summary

The aim of this repository is to explain how you can implement GooglePay mobile payment method easily. For more detailed information, please refer to the official documentation.

## Table of contents

* [How it works](#how_it_is_work)
* [Getting started](#getting_started)
* [Technology](#technology)
* [Troubleshooting](#troubleshooting)
* [Copyright and license](#copyright)

<span id="how_it_is_work"></span>
## How it works

To be able to do some payments, two elements are required:
* A contract with your Payment service provider
* A mobile app with a GooglePay integration: this integration is explained with this repository
* A merchant server that executes payments transactions with web-services: [merchant server demonstration](https://github.com/lyra/googlepay-payment-sparkjava-integration-sample)

<span id="getting_started"></span>
## Getting started

### Execute this sample

1. See merchant server repo, `https://github.com/lyra/googlepay-payment-sparkjava-integration-sample`. Follow steps of getting started chapter and run your server

2. Clone the repo, `git clone https://github.com/lyra/googlepay-payment-android-integration-sample.git`. 

3. Open the project under Android Studio

4. Edit the following fields in `MainActivity.kt`
    - SERVER_URL: replace by your merchant server url
    - GATEWAY_MERCHANT_ID: change by your right identifier
    - Check others FIXME to customize your GooglePay usage

5. Run it and that's all! :)


### Integration in an existing app

1. See merchant server repo, `https://github.com/lyra/googlepay-payment-sparkjava-integration-sample`. Follow steps of getting started chapter and run your server

2. Copy-paste `com.lyra.sampleandroidgooglepay.payment` package into your project

3. Add the following code  into your AndroidManifest file

    ```xml
    <uses-permission android:name="android.permission.INTERNET" />`
    ```
    
    In application part:
    ```xml
    <meta-data
        android:name="com.google.android.gms.wallet.api.enabled"
        android:value="true" />
    ```

4. Add the following dependencies into your build.gradle of app folder

    ```gradle
    implementation 'com.google.android.gms:play-services-wallet:16.0.0'
    implementation 'com.android.support:appcompat-v7:24.1.1'
    ```

5. In your activity where you want execute a payment, add an inheritance of this activity to `AbstractLyraActivity`.

	Example:
	```kotlin
	class MainActivity: AbstractLyraActivity() {
	```

6. Initialize the payment process by executing `LyraPayment.init(activity: Activity, mode: String, supportedNetworks: String)` method.
    - supportedNetworks: corresponds to your supported payment method (AMEX, DISCOVER, JCB, MASTERCARD, VISA). Ensure that you are able to process this payment methods with your PSP.
    - mode: corresponds to your targeted environment (TEST or PRODUCTION).

	Example:
	```kotlin
	val paymentsClient = LyraPayment.init(this, "TEST", "AMEX, VISA, MASTERCARD, DISCOVER, JCB")
	``` 

7. Check that payment is possible by executing `LyraPayment.isPaymentPossible(paymentsClient: PaymentsClient)` method. `paymentsClient` is the result of previous `init` method.

	Example:
	```kotlin
	LyraPayment.isPaymentPossible(paymentsClient).addOnCompleteListener { task ->
        try {
            val result = task.getResult(ApiException::class.java)
            if (result) {
                // show Google Pay button as a payment option
                payBtn.visibility = View.VISIBLE
            } else {
                Toast.makeText(this, "isPaymentPossible return false", Toast.LENGTH_LONG).show()
            }
        } catch (e: ApiException) {
            Toast.makeText(this, "isPaymentPossible exception catched", Toast.LENGTH_LONG).show()
        }
    }

	``` 

8. Execute `LyraPayment.execute(payload: PaymentData, serverUrl: String, gatewayMerchantId: String, activity: Activity, paymentsClient: PaymentsClient)` method by providing the following fields:
	- serverUrl: replace by your merchant server url
	- gatewayMerchantId: change by your right identifier
	- paymentsClient: result of `init` method
    - payload: PaymentData that represents your payment data:
		- orderId: *optional*, your order identifier
		- amount: *mandatory*, the related amount
		- email: *optional*, email
		- currency: *mandatory*, currency code, https://en.wikipedia.org/wiki/ISO_4217
		
	Example:
	```kotlin
	val payload = PaymentData()
    payload.setOrderId(orderText.text.toString())
    payload.setAmount(amountText.text.toString())
    payload.setEmail(emailText.text.toString())
    payload.setMode("TEST")
	payload.setCurrency("978")
		
	LyraPayment.execute(payload, "http://my-merchant-server", "my-gateway-merchantId", this, paymentsClient)
	``` 
	
9. Implement `handlePaymentResult(result: PaymentResult)` in order to handle the payment result.

    Example:
    ```kotlin
    override fun handlePaymentResult(result: PaymentResult) {
        if (result.isSuccess()) {
            Toast.makeText(this, "Payment successful" , Toast.LENGTH_LONG).show()
        } else {
            Toast.makeText(this, "Payment failed. errorCode = " + result.getErrorCode() + " and cause = " + result.getCause(), Toast.LENGTH_LONG).show()
        }
    }

    ```

10. Run it and that's all! :)


Lyra gateway only supports Google Pay API version 2.0


Fore more information about integration: https://developers.google.com/pay/api/android/guides/setup


<span id="technology"></span>	
## Technology

Developped in Android Studio 3.2, written in Kotlin 1.2.70, this sample app requires Android API 19 or superior.

<span id="troubleshooting"></span>	
## Troubleshooting

The following errors can occurred:

| Error  | Code | Cause |
| ------------- | ------------- | ------------- |
| UNKNOWN_ERROR  | 1 | An unknown error has occurred. This error can occur when the url of merchant server is incorrect. Check that your url is syntactically correct. |
| TIMEOUT_ERROR  | 2 | A timeout error has occurred. This error can occur when your mobile is not able to communicate with your merchant server. Check that your server is up and is reachable. |
| NO_CONNECTION_ERROR  | 3 | A no connection error has occurred. This error can occur when your mobile is not connected to Internet (by Wifi or by mobile network). Check your mobile connection | 
| SERVER_ERROR  | 4 | A server error has occurred. This error can occur when your merchant server returns an invalid data. Check that your payment data sent are correct. |
| PAYMENT_CANCELLED_ERROR  | 5 | A payment cancelled error has occurred. This error can occur when user cancels he payment process. |
| PAYMENT_REFUSED_ERROR  | 6 | A payment refused error has occurred. This error can occur when payment is refused. Check the credit card used. |
| PAYMENT_DATA_ERROR  | 7 | A payment data error has occurred. This error can occur when payment data are incorrect. Check that all required data are correct. |

<span id="copyright"></span>
## Copyright and license
	The MIT License

	Permission is hereby granted, free of charge, to any person obtaining a copy
	of this software and associated documentation files (the "Software"), to deal
	in the Software without restriction, including without limitation the rights
	to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
	copies of the Software, and to permit persons to whom the Software is
	furnished to do so, subject to the following conditions:

	The above copyright notice and this permission notice shall be included in
	all copies or substantial portions of the Software.

	THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
	IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
	FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
	AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
	LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
	OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
	THE SOFTWARE.