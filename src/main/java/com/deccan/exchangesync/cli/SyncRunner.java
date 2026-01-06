package com.deccan.exchangesync.cli;

import com.deccan.exchangesync.api.PublicApiExchangeRateClient;
import com.deccan.exchangesync.service.ExchangeRateSyncService;
import com.deccan.exchangesync.storage.JsonFileStore;

//Manual entry point for running the sync
public class SyncRunner {

 public static void main(String[] args) {
     String baseCurrency = args.length > 0 ? args[0] : "EUR";
     String filePath = "exchange_rates.json";
     
     System.out.println("Fetching Rates and Updating JSON");

     ExchangeRateSyncService service =
             new ExchangeRateSyncService(
                     new PublicApiExchangeRateClient(),
                     new JsonFileStore(filePath)
             );

     service.sync(baseCurrency);
 }
}