package com.deccan.exchangesync.service;

import com.deccan.exchangesync.api.ExchangeRateClient;
import com.deccan.exchangesync.model.ExchangeRates;
import com.deccan.exchangesync.storage.JsonFileStore;
import com.deccan.exchangesync.strategy.FullSyncStrategy;
import com.deccan.exchangesync.strategy.IncrementalSyncStrategy;
import com.deccan.exchangesync.strategy.SyncStrategy;

public class ExchangeRateSyncService {
	private final ExchangeRateClient client;
    private final JsonFileStore store;

    public ExchangeRateSyncService(ExchangeRateClient client, JsonFileStore store) {
        this.client = client;
        this.store = store;
    }

    public ExchangeRates sync(String baseCurrency) {
        ExchangeRates remote = client.fetchLatestRates(baseCurrency);
        
        if (!store.exists()) {
            store.write(remote);
            return remote;
        }

        ExchangeRates local = store.read();
        if (remote.getRates() == null || remote.getRates().isEmpty()) {
            return local;
        }
        SyncStrategy strategy = selectStrategy(local, remote);

        ExchangeRates merged = strategy.apply(remote, local);
        store.write(merged);

        return merged;
    }

    private SyncStrategy selectStrategy(ExchangeRates local, ExchangeRates remote) {
        if (local.getBaseCurrency() == null ||
            !local.getBaseCurrency().equals(remote.getBaseCurrency())) {
            return new FullSyncStrategy();
        }
        return new IncrementalSyncStrategy();
    }
}
