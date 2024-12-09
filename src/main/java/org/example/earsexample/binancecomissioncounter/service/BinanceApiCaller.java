package org.example.earsexample.binancecomissioncounter.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.CommandLineRunner;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.json.GsonBuilderUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

@Component
public class BinanceApiCaller implements CommandLineRunner {

    private final RestTemplate restTemplate;

    public BinanceApiCaller(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    String exchangeInfoUrl = "https://api.binance.com/api/v3/exchangeInfo";
    String myTradesUrl = "https://api.binance.com/api/v3/myTrades";
    String apiKey = "hQG6C0fDlyGm1XU2118JO2KbCDENXRtUHrUnjPuludvaDxkY5pKStJSTxEQNnuu8";
    String secretKey = "1MPnRKWGSx8nbKp42Eoa94K6bWnQGskFtYUptT9l7SwElaqMkeIwQ1VbMIyTfRWl";

    private ArrayList<String> getSymbols() throws JsonProcessingException {

        String response = restTemplate.getForObject(exchangeInfoUrl, String.class);
        ObjectMapper mapper = new ObjectMapper();
        JsonNode rootNode = mapper.readTree(response);
        JsonNode symbolsNode = rootNode.path("symbols");
        ArrayList<String> symbols = new ArrayList<>();
        for (JsonNode symbolNode : symbolsNode) {
            String symbol = symbolNode.path("symbol").asText();
            if (symbol.contains("USDC") || symbol.contains("USDT")) {
                symbols.add(symbol);
            }
        }
        return symbols;
    }

    @Override
    public void run(String... args) throws Exception {
        ArrayList<String> symbols = getSymbols();
        System.out.println("Size: " + symbols.size());
        Scanner sc = new Scanner(System.in);
        System.out.println("Press any key to continue...");
        sc.next();
        ArrayList<String> symbolsToCheck = new ArrayList<>();
        Map<String, BigDecimal> commissions = new HashMap<>();
        int totalSymbols = symbols.size();
        int processedSymbols = 0;
        for (String symbol : symbols) {
            Map<String, String> params = new HashMap<>();
            params.put("symbol", symbol);
            params.put("recvWindow", "5000");
            params.put("timestamp", String.valueOf(System.currentTimeMillis()));

            // Подписать запрос
            String signature = BinanceSignatureUtil.signRequest(params, secretKey);
            params.put("signature", signature);

            // Собрать query string
            String queryString = params.entrySet().stream()
                    .map(entry -> entry.getKey() + "=" + entry.getValue())
                    .reduce((a, b) -> a + "&" + b)
                    .orElse("");

            // Полный URL
            String url = myTradesUrl + "?" + queryString;

            // Настроить заголовки
            HttpHeaders headers = new HttpHeaders();
            headers.add("X-MBX-APIKEY", apiKey);

            // Отправить запрос
            RestTemplate restTemplate = new RestTemplate();
            HttpEntity<String> requestEntity = new HttpEntity<>(headers);
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, requestEntity, String.class);
            ObjectMapper mapper = new ObjectMapper();
            if (!response.getBody().equals("[]")) {
                symbolsToCheck.add(symbol);
                JsonNode tradesNode = mapper.readTree(response.getBody());
                for (JsonNode trade : tradesNode) {
                    String commissionAsset = trade.path("commissionAsset").asText();
                    BigDecimal commission = new BigDecimal(trade.path("commission").asText());
                    if (commissions.containsKey(commissionAsset)) {
                        commissions.put(commissionAsset, commissions.get(commissionAsset).add(commission));
                    } else {
                        commissions.put(commissionAsset, commission);
                    }
                }
            }
            // Обновить прогресс
            processedSymbols++;
            float progress = (float) (processedSymbols * 100) / totalSymbols;
            System.out.printf("Progress: %.2f%%\n", progress);
        }
        System.out.println("Commissions:"+commissions);
    }
}