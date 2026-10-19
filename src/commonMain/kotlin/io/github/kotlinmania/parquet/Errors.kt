// port-lint: source errors.rs
package io.github.kotlinmania.parquet

/**
 * Parquet error enumeration.
 */
public sealed class ParquetError(message: String? = null, cause: Throwable? = null) : Exception(message, cause) {
    /**
     * General Parquet error.
     * Returned when code violates normal workflow of working with Parquet files.
     */
    public class General(public val errorMessage: String, cause: Throwable? = null) :
        ParquetError("Parquet error: $errorMessage", cause)

    /**
     * "Not yet implemented" Parquet error.
     * Returned when functionality is not yet available.
     */
    public class NYI(public val errorMessage: String) :
        ParquetError("NYI: $errorMessage")

    /**
     * "End of file" Parquet error.
     * Returned when IO related failures occur, e.g. when there are not enough bytes to decode.
     */
    public class EOF(public val errorMessage: String) :
        ParquetError("EOF: $errorMessage")

    /**
     * Arrow error.
     * Returned when reading into arrow or writing from arrow.
     */
    public class Arrow(public val errorMessage: String, cause: Throwable? = null) :
        ParquetError("Arrow: $errorMessage", cause)

    /**
     * Error when the requested column index is more than the
     * number of columns in the row group.
     */
    public class IndexOutOfBound(public val index: Long, public val bound: Long) :
        ParquetError("Index $index out of bound: $bound")

    /**
     * An external error variant.
     */
    public class External(public val error: Throwable) :
        ParquetError("External: ${error.message ?: error.toString()}", error)

    /**
     * Returned when a function needs more data to complete properly. The [needed] field indicates
     * the total number of bytes required, not the number of additional bytes.
     */
    public class NeedMoreData(public val needed: Long) :
        ParquetError("NeedMoreData: $needed")

    /**
     * Returned when a function needs more data to complete properly.
     * The [start]..[end] indicates the range of bytes that are needed.
     */
    public class NeedMoreDataRange(public val start: Long, public val end: Long) :
        ParquetError("NeedMoreDataRange: $start..$end")
}

public fun generalErr(message: String, cause: Throwable? = null): ParquetError =
    ParquetError.General(message, cause)

public fun nyiErr(message: String): ParquetError =
    ParquetError.NYI(message)

public fun eofErr(message: String): ParquetError =
    ParquetError.EOF(message)

public fun arrowErr(message: String, cause: Throwable? = null): ParquetError =
    ParquetError.Arrow(message, cause)
