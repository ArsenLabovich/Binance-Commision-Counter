package org.example.earsexample.binancecommissioncounter.service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.stream.Collectors;

public class BinanceSignatureUtil {

    public static String signRequest(Map<String, String> params, String secretKey) {
        try {
            // 1. Соберите параметры в строку
            String queryString = params.entrySet().stream()
                    .map(entry -> entry.getKey() + "=" + entry.getValue())
                    .collect(Collectors.joining("&"));

            // 2. Создайте ключ для HMAC-SHA256
            SecretKeySpec secretKeySpec = new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(secretKeySpec);

            // 3. Подпишите строку
            byte[] hmacSha256 = mac.doFinal(queryString.getBytes(StandardCharsets.UTF_8));

            // 4. Преобразуйте в HEX
            StringBuilder hexString = new StringBuilder();
            for (byte b : hmacSha256) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }

            return hexString.toString();

        } catch (Exception e) {
            System.out.println(e.getMessage());
            throw new RuntimeException("Request Signature error", e);
        }
    }
}
