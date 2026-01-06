# Exchange Rate Sync Engine

## Overview
A Java-based service that synchronizes exchange rates from a public API and persists them locally using a deterministic, incremental update strategy. The project is designed to model real-world backend synchronization workflows with production-grade error handling and test coverage.

## Domain
Financial data synchronization and stateful backend services.

## Features
- Incremental vs full sync strategy selection
- Deterministic JSON persistence
- Defensive handling of partial, empty, or invalid API responses
- Idempotent behavior across repeated runs
- Comprehensive unit tests covering edge cases and failures

## Tech Stack
- Java 8
- Maven
- JUnit 5
- Docker / Docker Compose

## Repository Size
- ~600+ lines of Java code
- 20+ source files
- Modular structure (API, service, strategy, storage, tests)
