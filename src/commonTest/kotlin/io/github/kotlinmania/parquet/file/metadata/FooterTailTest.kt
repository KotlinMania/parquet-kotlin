// port-lint: tests file/metadata/footer_tail.rs
package io.github.kotlinmania.parquet.file.metadata

import io.github.kotlinmania.parquet.ParquetError
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FooterTailTest {
    @Test
    fun testFooterTailPlain() {
        val last8Bytes =
            byteArrayOf(
                0x1C.toByte(),
                0x00,
                0x00,
                0x00,
                'P'.code.toByte(),
                'A'.code.toByte(),
                'R'.code.toByte(),
                '1'.code.toByte(),
            )
        val footerTail = FooterTail.tryFrom(last8Bytes)
        assertEquals(28, footerTail.metadataLength())
        assertFalse(footerTail.isEncryptedFooter())
    }

    @Test
    fun testFooterTailEncrypted() {
        val last8Bytes =
            byteArrayOf(
                0x00,
                0x02,
                0x00,
                0x00,
                'P'.code.toByte(),
                'A'.code.toByte(),
                'R'.code.toByte(),
                'E'.code.toByte(),
            )
        val footerTail = FooterTail.tryFrom(last8Bytes)
        assertEquals(512, footerTail.metadataLength())
        assertTrue(footerTail.isEncryptedFooter())
    }

    @Test
    fun testFooterTailInvalidLength() {
        val bytes = byteArrayOf(0x00, 0x01)
        val err =
            assertFailsWith<ParquetError> {
                FooterTail.tryFrom(bytes)
            }
        assertTrue(err.message!!.contains("Invalid footer length"))
    }

    @Test
    fun testFooterTailCorruptMagic() {
        val last8Bytes =
            byteArrayOf(
                0x1C.toByte(),
                0x00,
                0x00,
                0x00,
                'X'.code.toByte(),
                'A'.code.toByte(),
                'R'.code.toByte(),
                '1'.code.toByte(),
            )
        val err =
            assertFailsWith<ParquetError> {
                FooterTail.tryFrom(last8Bytes)
            }
        assertTrue(err.message!!.contains("Invalid Parquet file. Corrupt footer"))
    }
}
