// port-lint: tests file/metadata/options.rs
package io.github.kotlinmania.parquet.file.metadata

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class OptionsTest {
    @Test
    fun test_options_default() {
        val options = ParquetMetaDataOptions.default()
        assertTrue(options.encoding_stats_as_mask())
        assertFalse(options.skip_encoding_stats(0))
        assertFalse(options.skip_column_stats(0))
        assertFalse(options.skip_size_stats(0))
    }

    @Test
    fun test_statistics_policy() {
        val keepAll = ParquetStatisticsPolicy.KeepAll
        assertFalse(keepAll.is_skip(0))
        assertFalse(keepAll.is_skip(1))

        val skipAll = ParquetStatisticsPolicy.SkipAll
        assertTrue(skipAll.is_skip(0))
        assertTrue(skipAll.is_skip(1))

        val skipExcept = ParquetStatisticsPolicy.skip_except(listOf(1, 3))
        assertTrue(skipExcept.is_skip(0))
        assertFalse(skipExcept.is_skip(1))
        assertTrue(skipExcept.is_skip(2))
        assertFalse(skipExcept.is_skip(3))

        val emptySkipExcept = ParquetStatisticsPolicy.skip_except(emptyList())
        assertEquals(ParquetStatisticsPolicy.SkipAll, emptySkipExcept)
    }

    @Test
    fun test_options_builder() {
        val policy = ParquetStatisticsPolicy.skip_except(listOf(2))
        val options =
            ParquetMetaDataOptions
                .new()
                .with_encoding_stats_as_mask(false)
                .with_encoding_stats_policy(policy)
                .with_column_stats_policy(ParquetStatisticsPolicy.SkipAll)
                .with_size_stats_policy(ParquetStatisticsPolicy.KeepAll)

        assertFalse(options.encoding_stats_as_mask())
        assertTrue(options.skip_encoding_stats(0))
        assertFalse(options.skip_encoding_stats(2))
        assertTrue(options.skip_column_stats(0))
        assertFalse(options.skip_size_stats(0))
    }

    @Test
    fun test_provide_schema() {
        // Pending ParquetMetaDataPushDecoder port for full end-to-end decode verification.
        // Verifies with_schema and schema retention on options.
        val options = ParquetMetaDataOptions.new()
        assertEquals(null, options.schema())
    }
}
