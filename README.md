Yellow Java SDK
=====================
This is the Yellow Java SDK. This simple SDK contains couple of functions that makes it easy to integrate with the Yellow API. To get started just:
```
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
        YellowSDK yellowSDK = new YellowSDK(api_key, api_secret);
        
        //create invoice
        Map<String, Object> payload = new HashMap<>();
        payload.put("base_price", 1);
        payload.put("base_ccy", "USD");
        payload.put("callback", "http://yourdomain.local/sdk/sample/ipn.php");
        payload.put("type", "embedded");
        System.out.println(yellowSDK.createInvoice(payload));
    }
}
```

You should see something similar to the following in your terminal:

```
{
    "address": "155xsayoDXxRFP9rxDoecmpVUo7y5xKtc7", # Invoice Address
    "base_ccy": "USD",
    "base_price": "0.05",
    "callback": "https://example.com",
    "expiration": "2015-03-10T18:17:51.248Z", # Each invoice expires after 10 minutes of creation
    "id": "6dd264975861fddbfe4404ed995f1ca4", # Invoice ID (to query the invoice later if you need to!)
    "invoice_ccy": "BTC",
    "invoice_price": "0.00017070",
    "received": "0",
    "redirect": null,
    "remaining": "0.00017070",
    "server_time": "2015-03-10T18:07:51.454Z",
    "status": "new", # Status of the invoice. Other values are "authorizing" for unconfirmed transactions, and "paid" for confirmed transactions
    "url": "https://cdn.yellowpay.co/invoice.5f0d082e.html?invoiceId=6dd264975861fddbfe4404ed995f1ca4" # Direct URL for the invoice. You can use it to embed the invoice widget in an iframe on your website.
}
```

To query an invoice that you created, just pass in the `invoice_id` to the `checkInvoiceStatus` function

```
System.out.println(yellowSDK.checkInvoiceStatus(invoice id));
```
