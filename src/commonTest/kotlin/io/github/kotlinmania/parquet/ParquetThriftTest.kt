// port-lint: tests parquet_thrift.rs
package io.github.kotlinmania.parquet

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class ParquetThriftTest {
    private fun <T : WriteThrift> testRoundtrip(value: T, reader: ReadThrift<T>) {
        val out = ThriftCompactOutputProtocol()
        value.writeThrift(out)
        val bytes = out.toByteArray()
        val prot = ThriftSliceInputProtocol(bytes)
        val readVal = reader.readThrift(prot)
        assertEquals(value, readVal)
    }

    @Test
    fun testEnumRoundtrip() {
        for (type in ParquetType.entries) {
            testRoundtrip(type, ParquetType)
        }
    }

    @Test
    fun testUnionAllEmptyRoundtrip() {
        for (unit in TimeUnit.entries) {
            testRoundtrip(unit, TimeUnit)
        }
    }

    @Test
    fun testDecodeEmptyList() {
        val data = byteArrayOf(0)
        val prot = ThriftSliceInputProtocol(data)
        val header = prot.readListBegin()
        assertEquals(0, header.size)
        assertEquals(ElementType.BYTE, header.elementType)
    }

    @Test
    fun testReadListBeginSizeAboveI32MaxReturnsErr() {
        val data =
            byteArrayOf(
                0xF8.toByte(),
                0x80.toByte(),
                0x80.toByte(),
                0x80.toByte(),
                0x80.toByte(),
                0x08.toByte(),
            )
        val prot = ThriftSliceInputProtocol(data)
        assertFailsWith<ThriftProtocolError> {
            prot.readListBegin()
        }
    }

    @Test
    fun testReadListWrongType() {
        val data = byteArrayOf(0x42.toByte(), 0x01.toByte())
        val prot = ThriftSliceInputProtocol(data)
        val err =
            assertFailsWith<ParquetError> {
                readThriftVecInt(prot)
            }
        assertTrue(err.message!!.contains("Expected list element type of I32 but got BOOL"))
    }

    @Test
    fun testReadThriftVecRoundtripI32() {
        val data = byteArrayOf(0x25.toByte(), 0x00.toByte(), 0x00.toByte())
        val prot = ThriftSliceInputProtocol(data)
        val result = readThriftVecInt(prot)
        assertEquals(listOf(0, 0), result)
    }

    @Test
    fun testReadThriftVecSizeExceedsRemainingReturnsErr() {
        val data = byteArrayOf(0xE5.toByte())
        val prot = ThriftSliceInputProtocol(data)
        val err =
            assertFailsWith<ParquetError> {
                readThriftVecInt(prot)
            }
        assertTrue(err.message!!.contains("Thrift list size 14 exceeds remaining input length 0"))
    }

    @Test
    fun testReadThriftVecHugeDeclaredSizeReturnsErr() {
        val data =
            byteArrayOf(
                0xF5.toByte(),
                0xFC.toByte(),
                0xFC.toByte(),
                0xFC.toByte(),
                0x33.toByte(),
                0x00.toByte(),
                0x00.toByte(),
            )
        val prot = ThriftSliceInputProtocol(data)
        val err =
            assertFailsWith<ParquetError> {
                readThriftVecInt(prot)
            }
        val msg = err.message!!
        assertTrue(msg.contains("Thrift list size 109002364 exceeds remaining input length 2"), msg)
    }
}
