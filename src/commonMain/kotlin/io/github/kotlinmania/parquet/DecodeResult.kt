package io.github.kotlinmania.parquet

/**
 * What data is needed to read the next item from a decoder.
 *
 * This is used to communicate between the decoder and the caller
 * to indicate what data is needed next, or what the result of decoding is.
 */
public sealed class DecodeResult<out T> {
    /**
     * The ranges of data necessary to proceed.
     */
    public data class NeedsData(
        public val ranges: List<LongRange>,
    ) : DecodeResult<Nothing>()

    /**
     * The decoder produced an output item.
     */
    public data class Data<T>(
        public val value: T,
    ) : DecodeResult<T>()

    /**
     * The decoder finished processing.
     */
    public data object Finished : DecodeResult<Nothing>()
}
