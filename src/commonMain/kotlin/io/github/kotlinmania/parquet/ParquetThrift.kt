// port-lint: source parquet_thrift.rs
package io.github.kotlinmania.parquet

import io.github.kotlinmania.io.Buffer
import io.github.kotlinmania.io.Sink
import io.github.kotlinmania.io.Source
import io.github.kotlinmania.io.readByteArray

/**
 * Protocol error thrown during Thrift decoding or encoding.
 */
public sealed class ThriftProtocolError(
    message: String? = null,
    cause: Throwable? = null,
) : Exception(message, cause) {
    public object Eof : ThriftProtocolError("Unexpected EOF")

    public class IO(
        cause: Throwable,
    ) : ThriftProtocolError(cause.message, cause)

    public class InvalidFieldType(
        public val value: UByte,
    ) : ThriftProtocolError("Unexpected struct field type $value")

    public class InvalidElementType(
        public val value: UByte,
    ) : ThriftProtocolError("Unexpected list/set element type $value")

    public class FieldDeltaOverflow(
        public val fieldDelta: UByte,
        public val lastFieldId: Short,
    ) : ThriftProtocolError("cannot add $fieldDelta to $lastFieldId")

    public class InvalidBoolean(
        public val value: UByte,
    ) : ThriftProtocolError("cannot convert $value into bool")

    public object IntegerOverflow : ThriftProtocolError("integer overflow decoding thrift value")

    public object Utf8Error : ThriftProtocolError("invalid utf8")

    public class SkipDepth(
        public val fieldType: FieldType,
    ) : ThriftProtocolError("cannot parse past $fieldType")

    public class SkipUnsupportedType(
        public val fieldType: FieldType,
    ) : ThriftProtocolError("cannot skip field type $fieldType")

    public fun toParquetError(): ParquetError =
        when (this) {
            is Eof -> {
                eofErr("Unexpected EOF")
            }

            is IO -> {
                generalErr(cause?.message ?: "IO error", cause)
            }

            is InvalidFieldType -> {
                when (val f = FieldType.tryFrom(value.toByte())) {
                    null -> generalErr("Unexpected struct field type $value")
                    else -> generalErr("Unexpected struct field type $f")
                }
            }

            is InvalidElementType -> {
                generalErr("Unexpected list/set element type $value")
            }

            is FieldDeltaOverflow -> {
                generalErr("cannot add $fieldDelta to $lastFieldId")
            }

            is InvalidBoolean -> {
                generalErr("cannot convert $value into bool")
            }

            is IntegerOverflow -> {
                generalErr("integer overflow decoding thrift value")
            }

            is Utf8Error -> {
                generalErr("invalid utf8")
            }

            is SkipDepth -> {
                generalErr("cannot parse past $fieldType")
            }

            is SkipUnsupportedType -> {
                generalErr("cannot skip field type $fieldType")
            }
        }
}

/**
 * Wrapper for thrift `double` fields. This is used to provide
 * an implementation of equality for floats using IEEE 754 total order.
 */
public data class OrderedF64(
    public val value: Double,
) : Comparable<OrderedF64> {
    override fun compareTo(other: OrderedF64): Int = value.compareTo(other.value)
}

/**
 * Thrift compact protocol types for struct fields.
 */
public enum class FieldType(
    public val value: Byte,
) {
    STOP(0),
    BOOLEAN_TRUE(1),
    BOOLEAN_FALSE(2),
    BYTE(3),
    I16(4),
    I32(5),
    I64(6),
    DOUBLE(7),
    BINARY(8),
    LIST(9),
    SET(10),
    MAP(11),
    STRUCT(12),
    UUID(13),
    ;

    public companion object {
        public fun tryFrom(value: Byte): FieldType? =
            when (value.toInt()) {
                0 -> STOP
                1 -> BOOLEAN_TRUE
                2 -> BOOLEAN_FALSE
                3 -> BYTE
                4 -> I16
                5 -> I32
                6 -> I64
                7 -> DOUBLE
                8 -> BINARY
                9 -> LIST
                10 -> SET
                11 -> MAP
                12 -> STRUCT
                13 -> UUID
                else -> null
            }

        public fun fromElementType(element: ElementType): FieldType =
            when (element) {
                ElementType.BOOL -> BOOLEAN_TRUE
                ElementType.BYTE -> BYTE
                ElementType.I16 -> I16
                ElementType.I32 -> I32
                ElementType.I64 -> I64
                ElementType.DOUBLE -> DOUBLE
                ElementType.BINARY -> BINARY
                ElementType.LIST -> LIST
                ElementType.SET -> SET
                ElementType.MAP -> MAP
                ElementType.STRUCT -> STRUCT
                ElementType.UUID -> UUID
            }
    }
}

/**
 * Thrift compact protocol types for list elements.
 */
public enum class ElementType(
    public val value: Byte,
) {
    BOOL(2),
    BYTE(3),
    I16(4),
    I32(5),
    I64(6),
    DOUBLE(7),
    BINARY(8),
    LIST(9),
    SET(10),
    MAP(11),
    STRUCT(12),
    UUID(13),
    ;

    public companion object {
        public fun tryFrom(value: Byte): ElementType? =
            when (value.toInt()) {
                1, 2 -> BOOL
                3 -> BYTE
                4 -> I16
                5 -> I32
                6 -> I64
                7 -> DOUBLE
                8 -> BINARY
                9 -> LIST
                10 -> SET
                11 -> MAP
                12 -> STRUCT
                13 -> UUID
                else -> null
            }
    }
}

/**
 * Struct used to describe a Thrift struct field during decoding.
 */
public data class FieldIdentifier(
    public val fieldType: FieldType,
    public val id: Short,
) {
    public fun boolVal(): Boolean =
        when (fieldType) {
            FieldType.BOOLEAN_TRUE -> true
            FieldType.BOOLEAN_FALSE -> false
            else -> throw ThriftProtocolError.InvalidFieldType(fieldType.value.toUByte())
        }
}

/**
 * Struct used to describe a Thrift list.
 */
public data class ListIdentifier(
    public val elementType: ElementType,
    public val size: Int,
)

/**
 * Low-level object used to deserialize structs encoded with the Thrift compact protocol.
 */
public interface ThriftCompactInputProtocol {
    public fun readByte(): Byte

    public fun readBytes(): kotlin.ByteArray

    public fun readBytesOwned(): kotlin.ByteArray

    public fun skipBytes(n: Int)

    public fun readDouble(): Double

    public fun remainingBytes(): Int? = null

    public fun readVlq(): ULong {
        val b = readByte().toUByte().toULong()
        if ((b and 0x80uL) == 0uL) {
            return b
        }
        var inProgress = b and 0x7FuL
        var shift = 7
        while (true) {
            val byte = readByte().toUByte().toULong()
            inProgress = inProgress or ((byte and 0x7FuL) shl shift)
            if ((byte and 0x80uL) == 0uL) {
                return inProgress
            }
            shift += 7
        }
    }

    public fun readZigZag(): Long {
        val v = readVlq().toLong()
        return (v ushr 1) xor -(v and 1L)
    }

    public fun readListBegin(): ListIdentifier {
        val header = readByte().toInt() and 0xFF
        if (header == 0) {
            return ListIdentifier(ElementType.BYTE, 0)
        }
        val elementByte = (header and 0x0F).toByte()
        val elementType =
            ElementType.tryFrom(elementByte)
                ?: throw ThriftProtocolError.InvalidElementType(elementByte.toUByte())

        val possibleElementCount = (header and 0xF0) ushr 4
        val elementCount =
            if (possibleElementCount != 15) {
                possibleElementCount
            } else {
                val vlq = readVlq()
                if (vlq > Int.MAX_VALUE.toULong()) {
                    throw ThriftProtocolError.IntegerOverflow
                }
                vlq.toInt()
            }

        return ListIdentifier(elementType, elementCount)
    }

    public fun readFullFieldId(): Short = readI16()

    public fun readFieldBegin(lastFieldId: Short): FieldIdentifier {
        val fieldTypeRaw = readByte().toInt() and 0xFF
        if ((fieldTypeRaw and 0x0F) == 0) {
            return FieldIdentifier(FieldType.STOP, 0)
        }

        val fieldDelta = (fieldTypeRaw ushr 4).toByte()
        val fTypeByte = (fieldTypeRaw and 0x0F).toByte()
        val fieldType =
            FieldType.tryFrom(fTypeByte)
                ?: throw ThriftProtocolError.InvalidFieldType(fTypeByte.toUByte())

        val id: Short =
            if (fieldDelta.toInt() != 0) {
                val sum = lastFieldId.toInt() + fieldDelta.toInt()
                if (sum > Short.MAX_VALUE || sum < Short.MIN_VALUE) {
                    throw ThriftProtocolError.FieldDeltaOverflow(fieldDelta.toUByte(), lastFieldId)
                }
                sum.toShort()
            } else {
                readFullFieldId()
            }

        return FieldIdentifier(fieldType, id)
    }

    public fun readFieldHeader(): Pair<Byte, Byte> {
        val raw = readByte().toInt() and 0xFF
        val fieldDelta = (raw ushr 4).toByte()
        val fieldType = (raw and 0x0F).toByte()
        return Pair(fieldType, fieldDelta)
    }

    public fun readBool(): Boolean {
        val b = readByte().toInt() and 0xFF
        return when (b) {
            0x01 -> true
            0x00, 0x02 -> false
            else -> throw ThriftProtocolError.InvalidBoolean(b.toUByte())
        }
    }

    public fun readString(): String = readBytesOwned().decodeToString()

    public fun readI8(): Byte = readByte()

    public fun readI16(): Short = readZigZag().toShort()

    public fun readI32(): Int = readZigZag().toInt()

    public fun readI64(): Long = readZigZag()

    public fun skipVlq() {
        while (true) {
            val byte = readByte().toInt() and 0xFF
            if ((byte and 0x80) == 0) {
                return
            }
        }
    }

    public fun skipBinary() {
        val lenVlq = readVlq()
        if (lenVlq > Int.MAX_VALUE.toULong()) throw ThriftProtocolError.IntegerOverflow
        skipBytes(lenVlq.toInt())
    }

    public fun skip(fieldType: FieldType) {
        val defaultSkipDepth: Byte = 64
        skipTillDepth(fieldType, defaultSkipDepth)
    }

    public fun skipEmptyStruct() {
        val b = readByte()
        if (b.toInt() != 0) {
            throw generalErr("Empty struct has fields")
        }
    }

    public fun skipTillDepth(fieldType: FieldType, depth: Byte) {
        if (depth <= 0) {
            throw ThriftProtocolError.SkipDepth(fieldType)
        }

        when (fieldType) {
            FieldType.BOOLEAN_FALSE, FieldType.BOOLEAN_TRUE -> {}

            FieldType.BYTE -> {
                readI8()
            }

            FieldType.I16, FieldType.I32, FieldType.I64 -> {
                skipVlq()
            }

            FieldType.DOUBLE -> {
                skipBytes(8)
            }

            FieldType.BINARY -> {
                skipBinary()
            }

            FieldType.STRUCT -> {
                while (true) {
                    val fieldIdent = readFieldBegin(0)
                    if (fieldIdent.fieldType == FieldType.STOP) {
                        break
                    }
                    skipTillDepth(fieldIdent.fieldType, (depth - 1).toByte())
                }
            }

            FieldType.LIST, FieldType.SET -> {
                val listIdent = readListBegin()
                val elemType = FieldType.fromElementType(listIdent.elementType)
                for (i in 0 until listIdent.size) {
                    skipTillDepth(elemType, (depth - 1).toByte())
                }
            }

            FieldType.MAP -> {
                val sizeVlq = readVlq()
                if (sizeVlq > Int.MAX_VALUE.toULong()) throw ThriftProtocolError.IntegerOverflow
                val size = sizeVlq.toInt()
                if (size > 0) {
                    val kv = readByte().toInt() and 0xFF
                    val keyByte = (kv ushr 4).toByte()
                    val valByte = (kv and 0x0F).toByte()
                    val keyElem =
                        ElementType.tryFrom(keyByte)
                            ?: throw ThriftProtocolError.InvalidElementType(keyByte.toUByte())
                    val valElem =
                        ElementType.tryFrom(valByte)
                            ?: throw ThriftProtocolError.InvalidElementType(valByte.toUByte())
                    val keyType = FieldType.fromElementType(keyElem)
                    val valType = FieldType.fromElementType(valElem)
                    for (i in 0 until size) {
                        skipTillDepth(keyType, (depth - 1).toByte())
                        skipTillDepth(valType, (depth - 1).toByte())
                    }
                }
            }

            FieldType.UUID -> {
                skipBytes(16)
            }

            FieldType.STOP -> {
                throw ThriftProtocolError.SkipUnsupportedType(fieldType)
            }
        }
    }
}

/**
 * A high performance Thrift reader that reads from a byte array.
 */
public class ThriftSliceInputProtocol(
    private val buffer: kotlin.ByteArray,
    offset: Int = 0,
    length: Int = buffer.size - offset,
) : ThriftCompactInputProtocol {
    private var pos: Int = offset
    private val limit: Int = offset + length

    public fun asSlice(): kotlin.ByteArray = buffer.copyOfRange(pos, limit)

    public fun remaining(): Int = limit - pos

    override fun remainingBytes(): Int? = remaining()

    override fun readByte(): Byte {
        if (pos >= limit) throw ThriftProtocolError.Eof
        return buffer[pos++]
    }

    override fun readBytes(): kotlin.ByteArray = readBytesOwned()

    override fun readBytesOwned(): kotlin.ByteArray {
        val lenVlq = readVlq()
        if (lenVlq > Int.MAX_VALUE.toULong()) throw ThriftProtocolError.IntegerOverflow
        val len = lenVlq.toInt()
        if (len > remaining()) throw ThriftProtocolError.Eof
        val result = buffer.copyOfRange(pos, pos + len)
        pos += len
        return result
    }

    override fun skipBytes(n: Int) {
        if (n < 0 || n > remaining()) throw ThriftProtocolError.Eof
        pos += n
    }

    override fun readDouble(): Double {
        if (remaining() < 8) throw ThriftProtocolError.Eof
        val b0 = buffer[pos++].toLong() and 0xFF
        val b1 = buffer[pos++].toLong() and 0xFF
        val b2 = buffer[pos++].toLong() and 0xFF
        val b3 = buffer[pos++].toLong() and 0xFF
        val b4 = buffer[pos++].toLong() and 0xFF
        val b5 = buffer[pos++].toLong() and 0xFF
        val b6 = buffer[pos++].toLong() and 0xFF
        val b7 = buffer[pos++].toLong() and 0xFF
        val bits =
            b0 or (b1 shl 8) or (b2 shl 16) or (b3 shl 24) or
                (b4 shl 32) or (b5 shl 40) or (b6 shl 48) or (b7 shl 56)
        return Double.fromBits(bits)
    }
}

/**
 * A Thrift input protocol that wraps a [Source] object.
 */
public class ThriftReadInputProtocol(
    private val source: Source,
) : ThriftCompactInputProtocol {
    override fun readByte(): Byte {
        try {
            return source.readByte()
        } catch (_: Exception) {
            throw ThriftProtocolError.Eof
        }
    }

    override fun readBytes(): kotlin.ByteArray = readBytesOwned()

    override fun readBytesOwned(): kotlin.ByteArray {
        val lenVlq = readVlq()
        if (lenVlq > Int.MAX_VALUE.toULong()) throw ThriftProtocolError.IntegerOverflow
        val len = lenVlq.toInt()
        val buf = kotlin.ByteArray(len)
        var readTotal = 0
        while (readTotal < len) {
            val count = source.readAtMostTo(buf, readTotal, len)
            if (count == -1) throw ThriftProtocolError.Eof
            readTotal += count
        }
        return buf
    }

    override fun skipBytes(n: Int) {
        if (n < 0) throw ThriftProtocolError.Eof
        try {
            source.skip(n.toLong())
        } catch (_: Exception) {
            throw ThriftProtocolError.Eof
        }
    }

    override fun readDouble(): Double {
        val buf = kotlin.ByteArray(8)
        var readTotal = 0
        while (readTotal < 8) {
            val count = source.readAtMostTo(buf, readTotal, 8)
            if (count == -1) throw ThriftProtocolError.Eof
            readTotal += count
        }
        val b0 = buf[0].toLong() and 0xFF
        val b1 = buf[1].toLong() and 0xFF
        val b2 = buf[2].toLong() and 0xFF
        val b3 = buf[3].toLong() and 0xFF
        val b4 = buf[4].toLong() and 0xFF
        val b5 = buf[5].toLong() and 0xFF
        val b6 = buf[6].toLong() and 0xFF
        val b7 = buf[7].toLong() and 0xFF
        val bits =
            b0 or (b1 shl 8) or (b2 shl 16) or (b3 shl 24) or
                (b4 shl 32) or (b5 shl 40) or (b6 shl 48) or (b7 shl 56)
        return Double.fromBits(bits)
    }

    override fun remainingBytes(): Int? = null
}

/**
 * Trait implemented for objects that can be deserialized from a Thrift input stream.
 */
public fun interface ReadThrift<T> {
    public fun readThrift(prot: ThriftCompactInputProtocol): T
}

public fun validateListType(expected: ElementType, got: ListIdentifier) {
    if (got.elementType != expected) {
        throw generalErr("Expected list element type of $expected but got ${got.elementType}")
    }
}

public fun checkListSize(remaining: Int?, size: Int) {
    if (remaining != null && size > remaining) {
        throw generalErr("Thrift list size $size exceeds remaining input length $remaining")
    }
}

public fun <T> readThriftVec(
    prot: ThriftCompactInputProtocol,
    reader: ReadThrift<T>,
    expectedType: ElementType,
): List<T> {
    val listIdent = prot.readListBegin()
    validateListType(expectedType, listIdent)
    val size = listIdent.size
    checkListSize(prot.remainingBytes(), size)
    val res = ArrayList<T>(size)
    for (i in 0 until size) {
        res.add(reader.readThrift(prot))
    }
    return res
}

public fun readThriftVecInt(prot: ThriftCompactInputProtocol): List<Int> {
    val listIdent = prot.readListBegin()
    validateListType(ElementType.I32, listIdent)
    val size = listIdent.size
    checkListSize(prot.remainingBytes(), size)
    val res = ArrayList<Int>(size)
    for (i in 0 until size) {
        res.add(prot.readI32())
    }
    return res
}

/**
 * Low-level object used to serialize structs to the Thrift compact output protocol.
 */
public class ThriftCompactOutputProtocol(
    private val sink: Sink,
) {
    public constructor() : this(Buffer())

    private var writePathInSchema: Boolean = true
    private var writeRgOrdinal: Boolean = true

    public fun setWritePathInSchema(`val`: Boolean) {
        writePathInSchema = `val`
    }

    public fun writePathInSchema(): Boolean = writePathInSchema

    public fun setWriteRowGroupOrdinal(`val`: Boolean) {
        writeRgOrdinal = `val`
    }

    public fun writeRowGroupOrdinal(): Boolean = writeRgOrdinal

    public fun writeByte(b: Byte) {
        sink.writeByte(b)
    }

    public fun writeVlq(value: ULong) {
        var v = value
        while (v > 0x7FuL) {
            writeByte(((v and 0x7FuL) or 0x80uL).toByte())
            v = v shr 7
        }
        writeByte(v.toByte())
    }

    public fun writeZigZag(value: Long) {
        val s = if (value < 0L) 1L else 0L
        val z = ((value xor -s) shl 1) + s
        writeVlq(z.toULong())
    }

    public fun writeFieldBegin(
        fieldType: FieldType,
        fieldId: Short,
        lastFieldId: Short,
    ) {
        val delta = (fieldId.toInt() - lastFieldId.toInt()).toShort()
        if (delta > 0 && delta <= 0x0F) {
            writeByte(((delta.toInt() shl 4) or fieldType.value.toInt()).toByte())
        } else {
            writeByte(fieldType.value)
            writeI16(fieldId)
        }
    }

    public fun writeListBegin(elementType: ElementType, len: Int) {
        if (len < 15) {
            writeByte(((len shl 4) or elementType.value.toInt()).toByte())
        } else {
            writeByte((0xF0 or elementType.value.toInt()).toByte())
            writeVlq(len.toULong())
        }
    }

    public fun writeStructEnd() {
        writeByte(0)
    }

    public fun writeBytes(bytes: kotlin.ByteArray) {
        writeVlq(bytes.size.toULong())
        sink.write(bytes, 0, bytes.size)
    }

    public fun writeEmptyStruct(fieldId: Short, lastFieldId: Short): Short {
        writeFieldBegin(FieldType.STRUCT, fieldId, lastFieldId)
        writeStructEnd()
        return lastFieldId
    }

    public fun writeBool(value: Boolean) {
        if (value) writeByte(1) else writeByte(2)
    }

    public fun writeI8(value: Byte) {
        writeByte(value)
    }

    public fun writeI16(value: Short) {
        writeZigZag(value.toLong())
    }

    public fun writeI32(value: Int) {
        writeZigZag(value.toLong())
    }

    public fun writeI64(value: Long) {
        writeZigZag(value)
    }

    public fun writeDouble(value: Double) {
        val bits = value.toBits()
        val bytes = kotlin.ByteArray(8)
        bytes[0] = (bits and 0xFF).toByte()
        bytes[1] = ((bits ushr 8) and 0xFF).toByte()
        bytes[2] = ((bits ushr 16) and 0xFF).toByte()
        bytes[3] = ((bits ushr 24) and 0xFF).toByte()
        bytes[4] = ((bits ushr 32) and 0xFF).toByte()
        bytes[5] = ((bits ushr 40) and 0xFF).toByte()
        bytes[6] = ((bits ushr 48) and 0xFF).toByte()
        bytes[7] = ((bits ushr 56) and 0xFF).toByte()
        sink.write(bytes, 0, 8)
    }

    public fun toByteArray(): kotlin.ByteArray =
        if (sink is Buffer) sink.copy().readByteArray() else throw UnsupportedOperationException("Sink is not a Buffer")
}

/**
 * Trait implemented by objects that are to be serialized to a Thrift compact output protocol stream.
 */
public interface WriteThrift {
    public val elementType: ElementType

    public fun writeThrift(writer: ThriftCompactOutputProtocol)
}

/**
 * Trait implemented by objects that are fields of Thrift structs.
 */
public interface WriteThriftField {
    public fun writeThriftField(
        writer: ThriftCompactOutputProtocol,
        fieldId: Short,
        lastFieldId: Short,
    ): Short
}
