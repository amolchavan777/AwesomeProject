# AwesomeProject


This project is a React Native application managed by Expo.

## Prerequisites

- [Node.js](https://nodejs.org/) (v10 or later recommended)
- [Yarn](https://yarnpkg.com/) or npm
- Expo CLI (`npm install -g expo-cli`)

## Install dependencies

From the repository root, run:

```bash
npm install
```

## Running the project

Start the development server with:

```bash
npm start
```

This will launch Expo in development mode. You can then:

- Press **`a`** to open the Android emulator (if available)
- Press **`i`** to open the iOS simulator (macOS only)
- Scan the QR code with the Expo Go app on your device

### Running in the browser

Expo supports running the project in a web browser via React Native Web. Launch
the development server with:

```bash
npm run web
```

This executes `expo start --web` under the hood, opening the app in your
default browser.

## Running tests

To execute the test suite, run:

```bash
npm test
```

Tests are based on Jest and React Native Testing Library.

## Dependency Mapper Prototype

The `dependency-mapper` directory contains a Spring Boot prototype implementing
automated dependency mapping. The `DependencyResolver` now uses a simplified
Bayesian approach to choose the most trustworthy dependency claims when multiple
sources provide conflicting information.

Data ingestion is handled entirely within the Spring Boot project using small Java adapters for logs, APIs and other sources.

### Building and running the dependency mapper

From the repository root run the following commands to start the Spring Boot
service:

```bash
cd dependency-mapper/dependency-mapper
mvn spring-boot:run
```

Use the normal Maven lifecycle for additional tasks such as running the tests
(`mvn test`) or creating a jar (`mvn package`).

### Configuration properties

Configuration options for the prototype are defined in
`dependency-mapper/dependency-mapper/src/main/resources/application.properties`.

- `snapshot.dir` &ndash; directory where `GraphSnapshotService` writes exported
  `.graphml` files. Defaults to `snapshots`.
- `source.priorities.*` &ndash; weights used by `WeightedConflictResolver` when
  multiple sources report the same dependency (for example
  `source.priorities.manual=10` and `source.priorities.auto=1`). Higher values
  give a source more influence in the conflict resolution process.
- `overrides.<from>-><to>` &ndash; optional manual override that forces the
  resolver to pick a claim from a specific source for a particular edge. For
  instance, `overrides.ServiceA->ServiceC=manual` ensures the dependency
   between `ServiceA` and `ServiceC` originates from the `manual` source.
- `ingestion.adapters` &ndash; comma separated list of built-in adapters to initialize when the application starts.

This repository contains a React Native project built with Expo.

## Release Logging

A release log is maintained in `RELEASE_LOG.md`. After each commit you can run the following commands to update the release log and append the latest commit information to this README:

```bash
npm run generate-release-log
npm run update-readme
```

These scripts capture commit metadata such as hash, author, date, and message. Each entry is appended to the release log and to the README so that commit history is tracked directly in the repository documentation.

### Fri Jul 4 09:51:11 2025 +0300
- Merge pull request #2 from amolchavan777/codex/assess-progress-on-application-dependency-mapping-system (873129f27e6474a7fb0e33983311026b5d38dfda)


### Fri Jul 4 13:49:40 2025 +0300
- some basic stuff (a17b175c2a897a789447b700da0cd63e5d8ef52f)

### Fri Jul 4 13:15:37 2025 +0000
- Implement Bayesian conflict resolution (0cadcf89c3851787b7242d31719e2fed57139d1a)

commit c5889700ae77b06c0a0617e7359687b941701c2d
Author: Codex
Date:   Sun Jul 6 08:10:31 2025 +0000
Message: Add transitive inference service
