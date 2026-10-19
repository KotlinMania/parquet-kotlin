// port-lint: tests errors.rs
package io.github.kotlinmania.parquet

import kotlin.test.Test
import kotlin.test.assertEquals

class ErrorsTest {
    @Test
    fun errorMacrosFormatInlineArgs() {
        val expected = 1
        val actual = 2
        assertEquals(
            "Parquet error: expected $expected, got $actual",
            generalErr("expected $expected, got $actual").message,
        )
        assertEquals(
            "NYI: not yet: $expected",
            nyiErr("not yet: $expected").message,
        )
        assertEquals(
            "EOF: eof at $actual",
            eofErr("eof at $actual").message,
        )
    }
}
