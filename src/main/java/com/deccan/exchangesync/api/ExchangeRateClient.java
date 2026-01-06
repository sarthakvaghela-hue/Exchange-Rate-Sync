package com.deccan.exchangesync.api;

import com.deccan.exchangesync.model.ExchangeRates;

public interface ExchangeRateClient {
	
    // Fetch latest exchange rates for the given base currency
	ExchangeRates fetchLatestRates(String baseCurrency);
	
}
