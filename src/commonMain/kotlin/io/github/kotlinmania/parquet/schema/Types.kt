// port-lint: source schema/types.rs
package io.github.kotlinmania.parquet.schema

import io.github.kotlinmania.parquet.ColumnOrder
import io.github.kotlinmania.parquet.ConvertedType
import io.github.kotlinmania.parquet.ElementType
import io.github.kotlinmania.parquet.FieldRepetitionType
import io.github.kotlinmania.parquet.LogicalType
import io.github.kotlinmania.parquet.ParquetType
import io.github.kotlinmania.parquet.SortOrder
import io.github.kotlinmania.parquet.ThriftCompactOutputProtocol
import io.github.kotlinmania.parquet.ThriftSliceInputProtocol
import io.github.kotlinmania.parquet.TimeUnit
import io.github.kotlinmania.parquet.file.metadata.HeapSize
import io.github.kotlinmania.parquet.file.metadata.heapSize
import io.github.kotlinmania.parquet.file.metadata.thrift.SchemaElement
import io.github.kotlinmania.parquet.generalErr
import io.github.kotlinmania.parquet.readThriftVec
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.pow

public typealias SchemaTypePtr = SchemaType
public typealias SchemaDescPtr = SchemaDescriptor
public typealias ColumnDescPtr = ColumnDescriptor

/**
 * Representation of a Parquet schema type.
 *
 * Used to describe primitive leaf fields and structs, including top-level schema.
 * Note that the top-level schema is represented using [SchemaType.GroupType] whose
 * repetition is `null`.
 */
public sealed class SchemaType : HeapSize {
    /**
     * Represents a primitive leaf field.
     */
    public data class PrimitiveType(
        private val basicInfoVal: BasicTypeInfo,
        private val physicalTypeVal: ParquetType,
        public val typeLength: Int,
        private val scaleVal: Int,
        private val precisionVal: Int,
    ) : SchemaType() {
        override fun heapSize(): Long = basicInfoVal.heapSize()

        override fun getBasicInfo(): BasicTypeInfo = basicInfoVal

        override fun getPhysicalType(): ParquetType = physicalTypeVal

        override fun getScale(): Int = scaleVal

        override fun getPrecision(): Int = precisionVal
    }

    /**
     * Represents a group of fields (similar to struct).
     */
    public data class GroupType(
        private val basicInfoVal: BasicTypeInfo,
        private val fieldsVal: List<SchemaType>,
    ) : SchemaType() {
        override fun heapSize(): Long = basicInfoVal.heapSize() + fieldsVal.sumOf { it.heapSize() }

        override fun getBasicInfo(): BasicTypeInfo = basicInfoVal

        override fun getFields(): List<SchemaType> = fieldsVal
    }

    public abstract fun getBasicInfo(): BasicTypeInfo

    /**
     * Returns this type's field name.
     */
    public fun name(): String = getBasicInfo().name()

    /**
     * Gets the fields from this group type. Throws if called on a non-group type.
     */
    public open fun getFields(): List<SchemaType> =
        throw generalErr("Cannot call get_fields() on a non-group type")

    /**
     * Gets physical type of this primitive type. Throws if called on a non-primitive type.
     */
    public open fun getPhysicalType(): ParquetType =
        throw generalErr("Cannot call get_physical_type() on a non-primitive type")

    /**
     * Gets precision of this primitive type. Throws if called on a non-primitive type.
     */
    public open fun getPrecision(): Int =
        throw generalErr("Cannot call get_precision() on non-primitive type")

    /**
     * Gets scale of this primitive type. Throws if called on a non-primitive type.
     */
    public open fun getScale(): Int =
        throw generalErr("Cannot call get_scale() on non-primitive type")

    /**
     * Checks if `subType` schema is part of current schema.
     */
    public fun checkContains(subType: SchemaType): Boolean {
        val basicMatch =
            getBasicInfo().name() == subType.getBasicInfo().name() &&
                (
                    (isSchema() && subType.isSchema()) ||
                        (
                            !isSchema() && !subType.isSchema() &&
                                getBasicInfo().repetition() == subType.getBasicInfo().repetition()
                        )
                )

        return when (this) {
            is PrimitiveType -> {
                if (basicMatch && subType.isPrimitive()) {
                    getPhysicalType() == subType.getPhysicalType()
                } else {
                    false
                }
            }

            is GroupType -> {
                if (basicMatch && subType.isGroup()) {
                    val fieldMap = HashMap<String, SchemaType>()
                    for (field in getFields()) {
                        fieldMap[field.name()] = field
                    }
                    for (field in subType.getFields()) {
                        val matching = fieldMap[field.name()] ?: return false
                        if (!matching.checkContains(field)) {
                            return false
                        }
                    }
                    true
                } else {
                    false
                }
            }
        }
    }

    /**
     * Returns `true` if this type is a primitive type, `false` otherwise.
     */
    public fun isPrimitive(): Boolean = this is PrimitiveType

    /**
     * Returns `true` if this type is a group type, `false` otherwise.
     */
    public fun isGroup(): Boolean = this is GroupType

    /**
     * Returns `true` if this type is the top-level schema type (message type).
     */
    public fun isSchema(): Boolean =
        when (this) {
            is GroupType -> !getBasicInfo().hasRepetition()
            is PrimitiveType -> false
        }

    /**
     * Returns `true` if this type is repeated or optional.
     */
    public fun isOptional(): Boolean =
        getBasicInfo().hasRepetition() && getBasicInfo().repetition() != FieldRepetitionType.REQUIRED

    /**
     * Returns `true` if this type is annotated as a list.
     */
    internal fun isList(): Boolean {
        if (isGroup()) {
            val basic = getBasicInfo()
            val logical = basic.logicalTypeRef()
            if (logical != null) {
                return logical is LogicalType.List
            }
            return basic.convertedType() == ConvertedType.LIST
        }
        return false
    }

    /**
     * Returns `true` if this type is a group with a single child field that is `repeated`.
     */
    internal fun hasSingleRepeatedChild(): Boolean {
        if (isGroup()) {
            val children = getFields()
            return children.size == 1 &&
                children[0].getBasicInfo().hasRepetition() &&
                children[0].getBasicInfo().repetition() == FieldRepetitionType.REPEATED
        }
        return false
    }

    public companion object {
        public fun primitiveTypeBuilder(
            name: String,
            physicalType: ParquetType,
        ): PrimitiveTypeBuilder = PrimitiveTypeBuilder(name, physicalType)

        public fun groupTypeBuilder(name: String): GroupTypeBuilder =
            GroupTypeBuilder(name)
    }
}

/**
 * Basic type info. This contains information such as the name of the type,
 * the repetition level, the logical type and the kind of the type (group, primitive).
 */
public data class BasicTypeInfo(
    private val nameVal: String,
    private val repetitionVal: FieldRepetitionType?,
    private val convertedTypeVal: ConvertedType,
    private val logicalTypeVal: LogicalType?,
    private val idVal: Int?,
    private val sortOrderVal: SortOrder,
) : HeapSize {
    override fun heapSize(): Long = nameVal.heapSize()

    public fun name(): String = nameVal

    public fun hasRepetition(): Boolean = repetitionVal != null

    public fun repetition(): FieldRepetitionType =
        repetitionVal ?: throw generalErr("Repetition is not set for field '$nameVal'")

    public fun convertedType(): ConvertedType = convertedTypeVal

    public fun logicalTypeRef(): LogicalType? = logicalTypeVal

    public fun hasId(): Boolean = idVal != null

    public fun id(): Int = idVal ?: throw generalErr("Id is not set for field '$nameVal'")

    public fun sortOrder(): SortOrder = sortOrderVal
}

/**
 * A builder for primitive types. All attributes are optional
 * except the name and physical type.
 */
public class PrimitiveTypeBuilder(
    private val name: String,
    private val physicalType: ParquetType,
) {
    private var repetition: FieldRepetitionType = FieldRepetitionType.OPTIONAL
    private var convertedType: ConvertedType = ConvertedType.NONE
    private var logicalType: LogicalType? = null
    private var length: Int = -1
    private var precision: Int = -1
    private var scale: Int = -1
    private var id: Int? = null

    public fun withRepetition(repetition: FieldRepetitionType): PrimitiveTypeBuilder {
        this.repetition = repetition
        return this
    }

    public fun withConvertedType(convertedType: ConvertedType): PrimitiveTypeBuilder {
        this.convertedType = convertedType
        return this
    }

    public fun withLogicalType(logicalType: LogicalType?): PrimitiveTypeBuilder {
        this.logicalType = logicalType
        return this
    }

    public fun withLength(length: Int): PrimitiveTypeBuilder {
        this.length = length
        return this
    }

    public fun withPrecision(precision: Int): PrimitiveTypeBuilder {
        this.precision = precision
        return this
    }

    public fun withScale(scale: Int): PrimitiveTypeBuilder {
        this.scale = scale
        return this
    }

    public fun withId(id: Int?): PrimitiveTypeBuilder {
        this.id = id
        return this
    }

    public fun build(): SchemaType.PrimitiveType {
        val sortOrder =
            ColumnOrder
                .columnOrderForType(
                    logicalType,
                    convertedType,
                    physicalType,
                ).sortOrder()

        var currentConverted = convertedType

        if (physicalType == ParquetType.FIXED_LEN_BYTE_ARRAY && length < 0) {
            throw generalErr("Invalid FIXED_LEN_BYTE_ARRAY length: $length for field '$name'")
        }

        val lType = logicalType
        if (lType != null) {
            if (convertedType != ConvertedType.NONE) {
                val expectedConverted = ConvertedType.from(lType)
                if (expectedConverted != convertedType) {
                    throw generalErr(
                        "Logical type $lType is incompatible with converted type $convertedType for field '$name'",
                    )
                }
            } else {
                currentConverted = ConvertedType.from(lType)
            }

            when (lType) {
                is LogicalType.Map, is LogicalType.List, is LogicalType.File -> {
                    throw generalErr("$lType cannot be applied to a primitive type for field '$name'")
                }

                is LogicalType.Enum -> {
                    if (physicalType != ParquetType.BYTE_ARRAY) {
                        throw generalErr("ENUM cannot annotate field '$name' because it is not a BYTE_ARRAY field")
                    }
                }

                is LogicalType.Decimal -> {
                    if (lType.scale != scale) {
                        throw generalErr(
                            "DECIMAL logical type scale ${lType.scale} must match self.scale $scale for field '$name'",
                        )
                    }
                    if (lType.precision != precision) {
                        throw generalErr(
                            "DECIMAL logical type precision ${lType.precision} must match self.precision $precision for field '$name'",
                        )
                    }
                    checkDecimalPrecisionScale()
                }

                is LogicalType.Date -> {
                    if (physicalType != ParquetType.INT32) {
                        throw generalErr("Cannot annotate DATE from $physicalType for field '$name'")
                    }
                }

                is LogicalType.Time -> {
                    if (physicalType == ParquetType.INT32) {
                        if (lType.unit != TimeUnit.MILLIS) {
                            throw generalErr("Cannot annotate TIME(${lType.unit}) from INT32 for field '$name'")
                        }
                    } else if (physicalType == ParquetType.INT64) {
                        if (lType.unit == TimeUnit.MILLIS) {
                            throw generalErr("Cannot use millisecond unit on INT64 type for field '$name'")
                        }
                    } else {
                        throw generalErr("Cannot annotate TIME from $physicalType for field '$name'")
                    }
                }

                is LogicalType.Timestamp -> {
                    if (physicalType != ParquetType.INT64) {
                        throw generalErr("Cannot annotate TIMESTAMP from $physicalType for field '$name'")
                    }
                }

                is LogicalType.Integer -> {
                    if (physicalType == ParquetType.INT32 && lType.bitWidth <= 32) {
                        // valid
                    } else if (physicalType == ParquetType.INT64 && lType.bitWidth == 64.toByte()) {
                        // valid
                    } else {
                        throw generalErr("Cannot annotate INTEGER(${lType.bitWidth}) from $physicalType for field '$name'")
                    }
                }

                is LogicalType.Unknown -> {}

                is LogicalType.String -> {
                    if (physicalType != ParquetType.BYTE_ARRAY) {
                        throw generalErr("STRING cannot annotate field '$name' because it is not a BYTE_ARRAY field")
                    }
                }

                is LogicalType.Json -> {
                    if (physicalType != ParquetType.BYTE_ARRAY) {
                        throw generalErr("JSON cannot annotate field '$name' because it is not a BYTE_ARRAY field")
                    }
                }

                is LogicalType.Bson -> {
                    if (physicalType != ParquetType.BYTE_ARRAY) {
                        throw generalErr("BSON cannot annotate field '$name' because it is not a BYTE_ARRAY field")
                    }
                }

                is LogicalType.Geometry -> {
                    if (physicalType != ParquetType.BYTE_ARRAY) {
                        throw generalErr("GEOMETRY cannot annotate field '$name' because it is not a BYTE_ARRAY field")
                    }
                }

                is LogicalType.Geography -> {
                    if (physicalType != ParquetType.BYTE_ARRAY) {
                        throw generalErr("GEOGRAPHY cannot annotate field '$name' because it is not a BYTE_ARRAY field")
                    }
                }

                is LogicalType.Uuid -> {
                    if (physicalType != ParquetType.FIXED_LEN_BYTE_ARRAY || length != 16) {
                        throw generalErr(
                            "UUID cannot annotate field '$name' because it is not a FIXED_LEN_BYTE_ARRAY(16) field",
                        )
                    }
                }

                is LogicalType.Float16 -> {
                    if (physicalType != ParquetType.FIXED_LEN_BYTE_ARRAY || length != 2) {
                        throw generalErr(
                            "FLOAT16 cannot annotate field '$name' because it is not a FIXED_LEN_BYTE_ARRAY(2) field",
                        )
                    }
                }

                else -> {}
            }
        }

        when (currentConverted) {
            ConvertedType.NONE -> {}

            ConvertedType.UTF8, ConvertedType.BSON, ConvertedType.JSON -> {
                if (physicalType != ParquetType.BYTE_ARRAY) {
                    throw generalErr(
                        "$currentConverted cannot annotate field '$name' because it is not a BYTE_ARRAY field",
                    )
                }
            }

            ConvertedType.DECIMAL -> {
                checkDecimalPrecisionScale()
            }

            ConvertedType.DATE,
            ConvertedType.TIME_MILLIS,
            ConvertedType.UINT_8,
            ConvertedType.UINT_16,
            ConvertedType.UINT_32,
            ConvertedType.INT_8,
            ConvertedType.INT_16,
            ConvertedType.INT_32,
            -> {
                if (physicalType != ParquetType.INT32) {
                    throw generalErr(
                        "$currentConverted cannot annotate field '$name' because it is not a INT32 field",
                    )
                }
            }

            ConvertedType.TIME_MICROS,
            ConvertedType.TIMESTAMP_MILLIS,
            ConvertedType.TIMESTAMP_MICROS,
            ConvertedType.UINT_64,
            ConvertedType.INT_64,
            -> {
                if (physicalType != ParquetType.INT64) {
                    throw generalErr(
                        "$currentConverted cannot annotate field '$name' because it is not a INT64 field",
                    )
                }
            }

            ConvertedType.INTERVAL -> {
                if (physicalType != ParquetType.FIXED_LEN_BYTE_ARRAY || length != 12) {
                    throw generalErr(
                        "INTERVAL cannot annotate field '$name' because it is not a FIXED_LEN_BYTE_ARRAY(12) field",
                    )
                }
            }

            ConvertedType.ENUM -> {
                if (physicalType != ParquetType.BYTE_ARRAY) {
                    throw generalErr(
                        "ENUM cannot annotate field '$name' because it is not a BYTE_ARRAY field",
                    )
                }
            }

            else -> {
                throw generalErr("$currentConverted cannot be applied to primitive field '$name'")
            }
        }

        val basicInfo =
            BasicTypeInfo(
                nameVal = name,
                repetitionVal = repetition,
                convertedTypeVal = currentConverted,
                logicalTypeVal = logicalType,
                idVal = id,
                sortOrderVal = sortOrder,
            )

        return SchemaType.PrimitiveType(
            basicInfoVal = basicInfo,
            physicalTypeVal = physicalType,
            typeLength = length,
            scaleVal = scale,
            precisionVal = precision,
        )
    }

    private fun checkDecimalPrecisionScale() {
        when (physicalType) {
            ParquetType.INT32,
            ParquetType.INT64,
            ParquetType.BYTE_ARRAY,
            ParquetType.FIXED_LEN_BYTE_ARRAY,
            -> {}

            else -> {
                throw generalErr(
                    "DECIMAL can only annotate INT32, INT64, BYTE_ARRAY and FIXED_LEN_BYTE_ARRAY",
                )
            }
        }

        if (precision < 1) {
            throw generalErr("Invalid DECIMAL precision: $precision")
        }
        if (scale < 0) {
            throw generalErr("Invalid DECIMAL scale: $scale")
        }
        if (scale > precision) {
            throw generalErr("Invalid DECIMAL: scale ($scale) cannot be greater than precision ($precision)")
        }

        when (physicalType) {
            ParquetType.INT32 -> {
                if (precision > 9) {
                    throw generalErr("Cannot represent INT32 as DECIMAL with precision $precision")
                }
            }

            ParquetType.INT64 -> {
                if (precision > 18) {
                    throw generalErr("Cannot represent INT64 as DECIMAL with precision $precision")
                }
            }

            ParquetType.FIXED_LEN_BYTE_ARRAY -> {
                val bitLen = length * 8
                val maxPrecision = floor(log10(2.0.pow(bitLen - 1) - 1.0)).toInt()
                if (precision > maxPrecision) {
                    throw generalErr(
                        "Cannot represent FIXED_LEN_BYTE_ARRAY as DECIMAL with length $length and " +
                            "precision $precision. The max precision can only be $maxPrecision",
                    )
                }
            }

            else -> {}
        }
    }
}

/**
 * A builder for group types. All attributes are optional except the name.
 */
public class GroupTypeBuilder(
    private val name: String,
) {
    private var repetition: FieldRepetitionType? = null
    private var convertedType: ConvertedType = ConvertedType.NONE
    private var logicalType: LogicalType? = null
    private var fields: List<SchemaType> = emptyList()
    private var id: Int? = null

    public fun withRepetition(repetition: FieldRepetitionType): GroupTypeBuilder {
        this.repetition = repetition
        return this
    }

    public fun withConvertedType(convertedType: ConvertedType): GroupTypeBuilder {
        this.convertedType = convertedType
        return this
    }

    public fun withLogicalType(logicalType: LogicalType?): GroupTypeBuilder {
        this.logicalType = logicalType
        return this
    }

    public fun withFields(fields: List<SchemaType>): GroupTypeBuilder {
        this.fields = fields
        return this
    }

    public fun withId(id: Int?): GroupTypeBuilder {
        this.id = id
        return this
    }

    public fun build(): SchemaType.GroupType {
        if (logicalType is LogicalType.File) {
            validateFileTypeFields(name, fields)
        }

        var currentConverted = convertedType
        val logical = logicalType
        if (logical != null && convertedType == ConvertedType.NONE) {
            currentConverted = ConvertedType.from(logical)
        }

        val basicInfo =
            BasicTypeInfo(
                nameVal = name,
                repetitionVal = repetition,
                convertedTypeVal = currentConverted,
                logicalTypeVal = logicalType,
                idVal = id,
                sortOrderVal = SortOrder.UNDEFINED,
            )

        return SchemaType.GroupType(
            basicInfoVal = basicInfo,
            fieldsVal = fields,
        )
    }
}

private fun validateFileTypeFields(name: String, fields: List<SchemaType>) {
    data class ValidField(
        val name: String,
        val physical: ParquetType,
        val logical: LogicalType?,
    )

    val validFields =
        listOf(
            ValidField("uri", ParquetType.BYTE_ARRAY, LogicalType.String),
            ValidField("offset", ParquetType.INT64, null),
            ValidField("size", ParquetType.INT64, null),
            ValidField("content_type", ParquetType.BYTE_ARRAY, LogicalType.String),
            ValidField("checksum", ParquetType.BYTE_ARRAY, LogicalType.String),
            ValidField("inline", ParquetType.BYTE_ARRAY, null),
        )

    for (field in fields) {
        val fieldName = field.getBasicInfo().name()
        val expected =
            validFields.firstOrNull { it.name == fieldName }
                ?: throw generalErr(
                    "FILE type group '$name' contains unrecognized field '$fieldName'. " +
                        "Valid fields are: uri, offset, size, content_type, checksum, inline",
                )

        val isOptional =
            field.getBasicInfo().hasRepetition() &&
                field.getBasicInfo().repetition() == FieldRepetitionType.OPTIONAL
        if (!isOptional) {
            throw generalErr("FILE type field '$fieldName' must be OPTIONAL in group '$name'")
        }

        if (field.isGroup()) {
            throw generalErr("FILE type field '$fieldName' in group '$name' must be a primitive type")
        }
        if (field.getPhysicalType() != expected.physical) {
            throw generalErr(
                "FILE type field '$fieldName' in group '$name' must have physical type ${expected.physical}",
            )
        }
        if (field.getBasicInfo().logicalTypeRef() != expected.logical) {
            throw generalErr(
                "FILE type field '$fieldName' in group '$name' must have logical type ${expected.logical}",
            )
        }
    }
}

/**
 * Represents the location of a column in a Parquet schema.
 */
public data class ColumnPath(
    private val partsVal: List<String>,
) : HeapSize {
    override fun heapSize(): Long = partsVal.sumOf { it.heapSize() }

    public fun string(): String = partsVal.joinToString(".")

    public fun append(tail: List<String>): ColumnPath =
        ColumnPath(partsVal + tail)

    public fun parts(): List<String> = partsVal

    override fun toString(): String = "\"${string()}\""

    public companion object {
        public fun new(parts: List<String>): ColumnPath = ColumnPath(parts)

        public fun from(singlePath: String): ColumnPath = ColumnPath(listOf(singlePath))

        public fun from(parts: List<String>): ColumnPath = ColumnPath(parts)
    }
}

/**
 * Physical type for leaf-level primitive columns.
 */
public class ColumnDescriptor(
    private val primitiveType: SchemaType,
    private val maxDefLevel: Short,
    private val maxRepLevel: Short,
    private val repeatedAncestorDefLevel: Short,
    private val path: ColumnPath,
) : HeapSize {
    public constructor(
        primitiveType: SchemaType,
        maxDefLevel: Short,
        maxRepLevel: Short,
        path: ColumnPath,
    ) : this(primitiveType, maxDefLevel, maxRepLevel, 0, path)

    override fun heapSize(): Long = path.heapSize()

    public fun maxDefLevel(): Short = maxDefLevel

    public fun maxRepLevel(): Short = maxRepLevel

    public fun repeatedAncestorDefLevel(): Short = repeatedAncestorDefLevel

    public fun path(): ColumnPath = path

    public fun selfType(): SchemaType = primitiveType

    public fun selfTypePtr(): SchemaType = primitiveType

    public fun getBasicInfo(): BasicTypeInfo = primitiveType.getBasicInfo()

    public fun name(): String = primitiveType.name()

    public fun convertedType(): ConvertedType = primitiveType.getBasicInfo().convertedType()

    public fun logicalTypeRef(): LogicalType? = primitiveType.getBasicInfo().logicalTypeRef()

    public fun physicalType(): ParquetType = primitiveType.getPhysicalType()

    public fun typeLength(): Int =
        when (primitiveType) {
            is SchemaType.PrimitiveType -> primitiveType.typeLength
            is SchemaType.GroupType -> throw generalErr("Expected primitive type!")
        }

    public fun typePrecision(): Int = primitiveType.getPrecision()

    public fun typeScale(): Int = primitiveType.getScale()

    public fun physical_type(): ParquetType = physicalType()

    public fun converted_type(): ConvertedType = convertedType()

    public fun logical_type_ref(): LogicalType? = logicalTypeRef()

    public fun type_precision(): Int = typePrecision()

    public fun type_scale(): Int = typeScale()

    public fun sortOrder(): SortOrder = primitiveType.getBasicInfo().sortOrder()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ColumnDescriptor) return false
        return primitiveType == other.primitiveType &&
            maxDefLevel == other.maxDefLevel &&
            maxRepLevel == other.maxRepLevel &&
            repeatedAncestorDefLevel == other.repeatedAncestorDefLevel &&
            path == other.path
    }

    override fun hashCode(): Int {
        var result = primitiveType.hashCode()
        result = 31 * result + maxDefLevel.hashCode()
        result = 31 * result + maxRepLevel.hashCode()
        result = 31 * result + repeatedAncestorDefLevel.hashCode()
        result = 31 * result + path.hashCode()
        return result
    }

    override fun toString(): String =
        "ColumnDescriptor(primitiveType=$primitiveType, maxDefLevel=$maxDefLevel, " +
            "maxRepLevel=$maxRepLevel, repeatedAncestorDefLevel=$repeatedAncestorDefLevel, path=$path)"

    public companion object {
        public fun new(
            primitiveType: SchemaType,
            maxDefLevel: Short,
            maxRepLevel: Short,
            path: ColumnPath,
        ): ColumnDescriptor = ColumnDescriptor(primitiveType, maxDefLevel, maxRepLevel, 0, path)

        public fun newWithRepeatedAncestor(
            primitiveType: SchemaType,
            maxDefLevel: Short,
            maxRepLevel: Short,
            path: ColumnPath,
            repeatedAncestorDefLevel: Short,
        ): ColumnDescriptor =
            ColumnDescriptor(primitiveType, maxDefLevel, maxRepLevel, repeatedAncestorDefLevel, path)
    }
}

/**
 * Schema of a Parquet file.
 */
public class SchemaDescriptor(
    private val schema: SchemaType,
    private val leaves: List<ColumnDescriptor>,
    private val leafToBase: List<Int>,
) : HeapSize {
    override fun heapSize(): Long =
        schema.heapSize() + leaves.sumOf { it.heapSize() } + leafToBase.size * 8L

    public fun column(i: Int): ColumnDescriptor {
        require(i in leaves.indices) { "Index out of bound: $i not in [0, ${leaves.size})" }
        return leaves[i]
    }

    public fun columns(): List<ColumnDescriptor> = leaves

    public fun numColumns(): Int = leaves.size

    public fun getColumnRoot(i: Int): SchemaType =
        schema.getFields()[getColumnRootIdx(i)]

    public fun getColumnRootPtr(i: Int): SchemaType = getColumnRoot(i)

    public fun getColumnRootIdx(leaf: Int): Int {
        require(leaf in leaves.indices) { "Index out of bound: $leaf not in [0, ${leaves.size})" }
        return leafToBase[leaf]
    }

    public fun rootSchema(): SchemaType = schema

    public fun rootSchemaPtr(): SchemaType = schema

    public fun name(): String = schema.name()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SchemaDescriptor) return false
        return schema == other.schema
    }

    override fun hashCode(): Int = schema.hashCode()

    override fun toString(): String = "SchemaDescriptor(schema=$schema)"

    public companion object {
        public fun new(tp: SchemaType): SchemaDescriptor {
            if (!tp.isGroup()) {
                throw generalErr("SchemaDescriptor should take a GroupType")
            }
            val nLeaves = numLeaves(tp)
            val leaves = ArrayList<ColumnDescriptor>(nLeaves)
            val leafToBase = ArrayList<Int>(nLeaves)
            val path = ArrayList<String>(16)

            for ((rootIdx, f) in tp.getFields().withIndex()) {
                path.clear()
                buildTree(
                    tp = f,
                    rootIdx = rootIdx,
                    maxRepLevel = 0,
                    maxDefLevel = 0,
                    repeatedAncestorDefLevel = 0,
                    leaves = leaves,
                    leafToBase = leafToBase,
                    pathSoFar = path,
                )
            }

            return SchemaDescriptor(tp, leaves, leafToBase)
        }
    }
}

public fun numNodes(tp: SchemaType): Int {
    if (!tp.isGroup()) {
        throw generalErr("Root schema must be Group type")
    }
    var nNodes = 1
    for (f in tp.getFields()) {
        nNodes = countNodes(f, nNodes)
    }
    return nNodes
}

internal fun countNodes(tp: SchemaType, current: Int): Int {
    var count = current + 1
    if (tp is SchemaType.GroupType) {
        for (f in tp.getFields()) {
            count = countNodes(f, count)
        }
    }
    return count
}

internal fun numLeaves(tp: SchemaType): Int {
    if (!tp.isGroup()) {
        throw generalErr("Root schema must be Group type")
    }
    var nLeaves = 0
    for (f in tp.getFields()) {
        nLeaves = countLeaves(f, nLeaves)
    }
    return nLeaves
}

internal fun countLeaves(tp: SchemaType, current: Int): Int =
    when (tp) {
        is SchemaType.PrimitiveType -> {
            current + 1
        }

        is SchemaType.GroupType -> {
            var count = current
            for (f in tp.getFields()) {
                count = countLeaves(f, count)
            }
            count
        }
    }

private fun buildTree(
    tp: SchemaType,
    rootIdx: Int,
    maxRepLevel: Short,
    maxDefLevel: Short,
    repeatedAncestorDefLevel: Short,
    leaves: MutableList<ColumnDescriptor>,
    leafToBase: MutableList<Int>,
    pathSoFar: MutableList<String>,
) {
    if (!tp.getBasicInfo().hasRepetition()) {
        throw generalErr("Type ${tp.name()} must have repetition")
    }

    pathSoFar.add(tp.name())
    var defLevel = maxDefLevel
    var repLevel = maxRepLevel
    var ancestorDef = repeatedAncestorDefLevel

    when (tp.getBasicInfo().repetition()) {
        FieldRepetitionType.OPTIONAL -> {
            defLevel = (defLevel + 1).toShort()
        }

        FieldRepetitionType.REPEATED -> {
            defLevel = (defLevel + 1).toShort()
            repLevel = (repLevel + 1).toShort()
            ancestorDef = defLevel
        }

        FieldRepetitionType.REQUIRED -> {}
    }

    when (tp) {
        is SchemaType.PrimitiveType -> {
            val desc =
                ColumnDescriptor.newWithRepeatedAncestor(
                    primitiveType = tp,
                    maxDefLevel = defLevel,
                    maxRepLevel = repLevel,
                    path = ColumnPath.new(ArrayList(pathSoFar)),
                    repeatedAncestorDefLevel = ancestorDef,
                )
            leaves.add(desc)
            leafToBase.add(rootIdx)
        }

        is SchemaType.GroupType -> {
            for (f in tp.getFields()) {
                buildTree(
                    tp = f,
                    rootIdx = rootIdx,
                    maxRepLevel = repLevel,
                    maxDefLevel = defLevel,
                    repeatedAncestorDefLevel = ancestorDef,
                    leaves = leaves,
                    leafToBase = leafToBase,
                    pathSoFar = pathSoFar,
                )
                pathSoFar.removeAt(pathSoFar.size - 1)
            }
        }
    }
}

internal fun checkLogicalType(logicalType: LogicalType?) {
    if (logicalType is LogicalType.Integer) {
        val bw = logicalType.bitWidth.toInt()
        if (bw != 8 && bw != 16 && bw != 32 && bw != 64) {
            throw generalErr("Bit width must be 8, 16, 32, or 64 for Integer logical type")
        }
    }
}

public fun parquetSchemaFromArray(elements: List<SchemaElement>): SchemaType {
    var index = 0
    val numElements = elements.size
    val schemaNodes = ArrayList<SchemaType>(1)
    val iterator = elements.iterator()

    while (index < numElements) {
        val (nextIndex, node) = schemaFromArrayHelper(iterator, numElements, index)
        index = nextIndex
        schemaNodes.add(node)
    }

    if (schemaNodes.size != 1) {
        throw generalErr("Expected exactly one root node, but found ${schemaNodes.size}")
    }

    val root = schemaNodes[0]
    if (!root.isGroup()) {
        throw generalErr("Expected root node to be a group type")
    }

    return root
}

private fun schemaFromArrayHelper(
    elements: Iterator<SchemaElement>,
    numElements: Int,
    index: Int,
): Pair<Int, SchemaType> {
    val isRootNode = index == 0
    if (index >= numElements) {
        throw generalErr("Index out of bound, index = $index, len = $numElements")
    }
    val element =
        if (elements.hasNext()) {
            elements.next()
        } else {
            throw generalErr("schema vector should not be empty")
        }

    if (isRootNode && (element.numChildren == null || element.numChildren == 0)) {
        val builder = SchemaType.groupTypeBuilder(element.name)
        return Pair(index + 1, builder.build())
    }

    val convertedType = element.convertedType ?: ConvertedType.NONE
    val logicalType = element.logicalType
    checkLogicalType(logicalType)

    val fieldId = element.fieldId
    val numChildren = element.numChildren

    return if (numChildren == null || numChildren == 0) {
        val repetition =
            element.repetitionType
                ?: throw generalErr("Repetition level must be defined for a primitive type")

        val pType = element.type
        if (pType != null) {
            val length = element.typeLength ?: -1
            val scale = element.scale ?: -1
            val precision = element.precision ?: -1
            val builder =
                SchemaType
                    .primitiveTypeBuilder(element.name, pType)
                    .withRepetition(repetition)
                    .withConvertedType(convertedType)
                    .withLogicalType(logicalType)
                    .withLength(length)
                    .withPrecision(precision)
                    .withScale(scale)
                    .withId(fieldId)
            Pair(index + 1, builder.build())
        } else {
            var builder =
                SchemaType
                    .groupTypeBuilder(element.name)
                    .withConvertedType(convertedType)
                    .withLogicalType(logicalType)
                    .withId(fieldId)
            if (!isRootNode) {
                builder = builder.withRepetition(repetition)
            }
            Pair(index + 1, builder.build())
        }
    } else {
        if (numChildren < 0) {
            throw generalErr("Integer overflow: negative num_children $numChildren")
        }
        val fields = ArrayList<SchemaType>(numChildren)
        var nextIndex = index + 1
        repeat(numChildren) {
            val (childIndex, childNode) = schemaFromArrayHelper(elements, numElements, nextIndex)
            nextIndex = childIndex
            fields.add(childNode)
        }

        var builder =
            SchemaType
                .groupTypeBuilder(element.name)
                .withConvertedType(convertedType)
                .withLogicalType(logicalType)
                .withFields(fields)
                .withId(fieldId)

        if (!isRootNode) {
            val rep =
                element.repetitionType
                    ?: throw generalErr("Repetition level must be defined for non-root types")
            builder = builder.withRepetition(rep)
        }
        Pair(nextIndex, builder.build())
    }
}

public fun writeSchema(schema: SchemaType, writer: ThriftCompactOutputProtocol) {
    if (!schema.isGroup()) {
        throw generalErr("Root schema must be Group type")
    }
    writeSchemaHelper(schema, writer)
}

public fun writeSchemaHelper(node: SchemaType, writer: ThriftCompactOutputProtocol) {
    when (node) {
        is SchemaType.PrimitiveType -> {
            val basic = node.getBasicInfo()
            val element =
                SchemaElement(
                    type = node.getPhysicalType(),
                    typeLength = if (node.typeLength >= 0) node.typeLength else null,
                    repetitionType = basic.repetition(),
                    name = basic.name(),
                    numChildren = null,
                    convertedType = if (basic.convertedType() != ConvertedType.NONE) basic.convertedType() else null,
                    scale = if (node.getScale() >= 0) node.getScale() else null,
                    precision = if (node.getPrecision() >= 0) node.getPrecision() else null,
                    fieldId = if (basic.hasId()) basic.id() else null,
                    logicalType = basic.logicalTypeRef(),
                )
            element.writeThrift(writer)
        }

        is SchemaType.GroupType -> {
            val basic = node.getBasicInfo()
            val repetition = if (basic.hasRepetition()) basic.repetition() else null
            val fields = node.getFields()
            val element =
                SchemaElement(
                    type = null,
                    typeLength = null,
                    repetitionType = repetition,
                    name = basic.name(),
                    numChildren = fields.size,
                    convertedType = if (basic.convertedType() != ConvertedType.NONE) basic.convertedType() else null,
                    scale = null,
                    precision = null,
                    fieldId = if (basic.hasId()) basic.id() else null,
                    logicalType = basic.logicalTypeRef(),
                )
            element.writeThrift(writer)
            for (field in fields) {
                writeSchemaHelper(field, writer)
            }
        }
    }
}

public fun schemaToBuf(schema: SchemaType): kotlin.ByteArray {
    val totalNodes = numNodes(schema)
    val writer = ThriftCompactOutputProtocol()
    writer.writeListBegin(ElementType.STRUCT, totalNodes)
    writeSchema(schema, writer)
    return writer.toByteArray()
}

public fun bufToSchemaList(buf: kotlin.ByteArray): List<SchemaElement> {
    val prot = ThriftSliceInputProtocol(buf)
    return readThriftVec(prot, SchemaElement, ElementType.STRUCT)
}

public fun roundtripSchema(schema: SchemaType): SchemaType {
    val buf = schemaToBuf(schema)
    val list = bufToSchemaList(buf)
    return parquetSchemaFromArray(list)
}
