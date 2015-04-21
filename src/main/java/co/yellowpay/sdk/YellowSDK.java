package co.yellowpay.sdk;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.HttpClients;
import org.json.simple.JSONValue;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;

/**
 *
 * @author Mahmoud
 */
public class YellowSDK {
    /**
     * sdk version
     */
    private final String VERSION = "0.1";

    /**
     * Server Root for Yellow API
     *
     * @var String
     */
    private String serverRoot = "https://api.yellowpay.co/v1";

    /**
     * create invoice URI
     *
     * @var String
     */
    private final String API_URI_CREATE_INVOICE = "/invoice/";

    /**
     * check invoice status URI
     *
     * @var String
     */
    private final String API_URI_CHECK_PAYMENT = "/invoice/[id]/";

    /**
     * api key
     *
     * @var String
     */
    private final String API_KEY;

    /**
     * api secret
     *
     * @var String
     */
    private final String API_SECRET;

    /**
     * constructor method
     * @note : if we want to use custom API server , we will read it automatically from EVN var
     * @param api_key => public key
     * @param api_secret => private key
     */
    public YellowSDK(String api_key, String api_secret) {
        // set custom API server
        String customServerRoot = System.getenv("YELLOW_API_SERVER");
        if( customServerRoot != null ){
            this.serverRoot = customServerRoot;
        }
        
        this.API_KEY    = api_key;
        this.API_SECRET = api_secret;
    }
    
    /**
     *
     * @param payload array for all api parameters like: amount, currency, callback ... etc.
     * @return HashMap of the response
     */
    public HashMap<String, String> createInvoice(Map<String, Object> payload)
    {
        String url  = this.serverRoot + this.API_URI_CREATE_INVOICE;
        String response;
        HashMap<String, String> responseMap = new HashMap<String, String>();
        
        try {
            response = this.makeHTTPRequest("POST", url, payload);
            responseMap = (HashMap)JSONValue.parse(response);
        } catch (IOException ex) {
            Logger.getLogger(YellowSDK.class.getName()).log(Level.SEVERE, null, ex);
        }
                
        return responseMap;
    }

    /**
     * check invoice status
     * @param id
     * @return HashMap of the response
     */
    public HashMap<String, String> checkInvoiceStatus(String id)
    {
        String url  = this.serverRoot + (this.API_URI_CHECK_PAYMENT).replace("[id]", id);
        String response;
        HashMap<String, String> responseMap = new HashMap<String, String>();
        
        try {
            response = this.makeHTTPRequest("GET", url, new HashMap<String, Object>());
            responseMap = (HashMap)JSONValue.parse(response);
        } catch (IOException ex) {
            response = ex.getMessage();
            Logger.getLogger(YellowSDK.class.getName()).log(Level.SEVERE, null, ex);
        }
                
        return responseMap;
    }
    
    /**
     * creates the http data array for both createInvoice / checkInvoiceStatus
     *
     * @param url url used to create signature
     * @param payload payload array
     * @return String
     */
    private String makeHTTPRequest(String type, String url, Map<String, Object> payload) 
            throws UnsupportedEncodingException, IOException
    {
        CloseableHttpClient httpclient = HttpClients.createDefault();
        
        HttpRequestBase httpRequest;
        switch (type) {
            case "POST":
                httpRequest = new HttpPost(url);
                break;
            case "GET":
                httpRequest = new HttpGet(url);
                break;
            default:
                throw new IllegalArgumentException();
        }

        String nonce = String.valueOf(System.currentTimeMillis());
        String body = "";
        String message;
        
        if( !payload.isEmpty() ){
            body        = JSONValue.toJSONString(payload);
            message     = nonce + url + body;
        }else{
            message     = nonce + url;
        }
        
        String signature = this.signMessage(message);
        
        // add header
        httpRequest.setHeader("API-Key", this.API_KEY);
        httpRequest.setHeader("API-Nonce", nonce);
        httpRequest.setHeader("API-Sign", signature);
        httpRequest.setHeader("API-Platform", this.getPlatformDetails());
        httpRequest.setHeader("API-Plugin", this.VERSION);
        httpRequest.setHeader("content-type", "application/json");

        // add body
        if( body.length() > 0 ){
            StringEntity entity = new StringEntity(body);
            ((HttpPost)httpRequest).setEntity(entity);
        }
        
        // handle response
        StringBuilder result;
        try (CloseableHttpResponse response = httpclient.execute(httpRequest)) {
            BufferedReader rd = new BufferedReader(
                    new InputStreamReader(response.getEntity().getContent()));
            result = new StringBuilder();
            String line;
            while ((line = rd.readLine()) != null) {
                result.append(line);
            }
        }
        
        return result.toString();
    }

    /**
     * get Platform(OS / Java) details
     *
     * @return string
     */
    private String getPlatformDetails()
    {
        String osName = System.getProperty("os.name");
        String osArch = System.getProperty("os.arch");
        String osVersion = System.getProperty("os.version");
        String javaVersion = System.getProperty("java.version");
        return osName + " " + osArch  + " " + osVersion + " - Java " + javaVersion;
    }

    /**
     * sign message using HmacSHA256 method
     * @param $message string
     * @return string
     */
    private String signMessage(String message)
    {
        return hmacDigest("HmacSHA256", message, this.API_SECRET);
    }
    
    /**
     * hmacDigest
     * @param algo string
     * @param msg string
     * @param keyString string
     * @return string
     */
    private String hmacDigest(String algo, String msg, String keyString) {
        String digest = null;
        try {
          SecretKeySpec key = new SecretKeySpec((keyString).getBytes("UTF-8"), algo);
          Mac mac = Mac.getInstance(algo);
          mac.init(key);

          byte[] bytes = mac.doFinal(msg.getBytes("ASCII"));

          StringBuilder hash = new StringBuilder();
          for (int i = 0; i < bytes.length; i++) {
            String hex = Integer.toHexString(0xFF & bytes[i]);
            if (hex.length() == 1) {
              hash.append('0');
            }
            hash.append(hex);
          }
          digest = hash.toString();
        } catch (UnsupportedEncodingException | InvalidKeyException | NoSuchAlgorithmException e) {
        }
        
        return digest;
    }

    /**
     * Validate IPN
     *
     * @param url string
     * @param signature string
     * @param nonce string
     * @param body
     * @return boolean
     */
    public boolean verifyIPN(String url, String signature, String nonce, String body)
    {
        if ( url == null || url.isEmpty() || 
             signature == null || signature.isEmpty() ||
             nonce == null || nonce.isEmpty() || 
             body == null || body.isEmpty() ){
            // missing headers OR an empty payload
            return false;
        }
        
        String message = nonce + url + body;
        String calculated_signature = this.signMessage(message);
        
        // valid or invalid IPN call
        return !calculated_signature.equals(signature);
    }

}
