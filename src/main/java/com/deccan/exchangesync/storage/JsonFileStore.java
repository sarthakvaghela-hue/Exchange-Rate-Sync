package com.deccan.exchangesync.storage;

import com.deccan.exchangesync.model.ExchangeRates;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;

public class JsonFileStore {

    private final File file;
    private final ObjectMapper mapper = new ObjectMapper();

    public JsonFileStore(String filePath) {
        this.file = new File(filePath);
    }

    public boolean exists() {
        return file.exists();
    }

    public ExchangeRates read() {
        try {
            return mapper.readValue(file, ExchangeRates.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to read local state", e);
        }
    }

    public void write(ExchangeRates rates) {
        try {
            mapper.writeValue(file, rates);
        } catch (Exception e) {
            throw new RuntimeException("Failed to write local state", e);
        }
    }
}