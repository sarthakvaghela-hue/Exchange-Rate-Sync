package com.deccan.exchangesync.strategy;

import com.deccan.exchangesync.model.ExchangeRates;

public class FullSyncStrategy implements SyncStrategy{

	 @Override
	    public ExchangeRates apply(ExchangeRates remote, ExchangeRates local) {
	        return remote;
	    }
	
}
