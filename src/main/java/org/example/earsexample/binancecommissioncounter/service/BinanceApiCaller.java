package org.example.earsexample.binancecommissioncounter.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

@Component
public class BinanceApiCaller {

    private final RestTemplate restTemplate;
    private final UserCredentialsService userCredentialsService;
    ObjectMapper mapper = new ObjectMapper();

    public BinanceApiCaller(RestTemplate restTemplate, UserCredentialsService userCredentialsService) {
        this.restTemplate = restTemplate;
        this.userCredentialsService = userCredentialsService;
    }

    String exchangeInfoUrl = "https://api.binance.com/api/v3/exchangeInfo";
    String myTradesUrl = "https://api.binance.com/api/v3/myTrades";
    String tickerPriceUrl = "https://api.binance.com/api/v3/ticker/price";
    String pingUrl = "https://api.binance.com/api/v3/ping";

    public boolean checkConnection() {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.add("X-MBX-APIKEY", userCredentialsService.getApiKey());
            HttpEntity<String> requestEntity = new HttpEntity<>(headers);
            ResponseEntity<String> response = restTemplate.exchange(pingUrl, HttpMethod.GET, requestEntity, String.class);
            return response.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            return false;
        }
    }

    private ArrayList<String> getSymbols() throws JsonProcessingException {
        String response = restTemplate.getForObject(exchangeInfoUrl, String.class);
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

    private Map<String, BigDecimal> covertCommissionsToUSDT(Map<String, BigDecimal> commissions) throws JsonProcessingException {
        Map<String, BigDecimal> result = new HashMap<>();
        result.put("USDT", commissions.get("USDT"));
        commissions.remove("USDT");
        for (String asset : commissions.keySet()) {
            String symbol = asset + "USDT";
            String url = tickerPriceUrl + "?symbol=" + symbol;
            String response = restTemplate.getForObject(url, String.class);
            JsonNode rootNode = mapper.readTree(response);
            BigDecimal price = new BigDecimal(rootNode.path("price").asText());
            BigDecimal commission = commissions.get(asset);
            result.put("USDT", result.get("USDT").add(commission.multiply(price)));
        }
        return result;
    }

    public void runWithProgress(Consumer<Double> progressCallback, Consumer<Map<String, BigDecimal>> resultCallback) throws Exception {
        ArrayList<String> symbols = getSymbols();
        ArrayList<String> symbolsToCheck = new ArrayList<>();
        Map<String, BigDecimal> commissions = new HashMap<>();
        int totalSymbols = symbols.size();
        int processedSymbols = 0;
        for (String symbol : symbols) {
            Map<String, String> params = new HashMap<>();
            params.put("symbol", symbol);
            params.put("recvWindow", "5000");
            params.put("timestamp", String.valueOf(System.currentTimeMillis()));

            String signature = BinanceSignatureUtil.signRequest(params, userCredentialsService.getSecretKey());
            params.put("signature", signature);

            String queryString = params.entrySet().stream()
                    .map(entry -> entry.getKey() + "=" + entry.getValue())
                    .reduce((a, b) -> a + "&" + b)
                    .orElse("");

            String url = myTradesUrl + "?" + queryString;

            HttpHeaders headers = new HttpHeaders();
            headers.add("X-MBX-APIKEY", userCredentialsService.getApiKey());

            HttpEntity<String> requestEntity = new HttpEntity<>(headers);
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, requestEntity, String.class);
            if (!response.getBody().equals("[]")) {
                symbolsToCheck.add(symbol);
                JsonNode tradesNode = mapper.readTree(response.getBody());
                for (JsonNode trade : tradesNode) {
                    String commissionAsset = trade.path("commissionAsset").asText();
                    BigDecimal commission = new BigDecimal(trade.path("commission").asText());
                    commissions.merge(commissionAsset, commission, BigDecimal::add);
                }
            }
            processedSymbols++;
            double progress = (double) processedSymbols / totalSymbols;
            progressCallback.accept(progress);
        }
        Map<String, BigDecimal> commissionsInUSDT = covertCommissionsToUSDT(commissions);
        resultCallback.accept(commissionsInUSDT);
    }
}