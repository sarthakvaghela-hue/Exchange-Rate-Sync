package com.deccan.exchangesync.model;

import java.util.Map;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class ExchangeRates {
	
	private final String baseCurrency;
	private final Map<String, Double> rates;
	private final long lastUpdatedEpochMillis;
	
	@JsonCreator
	public ExchangeRates(
	        @JsonProperty("baseCurrency") String baseCurrency,
	        @JsonProperty("rates") Map<String, Double> rates,
	        @JsonProperty("lastUpdatedEpochMillis") long lastUpdatedEpochMillis
	) {
	    this.baseCurrency = baseCurrency;
	    this.rates = rates;
	    this.lastUpdatedEpochMillis = lastUpdatedEpochMillis;
	}

	public String getBaseCurrency() {
		return baseCurrency;
	}

	public Map<String, Double> getRates() {
		return rates;
	}

	public long getLastUpdatedEpochMillis() {
		return lastUpdatedEpochMillis;
	}
	
	
}
