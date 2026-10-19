// port-lint: source record/api.rs
package io.github.kotlinmania.parquet.record

import io.github.kotlinmania.base64.prelude.BASE64_STANDARD
import io.github.kotlinmania.chrono.DateTime
import io.github.kotlinmania.chrono.naive.NaiveDate
import io.github.kotlinmania.half.F16
import io.github.kotlinmania.numbigint.BigInt
import io.github.kotlinmania.numbigint.Sign
import io.github.kotlinmania.parquet.ByteArray
import io.github.kotlinmania.parquet.ConvertedType
import io.github.kotlinmania.parquet.Decimal
import io.github.kotlinmania.parquet.Int96
import io.github.kotlinmania.parquet.LogicalType
import io.github.kotlinmania.parquet.ParquetError
import io.github.kotlinmania.parquet.ParquetType
import io.github.kotlinmania.parquet.generalErr
import io.github.kotlinmania.parquet.nyiErr
import io.github.kotlinmania.parquet.schema.ColumnDescriptor
import io.github.kotlinmania.serdejson.JsonMap
import io.github.kotlinmania.serdejson.JsonNumber
import io.github.kotlinmania.serdejson.Value

public typealias Item = RowColumn

/**
 * A column (field) within a Parquet [Row].
 */
public data class RowColumn(
    public val name: String,
    public val value: Field,
) {
    public val field: Field get() = value
    public val first: String get() = name
    public val second: Field get() = value
}

/**
 * A key-value entry within a Parquet [Map].
 */
public data class MapEntry(
    public val key: Field,
    public val value: Field,
) {
    public val first: Field get() = key
    public val second: Field get() = value
}

/**
 * `Row` represents a nested Parquet record.
 */
public class Row(
    private val fieldsVal: kotlin.collections.List<RowColumn>,
) : RowAccessor,
    RowFormatter {
    public companion object {
        /** Constructs a `Row` from the list of `fields` and returns it. */
        public fun new(fields: kotlin.collections.List<RowColumn>): Row = Row(fields)
    }

    /** Get the number of fields in this row. */
    public fun len(): Int = fieldsVal.size

    /** Move columns data out of the row. Useful to avoid internal data cloning. */
    public fun into_columns(): kotlin.collections.List<RowColumn> = fieldsVal

    /** Get an iterator to go through all columns in the row. */
    public fun get_column_iter(): RowColumnIter = RowColumnIter(fieldsVal)

    /** Converts the row into a JSON object. */
    public fun to_json_value(): Value {
        val jsonMap = JsonMap()
        for ((key, field) in fieldsVal) {
            jsonMap.insert(key, field.to_json_value())
        }
        return Value.Object(jsonMap)
    }

    /** Get Display reference for a given field. */
    override fun fmt(i: Int): Any =
        if (i in fieldsVal.indices) {
            fieldsVal[i].second
        } else {
            "<IndexOutOfBound>"
        }

    public fun fmt(): String = toString()

    override fun is_null(i: Int): Result<Boolean> {
        if (i !in fieldsVal.indices) {
            return Result.failure(ParquetError.IndexOutOfBound(i.toLong(), fieldsVal.size.toLong()))
        }
        return Result.success(fieldsVal[i].second is Field.Null)
    }

    override fun get_bool(i: Int): Result<Boolean> = getPrimitive(i, "Bool") { (it as? Field.Bool)?.value }

    override fun get_byte(i: Int): Result<Byte> = getPrimitive(i, "Byte") { (it as? Field.Byte)?.value }

    override fun get_short(i: Int): Result<Short> = getPrimitive(i, "Short") { (it as? Field.Short)?.value }

    override fun get_int(i: Int): Result<Int> = getPrimitive(i, "Int") { (it as? Field.Int)?.value }

    override fun get_long(i: Int): Result<Long> = getPrimitive(i, "Long") { (it as? Field.Long)?.value }

    override fun get_ubyte(i: Int): Result<UByte> = getPrimitive(i, "UByte") { (it as? Field.UByte)?.value }

    override fun get_ushort(i: Int): Result<UShort> = getPrimitive(i, "UShort") { (it as? Field.UShort)?.value }

    override fun get_uint(i: Int): Result<UInt> = getPrimitive(i, "UInt") { (it as? Field.UInt)?.value }

    override fun get_ulong(i: Int): Result<ULong> = getPrimitive(i, "ULong") { (it as? Field.ULong)?.value }

    override fun get_float16(i: Int): Result<F16> = getPrimitive(i, "Float16") { (it as? Field.Float16)?.value }

    override fun get_float(i: Int): Result<Float> = getPrimitive(i, "Float") { (it as? Field.Float)?.value }

    override fun get_double(i: Int): Result<Double> = getPrimitive(i, "Double") { (it as? Field.Double)?.value }

    override fun get_timestamp_millis(i: Int): Result<Long> =
        getPrimitive(i, "TimestampMillis") { (it as? Field.TimestampMillis)?.value }

    override fun get_timestamp_micros(i: Int): Result<Long> =
        getPrimitive(i, "TimestampMicros") { (it as? Field.TimestampMicros)?.value }

    override fun get_decimal(i: Int): Result<Decimal> =
        getComplex(i, "Decimal") { (it as? Field.Decimal)?.value }

    override fun get_string(i: Int): Result<String> =
        getComplex(i, "Str") { (it as? Field.Str)?.value }

    override fun get_bytes(i: Int): Result<ByteArray> =
        getComplex(i, "Bytes") { (it as? Field.Bytes)?.value }

    override fun get_group(i: Int): Result<Row> =
        getComplex(i, "Group") { (it as? Field.Group)?.value }

    override fun get_list(i: Int): Result<List> =
        getComplex(i, "ListInternal") { (it as? Field.ListInternal)?.value }

    override fun get_map(i: Int): Result<Map> =
        getComplex(i, "MapInternal") { (it as? Field.MapInternal)?.value }

    private inline fun <T> getPrimitive(
        i: Int,
        variantName: String,
        extract: (Field) -> T?,
    ): Result<T> {
        if (i !in fieldsVal.indices) {
            return Result.failure(ParquetError.IndexOutOfBound(i.toLong(), fieldsVal.size.toLong()))
        }
        val field = fieldsVal[i].second
        val v =
            extract(field)
                ?: return Result.failure(generalErr("Cannot access ${field.get_type_name()} as $variantName"))
        return Result.success(v)
    }

    private inline fun <T> getComplex(
        i: Int,
        variantName: String,
        extract: (Field) -> T?,
    ): Result<T> {
        if (i !in fieldsVal.indices) {
            return Result.failure(ParquetError.IndexOutOfBound(i.toLong(), fieldsVal.size.toLong()))
        }
        val field = fieldsVal[i].second
        val v =
            extract(field)
                ?: return Result.failure(generalErr("Cannot access ${field.get_type_name()} as $variantName"))
        return Result.success(v)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Row) return false
        return fieldsVal == other.fieldsVal
    }

    override fun hashCode(): Int = fieldsVal.hashCode()

    override fun toString(): String =
        buildString {
            append("{")
            for ((i, pair) in fieldsVal.withIndex()) {
                append(pair.name)
                append(": ")
                append(pair.field.toString())
                if (i < fieldsVal.size - 1) {
                    append(", ")
                }
            }
            append("}")
        }
}

/**
 * `RowColumnIter` represents an iterator over column names and values in a Row.
 */
public class RowColumnIter(
    private val fields: kotlin.collections.List<RowColumn>,
    private var curr: Int = 0,
    private val count: Int = fields.size,
) : Iterator<RowColumn> {
    override fun hasNext(): Boolean = curr < count

    override fun next(): RowColumn {
        if (!hasNext()) {
            throw NoSuchElementException()
        }
        val item = fields[curr]
        curr += 1
        return item
    }
}

/**
 * Trait for type-safe convenient access to fields within a Row.
 */
public interface RowAccessor {
    public fun is_null(i: Int): Result<Boolean>

    public fun get_bool(i: Int): Result<Boolean>

    public fun get_byte(i: Int): Result<Byte>

    public fun get_short(i: Int): Result<Short>

    public fun get_int(i: Int): Result<Int>

    public fun get_long(i: Int): Result<Long>

    public fun get_ubyte(i: Int): Result<UByte>

    public fun get_ushort(i: Int): Result<UShort>

    public fun get_uint(i: Int): Result<UInt>

    public fun get_ulong(i: Int): Result<ULong>

    public fun get_float16(i: Int): Result<F16>

    public fun get_float(i: Int): Result<Float>

    public fun get_double(i: Int): Result<Double>

    public fun get_timestamp_millis(i: Int): Result<Long>

    public fun get_timestamp_micros(i: Int): Result<Long>

    public fun get_decimal(i: Int): Result<Decimal>

    public fun get_string(i: Int): Result<String>

    public fun get_bytes(i: Int): Result<ByteArray>

    public fun get_group(i: Int): Result<Row>

    public fun get_list(i: Int): Result<List>

    public fun get_map(i: Int): Result<Map>
}

/**
 * Trait for formatting fields within a Row.
 */
public interface RowFormatter {
    public fun fmt(i: Int): Any
}

/**
 * `List` represents a list which contains an array of elements.
 */
public class List(
    private val elementsVal: kotlin.collections.List<Field>,
) : ListAccessor {
    public fun len(): Int = elementsVal.size

    public fun elements(): kotlin.collections.List<Field> = elementsVal

    override fun get_bool(i: Int): Result<Boolean> = getPrimitive(i, "Bool") { (it as? Field.Bool)?.value }

    override fun get_byte(i: Int): Result<Byte> = getPrimitive(i, "Byte") { (it as? Field.Byte)?.value }

    override fun get_short(i: Int): Result<Short> = getPrimitive(i, "Short") { (it as? Field.Short)?.value }

    override fun get_int(i: Int): Result<Int> = getPrimitive(i, "Int") { (it as? Field.Int)?.value }

    override fun get_long(i: Int): Result<Long> = getPrimitive(i, "Long") { (it as? Field.Long)?.value }

    override fun get_ubyte(i: Int): Result<UByte> = getPrimitive(i, "UByte") { (it as? Field.UByte)?.value }

    override fun get_ushort(i: Int): Result<UShort> = getPrimitive(i, "UShort") { (it as? Field.UShort)?.value }

    override fun get_uint(i: Int): Result<UInt> = getPrimitive(i, "UInt") { (it as? Field.UInt)?.value }

    override fun get_ulong(i: Int): Result<ULong> = getPrimitive(i, "ULong") { (it as? Field.ULong)?.value }

    override fun get_float16(i: Int): Result<F16> = getPrimitive(i, "Float16") { (it as? Field.Float16)?.value }

    override fun get_float(i: Int): Result<Float> = getPrimitive(i, "Float") { (it as? Field.Float)?.value }

    override fun get_double(i: Int): Result<Double> = getPrimitive(i, "Double") { (it as? Field.Double)?.value }

    override fun get_timestamp_millis(i: Int): Result<Long> =
        getPrimitive(i, "TimestampMillis") { (it as? Field.TimestampMillis)?.value }

    override fun get_timestamp_micros(i: Int): Result<Long> =
        getPrimitive(i, "TimestampMicros") { (it as? Field.TimestampMicros)?.value }

    override fun get_decimal(i: Int): Result<Decimal> =
        getComplex(i, "Decimal") { (it as? Field.Decimal)?.value }

    override fun get_string(i: Int): Result<String> =
        getComplex(i, "Str") { (it as? Field.Str)?.value }

    override fun get_bytes(i: Int): Result<ByteArray> =
        getComplex(i, "Bytes") { (it as? Field.Bytes)?.value }

    override fun get_group(i: Int): Result<Row> =
        getComplex(i, "Group") { (it as? Field.Group)?.value }

    override fun get_list(i: Int): Result<List> =
        getComplex(i, "ListInternal") { (it as? Field.ListInternal)?.value }

    override fun get_map(i: Int): Result<Map> =
        getComplex(i, "MapInternal") { (it as? Field.MapInternal)?.value }

    private inline fun <T> getPrimitive(
        i: Int,
        variantName: String,
        extract: (Field) -> T?,
    ): Result<T> {
        val field = elementsVal[i]
        val v =
            extract(field)
                ?: return Result.failure(generalErr("Cannot access ${field.get_type_name()} as $variantName"))
        return Result.success(v)
    }

    private inline fun <T> getComplex(
        i: Int,
        variantName: String,
        extract: (Field) -> T?,
    ): Result<T> {
        val field = elementsVal[i]
        val v =
            extract(field)
                ?: return Result.failure(generalErr("Cannot access ${field.get_type_name()} as $variantName"))
        return Result.success(v)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is List) return false
        return elementsVal == other.elementsVal
    }

    override fun hashCode(): Int = elementsVal.hashCode()

    override fun toString(): String =
        buildString {
            append("[")
            for ((i, f) in elementsVal.withIndex()) {
                append(f.toString())
                if (i < elementsVal.size - 1) append(", ")
            }
            append("]")
        }
}

/**
 * Constructs a `List` from the list of `fields` and returns it.
 */
public fun make_list(elements: kotlin.collections.List<Field>): List = List(elements)

/**
 * Trait for type-safe access of an index for a `List`.
 */
public interface ListAccessor {
    public fun get_bool(i: Int): Result<Boolean>

    public fun get_byte(i: Int): Result<Byte>

    public fun get_short(i: Int): Result<Short>

    public fun get_int(i: Int): Result<Int>

    public fun get_long(i: Int): Result<Long>

    public fun get_ubyte(i: Int): Result<UByte>

    public fun get_ushort(i: Int): Result<UShort>

    public fun get_uint(i: Int): Result<UInt>

    public fun get_ulong(i: Int): Result<ULong>

    public fun get_float16(i: Int): Result<F16>

    public fun get_float(i: Int): Result<Float>

    public fun get_double(i: Int): Result<Double>

    public fun get_timestamp_millis(i: Int): Result<Long>

    public fun get_timestamp_micros(i: Int): Result<Long>

    public fun get_decimal(i: Int): Result<Decimal>

    public fun get_string(i: Int): Result<String>

    public fun get_bytes(i: Int): Result<ByteArray>

    public fun get_group(i: Int): Result<Row>

    public fun get_list(i: Int): Result<List>

    public fun get_map(i: Int): Result<Map>
}

/**
 * `Map` represents a map which contains a list of key->value pairs.
 */
public class Map(
    private val entriesVal: kotlin.collections.List<MapEntry>,
) : MapAccessor {
    public companion object {
        public fun new(entries: kotlin.collections.List<MapEntry>): Map = Map(entries)
    }

    public fun len(): Int = entriesVal.size

    public fun entries(): kotlin.collections.List<MapEntry> = entriesVal

    override fun get_keys(): ListAccessor = MapList(entriesVal.map { it.key })

    override fun get_values(): ListAccessor = MapList(entriesVal.map { it.value })

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Map) return false
        return entriesVal == other.entriesVal
    }

    override fun hashCode(): Int = entriesVal.hashCode()

    override fun toString(): String =
        buildString {
            append("{")
            for ((i, entry) in entriesVal.withIndex()) {
                append(entry.key.toString())
                append(" -> ")
                append(entry.value.toString())
                if (i < entriesVal.size - 1) append(", ")
            }
            append("}")
        }
}

/**
 * Constructs a `Map` from the list of `entries` and returns it.
 */
public fun make_map(entries: kotlin.collections.List<MapEntry>): Map = Map(entries)

/**
 * Trait for type-safe access of an index for a `Map`.
 */
public interface MapAccessor {
    public fun get_keys(): ListAccessor

    public fun get_values(): ListAccessor
}

internal class MapList(
    private val elements: kotlin.collections.List<Field>,
) : ListAccessor {
    override fun get_bool(i: Int): Result<Boolean> = getPrimitive(i, "Bool") { (it as? Field.Bool)?.value }

    override fun get_byte(i: Int): Result<Byte> = getPrimitive(i, "Byte") { (it as? Field.Byte)?.value }

    override fun get_short(i: Int): Result<Short> = getPrimitive(i, "Short") { (it as? Field.Short)?.value }

    override fun get_int(i: Int): Result<Int> = getPrimitive(i, "Int") { (it as? Field.Int)?.value }

    override fun get_long(i: Int): Result<Long> = getPrimitive(i, "Long") { (it as? Field.Long)?.value }

    override fun get_ubyte(i: Int): Result<UByte> = getPrimitive(i, "UByte") { (it as? Field.UByte)?.value }

    override fun get_ushort(i: Int): Result<UShort> = getPrimitive(i, "UShort") { (it as? Field.UShort)?.value }

    override fun get_uint(i: Int): Result<UInt> = getPrimitive(i, "UInt") { (it as? Field.UInt)?.value }

    override fun get_ulong(i: Int): Result<ULong> = getPrimitive(i, "ULong") { (it as? Field.ULong)?.value }

    override fun get_float16(i: Int): Result<F16> = getPrimitive(i, "Float16") { (it as? Field.Float16)?.value }

    override fun get_float(i: Int): Result<Float> = getPrimitive(i, "Float") { (it as? Field.Float)?.value }

    override fun get_double(i: Int): Result<Double> = getPrimitive(i, "Double") { (it as? Field.Double)?.value }

    override fun get_timestamp_millis(i: Int): Result<Long> =
        getPrimitive(i, "TimestampMillis") { (it as? Field.TimestampMillis)?.value }

    override fun get_timestamp_micros(i: Int): Result<Long> =
        getPrimitive(i, "TimestampMicros") { (it as? Field.TimestampMicros)?.value }

    override fun get_decimal(i: Int): Result<Decimal> =
        getComplex(i, "Decimal") { (it as? Field.Decimal)?.value }

    override fun get_string(i: Int): Result<String> =
        getComplex(i, "Str") { (it as? Field.Str)?.value }

    override fun get_bytes(i: Int): Result<ByteArray> =
        getComplex(i, "Bytes") { (it as? Field.Bytes)?.value }

    override fun get_group(i: Int): Result<Row> =
        getComplex(i, "Group") { (it as? Field.Group)?.value }

    override fun get_list(i: Int): Result<List> =
        getComplex(i, "ListInternal") { (it as? Field.ListInternal)?.value }

    override fun get_map(i: Int): Result<Map> =
        getComplex(i, "MapInternal") { (it as? Field.MapInternal)?.value }

    private inline fun <T> getPrimitive(
        i: Int,
        variantName: String,
        extract: (Field) -> T?,
    ): Result<T> {
        val field = elements[i]
        val v =
            extract(field)
                ?: return Result.failure(generalErr("Cannot access ${field.get_type_name()} as $variantName"))
        return Result.success(v)
    }

    private inline fun <T> getComplex(
        i: Int,
        variantName: String,
        extract: (Field) -> T?,
    ): Result<T> {
        val field = elements[i]
        val v =
            extract(field)
                ?: return Result.failure(generalErr("Cannot access ${field.get_type_name()} as $variantName"))
        return Result.success(v)
    }
}

/**
 * API to represent a single field in a `Row`.
 */
public sealed class Field {
    abstract override fun toString(): String

    public object Null : Field() {
        override fun toString(): String = "null"
    }

    public data class Bool(
        public val value: Boolean,
    ) : Field() {
        override fun toString(): String = value.toString()
    }

    public data class Byte(
        public val value: kotlin.Byte,
    ) : Field() {
        override fun toString(): String = value.toString()
    }

    public data class Short(
        public val value: kotlin.Short,
    ) : Field() {
        override fun toString(): String = value.toString()
    }

    public data class Int(
        public val value: kotlin.Int,
    ) : Field() {
        override fun toString(): String = value.toString()
    }

    public data class Long(
        public val value: kotlin.Long,
    ) : Field() {
        override fun toString(): String = value.toString()
    }

    public data class UByte(
        public val value: kotlin.UByte,
    ) : Field() {
        override fun toString(): String = value.toString()
    }

    public data class UShort(
        public val value: kotlin.UShort,
    ) : Field() {
        override fun toString(): String = value.toString()
    }

    public data class UInt(
        public val value: kotlin.UInt,
    ) : Field() {
        override fun toString(): String = value.toString()
    }

    public data class ULong(
        public val value: kotlin.ULong,
    ) : Field() {
        override fun toString(): String = value.toString()
    }

    public data class Float16(
        public val value: F16,
    ) : Field() {
        override fun toString(): String = formatFloat16(value)
    }

    public data class Float(
        public val value: kotlin.Float,
    ) : Field() {
        override fun toString(): String = formatFloat(value)
    }

    public data class Double(
        public val value: kotlin.Double,
    ) : Field() {
        override fun toString(): String = formatDouble(value)
    }

    public data class Decimal(
        public val value: io.github.kotlinmania.parquet.Decimal,
    ) : Field() {
        override fun toString(): String = convert_decimal_to_string(value)
    }

    public data class Str(
        public val value: String,
    ) : Field() {
        override fun toString(): String = "\"$value\""
    }

    public data class Bytes(
        public val value: ByteArray,
    ) : Field() {
        override fun toString(): String = value.data().map { it.toInt() and 0xFF }.joinToString(prefix = "[", postfix = "]")
    }

    public data class Date(
        public val value: kotlin.Int,
    ) : Field() {
        override fun toString(): String = convert_date_to_string(value)
    }

    public data class TimeMillis(
        public val value: kotlin.Int,
    ) : Field() {
        override fun toString(): String = convert_time_millis_to_string(value)
    }

    public data class TimeMicros(
        public val value: kotlin.Long,
    ) : Field() {
        override fun toString(): String = convert_time_micros_to_string(value)
    }

    public data class TimestampMillis(
        public val value: kotlin.Long,
    ) : Field() {
        override fun toString(): String = convert_timestamp_millis_to_string(value)
    }

    public data class TimestampMicros(
        public val value: kotlin.Long,
    ) : Field() {
        override fun toString(): String = convert_timestamp_micros_to_string(value)
    }

    public data class Group(
        public val value: Row,
    ) : Field() {
        override fun toString(): String = value.toString()
    }

    public data class ListInternal(
        public val value: List,
    ) : Field() {
        override fun toString(): String = value.toString()
    }

    public data class MapInternal(
        public val value: Map,
    ) : Field() {
        override fun toString(): String = value.toString()
    }

    internal fun get_type_name(): String =
        when (this) {
            is Null -> "Null"
            is Bool -> "Bool"
            is Byte -> "Byte"
            is Short -> "Short"
            is Int -> "Int"
            is Long -> "Long"
            is UByte -> "UByte"
            is UShort -> "UShort"
            is UInt -> "UInt"
            is ULong -> "ULong"
            is Float16 -> "Float16"
            is Float -> "Float"
            is Double -> "Double"
            is Decimal -> "Decimal"
            is Date -> "Date"
            is Str -> "Str"
            is Bytes -> "Bytes"
            is TimeMillis -> "TimeMillis"
            is TimeMicros -> "TimeMicros"
            is TimestampMillis -> "TimestampMillis"
            is TimestampMicros -> "TimestampMicros"
            is Group -> "Group"
            is ListInternal -> "ListInternal"
            is MapInternal -> "MapInternal"
        }

    public fun is_primitive(): Boolean =
        when (this) {
            is Group, is ListInternal, is MapInternal -> false
            else -> true
        }

    public fun to_json_value(): Value =
        when (this) {
            is Null -> {
                Value.Null
            }

            is Bool -> {
                Value.Bool(value)
            }

            is Byte -> {
                Value.Number(JsonNumber.fromI64(value.toLong()))
            }

            is Short -> {
                Value.Number(JsonNumber.fromI64(value.toLong()))
            }

            is Int -> {
                Value.Number(JsonNumber.fromI64(value.toLong()))
            }

            is Long -> {
                Value.Number(JsonNumber.fromI64(value))
            }

            is UByte -> {
                Value.Number(JsonNumber.fromU64(value.toULong()))
            }

            is UShort -> {
                Value.Number(JsonNumber.fromU64(value.toULong()))
            }

            is UInt -> {
                Value.Number(JsonNumber.fromU64(value.toULong()))
            }

            is ULong -> {
                Value.Number(JsonNumber.fromU64(value))
            }

            is Float16 -> {
                JsonNumber.fromF64(value.toFloat().toDouble())?.let { Value.Number(it) } ?: Value.Null
            }

            is Float -> {
                JsonNumber.fromF64(value.toDouble())?.let { Value.Number(it) } ?: Value.Null
            }

            is Double -> {
                JsonNumber.fromF64(value)?.let { Value.Number(it) } ?: Value.Null
            }

            is Decimal -> {
                Value.Str(convert_decimal_to_string(value))
            }

            is Str -> {
                Value.Str(value)
            }

            is Bytes -> {
                Value.Str(BASE64_STANDARD.encode(value.data()))
            }

            is Date -> {
                Value.Str(convert_date_to_string(value))
            }

            is TimeMillis -> {
                Value.Str(convert_time_millis_to_string(value))
            }

            is TimeMicros -> {
                Value.Str(convert_time_micros_to_string(value))
            }

            is TimestampMillis -> {
                Value.Str(convert_timestamp_millis_to_string(value))
            }

            is TimestampMicros -> {
                Value.Str(convert_timestamp_micros_to_string(value))
            }

            is Group -> {
                value.to_json_value()
            }

            is ListInternal -> {
                Value.Array(value.elements().map { it.to_json_value() })
            }

            is MapInternal -> {
                val jsonMap = JsonMap()
                for ((keyField, valueField) in value.entries()) {
                    val keyVal = keyField.to_json_value()
                    val keyStr = keyVal.asStr() ?: keyVal.toString()
                    jsonMap.insert(keyStr, valueField.to_json_value())
                }
                Value.Object(jsonMap)
            }
        }

    public fun fmt(): String = toString()

    public companion object {
        public fun convert_bool(
            descr: ColumnDescriptor,
            value: Boolean,
        ): Field = Bool(value)

        public fun convert_int32(
            descr: ColumnDescriptor,
            value: kotlin.Int,
        ): Field =
            when (descr.convertedType()) {
                ConvertedType.INT_8 -> {
                    Byte(value.toByte())
                }

                ConvertedType.INT_16 -> {
                    Short(value.toShort())
                }

                ConvertedType.INT_32, ConvertedType.NONE -> {
                    Int(value)
                }

                ConvertedType.UINT_8 -> {
                    UByte(value.toUByte())
                }

                ConvertedType.UINT_16 -> {
                    UShort(value.toUShort())
                }

                ConvertedType.UINT_32 -> {
                    UInt(value.toUInt())
                }

                ConvertedType.DATE -> {
                    Date(value)
                }

                ConvertedType.TIME_MILLIS -> {
                    TimeMillis(value)
                }

                ConvertedType.DECIMAL -> {
                    Decimal(
                        io.github.kotlinmania.parquet.Decimal.fromI32(
                            value,
                            descr.typePrecision(),
                            descr.typeScale(),
                        ),
                    )
                }

                else -> {
                    throw nyiErr(
                        "Conversion for physical type ${descr.physicalType()}, converted type ${descr.convertedType()}, value $value",
                    )
                }
            }

        public fun convert_int64(
            descr: ColumnDescriptor,
            value: kotlin.Long,
        ): Field =
            when (descr.convertedType()) {
                ConvertedType.INT_64, ConvertedType.NONE -> {
                    Long(value)
                }

                ConvertedType.UINT_64 -> {
                    ULong(value.toULong())
                }

                ConvertedType.TIME_MICROS -> {
                    TimeMicros(value)
                }

                ConvertedType.TIMESTAMP_MILLIS -> {
                    TimestampMillis(value)
                }

                ConvertedType.TIMESTAMP_MICROS -> {
                    TimestampMicros(value)
                }

                ConvertedType.DECIMAL -> {
                    Decimal(
                        io.github.kotlinmania.parquet.Decimal.fromI64(
                            value,
                            descr.typePrecision(),
                            descr.typeScale(),
                        ),
                    )
                }

                else -> {
                    throw nyiErr(
                        "Conversion for physical type ${descr.physicalType()}, converted type ${descr.convertedType()}, value $value",
                    )
                }
            }

        public fun convert_int96(
            descr: ColumnDescriptor,
            value: Int96,
        ): Field = TimestampMillis(value.toMillis())

        public fun convert_float(
            descr: ColumnDescriptor,
            value: kotlin.Float,
        ): Field = Float(value)

        public fun convert_double(
            descr: ColumnDescriptor,
            value: kotlin.Double,
        ): Field = Double(value)

        public fun convert_byte_array(
            descr: ColumnDescriptor,
            value: ByteArray,
        ): Result<Field> =
            try {
                val field =
                    when (descr.physicalType()) {
                        ParquetType.BYTE_ARRAY -> {
                            when (descr.convertedType()) {
                                ConvertedType.UTF8, ConvertedType.ENUM, ConvertedType.JSON -> {
                                    val str = value.data().decodeToString()
                                    Str(str)
                                }

                                ConvertedType.BSON, ConvertedType.NONE -> {
                                    Bytes(value)
                                }

                                ConvertedType.DECIMAL -> {
                                    Decimal(
                                        io.github.kotlinmania.parquet.Decimal.fromBytes(
                                            value,
                                            descr.typePrecision(),
                                            descr.typeScale(),
                                        ),
                                    )
                                }

                                else -> {
                                    throw nyiErr(
                                        "Conversion for physical type ${descr.physicalType()}, converted type ${descr.convertedType()}, value $value",
                                    )
                                }
                            }
                        }

                        ParquetType.FIXED_LEN_BYTE_ARRAY -> {
                            when {
                                descr.convertedType() == ConvertedType.DECIMAL -> {
                                    Decimal(
                                        io.github.kotlinmania.parquet.Decimal.fromBytes(
                                            value,
                                            descr.typePrecision(),
                                            descr.typeScale(),
                                        ),
                                    )
                                }

                                descr.convertedType() == ConvertedType.NONE &&
                                    descr.logicalTypeRef() == LogicalType.Float16 -> {
                                    if (value.len() != 2) {
                                        return Result.failure(
                                            generalErr(
                                                "Error reading FIXED_LEN_BYTE_ARRAY as FLOAT16. Length must be 2, got ${value.len()}",
                                            ),
                                        )
                                    }
                                    val data = value.data()
                                    val b0 = data[0].toInt() and 0xFF
                                    val b1 = data[1].toInt() and 0xFF
                                    val bits = (b0 or (b1 shl 8)).toUShort()
                                    Float16(F16.fromBits(bits))
                                }

                                descr.convertedType() == ConvertedType.NONE -> {
                                    Bytes(value)
                                }

                                else -> {
                                    throw nyiErr(
                                        "Conversion for physical type ${descr.physicalType()}, converted type ${descr.convertedType()}, value $value",
                                    )
                                }
                            }
                        }

                        else -> {
                            throw nyiErr(
                                "Conversion for physical type ${descr.physicalType()}, converted type ${descr.convertedType()}, value $value",
                            )
                        }
                    }
                Result.success(field)
            } catch (e: ParquetError) {
                Result.failure(e)
            } catch (e: Exception) {
                Result.failure(generalErr("Error reading BYTE_ARRAY as String. Bytes: ${value.data().contentToString()} Error: $e", e))
            }
    }
}

private fun formatFloat16(value: F16): String {
    if (value.isNaN()) return "NaN"
    if (value == F16.INFINITY) return "inf"
    if (value == F16.NEG_INFINITY) return "-inf"
    if (value.toBits() == 0x8000.toUShort()) return "-0.0"
    if (value.toBits() == 0x0000.toUShort()) return "0.0"
    val f = value.toFloat()
    val s = f.toString()
    return if (!s.contains('.') && !s.contains('E') && !s.contains('e')) "$s.0" else s
}

private fun toScientificNotation(value: Float): String {
    val sign = if (value < 0) "-" else ""
    val absVal = kotlin.math.abs(value)
    val raw = absVal.toString()
    if (raw.contains('E') || raw.contains('e')) {
        return sign +
            raw
                .replace(".0E", "E")
                .replace(".0e", "E")
                .replace("E+", "E")
                .replace("e+", "E")
                .replace('e', 'E')
    }
    val dotIdx = raw.indexOf('.')
    val intPart = if (dotIdx >= 0) raw.substring(0, dotIdx) else raw
    val fracPart = if (dotIdx >= 0) raw.substring(dotIdx + 1) else ""
    val digits = intPart + fracPart
    val exp = intPart.length - 1
    val mantissa =
        if (digits.length > 1) {
            val rest = digits.substring(1).trimEnd('0')
            if (rest.isEmpty()) digits.substring(0, 1) else "${digits[0]}.$rest"
        } else {
            digits
        }
    return "$sign${mantissa}E$exp"
}

private fun toScientificNotation(value: Double): String {
    val sign = if (value < 0) "-" else ""
    val absVal = kotlin.math.abs(value)
    val raw = absVal.toString()
    if (raw.contains('E') || raw.contains('e')) {
        return sign +
            raw
                .replace(".0E", "E")
                .replace(".0e", "E")
                .replace("E+", "E")
                .replace("e+", "E")
                .replace('e', 'E')
    }
    val dotIdx = raw.indexOf('.')
    val intPart = if (dotIdx >= 0) raw.substring(0, dotIdx) else raw
    val fracPart = if (dotIdx >= 0) raw.substring(dotIdx + 1) else ""
    val digits = intPart + fracPart
    val exp = intPart.length - 1
    val mantissa =
        if (digits.length > 1) {
            val rest = digits.substring(1).trimEnd('0')
            if (rest.isEmpty()) digits.substring(0, 1) else "${digits[0]}.$rest"
        } else {
            digits
        }
    return "$sign${mantissa}E$exp"
}

private fun formatFloat(value: Float): String {
    val d = value.toDouble()
    val absD = kotlin.math.abs(d)
    if (absD < 1e-15 || absD > 1e19) {
        return toScientificNotation(value)
    }
    val s = value.toString()
    if (s.contains('E') || s.contains('e')) {
        return expandScientificToDecimal(s)
    }
    if (!s.contains('.')) {
        return "$s.0"
    }
    return s
}

private fun formatDouble(value: Double): String {
    val absD = kotlin.math.abs(value)
    if (absD < 1e-15 || absD > 1e19) {
        return toScientificNotation(value)
    }
    val s = value.toString()
    if (s.contains('E') || s.contains('e')) {
        return expandScientificToDecimal(s)
    }
    if (!s.contains('.')) {
        return "$s.0"
    }
    return s
}

private fun expandScientificToDecimal(s: String): String {
    val sign = if (s.startsWith('-')) "-" else ""
    val withoutSign = if (s.startsWith('-') || s.startsWith('+')) s.substring(1) else s
    val parts = withoutSign.split('E', 'e')
    if (parts.size != 2) return s
    val mantissa = parts[0]
    val exp = parts[1].toInt()

    val dotIdx = mantissa.indexOf('.')
    val intPart = if (dotIdx >= 0) mantissa.substring(0, dotIdx) else mantissa
    val fracPart = if (dotIdx >= 0) mantissa.substring(dotIdx + 1).trimEnd('0') else ""
    val digits = intPart + fracPart
    val currentDotPos = intPart.length
    val newDotPos = currentDotPos + exp

    return if (newDotPos <= 0) {
        val zeros = "0".repeat(-newDotPos)
        "$sign" + "0." + zeros + digits
    } else if (newDotPos >= digits.length) {
        val zeros = "0".repeat(newDotPos - digits.length)
        "$sign" + digits + zeros + ".0"
    } else {
        "$sign" + digits.substring(0, newDotPos) + "." + digits.substring(newDotPos)
    }
}

internal fun convert_date_to_string(value: Int): String {
    val dt = NaiveDate.fromEpochDays(value) ?: error("Invalid epoch days: $value")
    return dt.toString()
}

internal fun convert_timestamp_millis_to_string(value: Long): String {
    val dt = DateTime.fromTimestampMillis(value) ?: error("Invalid timestamp millis: $value")
    val local = dt.naiveUtc()
    val date = local.date()
    val time = local.time()
    val millis = (time.nanosecond() / 1_000_000u).toInt().toString().padStart(3, '0')
    val h = time.hour().toString().padStart(2, '0')
    val m = time.minute().toString().padStart(2, '0')
    val sec = time.second().toString().padStart(2, '0')
    return "$date $h:$m:$sec.$millis +00:00"
}

internal fun convert_timestamp_micros_to_string(value: Long): String {
    val dt = DateTime.fromTimestampMicros(value) ?: error("Invalid timestamp micros: $value")
    val local = dt.naiveUtc()
    val date = local.date()
    val time = local.time()
    val micros = (time.nanosecond() / 1000u).toInt().toString().padStart(6, '0')
    val h = time.hour().toString().padStart(2, '0')
    val m = time.minute().toString().padStart(2, '0')
    val sec = time.second().toString().padStart(2, '0')
    return "$date $h:$m:$sec.$micros +00:00"
}

internal fun convert_time_millis_to_string(value: Int): String {
    val totalMs = value.toLong() and 0xFFFFFFFFL
    val hours = totalMs / (60 * 60 * 1000)
    val minutes = (totalMs % (60 * 60 * 1000)) / (60 * 1000)
    val seconds = (totalMs % (60 * 1000)) / 1000
    val millis = totalMs % 1000
    val h = hours.toString().padStart(2, '0')
    val m = minutes.toString().padStart(2, '0')
    val s = seconds.toString().padStart(2, '0')
    val ms = millis.toString().padStart(3, '0')
    return "$h:$m:$s.$ms"
}

internal fun convert_time_micros_to_string(value: Long): String {
    val totalUs = value.toULong()
    val hours = totalUs / (60uL * 60uL * 1_000_000uL)
    val minutes = (totalUs % (60uL * 60uL * 1_000_000uL)) / (60uL * 1_000_000uL)
    val seconds = (totalUs % (60uL * 1_000_000uL)) / 1_000_000uL
    val micros = totalUs % 1_000_000uL
    val h = hours.toString().padStart(2, '0')
    val m = minutes.toString().padStart(2, '0')
    val s = seconds.toString().padStart(2, '0')
    val us = micros.toString().padStart(6, '0')
    return "$h:$m:$s.$us"
}

internal fun convert_decimal_to_string(decimal: Decimal): String {
    require(decimal.scale >= 0 && decimal.precision >= decimal.scale) {
        "decimal scale must be >= 0 and precision >= scale"
    }

    val ubytes = decimal.data().map { it.toUByte() }
    val num = BigInt.fromSignedBytesBe(ubytes)

    val negative = if (num.sign() == Sign.Minus) 1 else 0
    val numStr = StringBuilder(num.toString())
    var point = numStr.length - decimal.scale - negative

    if (point <= 0) {
        while (point < 0) {
            numStr.insert(negative, '0')
            point += 1
        }
        numStr.insert(negative, "0.")
    } else {
        numStr.insert(point + negative, '.')
    }

    return numStr.toString()
}
