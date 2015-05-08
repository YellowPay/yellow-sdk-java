package co.yellowpay.sdk;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Map;
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
     * SDK version
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
     * API key
     *
     * @var String
     */
    private final String API_KEY;

    /**
     * API secret
     *
     * @var String
     */
    private final String API_SECRET;

    /**
     * constructor method
     * if we want to use custom API server , we will read it automatically from EVN variable
     * @param apiKey public key
     * @param apiSecret private key
     */
    public YellowSDK(String apiKey, String apiSecret) {
        // set custom API server
        String customServerRoot = System.getenv("YELLOW_API_SERVER");
        if( customServerRoot != null ){
            this.serverRoot = customServerRoot;
        }
        
        this.API_KEY    = apiKey;
        this.API_SECRET = apiSecret;
    }
    
    /**
     *
     * @param payload array for all API parameters like: amount, currency, callback ... etc.
     * @return HashMap of the response
     * @throws co.yellowpay.sdk.YellowException
     */
    public HashMap<String, String> createInvoice(Map<String, String> payload) throws YellowException
    {
        String url  = this.serverRoot + this.API_URI_CREATE_INVOICE;
        String response;
        HashMap<String, String> responseMap = new HashMap<String, String>();
        
        try {
            response = this.makeHTTPRequest("POST", url, payload);
            responseMap = (HashMap)JSONValue.parse(response);
        } catch (IOException | NoSuchAlgorithmException | InvalidKeyException ex) {
            throw new YellowException(ex.getMessage());
        }
                
        return responseMap;
    }

    /**
     * check invoice status
     * @param id of the invoice wants to check
     * @return HashMap of the response
     * @throws co.yellowpay.sdk.YellowException
     */
    public HashMap<String, String> checkInvoiceStatus(String id) throws YellowException
    {
        String url  = this.serverRoot + (this.API_URI_CHECK_PAYMENT).replace("[id]", id);
        String response;
        HashMap<String, String> responseMap = new HashMap<String, String>();
        
        try {
            response = this.makeHTTPRequest("GET", url, new HashMap<String, String>());
            responseMap = (HashMap)JSONValue.parse(response);
        } catch (IOException | NoSuchAlgorithmException | InvalidKeyException  ex) {
            throw new YellowException(ex.getMessage());
        }
        
        return responseMap;
    }
    
    /**
     * Validate IPN
     *
     * @param url string
     * @param signature string
     * @param nonce string
     * @param body string
     * @return boolean
     * @throws co.yellowpay.sdk.YellowException
     */
    public boolean verifyIPN(String url, String signature, String nonce, String body) throws YellowException
    {
        if ( url == null || url.isEmpty() || 
             signature == null || signature.isEmpty() ||
             nonce == null || nonce.isEmpty() || 
             body == null ){
            // missing headers OR an empty payload
            return false;
        }
        
        String message = nonce + url + body;
        String calculated_signature = "";
        try {
            calculated_signature = this.signMessage(message);
        } catch (UnsupportedEncodingException | NoSuchAlgorithmException | InvalidKeyException ex) {
            throw new YellowException(ex.getMessage());
        }
        
        // valid or invalid IPN call
        return !calculated_signature.equals(signature);
    }
    
    /**
     * creates the HTTP data array for both createInvoice / checkInvoiceStatus
     *
     * @param url used to create signature
     * @param payload array
     * @return String
     * @throws co.yellowpay.sdk.YellowException
     */
    private String makeHTTPRequest(String type, String url, Map<String, String> payload) 
            throws IOException, NoSuchAlgorithmException, InvalidKeyException
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
            throws UnsupportedEncodingException, NoSuchAlgorithmException, InvalidKeyException
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
    private String hmacDigest(String algo, String msg, String keyString) 
            throws UnsupportedEncodingException, NoSuchAlgorithmException, InvalidKeyException {
        String digest;

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
        
        return digest;
    }


}
