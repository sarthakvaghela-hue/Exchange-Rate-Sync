package com.deccan.exchangesync.strategy;

import com.deccan.exchangesync.model.ExchangeRates;

public interface SyncStrategy {

    ExchangeRates apply(ExchangeRates remote, ExchangeRates local);
	
}
