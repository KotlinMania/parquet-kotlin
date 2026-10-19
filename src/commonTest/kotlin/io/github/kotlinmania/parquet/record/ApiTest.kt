// port-lint: tests record/api.rs
package io.github.kotlinmania.parquet.record

import io.github.kotlinmania.chrono.naive.NaiveDate
import io.github.kotlinmania.half.F16
import io.github.kotlinmania.parquet.ByteArray
import io.github.kotlinmania.parquet.ConvertedType
import io.github.kotlinmania.parquet.Decimal
import io.github.kotlinmania.parquet.Int96
import io.github.kotlinmania.parquet.LogicalType
import io.github.kotlinmania.parquet.ParquetType
import io.github.kotlinmania.parquet.schema.ColumnDescriptor
import io.github.kotlinmania.parquet.schema.ColumnPath
import io.github.kotlinmania.parquet.schema.PrimitiveTypeBuilder
import io.github.kotlinmania.serdejson.JsonMap
import io.github.kotlinmania.serdejson.JsonNumber
import io.github.kotlinmania.serdejson.Value
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ApiTest {
    private fun Pair(name: String, field: Field): RowColumn = RowColumn(name, field)

    private fun Pair(key: Field, value: Field): MapEntry = MapEntry(key, value)

    private fun make_column_descr(
        physicalType: ParquetType,
        logicalType: ConvertedType,
        len: Int = -1,
        prec: Int = -1,
        scale: Int = -1,
    ): ColumnDescriptor {
        val builder =
            PrimitiveTypeBuilder("col", physicalType)
                .withConvertedType(logicalType)
        if (len >= 0) builder.withLength(len)
        if (prec >= 0) builder.withPrecision(prec)
        if (scale >= 0) builder.withScale(scale)
        val type = builder.build()
        return ColumnDescriptor(type, 0, 0, ColumnPath.from("col"))
    }

    @Test
    fun test_row_convert_bool() {
        val descr = make_column_descr(ParquetType.BOOLEAN, ConvertedType.NONE)
        var row = Field.convert_bool(descr, true)
        assertEquals(Field.Bool(true), row)
        row = Field.convert_bool(descr, false)
        assertEquals(Field.Bool(false), row)
    }

    @Test
    fun test_row_convert_int32() {
        var descr = make_column_descr(ParquetType.INT32, ConvertedType.INT_8)
        var row = Field.convert_int32(descr, 111)
        assertEquals(Field.Byte(111), row)

        descr = make_column_descr(ParquetType.INT32, ConvertedType.INT_16)
        row = Field.convert_int32(descr, 222)
        assertEquals(Field.Short(222), row)

        descr = make_column_descr(ParquetType.INT32, ConvertedType.INT_32)
        row = Field.convert_int32(descr, 333)
        assertEquals(Field.Int(333), row)

        descr = make_column_descr(ParquetType.INT32, ConvertedType.UINT_8)
        row = Field.convert_int32(descr, -1)
        assertEquals(Field.UByte(255u), row)

        descr = make_column_descr(ParquetType.INT32, ConvertedType.UINT_16)
        row = Field.convert_int32(descr, 256)
        assertEquals(Field.UShort(256u), row)

        descr = make_column_descr(ParquetType.INT32, ConvertedType.UINT_32)
        row = Field.convert_int32(descr, 1234)
        assertEquals(Field.UInt(1234u), row)

        descr = make_column_descr(ParquetType.INT32, ConvertedType.NONE)
        row = Field.convert_int32(descr, 444)
        assertEquals(Field.Int(444), row)

        descr = make_column_descr(ParquetType.INT32, ConvertedType.DATE)
        row = Field.convert_int32(descr, 14611)
        assertEquals(Field.Date(14611), row)

        descr = make_column_descr(ParquetType.INT32, ConvertedType.TIME_MILLIS)
        row = Field.convert_int32(descr, 14611)
        assertEquals(Field.TimeMillis(14611), row)

        descr = make_column_descr(ParquetType.INT32, ConvertedType.DECIMAL, 0, 8, 2)
        row = Field.convert_int32(descr, 444)
        assertEquals(Field.Decimal(Decimal.fromI32(444, 8, 2)), row)
    }

    @Test
    fun test_row_convert_int64() {
        var descr = make_column_descr(ParquetType.INT64, ConvertedType.INT_64)
        var row = Field.convert_int64(descr, 1111)
        assertEquals(Field.Long(1111), row)

        descr = make_column_descr(ParquetType.INT64, ConvertedType.UINT_64)
        row = Field.convert_int64(descr, 78239823)
        assertEquals(Field.ULong(78239823uL), row)

        descr = make_column_descr(ParquetType.INT64, ConvertedType.TIMESTAMP_MILLIS)
        row = Field.convert_int64(descr, 1541186529153L)
        assertEquals(Field.TimestampMillis(1541186529153L), row)

        descr = make_column_descr(ParquetType.INT64, ConvertedType.TIMESTAMP_MICROS)
        row = Field.convert_int64(descr, 1541186529153123L)
        assertEquals(Field.TimestampMicros(1541186529153123L), row)

        descr = make_column_descr(ParquetType.INT64, ConvertedType.TIME_MICROS)
        row = Field.convert_int64(descr, 47445123456L)
        assertEquals(Field.TimeMicros(47445123456L), row)

        descr = make_column_descr(ParquetType.INT64, ConvertedType.NONE)
        row = Field.convert_int64(descr, 2222)
        assertEquals(Field.Long(2222), row)

        descr = make_column_descr(ParquetType.INT64, ConvertedType.DECIMAL, 0, 8, 2)
        row = Field.convert_int64(descr, 3333)
        assertEquals(Field.Decimal(Decimal.fromI64(3333, 8, 2)), row)
    }

    @Test
    fun test_row_convert_int96() {
        val descr = make_column_descr(ParquetType.INT96, ConvertedType.NONE)

        var value = Int96.from(listOf(0u, 0u, 2454923u))
        var row = Field.convert_int96(descr, value)
        assertEquals(Field.TimestampMillis(1238544000000L), row)

        value = Int96.from(listOf(4165425152u, 13u, 2454923u))
        row = Field.convert_int96(descr, value)
        assertEquals(Field.TimestampMillis(1238544060000L), row)
    }

    @Test
    fun test_row_convert_float() {
        val descr = make_column_descr(ParquetType.FLOAT, ConvertedType.NONE)
        val row = Field.convert_float(descr, 2.31f)
        assertEquals(Field.Float(2.31f), row)
    }

    @Test
    fun test_row_convert_double() {
        val descr = make_column_descr(ParquetType.DOUBLE, ConvertedType.NONE)
        val row = Field.convert_double(descr, 1.56)
        assertEquals(Field.Double(1.56), row)
    }

    @Test
    fun test_row_convert_byte_array() {
        // UTF8
        var descr = make_column_descr(ParquetType.BYTE_ARRAY, ConvertedType.UTF8)
        var value = ByteArray.from("ABCD")
        var row = Field.convert_byte_array(descr, value)
        assertEquals(Field.Str("ABCD"), row.getOrThrow())

        // ENUM
        descr = make_column_descr(ParquetType.BYTE_ARRAY, ConvertedType.ENUM)
        value = ByteArray.from("123")
        row = Field.convert_byte_array(descr, value)
        assertEquals(Field.Str("123"), row.getOrThrow())

        // JSON
        descr = make_column_descr(ParquetType.BYTE_ARRAY, ConvertedType.JSON)
        value = ByteArray.from("{\"a\":1}")
        row = Field.convert_byte_array(descr, value)
        assertEquals(Field.Str("{\"a\":1}"), row.getOrThrow())

        // NONE
        descr = make_column_descr(ParquetType.BYTE_ARRAY, ConvertedType.NONE)
        value = ByteArray.from(byteArrayOf(1, 2, 3, 4, 5))
        row = Field.convert_byte_array(descr, value)
        assertEquals(Field.Bytes(value), row.getOrThrow())

        // BSON
        descr = make_column_descr(ParquetType.BYTE_ARRAY, ConvertedType.BSON)
        value = ByteArray.from(byteArrayOf(1, 2, 3, 4, 5))
        row = Field.convert_byte_array(descr, value)
        assertEquals(Field.Bytes(value), row.getOrThrow())

        // DECIMAL
        descr = make_column_descr(ParquetType.BYTE_ARRAY, ConvertedType.DECIMAL, 0, 8, 2)
        value = ByteArray.from(byteArrayOf(207.toByte(), 200.toByte()))
        row = Field.convert_byte_array(descr, value)
        assertEquals(Field.Decimal(Decimal.fromBytes(value, 8, 2)), row.getOrThrow())

        // DECIMAL (FIXED_LEN_BYTE_ARRAY)
        descr = make_column_descr(ParquetType.FIXED_LEN_BYTE_ARRAY, ConvertedType.DECIMAL, 8, 17, 5)
        value = ByteArray.from(byteArrayOf(0, 0, 0, 0, 0, 4, 147.toByte(), 224.toByte()))
        row = Field.convert_byte_array(descr, value)
        assertEquals(Field.Decimal(Decimal.fromBytes(value, 17, 5)), row.getOrThrow())

        // FLOAT16
        val float16Type =
            PrimitiveTypeBuilder("col", ParquetType.FIXED_LEN_BYTE_ARRAY)
                .withLogicalType(LogicalType.Float16)
                .withLength(2)
                .build()
        descr = ColumnDescriptor(float16Type, 0, 0, ColumnPath.from("col"))
        value = ByteArray.from(F16.PI)
        row = Field.convert_byte_array(descr, value)
        assertEquals(Field.Float16(F16.PI), row.getOrThrow())

        // NONE (FIXED_LEN_BYTE_ARRAY)
        descr = make_column_descr(ParquetType.FIXED_LEN_BYTE_ARRAY, ConvertedType.NONE, 6, 0, 0)
        value = ByteArray.from(byteArrayOf(1, 2, 3, 4, 5, 6))
        row = Field.convert_byte_array(descr, value)
        assertEquals(Field.Bytes(value), row.getOrThrow())
    }

    private fun check_date_conversion(
        y: UInt,
        m: UInt,
        d: UInt,
    ) {
        val date = NaiveDate.fromYmdOpt(y.toInt(), m, d)!!.andHmsOpt(0u, 0u, 0u)!!
        val dt = date.andUtc()
        val res = convert_date_to_string((dt.timestamp() / 60 / 60 / 24).toInt())
        val exp = "${y.toString().padStart(4, '0')}-${m.toString().padStart(2, '0')}-${d.toString().padStart(2, '0')}"
        assertEquals(exp, res)
    }

    @Test
    fun test_convert_date_to_string() {
        check_date_conversion(1969u, 12u, 31u)
        check_date_conversion(2010u, 1u, 2u)
        check_date_conversion(2014u, 5u, 1u)
        check_date_conversion(2016u, 2u, 29u)
        check_date_conversion(2017u, 9u, 12u)
        check_date_conversion(2018u, 3u, 31u)
    }

    private fun check_datetime_conversion(
        y: UInt,
        m: UInt,
        d: UInt,
        h: UInt,
        mi: UInt,
        s: UInt,
        milli: UInt,
        exp: String,
    ) {
        val date = NaiveDate.fromYmdOpt(y.toInt(), m, d)!!.andHmsMilliOpt(h, mi, s, milli)!!
        val dt = date.andUtc()
        val res = convert_timestamp_millis_to_string(dt.timestampMillis())
        assertEquals(exp, res)
    }

    @Test
    fun test_convert_timestamp_millis_to_string() {
        check_datetime_conversion(1969u, 9u, 10u, 1u, 2u, 3u, 4u, "1969-09-10 01:02:03.004 +00:00")
        check_datetime_conversion(2010u, 1u, 2u, 13u, 12u, 54u, 42u, "2010-01-02 13:12:54.042 +00:00")
        check_datetime_conversion(2011u, 1u, 3u, 8u, 23u, 1u, 27u, "2011-01-03 08:23:01.027 +00:00")
        check_datetime_conversion(2012u, 4u, 5u, 11u, 6u, 32u, 0u, "2012-04-05 11:06:32.000 +00:00")
        check_datetime_conversion(2013u, 5u, 12u, 16u, 38u, 0u, 15u, "2013-05-12 16:38:00.015 +00:00")
        check_datetime_conversion(2014u, 11u, 28u, 21u, 15u, 12u, 59u, "2014-11-28 21:15:12.059 +00:00")
    }

    private fun check_datetime_conversion(
        y: UInt,
        m: UInt,
        d: UInt,
        h: UInt,
        mi: UInt,
        s: UInt,
        micro: ULong,
        exp: String,
    ) {
        val date = NaiveDate.fromYmdOpt(y.toInt(), m, d)!!.andHmsMicroOpt(h, mi, s, micro.toUInt())!!
        val dt = date.andUtc()
        val res = convert_timestamp_micros_to_string(dt.timestampMicros())
        assertEquals(exp, res)
    }

    @Test
    fun test_convert_timestamp_micros_to_string() {
        check_datetime_conversion(1969u, 9u, 10u, 1u, 2u, 3u, 4uL, "1969-09-10 01:02:03.000004 +00:00")
        check_datetime_conversion(2010u, 1u, 2u, 13u, 12u, 54u, 42uL, "2010-01-02 13:12:54.000042 +00:00")
        check_datetime_conversion(2011u, 1u, 3u, 8u, 23u, 1u, 27uL, "2011-01-03 08:23:01.000027 +00:00")
        check_datetime_conversion(2012u, 4u, 5u, 11u, 6u, 32u, 0uL, "2012-04-05 11:06:32.000000 +00:00")
        check_datetime_conversion(2013u, 5u, 12u, 16u, 38u, 0u, 15uL, "2013-05-12 16:38:00.000015 +00:00")
        check_datetime_conversion(2014u, 11u, 28u, 21u, 15u, 12u, 59uL, "2014-11-28 21:15:12.000059 +00:00")
    }

    @Test
    fun test_convert_float16_to_string() {
        assertEquals("1.0", Field.Float16(F16.ONE).toString())
        assertEquals("3.140625", Field.Float16(F16.PI).toString())
        assertEquals("65504.0", Field.Float16(F16.MAX).toString())
        assertEquals("NaN", Field.Float16(F16.NAN).toString())
        assertEquals("inf", Field.Float16(F16.INFINITY).toString())
        assertEquals("-inf", Field.Float16(F16.NEG_INFINITY).toString())
        assertEquals("0.0", Field.Float16(F16.ZERO).toString())
        assertEquals("-0.0", Field.Float16(F16.NEG_ZERO).toString())
    }

    @Test
    fun test_convert_float_to_string() {
        assertEquals("1.0", Field.Float(1.0f).toString())
        assertEquals("9.63", Field.Float(9.63f).toString())
        assertEquals("0.000000000000001", Field.Float(1e-15f).toString())
        assertEquals("1E-16", Field.Float(1e-16f).toString())
        assertEquals("10000000000000000000.0", Field.Float(1e19f).toString())
        assertEquals("1E20", Field.Float(1e20f).toString())
        assertEquals("1.7976931E30", Field.Float(1.7976931E30f).toString())
        assertEquals("-1.7976931E30", Field.Float(-1.7976931E30f).toString())
    }

    @Test
    fun test_convert_double_to_string() {
        assertEquals("1.0", Field.Double(1.0).toString())
        assertEquals("9.63", Field.Double(9.63).toString())
        assertEquals("0.000000000000001", Field.Double(1e-15).toString())
        assertEquals("1E-16", Field.Double(1e-16).toString())
        assertEquals("10000000000000000000.0", Field.Double(1e19).toString())
        assertEquals("1E20", Field.Double(1e20).toString())
        assertEquals("1.79769313486E308", Field.Double(1.79769313486E308).toString())
        assertEquals("-1.79769313486E308", Field.Double(-1.79769313486E308).toString())
    }

    private fun check_decimal(
        bytes: kotlin.ByteArray,
        precision: Int,
        scale: Int,
        res: String,
    ) {
        val decimal = Decimal.fromBytes(ByteArray.from(bytes), precision, scale)
        assertEquals(res, convert_decimal_to_string(decimal))
    }

    @Test
    fun test_convert_decimal_to_string() {
        check_decimal(
            byteArrayOf(0, 0, 0, 0, 0, 0, 0, 0, 13, 224.toByte(), 182.toByte(), 179.toByte(), 167.toByte(), 100, 0, 0),
            38,
            18,
            "1.000000000000000000",
        )
        check_decimal(
            byteArrayOf(
                249.toByte(),
                233.toByte(),
                247.toByte(),
                16,
                185.toByte(),
                192.toByte(),
                202.toByte(),
                223.toByte(),
                215.toByte(),
                165.toByte(),
                192.toByte(),
                166.toByte(),
                67,
                72,
            ),
            36,
            28,
            "-12344.0242342304923409234234293432",
        )
        check_decimal(byteArrayOf(0, 0, 0, 0, 0, 4, 147.toByte(), 224.toByte()), 17, 5, "3.00000")
        check_decimal(byteArrayOf(0, 0, 0, 0, 1, 201.toByte(), 195.toByte(), 140.toByte()), 18, 2, "300000.12")
        check_decimal(byteArrayOf(207.toByte(), 200.toByte()), 10, 2, "-123.44")
        check_decimal(byteArrayOf(207.toByte(), 200.toByte()), 10, 8, "-0.00012344")
        check_decimal(byteArrayOf(48, 57), 5, 5, "0.12345")
        check_decimal(byteArrayOf(207.toByte(), 199.toByte()), 5, 5, "-0.12345")
        check_decimal(byteArrayOf(45), 5, 5, "0.00045")
    }

    @Test
    fun test_row_display() {
        assertEquals("null", Field.Null.toString())
        assertEquals("true", Field.Bool(true).toString())
        assertEquals("false", Field.Bool(false).toString())
        assertEquals("1", Field.Byte(1).toString())
        assertEquals("2", Field.Short(2).toString())
        assertEquals("3", Field.Int(3).toString())
        assertEquals("4", Field.Long(4).toString())
        assertEquals("1", Field.UByte(1u).toString())
        assertEquals("2", Field.UShort(2u).toString())
        assertEquals("3", Field.UInt(3u).toString())
        assertEquals("4", Field.ULong(4u).toString())
        assertEquals("2.71875", Field.Float16(F16.E).toString())
        assertEquals("5.0", Field.Float(5.0f).toString())
        assertEquals("5.1234", Field.Float(5.1234f).toString())
        assertEquals("6.0", Field.Double(6.0).toString())
        assertEquals("6.1234", Field.Double(6.1234).toString())
        assertEquals("\"abc\"", Field.Str("abc").toString())
        assertEquals("[1, 2, 3]", Field.Bytes(ByteArray.from(byteArrayOf(1, 2, 3))).toString())
        assertEquals(convert_date_to_string(14611), Field.Date(14611).toString())
        assertEquals(convert_timestamp_millis_to_string(1262391174000L), Field.TimestampMillis(1262391174000L).toString())
        assertEquals(convert_timestamp_micros_to_string(1262391174000000L), Field.TimestampMicros(1262391174000000L).toString())
        assertEquals(convert_decimal_to_string(Decimal.fromI32(4, 8, 2)), Field.Decimal(Decimal.fromI32(4, 8, 2)).toString())

        val fields =
            listOf(
                Pair("x", Field.Null),
                Pair("Y", Field.Int(2)),
                Pair("z", Field.Float(3.1f)),
                Pair("a", Field.Str("abc")),
            )
        val row = Field.Group(Row.new(fields))
        assertEquals("{x: null, Y: 2, z: 3.1, a: \"abc\"}", row.toString())

        val listRow = Field.ListInternal(make_list(listOf(Field.Int(2), Field.Int(1), Field.Null, Field.Int(12))))
        assertEquals("[2, 1, null, 12]", listRow.toString())

        val mapRow =
            Field.MapInternal(
                make_map(
                    listOf(
                        Pair(Field.Int(1), Field.Float(1.2f)),
                        Pair(Field.Int(2), Field.Float(4.5f)),
                        Pair(Field.Int(3), Field.Float(2.3f)),
                    ),
                ),
            )
        assertEquals("{1 -> 1.2, 2 -> 4.5, 3 -> 2.3}", mapRow.toString())
    }

    @Test
    fun test_is_primitive() {
        assertTrue(Field.Null.is_primitive())
        assertTrue(Field.Bool(true).is_primitive())
        assertTrue(Field.Bool(false).is_primitive())
        assertTrue(Field.Byte(1).is_primitive())
        assertTrue(Field.Short(2).is_primitive())
        assertTrue(Field.Int(3).is_primitive())
        assertTrue(Field.Long(4).is_primitive())
        assertTrue(Field.UByte(1u).is_primitive())
        assertTrue(Field.UShort(2u).is_primitive())
        assertTrue(Field.UInt(3u).is_primitive())
        assertTrue(Field.ULong(4u).is_primitive())
        assertTrue(Field.Float16(F16.E).is_primitive())
        assertTrue(Field.Float(5.0f).is_primitive())
        assertTrue(Field.Float(5.1234f).is_primitive())
        assertTrue(Field.Double(6.0).is_primitive())
        assertTrue(Field.Double(6.1234).is_primitive())
        assertTrue(Field.Str("abc").is_primitive())
        assertTrue(Field.Bytes(ByteArray.from(byteArrayOf(1, 2, 3))).is_primitive())
        assertTrue(Field.TimestampMillis(12345678L).is_primitive())
        assertTrue(Field.TimestampMicros(12345678901L).is_primitive())
        assertTrue(Field.Decimal(Decimal.fromI32(4, 8, 2)).is_primitive())

        assertFalse(
            Field
                .Group(
                    Row.new(
                        listOf(
                            Pair("x", Field.Null),
                            Pair("Y", Field.Int(2)),
                            Pair("z", Field.Float(3.1f)),
                            Pair("a", Field.Str("abc")),
                        ),
                    ),
                ).is_primitive(),
        )

        assertFalse(Field.ListInternal(make_list(listOf(Field.Int(2), Field.Int(1), Field.Null, Field.Int(12)))).is_primitive())

        assertFalse(
            Field
                .MapInternal(
                    make_map(
                        listOf(
                            Pair(Field.Int(1), Field.Float(1.2f)),
                            Pair(Field.Int(2), Field.Float(4.5f)),
                            Pair(Field.Int(3), Field.Float(2.3f)),
                        ),
                    ),
                ).is_primitive(),
        )
    }

    @Test
    fun test_row_primitive_field_fmt() {
        val row =
            Row.new(
                listOf(
                    Pair("00", Field.Null),
                    Pair("01", Field.Bool(false)),
                    Pair("02", Field.Byte(3)),
                    Pair("03", Field.Short(4)),
                    Pair("04", Field.Int(5)),
                    Pair("05", Field.Long(6)),
                    Pair("06", Field.UByte(7u)),
                    Pair("07", Field.UShort(8u)),
                    Pair("08", Field.UInt(9u)),
                    Pair("09", Field.ULong(10u)),
                    Pair("10", Field.Float(11.1f)),
                    Pair("11", Field.Double(12.1)),
                    Pair("12", Field.Str("abc")),
                    Pair("13", Field.Bytes(ByteArray.from(byteArrayOf(1, 2, 3, 4, 5)))),
                    Pair("14", Field.Date(14611)),
                    Pair("15", Field.TimestampMillis(1262391174000L)),
                    Pair("16", Field.TimestampMicros(1262391174000000L)),
                    Pair("17", Field.Decimal(Decimal.fromI32(4, 7, 2))),
                    Pair("18", Field.Float16(F16.PI)),
                ),
            )

        assertEquals("null", row.fmt(0).toString())
        assertEquals("false", row.fmt(1).toString())
        assertEquals("3", row.fmt(2).toString())
        assertEquals("4", row.fmt(3).toString())
        assertEquals("5", row.fmt(4).toString())
        assertEquals("6", row.fmt(5).toString())
        assertEquals("7", row.fmt(6).toString())
        assertEquals("8", row.fmt(7).toString())
        assertEquals("9", row.fmt(8).toString())
        assertEquals("10", row.fmt(9).toString())
        assertEquals("11.1", row.fmt(10).toString())
        assertEquals("12.1", row.fmt(11).toString())
        assertEquals("\"abc\"", row.fmt(12).toString())
        assertEquals("[1, 2, 3, 4, 5]", row.fmt(13).toString())
        assertEquals(convert_date_to_string(14611), row.fmt(14).toString())
        assertEquals(convert_timestamp_millis_to_string(1262391174000L), row.fmt(15).toString())
        assertEquals(convert_timestamp_micros_to_string(1262391174000000L), row.fmt(16).toString())
        assertEquals("0.04", row.fmt(17).toString())
        assertEquals("3.140625", row.fmt(18).toString())
    }

    @Test
    fun test_row_complex_field_fmt() {
        val row =
            Row.new(
                listOf(
                    Pair("00", Field.Group(Row.new(listOf(Pair("x", Field.Null), Pair("Y", Field.Int(2)))))),
                    Pair("01", Field.ListInternal(make_list(listOf(Field.Int(2), Field.Int(1), Field.Null, Field.Int(12))))),
                    Pair(
                        "02",
                        Field.MapInternal(
                            make_map(
                                listOf(
                                    Pair(Field.Int(1), Field.Float(1.2f)),
                                    Pair(Field.Int(2), Field.Float(4.5f)),
                                    Pair(Field.Int(3), Field.Float(2.3f)),
                                ),
                            ),
                        ),
                    ),
                ),
            )

        assertEquals("{x: null, Y: 2}", row.fmt(0).toString())
        assertEquals("[2, 1, null, 12]", row.fmt(1).toString())
        assertEquals("{1 -> 1.2, 2 -> 4.5, 3 -> 2.3}", row.fmt(2).toString())
    }

    @Test
    fun test_row_primitive_accessors() {
        val row =
            Row.new(
                listOf(
                    Pair("a", Field.Null),
                    Pair("b", Field.Bool(false)),
                    Pair("c", Field.Byte(3)),
                    Pair("d", Field.Short(4)),
                    Pair("e", Field.Int(5)),
                    Pair("f", Field.Long(6)),
                    Pair("g", Field.UByte(3u)),
                    Pair("h", Field.UShort(4u)),
                    Pair("i", Field.UInt(5u)),
                    Pair("j", Field.ULong(6u)),
                    Pair("k", Field.Float(7.1f)),
                    Pair("l", Field.Double(8.1)),
                    Pair("m", Field.Str("abc")),
                    Pair("n", Field.Bytes(ByteArray.from(byteArrayOf(1, 2, 3, 4, 5)))),
                    Pair("o", Field.Decimal(Decimal.fromI32(4, 7, 2))),
                    Pair("p", Field.Float16(F16.fromF32(9.1f))),
                ),
            )

        assertTrue(row.is_null(0).getOrThrow())
        assertFalse(row.is_null(1).getOrThrow())
        assertFalse(row.get_bool(1).getOrThrow())
        assertEquals(3.toByte(), row.get_byte(2).getOrThrow())
        assertEquals(4.toShort(), row.get_short(3).getOrThrow())
        assertEquals(5, row.get_int(4).getOrThrow())
        assertEquals(6L, row.get_long(5).getOrThrow())
        assertEquals(3u.toUByte(), row.get_ubyte(6).getOrThrow())
        assertEquals(4u.toUShort(), row.get_ushort(7).getOrThrow())
        assertEquals(5u, row.get_uint(8).getOrThrow())
        assertEquals(6uL, row.get_ulong(9).getOrThrow())
        assertTrue(kotlin.math.abs(7.1f - row.get_float(10).getOrThrow()) < 1e-5f)
        assertTrue(kotlin.math.abs(8.1 - row.get_double(11).getOrThrow()) < 1e-9)
        assertEquals("abc", row.get_string(12).getOrThrow())
        assertEquals(5, row.get_bytes(13).getOrThrow().len())
        assertEquals(7, row.get_decimal(14).getOrThrow().precision)
        assertTrue(kotlin.math.abs(F16.fromF32(9.1f).toFloat() - row.get_float16(15).getOrThrow().toFloat()) < 1e-3f)

        assertTrue(row.is_null(16).isFailure)
        assertTrue(row.get_bool(16).isFailure)
        assertTrue(row.get_byte(16).isFailure)
        assertTrue(row.get_short(16).isFailure)
        assertTrue(row.get_int(16).isFailure)
        assertTrue(row.get_long(16).isFailure)
        assertTrue(row.get_ubyte(16).isFailure)
        assertTrue(row.get_ushort(16).isFailure)
        assertTrue(row.get_uint(16).isFailure)
        assertTrue(row.get_ulong(16).isFailure)
        assertTrue(row.get_float(16).isFailure)
        assertTrue(row.get_double(16).isFailure)
        assertTrue(row.get_string(16).isFailure)
        assertTrue(row.get_bytes(16).isFailure)
        assertTrue(row.get_decimal(16).isFailure)
        assertTrue(row.get_float16(16).isFailure)
    }

    @Test
    fun test_row_primitive_invalid_accessors() {
        val row =
            Row.new(
                listOf(
                    Pair("a", Field.Null),
                    Pair("b", Field.Bool(false)),
                    Pair("c", Field.Byte(3)),
                    Pair("d", Field.Short(4)),
                    Pair("e", Field.Int(5)),
                    Pair("f", Field.Long(6)),
                    Pair("g", Field.UByte(3u)),
                    Pair("h", Field.UShort(4u)),
                    Pair("i", Field.UInt(5u)),
                    Pair("j", Field.ULong(6u)),
                    Pair("k", Field.Float(7.1f)),
                    Pair("l", Field.Double(8.1)),
                    Pair("m", Field.Str("abc")),
                    Pair("n", Field.Bytes(ByteArray.from(byteArrayOf(1, 2, 3, 4, 5)))),
                    Pair("o", Field.Decimal(Decimal.fromI32(4, 7, 2))),
                    Pair("p", Field.Float16(F16.fromF32(9.1f))),
                ),
            )

        for (i in 0 until row.len()) {
            assertTrue(row.get_group(i).isFailure)
        }
    }

    @Test
    fun test_row_complex_accessors() {
        val row =
            Row.new(
                listOf(
                    Pair("a", Field.Group(Row.new(listOf(Pair("x", Field.Null), Pair("Y", Field.Int(2)))))),
                    Pair("b", Field.ListInternal(make_list(listOf(Field.Int(2), Field.Int(1), Field.Null, Field.Int(12))))),
                    Pair(
                        "c",
                        Field.MapInternal(
                            make_map(
                                listOf(
                                    Pair(Field.Int(1), Field.Float(1.2f)),
                                    Pair(Field.Int(2), Field.Float(4.5f)),
                                    Pair(Field.Int(3), Field.Float(2.3f)),
                                ),
                            ),
                        ),
                    ),
                ),
            )

        assertEquals(2, row.get_group(0).getOrThrow().len())
        assertEquals(4, row.get_list(1).getOrThrow().len())
        assertEquals(3, row.get_map(2).getOrThrow().len())
    }

    @Test
    fun test_row_complex_invalid_accessors() {
        val row =
            Row.new(
                listOf(
                    Pair("a", Field.Group(Row.new(listOf(Pair("x", Field.Null), Pair("Y", Field.Int(2)))))),
                    Pair("b", Field.ListInternal(make_list(listOf(Field.Int(2), Field.Int(1), Field.Null, Field.Int(12))))),
                    Pair(
                        "c",
                        Field.MapInternal(
                            make_map(
                                listOf(
                                    Pair(Field.Int(1), Field.Float(1.2f)),
                                    Pair(Field.Int(2), Field.Float(4.5f)),
                                    Pair(Field.Int(3), Field.Float(2.3f)),
                                ),
                            ),
                        ),
                    ),
                ),
            )

        assertEquals("Parquet error: Cannot access Group as Float", row.get_float(0).exceptionOrNull()?.message)
        assertEquals("Parquet error: Cannot access ListInternal as Float", row.get_float(1).exceptionOrNull()?.message)
        assertEquals("Parquet error: Cannot access MapInternal as Float", row.get_float(2).exceptionOrNull()?.message)
    }

    @Test
    fun test_list_primitive_accessors() {
        var list = make_list(listOf(Field.Bool(false)))
        assertFalse(list.get_bool(0).getOrThrow())

        list = make_list(listOf(Field.Byte(3), Field.Byte(4)))
        assertEquals(4.toByte(), list.get_byte(1).getOrThrow())

        list = make_list(listOf(Field.Short(4), Field.Short(5), Field.Short(6)))
        assertEquals(6.toShort(), list.get_short(2).getOrThrow())

        list = make_list(listOf(Field.Int(5)))
        assertEquals(5, list.get_int(0).getOrThrow())

        list = make_list(listOf(Field.Long(6), Field.Long(7)))
        assertEquals(7L, list.get_long(1).getOrThrow())

        list = make_list(listOf(Field.UByte(3u), Field.UByte(4u)))
        assertEquals(4u.toUByte(), list.get_ubyte(1).getOrThrow())

        list = make_list(listOf(Field.UShort(4u), Field.UShort(5u), Field.UShort(6u)))
        assertEquals(6u.toUShort(), list.get_ushort(2).getOrThrow())

        list = make_list(listOf(Field.UInt(5u)))
        assertEquals(5u, list.get_uint(0).getOrThrow())

        list = make_list(listOf(Field.ULong(6u), Field.ULong(7u)))
        assertEquals(7uL, list.get_ulong(1).getOrThrow())

        list = make_list(listOf(Field.Float16(F16.PI)))
        assertTrue(kotlin.math.abs(F16.PI.toFloat() - list.get_float16(0).getOrThrow().toFloat()) < 1e-3f)

        list = make_list(listOf(Field.Float(8.1f), Field.Float(9.2f), Field.Float(10.3f)))
        assertTrue(kotlin.math.abs(10.3f - list.get_float(2).getOrThrow()) < 1e-6f)

        list = make_list(listOf(Field.Double(kotlin.math.PI)))
        assertTrue(kotlin.math.abs(kotlin.math.PI - list.get_double(0).getOrThrow()) < 1e-9)

        list = make_list(listOf(Field.Str("abc")))
        assertEquals("abc", list.get_string(0).getOrThrow())

        list = make_list(listOf(Field.Bytes(ByteArray.from(byteArrayOf(1, 2, 3, 4, 5)))))
        assertContentEquals(byteArrayOf(1, 2, 3, 4, 5), list.get_bytes(0).getOrThrow().data())

        list = make_list(listOf(Field.Decimal(Decimal.fromI32(4, 5, 2))))
        assertContentEquals(byteArrayOf(0, 0, 0, 4), list.get_decimal(0).getOrThrow().data())
    }

    @Test
    fun test_list_primitive_invalid_accessors() {
        var list = make_list(listOf(Field.Bool(false)))
        assertTrue(list.get_byte(0).isFailure)

        list = make_list(listOf(Field.Byte(3), Field.Byte(4)))
        assertTrue(list.get_short(1).isFailure)

        list = make_list(listOf(Field.Short(4), Field.Short(5), Field.Short(6)))
        assertTrue(list.get_int(2).isFailure)

        list = make_list(listOf(Field.Int(5)))
        assertTrue(list.get_long(0).isFailure)

        list = make_list(listOf(Field.Long(6), Field.Long(7)))
        assertTrue(list.get_float(1).isFailure)

        list = make_list(listOf(Field.UByte(3u), Field.UByte(4u)))
        assertTrue(list.get_short(1).isFailure)

        list = make_list(listOf(Field.UShort(4u), Field.UShort(5u), Field.UShort(6u)))
        assertTrue(list.get_int(2).isFailure)

        list = make_list(listOf(Field.UInt(5u)))
        assertTrue(list.get_long(0).isFailure)

        list = make_list(listOf(Field.ULong(6u), Field.ULong(7u)))
        assertTrue(list.get_float(1).isFailure)

        list = make_list(listOf(Field.Float16(F16.PI)))
        assertTrue(list.get_string(0).isFailure)

        list = make_list(listOf(Field.Float(8.1f), Field.Float(9.2f), Field.Float(10.3f)))
        assertTrue(list.get_double(2).isFailure)

        list = make_list(listOf(Field.Double(kotlin.math.PI)))
        assertTrue(list.get_string(0).isFailure)

        list = make_list(listOf(Field.Str("abc")))
        assertTrue(list.get_bytes(0).isFailure)

        list = make_list(listOf(Field.Bytes(ByteArray.from(byteArrayOf(1, 2, 3, 4, 5)))))
        assertTrue(list.get_bool(0).isFailure)

        list = make_list(listOf(Field.Decimal(Decimal.fromI32(4, 5, 2))))
        assertTrue(list.get_bool(0).isFailure)
    }

    @Test
    fun test_list_complex_accessors() {
        var list = make_list(listOf(Field.Group(Row.new(listOf(Pair("x", Field.Null), Pair("Y", Field.Int(2)))))))
        assertEquals(2, list.get_group(0).getOrThrow().len())

        list = make_list(listOf(Field.ListInternal(make_list(listOf(Field.Int(2), Field.Int(1), Field.Null, Field.Int(12))))))
        assertEquals(4, list.get_list(0).getOrThrow().len())

        list =
            make_list(
                listOf(
                    Field.MapInternal(
                        make_map(
                            listOf(
                                Pair(Field.Int(1), Field.Float(1.2f)),
                                Pair(Field.Int(2), Field.Float(4.5f)),
                                Pair(Field.Int(3), Field.Float(2.3f)),
                            ),
                        ),
                    ),
                ),
            )
        assertEquals(3, list.get_map(0).getOrThrow().len())
    }

    @Test
    fun test_list_complex_invalid_accessors() {
        var list = make_list(listOf(Field.Group(Row.new(listOf(Pair("x", Field.Null), Pair("Y", Field.Int(2)))))))
        assertEquals("Parquet error: Cannot access Group as Float", list.get_float(0).exceptionOrNull()?.message)

        list = make_list(listOf(Field.ListInternal(make_list(listOf(Field.Int(2), Field.Int(1), Field.Null, Field.Int(12))))))
        assertEquals("Parquet error: Cannot access ListInternal as Float", list.get_float(0).exceptionOrNull()?.message)

        list =
            make_list(
                listOf(
                    Field.MapInternal(
                        make_map(
                            listOf(
                                Pair(Field.Int(1), Field.Float(1.2f)),
                                Pair(Field.Int(2), Field.Float(4.5f)),
                                Pair(Field.Int(3), Field.Float(2.3f)),
                            ),
                        ),
                    ),
                ),
            )
        assertEquals("Parquet error: Cannot access MapInternal as Float", list.get_float(0).exceptionOrNull()?.message)
    }

    @Test
    fun test_map_accessors() {
        val map =
            make_map(
                listOf(
                    Pair(Field.Int(1), Field.Str("a")),
                    Pair(Field.Int(2), Field.Str("b")),
                    Pair(Field.Int(3), Field.Str("c")),
                    Pair(Field.Int(4), Field.Str("d")),
                    Pair(Field.Int(5), Field.Str("e")),
                ),
            )

        assertEquals(5, map.len())
        for (i in 0 until 5) {
            assertEquals(i + 1, map.get_keys().get_int(i).getOrThrow())
            assertEquals(('a'.code + i).toChar().toString(), map.get_values().get_string(i).getOrThrow())
        }
    }

    @Test
    fun test_to_json_value() {
        assertEquals(Value.Null, Field.Null.to_json_value())
        assertEquals(Value.Bool(true), Field.Bool(true).to_json_value())
        assertEquals(Value.Bool(false), Field.Bool(false).to_json_value())
        assertEquals(Value.Number(JsonNumber.fromI64(1)), Field.Byte(1).to_json_value())
        assertEquals(Value.Number(JsonNumber.fromI64(2)), Field.Short(2).to_json_value())
        assertEquals(Value.Number(JsonNumber.fromI64(3)), Field.Int(3).to_json_value())
        assertEquals(Value.Number(JsonNumber.fromI64(4)), Field.Long(4).to_json_value())
        assertEquals(Value.Number(JsonNumber.fromU64(1u)), Field.UByte(1u).to_json_value())
        assertEquals(Value.Number(JsonNumber.fromU64(2u)), Field.UShort(2u).to_json_value())
        assertEquals(Value.Number(JsonNumber.fromU64(3u)), Field.UInt(3u).to_json_value())
        assertEquals(Value.Number(JsonNumber.fromU64(4u)), Field.ULong(4u).to_json_value())
        assertEquals(Value.Number(JsonNumber.fromF64(5.0)!!), Field.Float16(F16.fromF32(5.0f)).to_json_value())
        assertEquals(Value.Number(JsonNumber.fromF64(5.0)!!), Field.Float(5.0f).to_json_value())
        assertEquals(Value.Number(JsonNumber.fromF64(5.1234f.toDouble())!!), Field.Float(5.1234f).to_json_value())
        assertEquals(Value.Number(JsonNumber.fromF64(6.0)!!), Field.Double(6.0).to_json_value())
        assertEquals(Value.Number(JsonNumber.fromF64(6.1234)!!), Field.Double(6.1234).to_json_value())
        assertEquals(Value.Str("abc"), Field.Str("abc").to_json_value())
        assertEquals(Value.Str("0.04"), Field.Decimal(Decimal.fromI32(4, 8, 2)).to_json_value())
        assertEquals(Value.Str("AQID"), Field.Bytes(ByteArray.from(byteArrayOf(1, 2, 3))).to_json_value())
        assertEquals(Value.Str("1970-01-01 03:25:45.678 +00:00"), Field.TimestampMillis(12345678L).to_json_value())
        assertEquals(Value.Str("1970-01-01 03:25:45.678901 +00:00"), Field.TimestampMicros(12345678901L).to_json_value())
        assertEquals(Value.Str("13:10:45.123"), Field.TimeMillis(47445123).to_json_value())
        assertEquals(Value.Str("13:10:45.123456"), Field.TimeMicros(47445123456L).to_json_value())

        val fields =
            listOf(
                Pair("X", Field.Int(1)),
                Pair("Y", Field.Double(2.2)),
                Pair("Z", Field.Str("abc")),
            )
        val row = Field.Group(Row.new(fields))
        val expectedMap = JsonMap()
        expectedMap.insert("X", Value.Number(JsonNumber.fromI64(1)))
        expectedMap.insert("Y", Value.Number(JsonNumber.fromF64(2.2)!!))
        expectedMap.insert("Z", Value.Str("abc"))
        assertEquals(Value.Object(expectedMap), row.to_json_value())

        val listRow = Field.ListInternal(make_list(listOf(Field.Int(1), Field.Int(12), Field.Null)))
        val array =
            listOf(
                Value.Number(JsonNumber.fromI64(1)),
                Value.Number(JsonNumber.fromI64(12)),
                Value.Null,
            )
        assertEquals(Value.Array(array), listRow.to_json_value())

        val mapRow =
            Field.MapInternal(
                make_map(
                    listOf(
                        Pair(Field.Str("k1"), Field.Double(1.2)),
                        Pair(Field.Str("k2"), Field.Double(3.4)),
                        Pair(Field.Str("k3"), Field.Double(4.5)),
                    ),
                ),
            )
        val expectedMap2 = JsonMap()
        expectedMap2.insert("k1", Value.Number(JsonNumber.fromF64(1.2)!!))
        expectedMap2.insert("k2", Value.Number(JsonNumber.fromF64(3.4)!!))
        expectedMap2.insert("k3", Value.Number(JsonNumber.fromF64(4.5)!!))
        assertEquals(Value.Object(expectedMap2), mapRow.to_json_value())
    }

    @Test
    fun test_field_visibility() {
        val row =
            Row.new(
                listOf(
                    Pair(
                        "a",
                        Field.Group(
                            Row.new(
                                listOf(
                                    Pair("x", Field.Null),
                                    Pair("Y", Field.Int(2)),
                                ),
                            ),
                        ),
                    ),
                ),
            )

        val column = row.get_column_iter().next()
        assertNotNull(column)
        assertEquals("a", column.first)
        val field = column.second
        assertTrue(field is Field.Group)
        assertEquals(
            Row.new(
                listOf(
                    Pair("x", Field.Null),
                    Pair("Y", Field.Int(2)),
                ),
            ),
            field.value,
        )
    }

    @Test
    fun test_list_element_access() {
        val expected =
            listOf(
                Field.Int(1),
                Field.Group(
                    Row.new(
                        listOf(
                            Pair("x", Field.Null),
                            Pair("Y", Field.Int(2)),
                        ),
                    ),
                ),
            )

        val list = make_list(expected)
        assertEquals(expected, list.elements())
    }

    @Test
    fun test_map_entry_access() {
        val expected =
            listOf(
                Pair(Field.Str("one"), Field.Int(1)),
                Pair(Field.Str("two"), Field.Int(2)),
            )

        val map = make_map(expected)
        assertEquals(expected, map.entries())
    }
}
