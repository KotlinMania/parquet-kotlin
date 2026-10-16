# parquet-kotlin in Kotlin

[![GitHub link](https://img.shields.io/badge/GitHub-KotlinMania%2Fparquet-kotlin-blue.svg)](https://github.com/KotlinMania/parquet-kotlin)
[![Maven Central](https://img.shields.io/maven-central/v/io.github.kotlinmania/parquet-kotlin)](https://central.sonatype.com/artifact/io.github.kotlinmania/parquet-kotlin)
[![Build status](https://img.shields.io/github/actions/workflow/status/KotlinMania/parquet-kotlin/ci.yml?branch=main)](https://github.com/KotlinMania/parquet-kotlin/actions)

This is a Kotlin Multiplatform line-by-line transliteration port of [`apache/arrow-rs`](https://github.com/apache/arrow-rs) (crate `parquet`).

**Original Project:** This port is based on [`apache/arrow-rs`](https://github.com/apache/arrow-rs). All design credit and project intent belong to the upstream authors; this repository is a faithful port to Kotlin Multiplatform with no behavioural changes intended.

### Porting status

This is an **in-progress port**. The goal is feature parity with the upstream Rust crate while providing a native Kotlin Multiplatform API. Every Kotlin file carries a `// port-lint: source <path>` header naming its upstream Rust counterpart so the AST-distance tool can track provenance.

---

## Upstream README — `apache/arrow-rs`

> The text below is reproduced from [`https://github.com/apache/arrow-rs`](https://github.com/apache/arrow-rs). It is the upstream project's own description and remains under the upstream authors' authorship; links have been rewritten to absolute upstream URLs where appropriate so they continue to resolve from this repository.

<!---
  Licensed to the Apache Software Foundation (ASF) under one
  or more contributor license agreements.  See the NOTICE file
  distributed with this work for additional information
  regarding copyright ownership.  The ASF licenses this file
  to you under the Apache License, Version 2.0 (the
  "License"); you may not use this file except in compliance
  with the License.  You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing,
  software distributed under the License is distributed on an
  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
  KIND, either express or implied.  See the License for the
  specific language governing permissions and limitations
  under the License.
-->

# Apache Parquet Official Native Rust Implementation

[![crates.io](https://img.shields.io/crates/v/parquet.svg)](https://crates.io/crates/parquet)
[![docs.rs](https://img.shields.io/docsrs/parquet.svg)](https://docs.rs/parquet/latest/parquet/)

This crate contains the official Native Rust implementation of [Apache Parquet](https://parquet.apache.org/), which is part of the [Apache Arrow](https://arrow.apache.org/) project.

See the [API documentation](https://docs.rs/parquet/latest) for examples and the full API.

The API documentation for most recent, unreleased code is available [here](https://arrow.apache.org/rust/parquet/index.html).

## Rust Version Compatibility

This crate is tested with the latest stable version of Rust. We do not currently test against other, older versions of the Rust compiler.

## Versioning / Releases

The `parquet` crate follows the [SemVer standard] defined by Cargo and works well
within the Rust crate ecosystem. See the [repository README] for more details on
the release schedule, version and deprecation policy.

[semver standard]: https://doc.rust-lang.org/cargo/reference/semver.html
[repository readme]: https://github.com/apache/arrow-rs

Note that for historical reasons, this crate uses versions with major numbers
greater than `0.x` (e.g. `19.0.0`), unlike many other crates in the Rust
ecosystem which spend extended time releasing versions `0.x` to signal planned
ongoing API changes. Minor arrow releases contain only compatible changes, while
major releases may contain breaking API changes.

## Feature Flags

The `parquet` crate provides the following features which may be enabled in your `Cargo.toml`:

- `arrow` (default) - support for reading / writing [`arrow`] arrays to / from Parquet
- `async` - support `async` APIs for reading Parquet
- `json` - support for reading / writing `json` data to / from Parquet
- `brotli` (default) - support for Parquet using `brotli` compression
- `flate2` (default) - support for Parquet using `gzip` compression
- `lz4` (default) - support for Parquet using `lz4` compression
- `zstd` (default) - support for Parquet using `zstd` compression
- `snap` (default) - support for Parquet using `snappy` compression
- `cli` - parquet [CLI tools](https://github.com/apache/arrow-rs/tree/main/parquet/src/bin)
- `crc` - enables functionality to automatically verify checksums of each page (if present) when decoding
- `experimental` - Experimental APIs which may change, even between minor releases
- `simdutf8` (default) - Use the [`simdutf8`] crate for SIMD-accelerated UTF-8 validation
- `encryption` - support for reading / writing encrypted Parquet files
- `variant_experimental` - ⚠️ Experimental [Parquet Variant] support, which may change, even between minor releases.
- `geospatial` - ⚠️ Experimental geospatial support, which may change, even between minor releases.

[`arrow`]: https://crates.io/crates/arrow
[`simdutf8`]: https://crates.io/crates/simdutf8
[parquet variant]: https://github.com/apache/parquet-format/blob/master/VariantEncoding.md

## Parquet Feature Status

Please see the [Implementation Status Page] on the [Apache Parquet] website for
information on the status of this implementation.

[implementation status page]: https://parquet.apache.org/docs/file-format/implementationstatus/
[apache parquet]: https://parquet.apache.org/

## Security

See the [Security Policy] for information on the security model and how to report vulnerabilities.

[security policy]: https://github.com/apache/arrow-rs/blob/main/SECURITY.md

## License

Licensed under the Apache License, Version 2.0: <http://www.apache.org/licenses/LICENSE-2.0>.

---

## About this Kotlin port

### Installation

```kotlin
dependencies {
    implementation("io.github.kotlinmania:parquet-kotlin:0.1.0")
}
```

### Building

```bash
./gradlew build
./gradlew test
```

### Targets

- macOS arm64
- Linux x64
- Windows mingw-x64
- iOS arm64 / simulator-arm64 (Swift export + XCFramework)
- JS (browser + Node.js)
- Wasm-JS (browser + Node.js)
- Android (API 24+)

### Porting guidelines

See [AGENTS.md](AGENTS.md) and [CLAUDE.md](CLAUDE.md) for translator discipline, port-lint header convention, and Rust → Kotlin idiom mapping.

### License

This Kotlin port is distributed under the same license terms as the upstream [`apache/arrow-rs`](https://github.com/apache/arrow-rs). See [LICENSE](LICENSE) (and any sibling `LICENSE-*` / `NOTICE` files mirrored from upstream) for the full text.

Original work copyrighted by the parquet authors.  
Kotlin port: Copyright (c) 2026 Sydney Renee and The Solace Project.

### Acknowledgments

Thanks to the [`apache/arrow-rs`](https://github.com/apache/arrow-rs) maintainers and contributors for the original Rust implementation. This port reproduces their work in Kotlin Multiplatform; bug reports about upstream design or behavior should go to the upstream repository.
