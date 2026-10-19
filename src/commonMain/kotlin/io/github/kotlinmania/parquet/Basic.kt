// port-lint: source basic.rs
package io.github.kotlinmania.parquet

/**
 * Types supported by Parquet.
 *
 * These physical types are intended to be used in combination with the encodings to
 * control the on disk storage format.
 * For example INT16 is not included as a type since a good encoding of INT32
 * would handle this.
 */
public enum class Type(public val value: Int) {
    BOOLEAN(0),
    INT32(1),
    INT64(2),
    INT96(3), // deprecated, only used by legacy implementations.
    FLOAT(4),
    DOUBLE(5),
    BYTE_ARRAY(6),
    FIXED_LEN_BYTE_ARRAY(7);

    public companion object {
        public fun fromInt(value: Int): Type = when (value) {
            0 -> BOOLEAN
            1 -> INT32
            2 -> INT64
            3 -> INT96
            4 -> FLOAT
            5 -> DOUBLE
            6 -> BYTE_ARRAY
            7 -> FIXED_LEN_BYTE_ARRAY
            else -> throw generalErr("Invalid parquet type value: $value")
        }

        public fun fromString(s: String): Type = when (s) {
            "BOOLEAN" -> BOOLEAN
            "INT32" -> INT32
            "INT64" -> INT64
            "INT96" -> INT96
            "FLOAT" -> FLOAT
            "DOUBLE" -> DOUBLE
            "BYTE_ARRAY", "BINARY" -> BYTE_ARRAY
            "FIXED_LEN_BYTE_ARRAY" -> FIXED_LEN_BYTE_ARRAY
            else -> throw generalErr("Invalid parquet type $s")
        }
    }
}

/**
 * Common types (converted types) used by frameworks when using Parquet.
 *
 * This helps map between types in those frameworks to the base types in Parquet.
 * This is only metadata and not needed to read or write the data.
 */
public enum class ConvertedType(public val value: Int) {
    NONE(-1),
    UTF8(0),
    MAP(1),
    MAP_KEY_VALUE(2),
    LIST(3),
    ENUM(4),
    DECIMAL(5),
    DATE(6),
    TIME_MILLIS(7),
    TIME_MICROS(8),
    TIMESTAMP_MILLIS(9),
    TIMESTAMP_MICROS(10),
    UINT_8(11),
    UINT_16(12),
    UINT_32(13),
    UINT_64(14),
    INT_8(15),
    INT_16(16),
    INT_32(17),
    INT_64(18),
    JSON(19),
    BSON(20),
    INTERVAL(21);

    public companion object {
        public fun fromInt(value: Int): ConvertedType = when (value) {
            -1 -> NONE
            0 -> UTF8
            1 -> MAP
            2 -> MAP_KEY_VALUE
            3 -> LIST
            4 -> ENUM
            5 -> DECIMAL
            6 -> DATE
            7 -> TIME_MILLIS
            8 -> TIME_MICROS
            9 -> TIMESTAMP_MILLIS
            10 -> TIMESTAMP_MICROS
            11 -> UINT_8
            12 -> UINT_16
            13 -> UINT_32
            14 -> UINT_64
            15 -> INT_8
            16 -> INT_16
            17 -> INT_32
            18 -> INT_64
            19 -> JSON
            20 -> BSON
            21 -> INTERVAL
            else -> throw generalErr("Unexpected ConvertedType $value")
        }

        public fun fromString(s: String): ConvertedType = when (s) {
            "NONE" -> NONE
            "UTF8" -> UTF8
            "MAP" -> MAP
            "MAP_KEY_VALUE" -> MAP_KEY_VALUE
            "LIST" -> LIST
            "ENUM" -> ENUM
            "DECIMAL" -> DECIMAL
            "DATE" -> DATE
            "TIME_MILLIS" -> TIME_MILLIS
            "TIME_MICROS" -> TIME_MICROS
            "TIMESTAMP_MILLIS" -> TIMESTAMP_MILLIS
            "TIMESTAMP_MICROS" -> TIMESTAMP_MICROS
            "UINT_8" -> UINT_8
            "UINT_16" -> UINT_16
            "UINT_32" -> UINT_32
            "UINT_64" -> UINT_64
            "INT_8" -> INT_8
            "INT_16" -> INT_16
            "INT_32" -> INT_32
            "INT_64" -> INT_64
            "JSON" -> JSON
            "BSON" -> BSON
            "INTERVAL" -> INTERVAL
            else -> throw generalErr("Invalid parquet converted type $s")
        }

        public fun from(logicalType: LogicalType?): ConvertedType = when (logicalType) {
            null -> NONE
            is LogicalType.String -> UTF8
            is LogicalType.Map -> MAP
            is LogicalType.List -> LIST
            is LogicalType.Enum -> ENUM
            is LogicalType.Decimal -> DECIMAL
            is LogicalType.Date -> DATE
            is LogicalType.Time -> when (logicalType.unit) {
                TimeUnit.MILLIS -> TIME_MILLIS
                TimeUnit.MICROS -> TIME_MICROS
                TimeUnit.NANOS -> NONE
            }
            is LogicalType.Timestamp -> when (logicalType.unit) {
                TimeUnit.MILLIS -> TIMESTAMP_MILLIS
                TimeUnit.MICROS -> TIMESTAMP_MICROS
                TimeUnit.NANOS -> NONE
            }
            is LogicalType.Integer -> when {
                logicalType.bitWidth == 8.toByte() && logicalType.isSigned -> INT_8
                logicalType.bitWidth == 16.toByte() && logicalType.isSigned -> INT_16
                logicalType.bitWidth == 32.toByte() && logicalType.isSigned -> INT_32
                logicalType.bitWidth == 64.toByte() && logicalType.isSigned -> INT_64
                logicalType.bitWidth == 8.toByte() && !logicalType.isSigned -> UINT_8
                logicalType.bitWidth == 16.toByte() && !logicalType.isSigned -> UINT_16
                logicalType.bitWidth == 32.toByte() && !logicalType.isSigned -> UINT_32
                logicalType.bitWidth == 64.toByte() && !logicalType.isSigned -> UINT_64
                else -> throw generalErr(
                    "Integer type bitWidth=${logicalType.bitWidth}, signed=${logicalType.isSigned} is not supported"
                )
            }
            is LogicalType.Json -> JSON
            is LogicalType.Bson -> BSON
            is LogicalType.Uuid,
            is LogicalType.Float16,
            is LogicalType.Variant,
            is LogicalType.Geometry,
            is LogicalType.Geography,
            is LogicalType.File,
            is LogicalType.Unknown -> NONE
        }
    }
}

/**
 * Time unit for Time and Timestamp logical types.
 */
public enum class TimeUnit(public val value: Int) {
    MILLIS(1),
    MICROS(2),
    NANOS(3);

    public companion object {
        public fun fromInt(value: Int): TimeUnit = when (value) {
            1 -> MILLIS
            2 -> MICROS
            3 -> NANOS
            else -> throw generalErr("Invalid TimeUnit value: $value")
        }
    }
}

public data class DecimalType(
    public val scale: Int,
    public val precision: Int,
)

public data class TimestampType(
    public val isAdjustedToUTC: Boolean,
    public val unit: TimeUnit,
)

public typealias TimeType = TimestampType

public data class IntType(
    public val bitWidth: Byte,
    public val isSigned: Boolean,
)

public data class VariantType(
    public val specificationVersion: Byte?,
)

public data class GeometryType(
    public val crs: String?,
)

public data class GeographyType(
    public val crs: String?,
    public val algorithm: EdgeInterpolationAlgorithm? = null,
) {
    public fun algorithm(): EdgeInterpolationAlgorithm =
        algorithm ?: EdgeInterpolationAlgorithm.SPHERICAL
}

/**
 * Logical types used by version 2.4.0+ of the Parquet format.
 */
public sealed class LogicalType {
    public data object String : LogicalType() {
        override fun toString(): kotlin.String = "STRING"
    }

    public data object Map : LogicalType() {
        override fun toString(): kotlin.String = "MAP"
    }

    public data object List : LogicalType() {
        override fun toString(): kotlin.String = "LIST"
    }

    public data object Enum : LogicalType() {
        override fun toString(): kotlin.String = "ENUM"
    }

    public data class Decimal(public val scale: Int, public val precision: Int) : LogicalType() {
        override fun toString(): kotlin.String = "DECIMAL($scale, $precision)"
    }

    public data object Date : LogicalType() {
        override fun toString(): kotlin.String = "DATE"
    }

    public data class Time(public val isAdjustedToUTC: Boolean, public val unit: TimeUnit) : LogicalType() {
        override fun toString(): kotlin.String = "TIME(isAdjustedToUTC=$isAdjustedToUTC, unit=$unit)"
    }

    public data class Timestamp(public val isAdjustedToUTC: Boolean, public val unit: TimeUnit) : LogicalType() {
        override fun toString(): kotlin.String = "TIMESTAMP(isAdjustedToUTC=$isAdjustedToUTC, unit=$unit)"
    }

    public data class Integer(public val bitWidth: Byte, public val isSigned: Boolean) : LogicalType() {
        override fun toString(): kotlin.String = "INTEGER($bitWidth, isSigned=$isSigned)"
    }

    public data object Unknown : LogicalType() {
        override fun toString(): kotlin.String = "UNKNOWN"
    }

    public data object Json : LogicalType() {
        override fun toString(): kotlin.String = "JSON"
    }

    public data object Bson : LogicalType() {
        override fun toString(): kotlin.String = "BSON"
    }

    public data object Uuid : LogicalType() {
        override fun toString(): kotlin.String = "UUID"
    }

    public data object Float16 : LogicalType() {
        override fun toString(): kotlin.String = "FLOAT16"
    }

    public data class Variant(public val specificationVersion: Byte?) : LogicalType()

    public data class Geometry(public val crs: kotlin.String?) : LogicalType()

    public data class Geography(
        public val crs: kotlin.String?,
        public val algorithm: EdgeInterpolationAlgorithm? = null,
    ) : LogicalType()

    public data object File : LogicalType() {
        override fun toString(): kotlin.String = "FILE"
    }

    public companion object {
        public fun integer(bitWidth: Byte, isSigned: Boolean): LogicalType =
            Integer(bitWidth, isSigned)

        public fun decimal(scale: Int, precision: Int): LogicalType =
            Decimal(scale, precision)

        public fun time(isAdjustedToUTC: Boolean, unit: TimeUnit): LogicalType =
            Time(isAdjustedToUTC, unit)

        public fun timestamp(isAdjustedToUTC: Boolean, unit: TimeUnit): LogicalType =
            Timestamp(isAdjustedToUTC, unit)

        public fun variant(specificationVersion: Byte?): LogicalType =
            Variant(specificationVersion)

        public fun geometry(crs: kotlin.String?): LogicalType =
            Geometry(crs)

        public fun geography(crs: kotlin.String?, algorithm: EdgeInterpolationAlgorithm?): LogicalType =
            Geography(crs, algorithm)

        public fun fromString(s: kotlin.String): LogicalType = when (s) {
            "INTEGER" -> integer(8, false)
            "MAP" -> Map
            "LIST" -> List
            "ENUM" -> Enum
            "DECIMAL" -> decimal(-1, -1)
            "DATE" -> Date
            "TIME" -> time(false, TimeUnit.MILLIS)
            "TIMESTAMP" -> timestamp(false, TimeUnit.MILLIS)
            "STRING" -> String
            "JSON" -> Json
            "BSON" -> Bson
            "UUID" -> Uuid
            "UNKNOWN" -> Unknown
            "INTERVAL" -> throw generalErr("Interval parquet logical type not yet supported")
            "FLOAT16" -> Float16
            "VARIANT" -> variant(null)
            "FILE" -> File
            "GEOMETRY" -> geometry(null)
            "GEOGRAPHY" -> geography(null, EdgeInterpolationAlgorithm.SPHERICAL)
            else -> throw generalErr("Invalid parquet logical type $s")
        }
    }
}

/**
 * Representation of field types in schema.
 */
public enum class FieldRepetitionType(public val value: Int) {
    REQUIRED(0),
    OPTIONAL(1),
    REPEATED(2);

    public companion object {
        public fun fromInt(value: Int): FieldRepetitionType = when (value) {
            0 -> REQUIRED
            1 -> OPTIONAL
            2 -> REPEATED
            else -> throw generalErr("Invalid FieldRepetitionType value: $value")
        }

        public fun fromString(s: String): FieldRepetitionType = when (s) {
            "REQUIRED" -> REQUIRED
            "OPTIONAL" -> OPTIONAL
            "REPEATED" -> REPEATED
            else -> throw generalErr("Invalid parquet repetition $s")
        }
    }
}

public typealias Repetition = FieldRepetitionType

/**
 * Encodings supported by Parquet.
 */
public enum class Encoding(public val value: Int) {
    PLAIN(0),
    PLAIN_DICTIONARY(2),
    RLE(3),
    BIT_PACKED(4),
    DELTA_BINARY_PACKED(5),
    DELTA_LENGTH_BYTE_ARRAY(6),
    DELTA_BYTE_ARRAY(7),
    RLE_DICTIONARY(8),
    BYTE_STREAM_SPLIT(9),
    ALP(10);

    public companion object {
        public const val MAX_DISCRIMINANT: Int = 10

        public fun fromInt(value: Int): Encoding = when (value) {
            0 -> PLAIN
            2 -> PLAIN_DICTIONARY
            3 -> RLE
            4 -> BIT_PACKED
            5 -> DELTA_BINARY_PACKED
            6 -> DELTA_LENGTH_BYTE_ARRAY
            7 -> DELTA_BYTE_ARRAY
            8 -> RLE_DICTIONARY
            9 -> BYTE_STREAM_SPLIT
            10 -> ALP
            else -> throw generalErr("Impossible encoding $value")
        }

        public fun fromString(s: String): Encoding = when (s.lowercase()) {
            "plain" -> PLAIN
            "plain_dictionary" -> PLAIN_DICTIONARY
            "rle" -> RLE
            "bit_packed" -> BIT_PACKED
            "delta_binary_packed" -> DELTA_BINARY_PACKED
            "delta_length_byte_array" -> DELTA_LENGTH_BYTE_ARRAY
            "delta_byte_array" -> DELTA_BYTE_ARRAY
            "rle_dictionary" -> RLE_DICTIONARY
            "byte_stream_split" -> BYTE_STREAM_SPLIT
            "alp" -> ALP
            else -> throw generalErr("unknown encoding: $s")
        }
    }
}

/**
 * A bitmask representing the Encodings employed while encoding a Parquet column chunk.
 */
public data class EncodingMask(public var mask: Int = 0) {
    public fun asInt(): Int = mask

    public fun insert(valEncoding: Encoding) {
        mask = mask or (1 shl valEncoding.value)
    }

    public fun isSet(valEncoding: Encoding): Boolean =
        (mask and (1 shl valEncoding.value)) != 0

    public fun isOnly(valEncoding: Encoding): Boolean =
        mask == (1 shl valEncoding.value)

    public fun allSet(encodings: Iterable<Encoding>): Boolean =
        encodings.all { isSet(it) }

    public fun encodings(): List<Encoding> =
        (0..Encoding.MAX_DISCRIMINANT)
            .filter { (mask and (1 shl it)) != 0 }
            .map { Encoding.fromInt(it) }

    public companion object {
        private const val MAX_ENCODING: Int = Encoding.MAX_DISCRIMINANT
        private const val ALLOWED_MASK: Long =
            ((1L shl (MAX_ENCODING + 1)) - 1L) and (1L shl 1).inv()

        public fun tryNew(value: Int): Result<EncodingMask> {
            val uVal = value.toLong() and 0xFFFFFFFFL
            if ((uVal and ALLOWED_MASK.inv()) != 0L || (uVal and (1L shl 1)) != 0L) {
                val hexStr = "0x" + uVal.toString(16)
                return Result.failure(generalErr("Attempt to create invalid mask: $hexStr"))
            }
            return Result.success(EncodingMask(value))
        }

        public fun newFromEncodings(encodings: Iterable<Encoding>): EncodingMask {
            var m = 0
            for (e in encodings) {
                m = m or (1 shl e.value)
            }
            return EncodingMask(m)
        }
    }
}

/**
 * Supported compression algorithms in Thrift metadata.
 */
public enum class CompressionCodec(public val value: Int) {
    UNCOMPRESSED(0),
    SNAPPY(1),
    GZIP(2),
    LZO(3),
    BROTLI(4),
    LZ4(5),
    ZSTD(6),
    LZ4_RAW(7);

    public companion object {
        public fun fromInt(value: Int): CompressionCodec = when (value) {
            0 -> UNCOMPRESSED
            1 -> SNAPPY
            2 -> GZIP
            3 -> LZO
            4 -> BROTLI
            5 -> LZ4
            6 -> ZSTD
            7 -> LZ4_RAW
            else -> throw generalErr("Invalid CompressionCodec value: $value")
        }
    }
}

public data class GzipLevel(public val level: Int = 6) {
    init {
        require(level in 0..9) { "Invalid gzip compression level: $level" }
    }
    public companion object {
        public fun tryNew(level: Int): Result<GzipLevel> =
            if (level in 0..9) Result.success(GzipLevel(level))
            else Result.failure(generalErr("Invalid gzip compression level: $level"))
    }
}

public data class BrotliLevel(public val level: Int = 1) {
    init {
        require(level in 0..11) { "Invalid brotli compression level: $level" }
    }
    public companion object {
        public fun tryNew(level: Int): Result<BrotliLevel> =
            if (level in 0..11) Result.success(BrotliLevel(level))
            else Result.failure(generalErr("Invalid brotli compression level: $level"))
    }
}

public data class ZstdLevel(public val level: Int = 1) {
    public companion object {
        public fun tryNew(level: Int): Result<ZstdLevel> =
            Result.success(ZstdLevel(level))
    }
}

/**
 * Supported block compression algorithms.
 */
public sealed class Compression {
    public data object UNCOMPRESSED : Compression() {
        override fun toString(): String = "UNCOMPRESSED"
    }

    public data object SNAPPY : Compression() {
        override fun toString(): String = "SNAPPY"
    }

    public data class GZIP(public val level: GzipLevel = GzipLevel()) : Compression() {
        override fun toString(): String = "GZIP(GzipLevel(${level.level}))"
    }

    public data object LZO : Compression() {
        override fun toString(): String = "LZO"
    }

    public data class BROTLI(public val level: BrotliLevel = BrotliLevel()) : Compression() {
        override fun toString(): String = "BROTLI(BrotliLevel(${level.level}))"
    }

    public data object LZ4 : Compression() {
        override fun toString(): String = "LZ4"
    }

    public data class ZSTD(public val level: ZstdLevel = ZstdLevel()) : Compression() {
        override fun toString(): String = "ZSTD(ZstdLevel(${level.level}))"
    }

    public data object LZ4_RAW : Compression() {
        override fun toString(): String = "LZ4_RAW"
    }

    public fun toCodec(): CompressionCodec = when (this) {
        is UNCOMPRESSED -> CompressionCodec.UNCOMPRESSED
        is SNAPPY -> CompressionCodec.SNAPPY
        is GZIP -> CompressionCodec.GZIP
        is LZO -> CompressionCodec.LZO
        is BROTLI -> CompressionCodec.BROTLI
        is LZ4 -> CompressionCodec.LZ4
        is ZSTD -> CompressionCodec.ZSTD
        is LZ4_RAW -> CompressionCodec.LZ4_RAW
    }

    public companion object {
        public fun fromCodec(codec: CompressionCodec): Compression = when (codec) {
            CompressionCodec.UNCOMPRESSED -> UNCOMPRESSED
            CompressionCodec.SNAPPY -> SNAPPY
            CompressionCodec.GZIP -> GZIP()
            CompressionCodec.LZO -> LZO
            CompressionCodec.BROTLI -> BROTLI()
            CompressionCodec.LZ4 -> LZ4
            CompressionCodec.ZSTD -> ZSTD()
            CompressionCodec.LZ4_RAW -> LZ4_RAW
        }

        public fun fromString(s: String): Compression {
            val (codec, level) = splitCompressionString(s)
            return when (codec.lowercase()) {
                "uncompressed" -> {
                    checkLevelIsNone(level)
                    UNCOMPRESSED
                }
                "snappy" -> {
                    checkLevelIsNone(level)
                    SNAPPY
                }
                "gzip" -> {
                    val lvl = requireLevel(codec, level)
                    GZIP(GzipLevel.tryNew(lvl).getOrThrow())
                }
                "lzo" -> {
                    checkLevelIsNone(level)
                    LZO
                }
                "brotli" -> {
                    val lvl = requireLevel(codec, level)
                    BROTLI(BrotliLevel.tryNew(lvl).getOrThrow())
                }
                "lz4" -> {
                    checkLevelIsNone(level)
                    LZ4
                }
                "zstd" -> {
                    val lvl = requireLevel(codec, level)
                    ZSTD(ZstdLevel.tryNew(lvl).getOrThrow())
                }
                "lz4_raw" -> {
                    checkLevelIsNone(level)
                    LZ4_RAW
                }
                else -> throw generalErr("unsupported compression $codec")
            }
        }

        private fun splitCompressionString(s: String): Pair<String, Int?> {
            val idx = s.indexOf('(')
            return if (idx >= 0 && s.endsWith(')')) {
                val codec = s.substring(0, idx)
                val lvlStr = s.substring(idx + 1, s.length - 1)
                val lvl = lvlStr.toIntOrNull()
                    ?: throw generalErr("invalid compression level: $lvlStr")
                Pair(codec, lvl)
            } else {
                Pair(s, null)
            }
        }

        private fun checkLevelIsNone(level: Int?) {
            if (level != null) throw generalErr("compression level is not supported")
        }

        private fun requireLevel(codec: String, level: Int?): Int =
            level ?: throw generalErr("$codec requires a compression level")
    }
}

/**
 * Available data pages for Parquet file format.
 */
public enum class PageType(public val value: Int) {
    DATA_PAGE(0),
    INDEX_PAGE(1),
    DICTIONARY_PAGE(2),
    DATA_PAGE_V2(3);

    public companion object {
        public fun fromInt(value: Int): PageType = when (value) {
            0 -> DATA_PAGE
            1 -> INDEX_PAGE
            2 -> DICTIONARY_PAGE
            3 -> DATA_PAGE_V2
            else -> throw generalErr("Invalid PageType value: $value")
        }
    }
}

/**
 * Enum to annotate whether lists of min/max elements inside ColumnIndex
 * are ordered and if so, in which direction.
 */
public enum class BoundaryOrder(public val value: Int) {
    UNORDERED(0),
    ASCENDING(1),
    DESCENDING(2);

    public companion object {
        public fun fromInt(value: Int): BoundaryOrder = when (value) {
            0 -> UNORDERED
            1 -> ASCENDING
            2 -> DESCENDING
            else -> throw generalErr("Invalid BoundaryOrder value: $value")
        }
    }
}

/**
 * Edge interpolation algorithm for LogicalType::Geography
 */
public sealed class EdgeInterpolationAlgorithm {
    public abstract val value: Int

    public data object SPHERICAL : EdgeInterpolationAlgorithm() {
        override val value: Int = 0
        override fun toString(): String = "SPHERICAL"
    }

    public data object VINCENTY : EdgeInterpolationAlgorithm() {
        override val value: Int = 1
        override fun toString(): String = "VINCENTY"
    }

    public data object THOMAS : EdgeInterpolationAlgorithm() {
        override val value: Int = 2
        override fun toString(): String = "THOMAS"
    }

    public data object ANDOYER : EdgeInterpolationAlgorithm() {
        override val value: Int = 3
        override fun toString(): String = "ANDOYER"
    }

    public data object KARNEY : EdgeInterpolationAlgorithm() {
        override val value: Int = 4
        override fun toString(): String = "KARNEY"
    }

    public data class Unknown(public val id: Int) : EdgeInterpolationAlgorithm() {
        override val value: Int = id
        override fun toString(): String = "_Unknown($id)"
    }

    public companion object {
        public fun fromInt(value: Int): EdgeInterpolationAlgorithm = when (value) {
            0 -> SPHERICAL
            1 -> VINCENTY
            2 -> THOMAS
            3 -> ANDOYER
            4 -> KARNEY
            else -> Unknown(value)
        }

        public fun fromString(s: String): EdgeInterpolationAlgorithm = when (s.uppercase()) {
            "SPHERICAL" -> SPHERICAL
            "VINCENTY" -> VINCENTY
            "THOMAS" -> THOMAS
            "ANDOYER" -> ANDOYER
            "KARNEY" -> KARNEY
            else -> throw generalErr("Unknown edge interpolation algorithm: $s")
        }
    }
}

/**
 * Sort order for page and column statistics.
 */
public enum class SortOrder {
    SIGNED,
    UNSIGNED,
    UNDEFINED,
    TOTAL_ORDER,
    INT96_TIMESTAMP;

    public val isSigned: Boolean
        get() = this == SIGNED
}

/**
 * Column order that specifies what method was used to aggregate min/max values for statistics.
 */
public sealed class ColumnOrder {
    public data class TypeDefinedOrder(public val sortOrder: SortOrder) : ColumnOrder() {
        override fun toString(): String = "TYPE_DEFINED_ORDER($sortOrder)"
    }

    public data object Ieee754TotalOrder : ColumnOrder() {
        override fun toString(): String = "IEEE_754_TOTAL_ORDER"
    }

    public data object Int96TimestampOrder : ColumnOrder() {
        override fun toString(): String = "INT96_TIMESTAMP_ORDER"
    }

    public data object Undefined : ColumnOrder() {
        override fun toString(): String = "UNDEFINED"
    }

    public data object Unknown : ColumnOrder() {
        override fun toString(): String = "UNKNOWN"
    }

    public fun sortOrder(): SortOrder = when (this) {
        is TypeDefinedOrder -> sortOrder
        is Ieee754TotalOrder -> SortOrder.TOTAL_ORDER
        is Int96TimestampOrder -> SortOrder.INT96_TIMESTAMP
        is Undefined -> SortOrder.SIGNED
        is Unknown -> SortOrder.UNDEFINED
    }

    public companion object {
        public fun columnOrderForType(
            logicalType: LogicalType?,
            convertedType: ConvertedType,
            physicalType: Type,
        ): ColumnOrder {
            return if (logicalType is LogicalType.Float16 || physicalType == Type.FLOAT || physicalType == Type.DOUBLE) {
                Ieee754TotalOrder
            } else if (physicalType == Type.INT96) {
                Int96TimestampOrder
            } else {
                val sortOrder = getSortOrderForType(logicalType, convertedType, physicalType, true)
                TypeDefinedOrder(sortOrder)
            }
        }

        public fun getSortOrderForType(
            logicalType: LogicalType?,
            convertedType: ConvertedType,
            physicalType: Type,
            isTypeDefined: Boolean,
        ): SortOrder {
            return if (logicalType != null) {
                when (logicalType) {
                    is LogicalType.String,
                    is LogicalType.Enum,
                    is LogicalType.Json,
                    is LogicalType.Bson -> SortOrder.UNSIGNED
                    is LogicalType.Integer -> if (logicalType.isSigned) SortOrder.SIGNED else SortOrder.UNSIGNED
                    is LogicalType.Map,
                    is LogicalType.List -> SortOrder.UNDEFINED
                    is LogicalType.Decimal,
                    is LogicalType.Date,
                    is LogicalType.Time,
                    is LogicalType.Timestamp -> SortOrder.SIGNED
                    is LogicalType.Unknown -> SortOrder.UNDEFINED
                    is LogicalType.Uuid -> SortOrder.UNSIGNED
                    is LogicalType.Float16 -> if (isTypeDefined) SortOrder.SIGNED else SortOrder.TOTAL_ORDER
                    is LogicalType.Variant,
                    is LogicalType.Geometry,
                    is LogicalType.Geography,
                    is LogicalType.File -> SortOrder.UNDEFINED
                }
            } else {
                getConvertedSortOrder(convertedType, physicalType, isTypeDefined)
            }
        }

        public fun getConvertedSortOrder(
            convertedType: ConvertedType,
            physicalType: Type,
            isTypeDefined: Boolean,
        ): SortOrder {
            return when (convertedType) {
                ConvertedType.UTF8,
                ConvertedType.JSON,
                ConvertedType.BSON,
                ConvertedType.ENUM -> SortOrder.UNSIGNED

                ConvertedType.INT_8,
                ConvertedType.INT_16,
                ConvertedType.INT_32,
                ConvertedType.INT_64 -> SortOrder.SIGNED

                ConvertedType.UINT_8,
                ConvertedType.UINT_16,
                ConvertedType.UINT_32,
                ConvertedType.UINT_64 -> SortOrder.UNSIGNED

                ConvertedType.DECIMAL -> SortOrder.SIGNED
                ConvertedType.DATE -> SortOrder.SIGNED

                ConvertedType.TIME_MILLIS,
                ConvertedType.TIME_MICROS,
                ConvertedType.TIMESTAMP_MILLIS,
                ConvertedType.TIMESTAMP_MICROS -> SortOrder.SIGNED

                ConvertedType.INTERVAL -> SortOrder.UNDEFINED

                ConvertedType.LIST,
                ConvertedType.MAP,
                ConvertedType.MAP_KEY_VALUE -> SortOrder.UNDEFINED

                ConvertedType.NONE -> getDefaultSortOrder(physicalType, isTypeDefined)
            }
        }

        public fun getDefaultSortOrder(physicalType: Type, isTypeDefined: Boolean): SortOrder {
            return when (physicalType) {
                Type.BOOLEAN -> SortOrder.UNSIGNED
                Type.INT32, Type.INT64 -> SortOrder.SIGNED
                Type.INT96 -> if (isTypeDefined) SortOrder.UNDEFINED else SortOrder.INT96_TIMESTAMP
                Type.FLOAT, Type.DOUBLE -> if (isTypeDefined) SortOrder.SIGNED else SortOrder.TOTAL_ORDER
                Type.BYTE_ARRAY, Type.FIXED_LEN_BYTE_ARRAY -> SortOrder.UNSIGNED
            }
        }
    }
}
