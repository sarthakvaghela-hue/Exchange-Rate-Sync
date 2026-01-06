package com.deccan.exchangesync;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import com.deccan.exchangesync.api.ExchangeRateClient;
import com.deccan.exchangesync.model.ExchangeRates;
import com.deccan.exchangesync.service.ExchangeRateSyncService;
import com.deccan.exchangesync.storage.JsonFileStore;

public class ExchangeRateSyncTest {

    private final String filePath = "test_rates.json";

    @AfterEach
    void cleanup() {
        new File(filePath).delete();
    }

    // Initial run with no local state should create the JSON file
    @Test
    void initialSyncCreatesLocalFile() {
        ExchangeRateSyncService service =
                new ExchangeRateSyncService(
                        mockClient("EUR", rate("USD", 1.1)),
                        new JsonFileStore(filePath)
                );

        ExchangeRates result = service.sync("EUR");

        assertTrue(new File(filePath).exists());
        assertEquals(1.1, result.getRates().get("USD"));
    }

    // Incremental sync should merge new rates without dropping existing ones
    @Test
    void incrementalSyncPreservesExistingRates() {
        JsonFileStore store = new JsonFileStore(filePath);
        store.write(new ExchangeRates("EUR", rate("USD", 1.1), 1L));

        ExchangeRateSyncService service =
                new ExchangeRateSyncService(
                        mockClient("EUR", rate("GBP", 0.9)),
                        store
                );

        ExchangeRates result = service.sync("EUR");

        assertEquals(2, result.getRates().size());
        assertEquals(1.1, result.getRates().get("USD"));
        assertEquals(0.9, result.getRates().get("GBP"));
    }

    // API failures must not corrupt or delete existing local data
    @Test
    void apiFailureDoesNotCorruptLocalState() {
        JsonFileStore store = new JsonFileStore(filePath);
        store.write(new ExchangeRates("EUR", rate("USD", 1.1), 1L));

        ExchangeRateSyncService service =
                new ExchangeRateSyncService(
                        base -> { throw new RuntimeException(); },
                        store
                );

        assertThrows(RuntimeException.class, () -> service.sync("EUR"));

        ExchangeRates persisted = store.read();
        assertEquals(1.1, persisted.getRates().get("USD"));
    }

    // A base currency change should force a full sync instead of incremental
    @Test
    void baseCurrencyChangeForcesFullSync() {
        JsonFileStore store = new JsonFileStore(filePath);
        store.write(new ExchangeRates("EUR", rate("USD", 1.1), 1L));

        ExchangeRateSyncService service =
                new ExchangeRateSyncService(
                        mockClient("USD", rate("EUR", 0.9)),
                        store
                );

        ExchangeRates result = service.sync("USD");

        assertEquals("USD", result.getBaseCurrency());
        assertEquals(1, result.getRates().size());
        assertTrue(result.getRates().containsKey("EUR"));
    }

    // Empty API responses must not wipe out existing exchange rates
    @Test
    void emptyRemoteRatesDoNotOverwriteLocalState() {
        JsonFileStore store = new JsonFileStore(filePath);
        store.write(new ExchangeRates("EUR", rate("USD", 1.1), 1L));

        ExchangeRateSyncService service =
                new ExchangeRateSyncService(
                        mockClient("EUR", new HashMap<>()),
                        store
                );

        ExchangeRates result = service.sync("EUR");

        assertEquals(1.1, result.getRates().get("USD"));
    }

    // Repeated syncs with the same data should produce identical file output
    @Test
    void repeatedSyncProducesDeterministicFileContent() {
        JsonFileStore store = new JsonFileStore(filePath);

        ExchangeRateClient client =
                mockClient("EUR", rate("USD", 1.1));

        ExchangeRateSyncService service =
                new ExchangeRateSyncService(client, store);

        service.sync("EUR");
        String first = readRawFile();

        service.sync("EUR");
        String second = readRawFile();

        assertEquals(first, second);
    }

    // A failed write must not leave the persisted file in a broken state
    @Test
    void failedWriteDoesNotCorruptExistingFile() {
        JsonFileStore store = new JsonFileStore(filePath);
        store.write(new ExchangeRates("EUR", rate("USD", 1.1), 1L));

        JsonFileStore failingStore = new JsonFileStore(filePath) {
            @Override
            public void write(ExchangeRates rates) {
                throw new RuntimeException("disk error");
            }
        };

        ExchangeRateSyncService service =
                new ExchangeRateSyncService(
                        mockClient("EUR", rate("GBP", 0.9)),
                        failingStore
                );

        assertThrows(RuntimeException.class, () -> service.sync("EUR"));

        ExchangeRates persisted = store.read();
        assertEquals(1.1, persisted.getRates().get("USD"));
    }

    // Remote rates missing some currencies should not delete local ones
    @Test
    void partialRemoteUpdateDoesNotDeleteExistingRates() {
        JsonFileStore store = new JsonFileStore(filePath);
        Map<String, Double> localRates = new HashMap<>();
        localRates.put("USD", 1.1);
        localRates.put("GBP", 0.9);
        store.write(new ExchangeRates("EUR", localRates, 1L));

        ExchangeRateSyncService service =
                new ExchangeRateSyncService(
                        mockClient("EUR", rate("USD", 1.2)),
                        store
                );

        ExchangeRates result = service.sync("EUR");

        assertEquals(2, result.getRates().size());
        assertEquals(1.2, result.getRates().get("USD"));
        assertEquals(0.9, result.getRates().get("GBP"));
    }
    
    // Remote response with null rates map should not wipe local state
    @Test
    void nullRemoteRatesDoesNotOverwriteLocalState() {
        JsonFileStore store = new JsonFileStore(filePath);
        store.write(new ExchangeRates("EUR", rate("USD", 1.1), 1L));

        ExchangeRateSyncService service =
                new ExchangeRateSyncService(
                        currency -> new ExchangeRates("EUR", null, System.currentTimeMillis()),
                        store
                );

        ExchangeRates result = service.sync("EUR");

        assertEquals(1.1, result.getRates().get("USD"));
    }
    
    private ExchangeRateClient mockClient(String base, Map<String, Double> rates) {
        return currency -> new ExchangeRates(base, rates, System.currentTimeMillis());
    }
    
    // Syncing with identical data must not change timestamp or file content
    @Test
    void identicalRemoteDataDoesNotChangeTimestamp() {
        JsonFileStore store = new JsonFileStore(filePath);
        store.write(new ExchangeRates("EUR", rate("USD", 1.1), 123L));

        ExchangeRateSyncService service =
                new ExchangeRateSyncService(
                        mockClient("EUR", rate("USD", 1.1)),
                        store
                );

        ExchangeRates result = service.sync("EUR");

        assertEquals(123L, result.getLastUpdatedEpochMillis());
    }
    
    // Sync must be idempotent across multiple executions
    @Test
    void syncIsIdempotentAcrossMultipleRuns() {
        ExchangeRateSyncService service =
                new ExchangeRateSyncService(
                        mockClient("EUR", rate("USD", 1.1)),
                        new JsonFileStore(filePath)
                );

        service.sync("EUR");
        String first = readRawFile();

        service.sync("EUR");
        String second = readRawFile();

        service.sync("EUR");
        String third = readRawFile();

        assertEquals(first, second);
        assertEquals(second, third);
    }
    
    // Corrupt JSON file should not crash silently
    @Test
    void corruptLocalFileFailsFast() throws Exception {
        File f = new File(filePath);
        f.createNewFile();
        Files.write(f.toPath(), "{invalid-json".getBytes());

        ExchangeRateSyncService service =
                new ExchangeRateSyncService(
                        mockClient("EUR", rate("USD", 1.1)),
                        new JsonFileStore(filePath)
                );

        assertThrows(RuntimeException.class, () -> service.sync("EUR"));
    }
    
    // Read-only file should fail without corrupting content
    @Test
    void readOnlyFileDoesNotGetCorrupted() throws Exception {
        JsonFileStore store = new JsonFileStore(filePath);
        store.write(new ExchangeRates("EUR", rate("USD", 1.1), 1L));

        File f = new File(filePath);
        f.setReadOnly();

        ExchangeRateSyncService service =
                new ExchangeRateSyncService(
                        mockClient("EUR", rate("GBP", 0.9)),
                        store
                );

        assertThrows(RuntimeException.class, () -> service.sync("EUR"));

        ExchangeRates persisted = store.read();
        assertEquals(1.1, persisted.getRates().get("USD"));
    }
    
    // Base currency mismatch must ignore overlapping currencies
    @Test
    void fullSyncDoesNotMergeWhenBaseCurrencyChanges() {
        JsonFileStore store = new JsonFileStore(filePath);
        store.write(new ExchangeRates("EUR", rate("USD", 1.1), 1L));

        ExchangeRateSyncService service =
                new ExchangeRateSyncService(
                        mockClient("USD", rate("EUR", 0.9)),
                        store
                );

        ExchangeRates result = service.sync("USD");

        assertFalse(result.getRates().containsKey("USD"));
        assertEquals("USD", result.getBaseCurrency());
    }
    
    // API timeout should not touch local state
    @Test
    void apiTimeoutDoesNotModifyLocalState() {
        JsonFileStore store = new JsonFileStore(filePath);
        store.write(new ExchangeRates("EUR", rate("USD", 1.1), 1L));

        ExchangeRateSyncService service =
                new ExchangeRateSyncService(
                        base -> { throw new RuntimeException("timeout"); },
                        store
                );

        assertThrows(RuntimeException.class, () -> service.sync("EUR"));

        ExchangeRates persisted = store.read();
        assertEquals(1.1, persisted.getRates().get("USD"));
    }
    
    private Map<String, Double> rate(String k, double v) {
        Map<String, Double> m = new HashMap<>();
        m.put(k, v);
        return m;
    }
    
    private String readRawFile() {
        try (BufferedReader r = new BufferedReader(new FileReader(filePath))) {
            StringBuilder b = new StringBuilder();
            String line;
            while ((line = r.readLine()) != null) {
                b.append(line);
            }
            return b.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}

