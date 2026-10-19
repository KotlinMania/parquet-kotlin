package io.github.kotlinmania.parquet.file.metadata.thrift

import io.github.kotlinmania.parquet.ConvertedType
import io.github.kotlinmania.parquet.ElementType
import io.github.kotlinmania.parquet.FieldRepetitionType
import io.github.kotlinmania.parquet.FieldType
import io.github.kotlinmania.parquet.LogicalType
import io.github.kotlinmania.parquet.ParquetType
import io.github.kotlinmania.parquet.ReadThrift
import io.github.kotlinmania.parquet.ThriftCompactInputProtocol
import io.github.kotlinmania.parquet.ThriftCompactOutputProtocol
import io.github.kotlinmania.parquet.WriteThrift
import io.github.kotlinmania.parquet.WriteThriftField
import io.github.kotlinmania.parquet.generalErr

/**
 * Struct used to describe a Thrift SchemaElement.
 */
public data class SchemaElement(
    public val type: ParquetType? = null,
    public val typeLength: Int? = null,
    public val repetitionType: FieldRepetitionType? = null,
    public val name: String,
    public val numChildren: Int? = null,
    public val convertedType: ConvertedType? = null,
    public val scale: Int? = null,
    public val precision: Int? = null,
    public val fieldId: Int? = null,
    public val logicalType: LogicalType? = null,
) : WriteThrift,
    WriteThriftField {
    override val elementType: ElementType = ElementType.STRUCT

    override fun writeThrift(writer: ThriftCompactOutputProtocol) {
        var lastId: Short = 0
        if (type != null) {
            lastId = type.writeThriftField(writer, 1, lastId)
        }
        if (typeLength != null) {
            writer.writeFieldBegin(FieldType.I32, 2, lastId)
            writer.writeI32(typeLength)
            lastId = 2
        }
        if (repetitionType != null) {
            lastId = repetitionType.writeThriftField(writer, 3, lastId)
        }
        writer.writeFieldBegin(FieldType.BINARY, 4, lastId)
        writer.writeBytes(name.encodeToByteArray())
        lastId = 4
        if (numChildren != null) {
            writer.writeFieldBegin(FieldType.I32, 5, lastId)
            writer.writeI32(numChildren)
            lastId = 5
        }
        if (convertedType != null && convertedType != ConvertedType.NONE) {
            lastId = convertedType.writeThriftField(writer, 6, lastId)
        }
        if (scale != null) {
            writer.writeFieldBegin(FieldType.I32, 7, lastId)
            writer.writeI32(scale)
            lastId = 7
        }
        if (precision != null) {
            writer.writeFieldBegin(FieldType.I32, 8, lastId)
            writer.writeI32(precision)
            lastId = 8
        }
        if (fieldId != null) {
            writer.writeFieldBegin(FieldType.I32, 9, lastId)
            writer.writeI32(fieldId)
            lastId = 9
        }
        if (logicalType != null) {
            lastId = logicalType.writeThriftField(writer, 10, lastId)
        }
        writer.writeStructEnd()
    }

    override fun writeThriftField(
        writer: ThriftCompactOutputProtocol,
        fieldId: Short,
        lastFieldId: Short,
    ): Short {
        writer.writeFieldBegin(FieldType.STRUCT, fieldId, lastFieldId)
        writeThrift(writer)
        return fieldId
    }

    public companion object : ReadThrift<SchemaElement> {
        override fun readThrift(prot: ThriftCompactInputProtocol): SchemaElement {
            var type: ParquetType? = null
            var typeLength: Int? = null
            var repetitionType: FieldRepetitionType? = null
            var name: String? = null
            var numChildren: Int? = null
            var convertedType: ConvertedType? = null
            var scale: Int? = null
            var precision: Int? = null
            var fieldId: Int? = null
            var logicalType: LogicalType? = null

            var lastId: Short = 0
            while (true) {
                val f = prot.readFieldBegin(lastId)
                if (f.fieldType == FieldType.STOP) break
                lastId = f.id
                when (f.id.toInt()) {
                    1 -> type = ParquetType.readThrift(prot)
                    2 -> typeLength = prot.readI32()
                    3 -> repetitionType = FieldRepetitionType.readThrift(prot)
                    4 -> name = prot.readString()
                    5 -> numChildren = prot.readI32()
                    6 -> convertedType = ConvertedType.readThrift(prot)
                    7 -> scale = prot.readI32()
                    8 -> precision = prot.readI32()
                    9 -> fieldId = prot.readI32()
                    10 -> logicalType = LogicalType.readThrift(prot)
                    else -> prot.skip(f.fieldType)
                }
            }

            val finalName = name ?: throw generalErr("Field 'name' is required for SchemaElement")
            return SchemaElement(
                type = type,
                typeLength = typeLength,
                repetitionType = repetitionType,
                name = finalName,
                numChildren = numChildren,
                convertedType = convertedType,
                scale = scale,
                precision = precision,
                fieldId = fieldId,
                logicalType = logicalType,
            )
        }
    }
}
