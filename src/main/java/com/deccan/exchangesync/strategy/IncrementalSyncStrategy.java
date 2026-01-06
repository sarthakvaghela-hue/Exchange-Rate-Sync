package com.deccan.exchangesync.strategy;

import java.util.HashMap;
import java.util.Map;

import com.deccan.exchangesync.model.ExchangeRates;

public class IncrementalSyncStrategy implements SyncStrategy{

	@Override
	public ExchangeRates apply(ExchangeRates remote, ExchangeRates local) {
		Map<String, Double> merged = new HashMap<>(local.getRates());

        for (Map.Entry<String, Double> entry : remote.getRates().entrySet()) {
            merged.put(entry.getKey(), entry.getValue());
        }

        return new ExchangeRates(
                remote.getBaseCurrency(),
                merged,
                local.getLastUpdatedEpochMillis());
	}

}
