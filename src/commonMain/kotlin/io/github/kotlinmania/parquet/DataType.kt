// port-lint: source data_type.rs
package io.github.kotlinmania.parquet

import io.github.kotlinmania.bytes.Bytes

/**
 * Representation for logical type INT96, value is backed by an array of 3 unsigned 32-bit integers.
 * The type only takes 12 bytes, without extra padding.
 */
public data class Int96(
    private val value: UIntArray = UIntArray(3),
) : Comparable<Int96>, AsBytes {
    public constructor() : this(UIntArray(3))

    public fun data(): UIntArray = value

    public fun setData(elem0: UInt, elem1: UInt, elem2: UInt) {
        value[0] = elem0
        value[1] = elem1
        value[2] = elem2
    }

    public fun toSeconds(): Long {
        val (day, nanos) = dataAsDaysAndNanos()
        return (day.toLong() - JULIAN_DAY_OF_EPOCH) * SECONDS_IN_DAY + (nanos / 1_000_000_000L)
    }

    public fun toMillis(): Long {
        val (day, nanos) = dataAsDaysAndNanos()
        return (day.toLong() - JULIAN_DAY_OF_EPOCH) * MILLISECONDS_IN_DAY + (nanos / 1_000_000L)
    }

    public fun toMicros(): Long {
        val (day, nanos) = dataAsDaysAndNanos()
        return (day.toLong() - JULIAN_DAY_OF_EPOCH) * MICROSECONDS_IN_DAY + (nanos / 1_000L)
    }

    public fun toNanos(): Long {
        val (day, nanos) = dataAsDaysAndNanos()
        return (day.toLong() - JULIAN_DAY_OF_EPOCH) * NANOSECONDS_IN_DAY + nanos
    }

    public fun getDays(): Int = value[2].toInt()

    public fun getNanos(): Long =
        ((value[1].toLong() and 0xFFFFFFFFL) shl 32) or (value[0].toLong() and 0xFFFFFFFFL)

    internal fun dataAsDaysAndNanos(): DaysAndNanos =
        DaysAndNanos(getDays(), getNanos())

    override fun compareTo(other: Int96): Int {
        val cmp = getDays().compareTo(other.getDays())
        return if (cmp != 0) cmp else getNanos().compareTo(other.getNanos())
    }

    override fun asBytes(): kotlin.ByteArray {
        val bytes = kotlin.ByteArray(12)
        for (i in 0..2) {
            val v = value[i]
            bytes[i * 4] = (v and 0xFFu).toByte()
            bytes[i * 4 + 1] = ((v shr 8) and 0xFFu).toByte()
            bytes[i * 4 + 2] = ((v shr 16) and 0xFFu).toByte()
            bytes[i * 4 + 3] = ((v shr 24) and 0xFFu).toByte()
        }
        return bytes
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Int96) return false
        return value.contentEquals(other.value)
    }

    override fun hashCode(): Int = value.contentHashCode()

    override fun toString(): String =
        "[${value[0]}, ${value[1]}, ${value[2]}]"

    public companion object {
        public const val JULIAN_DAY_OF_EPOCH: Long = 2_440_588L
        public const val SECONDS_IN_DAY: Long = 86_400L
        public const val MILLISECONDS: Long = 1_000L
        public const val MICROSECONDS: Long = 1_000_000L
        public const val NANOSECONDS: Long = 1_000_000_000L
        public const val MILLISECONDS_IN_DAY: Long = SECONDS_IN_DAY * MILLISECONDS
        public const val MICROSECONDS_IN_DAY: Long = SECONDS_IN_DAY * MICROSECONDS
        public const val NANOSECONDS_IN_DAY: Long = SECONDS_IN_DAY * NANOSECONDS

        public fun new(): Int96 = Int96()

        public fun from(buf: List<UInt>): Int96 {
            require(buf.size == 3) { "Int96 requires exactly 3 elements" }
            val res = Int96()
            res.setData(buf[0], buf[1], buf[2])
            return res
        }
    }
}

internal data class DaysAndNanos(val days: Int, val nanos: Long)

/**
 * Representation for BYTE_ARRAY and FIXED_LEN_BYTE_ARRAY Parquet physical types.
 * Value is backed by a byte buffer.
 */
public class ByteArray(
    public var rawData: Bytes? = null,
) : Comparable<ByteArray>, AsBytes {
    public constructor() : this(null)

    public fun len(): Int {
        val d = rawData ?: throw generalErr("set_data should have been called")
        return d.len()
    }

    public fun isEmpty(): Boolean = len() == 0

    public fun data(): kotlin.ByteArray {
        val d = rawData ?: throw generalErr("set_data should have been called")
        return d.asSlice()
    }

    public fun bytes(): Bytes? = rawData

    public fun setData(data: Bytes) {
        this.rawData = data
    }

    public fun slice(start: Int, len: Int): ByteArray {
        val d = rawData ?: throw generalErr("set_data should have been called")
        return ByteArray(d.slice(start, start + len))
    }

    public fun asUtf8(): String {
        val d = rawData ?: throw generalErr("Can't convert empty byte array to utf8")
        return d.asSlice().decodeToString()
    }

    override fun asBytes(): kotlin.ByteArray = data()

    override fun compareTo(other: ByteArray): Int {
        val d1 = rawData
        val d2 = other.rawData
        if (d1 == null && d2 == null) return 0
        if (d1 == null) return -1
        if (d2 == null) return 1
        val b1 = d1.asSlice()
        val b2 = d2.asSlice()
        val minLen = minOf(b1.size, b2.size)
        for (i in 0 until minLen) {
            val cmp = (b1[i].toInt() and 0xFF).compareTo(b2[i].toInt() and 0xFF)
            if (cmp != 0) return cmp
        }
        return b1.size.compareTo(b2.size)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ByteArray) return false
        val d1 = rawData
        val d2 = other.rawData
        if (d1 == null && d2 == null) return true
        if (d1 == null || d2 == null) return false
        return d1.asSlice().contentEquals(d2.asSlice())
    }

    override fun hashCode(): Int = rawData?.asSlice()?.contentHashCode() ?: 0

    override fun toString(): String {
        val d = rawData ?: return "null"
        return d.asSlice().contentToString()
    }

    public companion object {
        public fun new(): ByteArray = ByteArray()
        public fun from(bytes: Bytes): ByteArray = ByteArray(bytes)
        public fun from(buf: kotlin.ByteArray): ByteArray = ByteArray(Bytes.from(buf))
        public fun from(s: String): ByteArray = ByteArray(Bytes.from(s))
    }
}

/**
 * Wrapper type for FIXED_LEN_BYTE_ARRAY Parquet physical type.
 */
public class FixedLenByteArray(
    public val byteArray: ByteArray = ByteArray(),
) : Comparable<FixedLenByteArray>, AsBytes {
    public constructor(buf: kotlin.ByteArray) : this(ByteArray.from(buf))
    public constructor(data: Bytes) : this(ByteArray.from(data))

    public fun len(): Int = byteArray.len()
    public fun isEmpty(): Boolean = byteArray.isEmpty()
    public fun data(): kotlin.ByteArray = byteArray.data()
    public fun bytes(): Bytes? = byteArray.bytes()
    public fun setData(data: Bytes): Unit = byteArray.setData(data)
    public fun slice(start: Int, len: Int): ByteArray = byteArray.slice(start, len)

    override fun asBytes(): kotlin.ByteArray = byteArray.asBytes()

    override fun compareTo(other: FixedLenByteArray): Int =
        byteArray.compareTo(other.byteArray)

    public fun compareTo(other: ByteArray): Int =
        byteArray.compareTo(other)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other is FixedLenByteArray) return byteArray == other.byteArray
        if (other is ByteArray) return byteArray == other
        return false
    }

    override fun hashCode(): Int = byteArray.hashCode()

    override fun toString(): String = byteArray.toString()

    public companion object {
        public fun from(other: ByteArray): FixedLenByteArray = FixedLenByteArray(other)
        public fun from(buf: kotlin.ByteArray): FixedLenByteArray = FixedLenByteArray(ByteArray.from(buf))
    }
}

/**
 * Representation for Decimal values.
 *
 * This is a container for raw parts of decimal values: unscaled value in bytes, precision and scale.
 */
public sealed class Decimal : AsBytes {
    public abstract val precision: Int
    public abstract val scale: Int
    public abstract fun data(): kotlin.ByteArray

    public data class Int32(
        public val value: kotlin.ByteArray,
        override val precision: Int,
        override val scale: Int,
    ) : Decimal() {
        override fun data(): kotlin.ByteArray = value

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Decimal) return false
            return precision == other.precision && scale == other.scale && value.contentEquals(other.data())
        }

        override fun hashCode(): Int {
            var result = value.contentHashCode()
            result = 31 * result + precision
            result = 31 * result + scale
            return result
        }
    }

    public data class Int64(
        public val value: kotlin.ByteArray,
        override val precision: Int,
        override val scale: Int,
    ) : Decimal() {
        override fun data(): kotlin.ByteArray = value

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Decimal) return false
            return precision == other.precision && scale == other.scale && value.contentEquals(other.data())
        }

        override fun hashCode(): Int {
            var result = value.contentHashCode()
            result = 31 * result + precision
            result = 31 * result + scale
            return result
        }
    }

    public data class Bytes(
        public val value: ByteArray,
        override val precision: Int,
        override val scale: Int,
    ) : Decimal() {
        override fun data(): kotlin.ByteArray = value.data()

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Decimal) return false
            return precision == other.precision && scale == other.scale && data().contentEquals(other.data())
        }

        override fun hashCode(): Int {
            var result = value.hashCode()
            result = 31 * result + precision
            result = 31 * result + scale
            return result
        }
    }

    override fun asBytes(): kotlin.ByteArray = data()

    public companion object {
        public fun default(): Decimal = fromI32(0, 0, 0)

        public fun fromI32(value: Int, precision: Int, scale: Int): Decimal {
            val bytes = kotlin.ByteArray(4)
            bytes[0] = ((value shr 24) and 0xFF).toByte()
            bytes[1] = ((value shr 16) and 0xFF).toByte()
            bytes[2] = ((value shr 8) and 0xFF).toByte()
            bytes[3] = (value and 0xFF).toByte()
            return Int32(bytes, precision, scale)
        }

        public fun fromI64(value: Long, precision: Int, scale: Int): Decimal {
            val bytes = kotlin.ByteArray(8)
            for (i in 0..7) {
                bytes[i] = ((value shr ((7 - i) * 8)) and 0xFF).toByte()
            }
            return Int64(bytes, precision, scale)
        }

        public fun fromBytes(value: ByteArray, precision: Int, scale: Int): Decimal =
            Bytes(value, precision, scale)
    }
}

/**
 * Converts an instance of data type to a slice of bytes as bytes.
 */
public interface AsBytes {
    public fun asBytes(): kotlin.ByteArray
}

/**
 * Converts a slice of a data type to a slice of bytes.
 */
public interface SliceAsBytes {
    public fun sliceAsBytes(data: kotlin.ByteArray): kotlin.ByteArray = data
}

/**
 * Encoded size pair for dictionary encoding: (fixed size in bytes, variable size in bytes).
 */
public data class DictEncodingSize(
    public val fixedSize: Int,
    public val variableSize: Int,
)

/**
 * Value type contract for Parquet values.
 */
public interface ParquetValueType : AsBytes {
    public val physicalType: ParquetType

    public fun dictEncodingSize(): DictEncodingSize = DictEncodingSize(0, 0)

    public fun asI64(): Long = throw generalErr("Type cannot be converted to i64")

    public fun asU64(): ULong = asI64().toULong()

    public fun setFromBytes(data: Bytes) {
        throw nyiErr("setFromBytes is only supported for ByteArray types")
    }

    public companion object {
        public fun variableLengthBytes(values: List<ByteArray>): Long? =
            values.sumOf { it.len().toLong() }
    }
}

/**
 * Parquet physical type information as well as primitive type presentation.
 */
public interface DataType {
    public val physicalType: ParquetType
    public val typeSize: Int
}

public data object BoolType : DataType {
    override val physicalType: ParquetType = ParquetType.BOOLEAN
    override val typeSize: Int = 1
}

public data object Int32Type : DataType {
    override val physicalType: ParquetType = ParquetType.INT32
    override val typeSize: Int = 4
}

public data object Int64Type : DataType {
    override val physicalType: ParquetType = ParquetType.INT64
    override val typeSize: Int = 8
}

public data object Int96Type : DataType {
    override val physicalType: ParquetType = ParquetType.INT96
    override val typeSize: Int = 12
}

public data object FloatType : DataType {
    override val physicalType: ParquetType = ParquetType.FLOAT
    override val typeSize: Int = 4
}

public data object DoubleType : DataType {
    override val physicalType: ParquetType = ParquetType.DOUBLE
    override val typeSize: Int = 8
}

public data object ByteArrayType : DataType {
    override val physicalType: ParquetType = ParquetType.BYTE_ARRAY
    override val typeSize: Int = 0
}

public data object FixedLenByteArrayType : DataType {
    override val physicalType: ParquetType = ParquetType.FIXED_LEN_BYTE_ARRAY
    override val typeSize: Int = 0
}
