// port-lint: source file/metadata/footer_tail.rs
package io.github.kotlinmania.parquet.file.metadata

import io.github.kotlinmania.parquet.file.FOOTER_SIZE
import io.github.kotlinmania.parquet.file.PARQUET_MAGIC
import io.github.kotlinmania.parquet.file.PARQUET_MAGIC_ENCR_FOOTER
import io.github.kotlinmania.parquet.generalErr

/**
 * Parsed Parquet footer tail (last 8 bytes of a Parquet file).
 */
public data class FooterTail(
    private val metadataLengthVal: Int,
    private val encryptedFooterVal: Boolean,
) {
    public fun metadataLength(): Int = metadataLengthVal

    public fun isEncryptedFooter(): Boolean = encryptedFooterVal

    public companion object {
        public fun tryNew(slice: ByteArray): FooterTail {
            if (slice.size != FOOTER_SIZE) {
                throw generalErr("Invalid footer length ${slice.size}, expected $FOOTER_SIZE")
            }
            val magic = slice.copyOfRange(4, 8)
            val encryptedFooter =
                if (magic.contentEquals(PARQUET_MAGIC_ENCR_FOOTER)) {
                    true
                } else if (magic.contentEquals(PARQUET_MAGIC)) {
                    false
                } else {
                    throw generalErr("Invalid Parquet file. Corrupt footer")
                }

            val metadataLenLong =
                (slice[0].toLong() and 0xFFL) or
                    ((slice[1].toLong() and 0xFFL) shl 8) or
                    ((slice[2].toLong() and 0xFFL) shl 16) or
                    ((slice[3].toLong() and 0xFFL) shl 24)

            if (metadataLenLong < 0 || metadataLenLong > Int.MAX_VALUE) {
                throw generalErr("Metadata length $metadataLenLong exceeds maximum supported size")
            }

            return FooterTail(
                metadataLengthVal = metadataLenLong.toInt(),
                encryptedFooterVal = encryptedFooter,
            )
        }

        public fun tryFrom(slice: ByteArray): FooterTail = tryNew(slice)
    }
}
