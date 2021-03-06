Yellow Java SDK
=====================
This is the Yellow Java SDK. This simple SDK contains couple of functions that makes it easy to integrate with the Yellow API. To get started just:

```
git clone https://github.com/YellowPay/yellow-sdk-java.git yellow-sdk-java
cd yellow-sdk-java
mvn install
```

Examples
---------

To create invoice, create map with the invoice data then pass it to the `createInvoice` function

```
import co.yellowpay.sdk.YellowSDK;
import java.util.Map;
import java.util.HashMap;

public class Demo {
    
    public static void main(String[] args) {
        String apiKey = "<enter your api key>";
        String apiSecret = "<enter your api secret>";
        YellowSDK yellowSDK = new YellowSDK(apiKey, apiSecret);
        
        //create invoice
        Map<String, String> payload = new HashMap<>();
        payload.put("base_price", "1");
        payload.put("base_ccy", "USD");
        payload.put("callback", "https://yourdomain.local/sdk/sample/ipn.php");
        payload.put("type", "cart");
        System.out.println(yellowSDK.createInvoice(payload));
    }
}
```

You should see something similar to the following in your terminal:

```
{
    "server_time":"2015-04-24T02:15:43.671Z",
    "address":"1A58yrV7fF2LfQdcyCrCWpMpXABFSZM2HF", # Invoice Address
    "base_ccy":"USD",
    "invoice_ccy":"BTC",
    "received":"0",
    "type":"cart",
    "url":"\/\/cdn.yellowpay.co\/invoice.70d473ee.html?invoiceId=JNRWCN8CM7F2395J8B879K5AZV",
    "remaining":"0.00430256",
    "base_price":"1.00000000",
    "callback":"https:\/\/yourdomain.local\/sdk\/sample\/ipn.php",
    "expiration":"2015-04-24T02:25:43.588Z", # Each invoice expires after 10 minutes of creation
    "id":"JNRWCN8CM7F2395J8B879K5AZV", # Invoice ID (to query the invoice later if you need to!)
    "invoice_price":"0.00430256",
    "order":null,
    "status": "new", # Status of the invoice. Other values are "authorizing" for unconfirmed transactions, and "paid" for confirmed transactions
}
```

To query an invoice that you created, just pass in the `invoice_id` to the `checkInvoiceStatus` function

```
String invoiceId = "<enter your invoice id>";
System.out.println(yellowSDK.checkInvoiceStatus(invoiceId));
```

### IPN validation :
 To validate the IPN you can use the following snippet on your IPN page/controller. See [here](https://github.com/YellowPay/yellowdemo-java/blob/master/src/java/co/yellowpay/IPN.java#L38-L41) for one example of how to extract the url, signature, nonce and body from a request.

```
 String url = "<enter your callback url>";
 String signature = "<enter signature>";
 String nonce = "<enter nonce>";
 String body = "<enter body>";
 boolean isVerified = yellowSDK.verifyIPN(url, signature, nonce, body)
```

### Documentation

More information can be found in the online documentation at
http://yellowpay.co/docs/api/.
