// port-lint: source file/metadata/memory.rs
package io.github.kotlinmania.parquet.file.metadata

/**
 * Trait for calculating the size of various containers.
 */
public interface HeapSize {
    /**
     * Return the size of any bytes allocated on the heap by this object,
     * including heap memory in those structures.
     */
    public fun heapSize(): Long
}

public fun <T : HeapSize> List<T>.heapSize(): Long =
    sumOf { it.heapSize() }

public fun String.heapSize(): Long = length.toLong()
