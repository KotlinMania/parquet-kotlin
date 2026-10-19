// port-lint: source file/metadata/options.rs
package io.github.kotlinmania.parquet.file.metadata

import io.github.kotlinmania.parquet.schema.SchemaDescPtr

/**
 * Enum to control decoding of some Parquet statistics fields.
 */
public sealed class ParquetStatisticsPolicy {
    /** Decode the relevant statistics for all columns. */
    public object KeepAll : ParquetStatisticsPolicy()

    /** Skip decoding the relevant statistics for all columns. */
    public object SkipAll : ParquetStatisticsPolicy()

    /**
     * Skip decoding the relevant statistics for all columns not in the provided set
     * of column indices.
     */
    public data class SkipExcept(
        public val keep: Set<Int>,
    ) : ParquetStatisticsPolicy()

    public companion object {
        public fun default(): ParquetStatisticsPolicy = KeepAll

        /**
         * Create a `ParquetStatisticsPolicy` to skip all columns except those in `keep`.
         *
         * If `keep` is empty, then this returns [SkipAll].
         */
        public fun skipExcept(keep: List<Int>): ParquetStatisticsPolicy =
            if (keep.isEmpty()) {
                SkipAll
            } else {
                SkipExcept(keep.toSet())
            }

        public fun skip_except(keep: List<Int>): ParquetStatisticsPolicy = skipExcept(keep)
    }

    /**
     * Returns whether the policy for the given column index is to skip the statistics.
     */
    internal fun isSkip(colIndex: Int): Boolean =
        when (this) {
            is KeepAll -> false
            is SkipAll -> true
            is SkipExcept -> !keep.contains(colIndex)
        }

    internal fun is_skip(colIndex: Int): Boolean = isSkip(colIndex)
}

/**
 * Options that can be set to control what parts of the Parquet file footer
 * metadata will be decoded and made present in the [ParquetMetaData] returned
 * by [ParquetMetaDataReader] and [ParquetMetaDataPushDecoder].
 */
public class ParquetMetaDataOptions(
    private var schemaDescr: SchemaDescPtr? = null,
    private var encodingStatsAsMask: Boolean = true,
    private var encodingStatsPolicy: ParquetStatisticsPolicy = ParquetStatisticsPolicy.KeepAll,
    private var columnStatsPolicy: ParquetStatisticsPolicy = ParquetStatisticsPolicy.KeepAll,
    private var sizeStatsPolicy: ParquetStatisticsPolicy = ParquetStatisticsPolicy.KeepAll,
) {
    public companion object {
        public fun default(): ParquetMetaDataOptions = ParquetMetaDataOptions()

        public fun new(): ParquetMetaDataOptions = default()
    }

    /**
     * Returns an optional [SchemaDescPtr] to use when decoding. If this is not `null` then
     * the schema in the footer will be skipped.
     */
    public fun schema(): SchemaDescPtr? = schemaDescr

    /**
     * Provide a schema to use when decoding the metadata.
     */
    public fun setSchema(valParam: SchemaDescPtr) {
        this.schemaDescr = valParam
    }

    public fun set_schema(valParam: SchemaDescPtr) {
        setSchema(valParam)
    }

    /**
     * Call [setSchema] and return `this` for chaining.
     */
    public fun withSchema(valParam: SchemaDescPtr): ParquetMetaDataOptions {
        setSchema(valParam)
        return this
    }

    public fun with_schema(valParam: SchemaDescPtr): ParquetMetaDataOptions = withSchema(valParam)

    /**
     * Returns whether to present the `encoding_stats` field of the Parquet `ColumnMetaData`
     * as a bitmask (defaults to `true`).
     */
    public fun encodingStatsAsMask(): Boolean = encodingStatsAsMask

    public fun encoding_stats_as_mask(): Boolean = encodingStatsAsMask()

    /**
     * Convert `encoding_stats` from a vector of `PageEncodingStats` to a bitmask.
     */
    public fun setEncodingStatsAsMask(valParam: Boolean) {
        this.encodingStatsAsMask = valParam
    }

    public fun set_encoding_stats_as_mask(valParam: Boolean) {
        setEncodingStatsAsMask(valParam)
    }

    /**
     * Call [setEncodingStatsAsMask] and return `this` for chaining.
     */
    public fun withEncodingStatsAsMask(valParam: Boolean): ParquetMetaDataOptions {
        setEncodingStatsAsMask(valParam)
        return this
    }

    public fun with_encoding_stats_as_mask(valParam: Boolean): ParquetMetaDataOptions = withEncodingStatsAsMask(valParam)

    /**
     * Returns whether to skip decoding the `encoding_stats` in the Parquet `ColumnMetaData`
     * for the column indexed by `colIndex`.
     */
    public fun skipEncodingStats(colIndex: Int): Boolean = encodingStatsPolicy.isSkip(colIndex)

    public fun skip_encoding_stats(colIndex: Int): Boolean = skipEncodingStats(colIndex)

    /**
     * Sets the decoding policy for `encoding_stats` in the Parquet `ColumnMetaData`.
     */
    public fun setEncodingStatsPolicy(policy: ParquetStatisticsPolicy) {
        this.encodingStatsPolicy = policy
    }

    public fun set_encoding_stats_policy(policy: ParquetStatisticsPolicy) {
        setEncodingStatsPolicy(policy)
    }

    /**
     * Call [setEncodingStatsPolicy] and return `this` for chaining.
     */
    public fun withEncodingStatsPolicy(policy: ParquetStatisticsPolicy): ParquetMetaDataOptions {
        setEncodingStatsPolicy(policy)
        return this
    }

    public fun with_encoding_stats_policy(policy: ParquetStatisticsPolicy): ParquetMetaDataOptions = withEncodingStatsPolicy(policy)

    /**
     * Returns whether to skip decoding the `statistics` in the Parquet `ColumnMetaData`
     * for the column indexed by `colIndex`.
     */
    public fun skipColumnStats(colIndex: Int): Boolean = columnStatsPolicy.isSkip(colIndex)

    public fun skip_column_stats(colIndex: Int): Boolean = skipColumnStats(colIndex)

    /**
     * Sets the decoding policy for `statistics` in the Parquet `ColumnMetaData`.
     */
    public fun setColumnStatsPolicy(policy: ParquetStatisticsPolicy) {
        this.columnStatsPolicy = policy
    }

    public fun set_column_stats_policy(policy: ParquetStatisticsPolicy) {
        setColumnStatsPolicy(policy)
    }

    /**
     * Call [setColumnStatsPolicy] and return `this` for chaining.
     */
    public fun withColumnStatsPolicy(policy: ParquetStatisticsPolicy): ParquetMetaDataOptions {
        setColumnStatsPolicy(policy)
        return this
    }

    public fun with_column_stats_policy(policy: ParquetStatisticsPolicy): ParquetMetaDataOptions = withColumnStatsPolicy(policy)

    /**
     * Returns whether to skip decoding the `size_statistics` in the Parquet `ColumnMetaData`
     * for the column indexed by `colIndex`.
     */
    public fun skipSizeStats(colIndex: Int): Boolean = sizeStatsPolicy.isSkip(colIndex)

    public fun skip_size_stats(colIndex: Int): Boolean = skipSizeStats(colIndex)

    /**
     * Sets the decoding policy for `size_statistics` in the Parquet `ColumnMetaData`.
     */
    public fun setSizeStatsPolicy(policy: ParquetStatisticsPolicy) {
        this.sizeStatsPolicy = policy
    }

    public fun set_size_stats_policy(policy: ParquetStatisticsPolicy) {
        setSizeStatsPolicy(policy)
    }

    /**
     * Call [setSizeStatsPolicy] and return `this` for chaining.
     */
    public fun withSizeStatsPolicy(policy: ParquetStatisticsPolicy): ParquetMetaDataOptions {
        setSizeStatsPolicy(policy)
        return this
    }

    public fun with_size_stats_policy(policy: ParquetStatisticsPolicy): ParquetMetaDataOptions = withSizeStatsPolicy(policy)
}
