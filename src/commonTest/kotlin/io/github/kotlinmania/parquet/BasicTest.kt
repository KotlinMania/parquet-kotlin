// port-lint: tests basic.rs
package io.github.kotlinmania.parquet

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BasicTest {
    @Test
    fun testDisplayType() {
        assertEquals("BOOLEAN", ParquetType.BOOLEAN.toString())
        assertEquals("INT32", ParquetType.INT32.toString())
        assertEquals("INT64", ParquetType.INT64.toString())
        assertEquals("INT96", ParquetType.INT96.toString())
        assertEquals("FLOAT", ParquetType.FLOAT.toString())
        assertEquals("DOUBLE", ParquetType.DOUBLE.toString())
        assertEquals("BYTE_ARRAY", ParquetType.BYTE_ARRAY.toString())
        assertEquals("FIXED_LEN_BYTE_ARRAY", ParquetType.FIXED_LEN_BYTE_ARRAY.toString())
    }

    @Test
    fun testFromStringIntoType() {
        assertEquals(ParquetType.BOOLEAN, ParquetType.fromString(ParquetType.BOOLEAN.toString()))
        assertEquals(ParquetType.INT32, ParquetType.fromString(ParquetType.INT32.toString()))
        assertEquals(ParquetType.INT64, ParquetType.fromString(ParquetType.INT64.toString()))
        assertEquals(ParquetType.INT96, ParquetType.fromString(ParquetType.INT96.toString()))
        assertEquals(ParquetType.FLOAT, ParquetType.fromString(ParquetType.FLOAT.toString()))
        assertEquals(ParquetType.DOUBLE, ParquetType.fromString(ParquetType.DOUBLE.toString()))
        assertEquals(ParquetType.BYTE_ARRAY, ParquetType.fromString(ParquetType.BYTE_ARRAY.toString()))
        assertEquals(ParquetType.BYTE_ARRAY, ParquetType.fromString("BINARY"))
        assertEquals(
            ParquetType.FIXED_LEN_BYTE_ARRAY,
            ParquetType.fromString(ParquetType.FIXED_LEN_BYTE_ARRAY.toString()),
        )
    }

    private fun <T : WriteThrift> testRoundtrip(value: T, reader: ReadThrift<T>) {
        val out = ThriftCompactOutputProtocol()
        value.writeThrift(out)
        val bytes = out.toByteArray()
        val prot = ThriftSliceInputProtocol(bytes)
        val readVal = reader.readThrift(prot)
        assertEquals(value, readVal)
    }

    @Test
    fun testConvertedTypeRoundtrip() {
        testRoundtrip(ConvertedType.UTF8, ConvertedType)
        testRoundtrip(ConvertedType.MAP, ConvertedType)
        testRoundtrip(ConvertedType.MAP_KEY_VALUE, ConvertedType)
        testRoundtrip(ConvertedType.LIST, ConvertedType)
        testRoundtrip(ConvertedType.ENUM, ConvertedType)
        testRoundtrip(ConvertedType.DECIMAL, ConvertedType)
        testRoundtrip(ConvertedType.DATE, ConvertedType)
        testRoundtrip(ConvertedType.TIME_MILLIS, ConvertedType)
        testRoundtrip(ConvertedType.TIME_MICROS, ConvertedType)
        testRoundtrip(ConvertedType.TIMESTAMP_MILLIS, ConvertedType)
        testRoundtrip(ConvertedType.TIMESTAMP_MICROS, ConvertedType)
        testRoundtrip(ConvertedType.UINT_8, ConvertedType)
        testRoundtrip(ConvertedType.UINT_16, ConvertedType)
        testRoundtrip(ConvertedType.UINT_32, ConvertedType)
        testRoundtrip(ConvertedType.UINT_64, ConvertedType)
        testRoundtrip(ConvertedType.INT_8, ConvertedType)
        testRoundtrip(ConvertedType.INT_16, ConvertedType)
        testRoundtrip(ConvertedType.INT_32, ConvertedType)
        testRoundtrip(ConvertedType.INT_64, ConvertedType)
        testRoundtrip(ConvertedType.JSON, ConvertedType)
        testRoundtrip(ConvertedType.BSON, ConvertedType)
        testRoundtrip(ConvertedType.INTERVAL, ConvertedType)
    }

    @Test
    fun testReadInvalidConvertedType() {
        val prot = ThriftSliceInputProtocol(byteArrayOf(0x7E.toByte()))
        val err =
            assertFailsWith<ParquetError> {
                ConvertedType.readThrift(prot)
            }
        assertEquals("Parquet error: Unexpected ConvertedType 63", err.message)
    }

    @Test
    fun testLogicalTypeRoundtrip() {
        testRoundtrip(LogicalType.String, LogicalType)
        testRoundtrip(LogicalType.Map, LogicalType)
        testRoundtrip(LogicalType.List, LogicalType)
        testRoundtrip(LogicalType.Enum, LogicalType)
        testRoundtrip(LogicalType.decimal(0, 20), LogicalType)
        testRoundtrip(LogicalType.Date, LogicalType)
        testRoundtrip(LogicalType.time(true, TimeUnit.MICROS), LogicalType)
        testRoundtrip(LogicalType.time(false, TimeUnit.MILLIS), LogicalType)
        testRoundtrip(LogicalType.time(false, TimeUnit.NANOS), LogicalType)
        testRoundtrip(LogicalType.timestamp(false, TimeUnit.MICROS), LogicalType)
        testRoundtrip(LogicalType.timestamp(true, TimeUnit.MILLIS), LogicalType)
        testRoundtrip(LogicalType.timestamp(true, TimeUnit.NANOS), LogicalType)
        testRoundtrip(LogicalType.integer(8, true), LogicalType)
        testRoundtrip(LogicalType.integer(16, false), LogicalType)
        testRoundtrip(LogicalType.integer(32, true), LogicalType)
        testRoundtrip(LogicalType.integer(64, false), LogicalType)
        testRoundtrip(LogicalType.Json, LogicalType)
        testRoundtrip(LogicalType.Bson, LogicalType)
        testRoundtrip(LogicalType.Uuid, LogicalType)
        testRoundtrip(LogicalType.Float16, LogicalType)
        testRoundtrip(LogicalType.variant(1.toByte()), LogicalType)
        testRoundtrip(LogicalType.variant(null), LogicalType)
        testRoundtrip(LogicalType.geometry("foo"), LogicalType)
        testRoundtrip(LogicalType.geometry(null), LogicalType)
        testRoundtrip(
            LogicalType.geography("foo", EdgeInterpolationAlgorithm.ANDOYER),
            LogicalType,
        )
        testRoundtrip(
            LogicalType.geography(null, EdgeInterpolationAlgorithm.KARNEY),
            LogicalType,
        )
        testRoundtrip(
            LogicalType.geography("foo", EdgeInterpolationAlgorithm.SPHERICAL),
            LogicalType,
        )
        testRoundtrip(
            LogicalType.geography(null, EdgeInterpolationAlgorithm.SPHERICAL),
            LogicalType,
        )
    }

    @Test
    fun testColumnOrderRoundtrip() {
        testRoundtrip(ColumnOrder.TypeDefinedOrder(SortOrder.SIGNED), ColumnOrder)
    }

    @Test
    fun testDisplayConvertedType() {
        assertEquals("NONE", ConvertedType.NONE.toString())
        assertEquals("UTF8", ConvertedType.UTF8.toString())
        assertEquals("MAP", ConvertedType.MAP.toString())
        assertEquals("MAP_KEY_VALUE", ConvertedType.MAP_KEY_VALUE.toString())
        assertEquals("LIST", ConvertedType.LIST.toString())
        assertEquals("ENUM", ConvertedType.ENUM.toString())
        assertEquals("DECIMAL", ConvertedType.DECIMAL.toString())
        assertEquals("DATE", ConvertedType.DATE.toString())
        assertEquals("TIME_MILLIS", ConvertedType.TIME_MILLIS.toString())
        assertEquals("TIME_MICROS", ConvertedType.TIME_MICROS.toString())
        assertEquals("TIMESTAMP_MILLIS", ConvertedType.TIMESTAMP_MILLIS.toString())
        assertEquals("TIMESTAMP_MICROS", ConvertedType.TIMESTAMP_MICROS.toString())
        assertEquals("UINT_8", ConvertedType.UINT_8.toString())
        assertEquals("UINT_16", ConvertedType.UINT_16.toString())
        assertEquals("UINT_32", ConvertedType.UINT_32.toString())
        assertEquals("UINT_64", ConvertedType.UINT_64.toString())
        assertEquals("INT_8", ConvertedType.INT_8.toString())
        assertEquals("INT_16", ConvertedType.INT_16.toString())
        assertEquals("INT_32", ConvertedType.INT_32.toString())
        assertEquals("INT_64", ConvertedType.INT_64.toString())
        assertEquals("JSON", ConvertedType.JSON.toString())
        assertEquals("BSON", ConvertedType.BSON.toString())
        assertEquals("INTERVAL", ConvertedType.INTERVAL.toString())
    }

    @Test
    fun testFromStringIntoConvertedType() {
        assertEquals(ConvertedType.NONE, ConvertedType.fromString(ConvertedType.NONE.toString()))
        assertEquals(ConvertedType.UTF8, ConvertedType.fromString(ConvertedType.UTF8.toString()))
        assertEquals(ConvertedType.MAP, ConvertedType.fromString(ConvertedType.MAP.toString()))
        assertEquals(
            ConvertedType.MAP_KEY_VALUE,
            ConvertedType.fromString(ConvertedType.MAP_KEY_VALUE.toString()),
        )
        assertEquals(ConvertedType.LIST, ConvertedType.fromString(ConvertedType.LIST.toString()))
        assertEquals(ConvertedType.ENUM, ConvertedType.fromString(ConvertedType.ENUM.toString()))
        assertEquals(ConvertedType.DECIMAL, ConvertedType.fromString(ConvertedType.DECIMAL.toString()))
        assertEquals(ConvertedType.DATE, ConvertedType.fromString(ConvertedType.DATE.toString()))
        assertEquals(
            ConvertedType.TIME_MILLIS,
            ConvertedType.fromString(ConvertedType.TIME_MILLIS.toString()),
        )
        assertEquals(
            ConvertedType.TIME_MICROS,
            ConvertedType.fromString(ConvertedType.TIME_MICROS.toString()),
        )
        assertEquals(
            ConvertedType.TIMESTAMP_MILLIS,
            ConvertedType.fromString(ConvertedType.TIMESTAMP_MILLIS.toString()),
        )
        assertEquals(
            ConvertedType.TIMESTAMP_MICROS,
            ConvertedType.fromString(ConvertedType.TIMESTAMP_MICROS.toString()),
        )
        assertEquals(ConvertedType.UINT_8, ConvertedType.fromString(ConvertedType.UINT_8.toString()))
        assertEquals(ConvertedType.UINT_16, ConvertedType.fromString(ConvertedType.UINT_16.toString()))
        assertEquals(ConvertedType.UINT_32, ConvertedType.fromString(ConvertedType.UINT_32.toString()))
        assertEquals(ConvertedType.UINT_64, ConvertedType.fromString(ConvertedType.UINT_64.toString()))
        assertEquals(ConvertedType.INT_8, ConvertedType.fromString(ConvertedType.INT_8.toString()))
        assertEquals(ConvertedType.INT_16, ConvertedType.fromString(ConvertedType.INT_16.toString()))
        assertEquals(ConvertedType.INT_32, ConvertedType.fromString(ConvertedType.INT_32.toString()))
        assertEquals(ConvertedType.INT_64, ConvertedType.fromString(ConvertedType.INT_64.toString()))
        assertEquals(ConvertedType.JSON, ConvertedType.fromString(ConvertedType.JSON.toString()))
        assertEquals(ConvertedType.BSON, ConvertedType.fromString(ConvertedType.BSON.toString()))
        assertEquals(ConvertedType.INTERVAL, ConvertedType.fromString(ConvertedType.INTERVAL.toString()))
    }

    @Test
    fun testLogicalToConvertedType() {
        val logicalNone: LogicalType? = null
        assertEquals(ConvertedType.NONE, ConvertedType.from(logicalNone))
        assertEquals(
            ConvertedType.DECIMAL,
            ConvertedType.from(LogicalType.decimal(5, 20)),
        )
        assertEquals(ConvertedType.BSON, ConvertedType.from(LogicalType.Bson))
        assertEquals(ConvertedType.JSON, ConvertedType.from(LogicalType.Json))
        assertEquals(ConvertedType.UTF8, ConvertedType.from(LogicalType.String))
        assertEquals(ConvertedType.DATE, ConvertedType.from(LogicalType.Date))
        assertEquals(
            ConvertedType.TIME_MILLIS,
            ConvertedType.from(LogicalType.time(true, TimeUnit.MILLIS)),
        )
        assertEquals(
            ConvertedType.TIME_MICROS,
            ConvertedType.from(LogicalType.time(true, TimeUnit.MICROS)),
        )
        assertEquals(
            ConvertedType.NONE,
            ConvertedType.from(LogicalType.time(false, TimeUnit.NANOS)),
        )
        assertEquals(
            ConvertedType.TIMESTAMP_MILLIS,
            ConvertedType.from(LogicalType.timestamp(true, TimeUnit.MILLIS)),
        )
        assertEquals(
            ConvertedType.TIMESTAMP_MICROS,
            ConvertedType.from(LogicalType.timestamp(false, TimeUnit.MICROS)),
        )
        assertEquals(
            ConvertedType.NONE,
            ConvertedType.from(LogicalType.timestamp(false, TimeUnit.NANOS)),
        )
        assertEquals(
            ConvertedType.UINT_8,
            ConvertedType.from(LogicalType.integer(8, false)),
        )
        assertEquals(
            ConvertedType.INT_8,
            ConvertedType.from(LogicalType.integer(8, true)),
        )
        assertEquals(
            ConvertedType.UINT_16,
            ConvertedType.from(LogicalType.integer(16, false)),
        )
        assertEquals(
            ConvertedType.INT_16,
            ConvertedType.from(LogicalType.integer(16, true)),
        )
        assertEquals(
            ConvertedType.UINT_32,
            ConvertedType.from(LogicalType.integer(32, false)),
        )
        assertEquals(
            ConvertedType.INT_32,
            ConvertedType.from(LogicalType.integer(32, true)),
        )
        assertEquals(
            ConvertedType.UINT_64,
            ConvertedType.from(LogicalType.integer(64, false)),
        )
        assertEquals(
            ConvertedType.INT_64,
            ConvertedType.from(LogicalType.integer(64, true)),
        )
        assertEquals(ConvertedType.LIST, ConvertedType.from(LogicalType.List))
        assertEquals(ConvertedType.MAP, ConvertedType.from(LogicalType.Map))
        assertEquals(ConvertedType.NONE, ConvertedType.from(LogicalType.Uuid))
        assertEquals(ConvertedType.ENUM, ConvertedType.from(LogicalType.Enum))
        assertEquals(ConvertedType.NONE, ConvertedType.from(LogicalType.Float16))
        assertEquals(ConvertedType.NONE, ConvertedType.from(LogicalType.variant(null)))
        assertEquals(ConvertedType.NONE, ConvertedType.from(LogicalType.geometry(null)))
        assertEquals(
            ConvertedType.NONE,
            ConvertedType.from(LogicalType.geography(null, EdgeInterpolationAlgorithm.SPHERICAL)),
        )
        assertEquals(ConvertedType.NONE, ConvertedType.from(LogicalType.Unknown))
    }

    @Test
    fun testDisplayRepetition() {
        assertEquals("REQUIRED", Repetition.REQUIRED.toString())
        assertEquals("OPTIONAL", Repetition.OPTIONAL.toString())
        assertEquals("REPEATED", Repetition.REPEATED.toString())
    }

    @Test
    fun testFromStringIntoRepetition() {
        assertEquals(
            Repetition.REQUIRED,
            Repetition.fromString(Repetition.REQUIRED.toString()),
        )
        assertEquals(
            Repetition.OPTIONAL,
            Repetition.fromString(Repetition.OPTIONAL.toString()),
        )
        assertEquals(
            Repetition.REPEATED,
            Repetition.fromString(Repetition.REPEATED.toString()),
        )
    }

    @Test
    fun testDisplayEncoding() {
        assertEquals("PLAIN", Encoding.PLAIN.toString())
        assertEquals("PLAIN_DICTIONARY", Encoding.PLAIN_DICTIONARY.toString())
        assertEquals("RLE", Encoding.RLE.toString())
        assertEquals("BIT_PACKED", Encoding.BIT_PACKED.toString())
        assertEquals("DELTA_BINARY_PACKED", Encoding.DELTA_BINARY_PACKED.toString())
        assertEquals("DELTA_LENGTH_BYTE_ARRAY", Encoding.DELTA_LENGTH_BYTE_ARRAY.toString())
        assertEquals("DELTA_BYTE_ARRAY", Encoding.DELTA_BYTE_ARRAY.toString())
        assertEquals("RLE_DICTIONARY", Encoding.RLE_DICTIONARY.toString())
        assertEquals("ALP", Encoding.ALP.toString())
    }

    @Test
    fun testCompressionConversion() {
        assertEquals(CompressionCodec.UNCOMPRESSED, CompressionCodec.fromCompression(Compression.UNCOMPRESSED))
        assertEquals(CompressionCodec.SNAPPY, CompressionCodec.fromCompression(Compression.SNAPPY))
        assertEquals(CompressionCodec.GZIP, CompressionCodec.fromCompression(Compression.GZIP()))
        assertEquals(CompressionCodec.LZO, CompressionCodec.fromCompression(Compression.LZO))
        assertEquals(CompressionCodec.BROTLI, CompressionCodec.fromCompression(Compression.BROTLI()))
        assertEquals(CompressionCodec.LZ4, CompressionCodec.fromCompression(Compression.LZ4))
        assertEquals(CompressionCodec.ZSTD, CompressionCodec.fromCompression(Compression.ZSTD()))
        assertEquals(CompressionCodec.LZ4_RAW, CompressionCodec.fromCompression(Compression.LZ4_RAW))

        assertEquals(Compression.UNCOMPRESSED, Compression.fromCodec(CompressionCodec.UNCOMPRESSED))
        assertEquals(Compression.SNAPPY, Compression.fromCodec(CompressionCodec.SNAPPY))
        assertEquals(Compression.GZIP(), Compression.fromCodec(CompressionCodec.GZIP))
        assertEquals(Compression.LZO, Compression.fromCodec(CompressionCodec.LZO))
        assertEquals(Compression.BROTLI(), Compression.fromCodec(CompressionCodec.BROTLI))
        assertEquals(Compression.LZ4, Compression.fromCodec(CompressionCodec.LZ4))
        assertEquals(Compression.ZSTD(), Compression.fromCodec(CompressionCodec.ZSTD))
        assertEquals(Compression.LZ4_RAW, Compression.fromCodec(CompressionCodec.LZ4_RAW))
    }

    @Test
    fun testDisplayCompression() {
        assertEquals("UNCOMPRESSED", Compression.UNCOMPRESSED.toString())
        assertEquals("SNAPPY", Compression.SNAPPY.toString())
        assertEquals("GZIP(GzipLevel(6))", Compression.GZIP().toString())
        assertEquals("LZO", Compression.LZO.toString())
        assertEquals("BROTLI(BrotliLevel(1))", Compression.BROTLI().toString())
        assertEquals("LZ4", Compression.LZ4.toString())
        assertEquals("ZSTD(ZstdLevel(1))", Compression.ZSTD().toString())
    }

    @Test
    fun testDisplayPageType() {
        assertEquals("DATA_PAGE", PageType.DATA_PAGE.toString())
        assertEquals("INDEX_PAGE", PageType.INDEX_PAGE.toString())
        assertEquals("DICTIONARY_PAGE", PageType.DICTIONARY_PAGE.toString())
        assertEquals("DATA_PAGE_V2", PageType.DATA_PAGE_V2.toString())
    }

    @Test
    fun testDisplaySortOrder() {
        assertEquals("SIGNED", SortOrder.SIGNED.toString())
        assertEquals("UNSIGNED", SortOrder.UNSIGNED.toString())
        assertEquals("UNDEFINED", SortOrder.UNDEFINED.toString())
        assertEquals("TOTAL_ORDER", SortOrder.TOTAL_ORDER.toString())
        assertEquals("INT96_TIMESTAMP", SortOrder.INT96_TIMESTAMP.toString())
    }

    @Test
    fun testDisplayColumnOrder() {
        assertEquals("TYPE_DEFINED_ORDER(SIGNED)", ColumnOrder.TypeDefinedOrder(SortOrder.SIGNED).toString())
        assertEquals("TYPE_DEFINED_ORDER(UNSIGNED)", ColumnOrder.TypeDefinedOrder(SortOrder.UNSIGNED).toString())
        assertEquals("TYPE_DEFINED_ORDER(UNDEFINED)", ColumnOrder.TypeDefinedOrder(SortOrder.UNDEFINED).toString())
        assertEquals("IEEE_754_TOTAL_ORDER", ColumnOrder.Ieee754TotalOrder.toString())
        assertEquals("INT96_TIMESTAMP_ORDER", ColumnOrder.Int96TimestampOrder.toString())
        assertEquals("UNDEFINED", ColumnOrder.Undefined.toString())
    }

    @Test
    fun testColumnOrderGetLogicalTypeSortOrder() {
        fun checkSortOrder(types: List<LogicalType>, expectedOrder: SortOrder) {
            for (type in types) {
                assertEquals(
                    expectedOrder,
                    ColumnOrder.columnOrderForType(type, ConvertedType.NONE, ParquetType.BYTE_ARRAY).sortOrder(),
                )
            }
        }

        val unsigned =
            listOf(
                LogicalType.String,
                LogicalType.Json,
                LogicalType.Bson,
                LogicalType.Enum,
                LogicalType.Uuid,
                LogicalType.integer(8, false),
                LogicalType.integer(16, false),
                LogicalType.integer(32, false),
                LogicalType.integer(64, false),
            )
        checkSortOrder(unsigned, SortOrder.UNSIGNED)

        val signed =
            listOf(
                LogicalType.integer(8, true),
                LogicalType.integer(16, true),
                LogicalType.integer(32, true),
                LogicalType.integer(64, true),
                LogicalType.decimal(20, 4),
                LogicalType.Date,
                LogicalType.time(false, TimeUnit.MILLIS),
                LogicalType.time(false, TimeUnit.MICROS),
                LogicalType.time(true, TimeUnit.NANOS),
                LogicalType.timestamp(false, TimeUnit.MILLIS),
                LogicalType.timestamp(false, TimeUnit.MICROS),
                LogicalType.timestamp(true, TimeUnit.NANOS),
            )
        checkSortOrder(signed, SortOrder.SIGNED)

        val float = listOf(LogicalType.Float16)
        checkSortOrder(float, SortOrder.TOTAL_ORDER)

        val undefined =
            listOf(
                LogicalType.List,
                LogicalType.Map,
                LogicalType.variant(null),
                LogicalType.geometry(null),
                LogicalType.geography(null, EdgeInterpolationAlgorithm.SPHERICAL),
            )
        checkSortOrder(undefined, SortOrder.UNDEFINED)
    }

    @Test
    fun testColumnOrderGetConvertedTypeSortOrder() {
        fun checkSortOrder(types: List<ConvertedType>, expectedOrder: SortOrder) {
            for (type in types) {
                assertEquals(
                    expectedOrder,
                    ColumnOrder.columnOrderForType(null, type, ParquetType.BYTE_ARRAY).sortOrder(),
                )
            }
        }

        val unsigned =
            listOf(
                ConvertedType.UTF8,
                ConvertedType.JSON,
                ConvertedType.BSON,
                ConvertedType.ENUM,
                ConvertedType.UINT_8,
                ConvertedType.UINT_16,
                ConvertedType.UINT_32,
                ConvertedType.UINT_64,
            )
        checkSortOrder(unsigned, SortOrder.UNSIGNED)

        val signed =
            listOf(
                ConvertedType.INT_8,
                ConvertedType.INT_16,
                ConvertedType.INT_32,
                ConvertedType.INT_64,
                ConvertedType.DECIMAL,
                ConvertedType.DATE,
                ConvertedType.TIME_MILLIS,
                ConvertedType.TIME_MICROS,
                ConvertedType.TIMESTAMP_MILLIS,
                ConvertedType.TIMESTAMP_MICROS,
            )
        checkSortOrder(signed, SortOrder.SIGNED)

        val undefined =
            listOf(
                ConvertedType.LIST,
                ConvertedType.MAP,
                ConvertedType.MAP_KEY_VALUE,
                ConvertedType.INTERVAL,
            )
        checkSortOrder(undefined, SortOrder.UNDEFINED)

        checkSortOrder(listOf(ConvertedType.NONE), SortOrder.UNSIGNED)
    }

    @Test
    fun testColumnOrderGetDefaultSortOrder() {
        assertEquals(SortOrder.UNSIGNED, ColumnOrder.getDefaultSortOrder(ParquetType.BOOLEAN, true))
        assertEquals(SortOrder.SIGNED, ColumnOrder.getDefaultSortOrder(ParquetType.INT32, true))
        assertEquals(SortOrder.SIGNED, ColumnOrder.getDefaultSortOrder(ParquetType.INT64, true))
        assertEquals(SortOrder.UNDEFINED, ColumnOrder.getDefaultSortOrder(ParquetType.INT96, true))
        assertEquals(SortOrder.INT96_TIMESTAMP, ColumnOrder.getDefaultSortOrder(ParquetType.INT96, false))
        assertEquals(SortOrder.TOTAL_ORDER, ColumnOrder.getDefaultSortOrder(ParquetType.FLOAT, false))
        assertEquals(SortOrder.TOTAL_ORDER, ColumnOrder.getDefaultSortOrder(ParquetType.DOUBLE, false))
        assertEquals(SortOrder.SIGNED, ColumnOrder.getDefaultSortOrder(ParquetType.FLOAT, true))
        assertEquals(SortOrder.SIGNED, ColumnOrder.getDefaultSortOrder(ParquetType.DOUBLE, true))
        assertEquals(SortOrder.UNSIGNED, ColumnOrder.getDefaultSortOrder(ParquetType.BYTE_ARRAY, true))
        assertEquals(SortOrder.UNSIGNED, ColumnOrder.getDefaultSortOrder(ParquetType.FIXED_LEN_BYTE_ARRAY, true))
    }

    @Test
    fun testColumnOrderSortOrder() {
        assertEquals(SortOrder.SIGNED, ColumnOrder.TypeDefinedOrder(SortOrder.SIGNED).sortOrder())
        assertEquals(SortOrder.UNSIGNED, ColumnOrder.TypeDefinedOrder(SortOrder.UNSIGNED).sortOrder())
        assertEquals(SortOrder.UNDEFINED, ColumnOrder.TypeDefinedOrder(SortOrder.UNDEFINED).sortOrder())
        assertEquals(SortOrder.TOTAL_ORDER, ColumnOrder.Ieee754TotalOrder.sortOrder())
        assertEquals(SortOrder.INT96_TIMESTAMP, ColumnOrder.Int96TimestampOrder.sortOrder())
        assertEquals(SortOrder.SIGNED, ColumnOrder.Undefined.sortOrder())
    }

    @Test
    fun testParseEncoding() {
        assertEquals(Encoding.PLAIN, Encoding.fromString("PLAIN"))
        assertEquals(Encoding.PLAIN_DICTIONARY, Encoding.fromString("PLAIN_DICTIONARY"))
        assertEquals(Encoding.RLE, Encoding.fromString("RLE"))
        assertEquals(Encoding.BIT_PACKED, Encoding.fromString("BIT_PACKED"))
        assertEquals(Encoding.DELTA_BINARY_PACKED, Encoding.fromString("DELTA_BINARY_PACKED"))
        assertEquals(Encoding.DELTA_LENGTH_BYTE_ARRAY, Encoding.fromString("DELTA_LENGTH_BYTE_ARRAY"))
        assertEquals(Encoding.DELTA_BYTE_ARRAY, Encoding.fromString("DELTA_BYTE_ARRAY"))
        assertEquals(Encoding.RLE_DICTIONARY, Encoding.fromString("RLE_DICTIONARY"))
        assertEquals(Encoding.BYTE_STREAM_SPLIT, Encoding.fromString("BYTE_STREAM_SPLIT"))
        assertEquals(Encoding.ALP, Encoding.fromString("alp"))
        assertEquals(Encoding.BYTE_STREAM_SPLIT, Encoding.fromString("byte_stream_split"))

        val ex =
            assertFailsWith<ParquetError.General> {
                Encoding.fromString("plain_xxx")
            }
        assertEquals("Parquet error: unknown encoding: plain_xxx", ex.message)
    }

    @Test
    fun testParseCompression() {
        assertEquals(Compression.SNAPPY, Compression.fromString("snappy"))
        assertEquals(Compression.LZO, Compression.fromString("lzo"))
        assertEquals(Compression.ZSTD(ZstdLevel.tryNew(3).getOrThrow()), Compression.fromString("zstd(3)"))
        assertEquals(Compression.ZSTD(ZstdLevel.tryNew(-3).getOrThrow()), Compression.fromString("zstd(-3)"))
        assertEquals(Compression.LZ4_RAW, Compression.fromString("LZ4_RAW"))
        assertEquals(Compression.UNCOMPRESSED, Compression.fromString("uncompressed"))
        assertEquals(Compression.SNAPPY, Compression.fromString("snappy"))
        assertEquals(Compression.GZIP(GzipLevel.tryNew(9).getOrThrow()), Compression.fromString("gzip(9)"))
        assertEquals(Compression.LZO, Compression.fromString("lzo"))
        assertEquals(Compression.BROTLI(BrotliLevel.tryNew(3).getOrThrow()), Compression.fromString("brotli(3)"))
        assertEquals(Compression.LZ4, Compression.fromString("lz4"))

        val err1 =
            assertFailsWith<ParquetError.General> {
                Encoding.fromString("plain_xxx")
            }
        assertEquals("Parquet error: unknown encoding: plain_xxx", err1.message)

        val err2 =
            assertFailsWith<ParquetError.General> {
                Encoding.fromString("gzip(-10)")
            }
        assertEquals("Parquet error: unknown encoding: gzip(-10)", err2.message)
    }

    @Test
    fun testDisplayBoundaryOrder() {
        assertEquals("ASCENDING", BoundaryOrder.ASCENDING.toString())
        assertEquals("DESCENDING", BoundaryOrder.DESCENDING.toString())
        assertEquals("UNORDERED", BoundaryOrder.UNORDERED.toString())
    }

    @Test
    fun testDisplayEdgeAlgo() {
        assertEquals("SPHERICAL", EdgeInterpolationAlgorithm.SPHERICAL.toString())
        assertEquals("VINCENTY", EdgeInterpolationAlgorithm.VINCENTY.toString())
        assertEquals("THOMAS", EdgeInterpolationAlgorithm.THOMAS.toString())
        assertEquals("ANDOYER", EdgeInterpolationAlgorithm.ANDOYER.toString())
        assertEquals("KARNEY", EdgeInterpolationAlgorithm.KARNEY.toString())
    }

    @Test
    fun testFromStrEdgeAlgo() {
        assertEquals(EdgeInterpolationAlgorithm.SPHERICAL, EdgeInterpolationAlgorithm.fromString("spHErical"))
        assertEquals(EdgeInterpolationAlgorithm.VINCENTY, EdgeInterpolationAlgorithm.fromString("vinceNTY"))
        assertEquals(EdgeInterpolationAlgorithm.THOMAS, EdgeInterpolationAlgorithm.fromString("tHOmas"))
        assertEquals(EdgeInterpolationAlgorithm.ANDOYER, EdgeInterpolationAlgorithm.fromString("anDOYEr"))
        assertEquals(EdgeInterpolationAlgorithm.KARNEY, EdgeInterpolationAlgorithm.fromString("kaRNey"))
        assertFailsWith<ParquetError.General> {
            EdgeInterpolationAlgorithm.fromString("does not exist")
        }
    }

    private fun encodingsRoundtrip(encodings: List<Encoding>) {
        val sorted = encodings.sorted()
        val mask = EncodingMask.newFromEncodings(sorted)
        assertTrue(mask.allSet(sorted))
        val v = mask.encodings()
        assertEquals(sorted, v)
    }

    @Test
    fun testEncodingRoundtrip() {
        encodingsRoundtrip(listOf(Encoding.RLE, Encoding.PLAIN, Encoding.DELTA_BINARY_PACKED))
        encodingsRoundtrip(listOf(Encoding.RLE_DICTIONARY, Encoding.PLAIN_DICTIONARY))
        encodingsRoundtrip(emptyList())
        val encodings =
            listOf(
                Encoding.PLAIN,
                Encoding.BIT_PACKED,
                Encoding.RLE,
                Encoding.DELTA_BINARY_PACKED,
                Encoding.DELTA_BYTE_ARRAY,
                Encoding.DELTA_LENGTH_BYTE_ARRAY,
                Encoding.PLAIN_DICTIONARY,
                Encoding.RLE_DICTIONARY,
                Encoding.BYTE_STREAM_SPLIT,
                Encoding.ALP,
            )
        encodingsRoundtrip(encodings)
    }

    @Test
    fun testInvalidEncodingMask() {
        val res1 = EncodingMask.tryNew(-1)
        assertTrue(res1.isFailure)
        assertEquals(
            "Parquet error: Attempt to create invalid mask: 0xffffffff",
            res1.exceptionOrNull()?.message,
        )

        val res2 = EncodingMask.tryNew(2)
        assertTrue(res2.isFailure)
        assertEquals(
            "Parquet error: Attempt to create invalid mask: 0x2",
            res2.exceptionOrNull()?.message,
        )
    }

    @Test
    fun testEncodingMaskIsOnly() {
        val mask1 = EncodingMask.newFromEncodings(listOf(Encoding.PLAIN))
        assertTrue(mask1.isOnly(Encoding.PLAIN))

        val mask2 = EncodingMask.newFromEncodings(listOf(Encoding.PLAIN, Encoding.PLAIN_DICTIONARY))
        assertFalse(mask2.isOnly(Encoding.PLAIN))
    }
}
