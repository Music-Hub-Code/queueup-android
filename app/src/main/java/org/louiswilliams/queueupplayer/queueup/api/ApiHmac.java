package org.louiswilliams.queueupplayer.queueup.api;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.Hex;
import org.apache.http.client.methods.HttpRequestBase;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public class ApiHmac {

    public static final String HMAC_SHA1_ALGORITHM = "HmacSHA1";
    public static final String HMAC_SHA256_ALGORITHM = "HmacSHA256";

    private Mac hmac;

    public ApiHmac(String algorithm, String key) throws NoSuchAlgorithmException, InvalidKeyException {
        hmac = Mac.getInstance(algorithm);
        hmac.init(new SecretKeySpec(key.getBytes(), algorithm));
    }

    public static ApiHmac hmacSha1(String key) {
        ApiHmac result;
        try {
            result = new ApiHmac(HMAC_SHA1_ALGORITHM, key);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            result = null;
        }
        return result;
    }

    public static ApiHmac hmacSha256(String key) {
        ApiHmac result;
        try {
            result = new ApiHmac(HMAC_SHA256_ALGORITHM, key);
        } catch (NoSuchAlgorithmException  | InvalidKeyException e) {
            result = null;
        }

        return result;
    }

    private String digestMessage(String message) {
        byte[] raw = hmac.doFinal (message.getBytes());
        return new String(Hex.encodeHex(raw));
    }

    private String digestMessage(Object ... arguments) {
        String message = "";
        for (int i = 0; i < arguments.length; i++) {
            message += arguments[i];
            if (i < arguments.length - 1){
                message += "+";
            }
        }
        return digestMessage(message);
    }

    public void setHeadersForUser(HttpRequestBase request, String userId) {
        String method = request.getMethod();
        String hostname = request.getURI().getHost();
        String uri = request.getURI().getPath();
        Date now = new Date();
        DateFormat df = new SimpleDateFormat("yyy-MM-dd'T'HH:mm:ssZ");

        String unixSeconds = String.valueOf(now.getTime() / 1000);
        String digest = digestMessage(method, hostname, uri, unixSeconds);

        String authHeader = new String(Base64.encodeBase64((userId + ":" + digest).getBytes()));

        request.setHeader("Date", df.format(now));
        request.setHeader("Authorization", "Basic " +authHeader);

    }

}
