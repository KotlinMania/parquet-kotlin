package io.github.kotlinmania.parquet.file

/**
 * The length of the parquet footer in bytes.
 */
public const val FOOTER_SIZE: Int = 8

public val PARQUET_MAGIC: ByteArray = byteArrayOf('P'.code.toByte(), 'A'.code.toByte(), 'R'.code.toByte(), '1'.code.toByte())
public val PARQUET_MAGIC_ENCR_FOOTER: ByteArray = byteArrayOf('P'.code.toByte(), 'A'.code.toByte(), 'R'.code.toByte(), 'E'.code.toByte())
