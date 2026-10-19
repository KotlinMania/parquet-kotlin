// port-lint: tests data_type.rs
package io.github.kotlinmania.parquet

import io.github.kotlinmania.bytes.Bytes
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class DataTypeTest {
    @Test
    fun testAsBytes() {
        // Test Int96
        val i96 = Int96.from(listOf(1u, 2u, 3u))
        assertContentEquals(
            byteArrayOf(1, 0, 0, 0, 2, 0, 0, 0, 3, 0, 0, 0),
            i96.asBytes(),
        )

        // Test ByteArray
        val byteArr = ByteArray.from(byteArrayOf(1, 2, 3))
        assertContentEquals(byteArrayOf(1, 2, 3), byteArr.asBytes())

        // Test Decimal
        val decimal1 = Decimal.fromI32(123, 5, 2)
        assertContentEquals(byteArrayOf(0, 0, 0, 123), decimal1.asBytes())
        val decimal2 = Decimal.fromI64(123L, 5, 2)
        assertContentEquals(byteArrayOf(0, 0, 0, 0, 0, 0, 0, 123), decimal2.asBytes())
        val decimal3 = Decimal.fromBytes(ByteArray.from(byteArrayOf(1, 2, 3)), 5, 2)
        assertContentEquals(byteArrayOf(1, 2, 3), decimal3.asBytes())
    }

    @Test
    fun testInt96From() {
        val i96 = Int96.from(listOf(1u, 12345u, 1234567890u))
        assertEquals(1u, i96.data()[0])
        assertEquals(12345u, i96.data()[1])
        assertEquals(1234567890u, i96.data()[2])
    }

    @Test
    fun testByteArrayFrom() {
        val abcBytes = "ABC".encodeToByteArray()
        assertContentEquals(abcBytes, ByteArray.from(abcBytes).data())
        assertContentEquals(abcBytes, ByteArray.from("ABC").data())
        assertContentEquals(
            byteArrayOf(1, 2, 3, 4, 5),
            ByteArray.from(Bytes.from(byteArrayOf(1, 2, 3, 4, 5))).data(),
        )
        val buf = byteArrayOf(6, 7, 8, 9, 10)
        assertContentEquals(byteArrayOf(6, 7, 8, 9, 10), ByteArray.from(buf).data())
    }

    @Test
    fun testDecimalPartialEq() {
        assertEquals(Decimal.default(), Decimal.fromI32(0, 0, 0))
        assertEquals(Decimal.fromI32(222, 5, 2), Decimal.fromI32(222, 5, 2))
        assertEquals(
            Decimal.fromBytes(ByteArray.from(byteArrayOf(0, 0, 0, 3)), 5, 2),
            Decimal.fromI32(3, 5, 2),
        )

        assertNotEquals(Decimal.fromI32(222, 5, 2), Decimal.fromI32(111, 5, 2))
        assertNotEquals(Decimal.fromI32(222, 5, 2), Decimal.fromI32(222, 6, 2))
        assertNotEquals(Decimal.fromI32(222, 5, 2), Decimal.fromI32(222, 5, 3))

        assertNotEquals(Decimal.fromI64(222L, 5, 2), Decimal.fromI32(222, 5, 2))
    }

    @Test
    fun testByteArrayOrd() {
        val byteArr1 = ByteArray.from(byteArrayOf(1, 2, 3))
        val byteArr11 = ByteArray.from(byteArrayOf(1, 2, 3))
        val byteArr2 = ByteArray.from(byteArrayOf(3, 4))
        val byteArr3 = ByteArray.from(byteArrayOf(1, 2, 4))
        val byteArr4 = ByteArray.from(byteArrayOf())
        val byteArr5 = ByteArray.from(byteArrayOf(2, 2, 3))

        assertTrue(byteArr1 < byteArr2)
        assertTrue(byteArr3 > byteArr1)
        assertTrue(byteArr1 > byteArr4)
        assertEquals(byteArr1, byteArr11)
        assertTrue(byteArr5 > byteArr1)
    }
}
