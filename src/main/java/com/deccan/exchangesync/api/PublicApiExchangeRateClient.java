package com.deccan.exchangesync.api;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import com.deccan.exchangesync.model.ExchangeRates;

public class PublicApiExchangeRateClient implements ExchangeRateClient{


    private static final String API_URL = "https://api.frankfurter.app/latest?from=%s";
	
    public ExchangeRates fetchLatestRates(String baseCurrency) {
        try {
            URL url = new URL(String.format(API_URL, baseCurrency));
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);

            BufferedReader reader =
                    new BufferedReader(
                            new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }

            return parseResponse(baseCurrency, response.toString());

        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch exchange rates", e);
        }
    }
    // Very small, controlled parser for known API response shape
	private ExchangeRates parseResponse(String baseCurrency, String json) {
		Map<String, Double> rates = new HashMap<String, Double>();
		
        int ratesIndex = json.indexOf("\"rates\":");
        int start = json.indexOf("{", ratesIndex);
        int end = json.indexOf("}", start);
        
        String ratesBlock = json.substring(start + 1, end);
        String[] entries = ratesBlock.split(",");
        
        for (String entry : entries) {
            String[] parts = entry.split(":");
            String currency = parts[0].replace("\"", "").trim();
            Double value = Double.valueOf(parts[1].trim());
            rates.put(currency, value);
        }
		
        return new ExchangeRates(baseCurrency, rates, System.currentTimeMillis());
	}

}
