// port-lint: tests schema/types.rs
package io.github.kotlinmania.parquet.schema

import io.github.kotlinmania.parquet.ConvertedType
import io.github.kotlinmania.parquet.FieldRepetitionType
import io.github.kotlinmania.parquet.LogicalType
import io.github.kotlinmania.parquet.ParquetError
import io.github.kotlinmania.parquet.ParquetType
import io.github.kotlinmania.parquet.file.metadata.thrift.SchemaElement
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TypesTest {
    @Test
    fun testPrimitiveType() {
        val tp =
            SchemaType
                .primitiveTypeBuilder("foo", ParquetType.INT32)
                .withLogicalType(LogicalType.integer(32, true))
                .withId(0)
                .build()

        assertTrue(tp.isPrimitive())
        assertFalse(tp.isGroup())
        val basicInfo = tp.getBasicInfo()
        assertEquals(FieldRepetitionType.OPTIONAL, basicInfo.repetition())
        assertEquals(LogicalType.integer(32, true), basicInfo.logicalTypeRef())
        assertEquals(ConvertedType.INT_32, basicInfo.convertedType())
        assertEquals(0, basicInfo.id())
        assertEquals(ParquetType.INT32, tp.getPhysicalType())

        // Test illegal inputs with logical type
        val err1 =
            assertFailsWith<ParquetError> {
                SchemaType
                    .primitiveTypeBuilder("foo", ParquetType.INT64)
                    .withRepetition(FieldRepetitionType.REPEATED)
                    .withLogicalType(LogicalType.integer(8, true))
                    .build()
            }
        assertTrue(err1.message!!.contains("Cannot annotate INTEGER(8) from INT64 for field 'foo'"))

        // Test illegal inputs with converted type
        val err2 =
            assertFailsWith<ParquetError> {
                SchemaType
                    .primitiveTypeBuilder("foo", ParquetType.INT64)
                    .withRepetition(FieldRepetitionType.REPEATED)
                    .withConvertedType(ConvertedType.BSON)
                    .build()
            }
        assertTrue(err2.message!!.contains("BSON cannot annotate field 'foo' because it is not a BYTE_ARRAY field"))

        val err3 =
            assertFailsWith<ParquetError> {
                SchemaType
                    .primitiveTypeBuilder("foo", ParquetType.INT96)
                    .withRepetition(FieldRepetitionType.REQUIRED)
                    .withConvertedType(ConvertedType.DECIMAL)
                    .withPrecision(-1)
                    .withScale(-1)
                    .build()
            }
        assertTrue(
            err3.message!!.contains(
                "DECIMAL can only annotate INT32, INT64, BYTE_ARRAY and FIXED_LEN_BYTE_ARRAY",
            ),
        )

        val err4 =
            assertFailsWith<ParquetError> {
                SchemaType
                    .primitiveTypeBuilder("foo", ParquetType.BYTE_ARRAY)
                    .withRepetition(FieldRepetitionType.REQUIRED)
                    .withLogicalType(LogicalType.decimal(32, 12))
                    .withPrecision(-1)
                    .withScale(-1)
                    .build()
            }
        assertTrue(
            err4.message!!.contains(
                "DECIMAL logical type scale 32 must match self.scale -1 for field 'foo'",
            ),
        )

        val err5 =
            assertFailsWith<ParquetError> {
                SchemaType
                    .primitiveTypeBuilder("foo", ParquetType.BYTE_ARRAY)
                    .withRepetition(FieldRepetitionType.REQUIRED)
                    .withConvertedType(ConvertedType.DECIMAL)
                    .withPrecision(-1)
                    .withScale(-1)
                    .build()
            }
        assertTrue(err5.message!!.contains("Invalid DECIMAL precision: -1"))

        val err6 =
            assertFailsWith<ParquetError> {
                SchemaType
                    .primitiveTypeBuilder("foo", ParquetType.BYTE_ARRAY)
                    .withRepetition(FieldRepetitionType.REQUIRED)
                    .withConvertedType(ConvertedType.DECIMAL)
                    .withPrecision(0)
                    .withScale(-1)
                    .build()
            }
        assertTrue(err6.message!!.contains("Invalid DECIMAL precision: 0"))

        val err7 =
            assertFailsWith<ParquetError> {
                SchemaType
                    .primitiveTypeBuilder("foo", ParquetType.BYTE_ARRAY)
                    .withRepetition(FieldRepetitionType.REQUIRED)
                    .withConvertedType(ConvertedType.DECIMAL)
                    .withPrecision(1)
                    .withScale(-1)
                    .build()
            }
        assertTrue(err7.message!!.contains("Invalid DECIMAL scale: -1"))

        val err8 =
            assertFailsWith<ParquetError> {
                SchemaType
                    .primitiveTypeBuilder("foo", ParquetType.BYTE_ARRAY)
                    .withRepetition(FieldRepetitionType.REQUIRED)
                    .withConvertedType(ConvertedType.DECIMAL)
                    .withPrecision(1)
                    .withScale(2)
                    .build()
            }
        assertTrue(
            err8.message!!.contains(
                "Invalid DECIMAL: scale (2) cannot be greater than precision (1)",
            ),
        )

        // It is OK if precision == scale
        val okDecimal =
            SchemaType
                .primitiveTypeBuilder("foo", ParquetType.BYTE_ARRAY)
                .withRepetition(FieldRepetitionType.REQUIRED)
                .withConvertedType(ConvertedType.DECIMAL)
                .withPrecision(1)
                .withScale(1)
                .build()
        assertEquals(1, okDecimal.getPrecision())
        assertEquals(1, okDecimal.getScale())

        val err9 =
            assertFailsWith<ParquetError> {
                SchemaType
                    .primitiveTypeBuilder("foo", ParquetType.INT32)
                    .withRepetition(FieldRepetitionType.REQUIRED)
                    .withConvertedType(ConvertedType.DECIMAL)
                    .withPrecision(18)
                    .withScale(2)
                    .build()
            }
        assertTrue(
            err9.message!!.contains("Cannot represent INT32 as DECIMAL with precision 18"),
        )

        val err10 =
            assertFailsWith<ParquetError> {
                SchemaType
                    .primitiveTypeBuilder("foo", ParquetType.INT64)
                    .withRepetition(FieldRepetitionType.REQUIRED)
                    .withConvertedType(ConvertedType.DECIMAL)
                    .withPrecision(32)
                    .withScale(2)
                    .build()
            }
        assertTrue(
            err10.message!!.contains("Cannot represent INT64 as DECIMAL with precision 32"),
        )

        val err11 =
            assertFailsWith<ParquetError> {
                SchemaType
                    .primitiveTypeBuilder("foo", ParquetType.FIXED_LEN_BYTE_ARRAY)
                    .withRepetition(FieldRepetitionType.REQUIRED)
                    .withConvertedType(ConvertedType.DECIMAL)
                    .withLength(5)
                    .withPrecision(12)
                    .withScale(2)
                    .build()
            }
        assertTrue(
            err11.message!!.contains(
                "Cannot represent FIXED_LEN_BYTE_ARRAY as DECIMAL with length 5 and precision 12. The max precision can only be 11",
            ),
        )
    }

    @Test
    fun testGroupType() {
        val f1 =
            SchemaType
                .primitiveTypeBuilder("f1", ParquetType.INT32)
                .withConvertedType(ConvertedType.INT_32)
                .withId(0)
                .build()
        val f2 =
            SchemaType
                .primitiveTypeBuilder("f2", ParquetType.BYTE_ARRAY)
                .withConvertedType(ConvertedType.UTF8)
                .withId(1)
                .build()

        val fields = listOf(f1, f2)
        val tp =
            SchemaType
                .groupTypeBuilder("foo")
                .withRepetition(FieldRepetitionType.REPEATED)
                .withLogicalType(LogicalType.List)
                .withFields(fields)
                .withId(1)
                .build()

        val basicInfo = tp.getBasicInfo()
        assertTrue(tp.isGroup())
        assertFalse(tp.isPrimitive())
        assertEquals(FieldRepetitionType.REPEATED, basicInfo.repetition())
        assertEquals(LogicalType.List, basicInfo.logicalTypeRef())
        assertEquals(ConvertedType.LIST, basicInfo.convertedType())
        assertEquals(1, basicInfo.id())
        assertEquals(2, tp.getFields().size)
        assertEquals("f1", tp.getFields()[0].name())
        assertEquals("f2", tp.getFields()[1].name())
    }

    @Test
    fun testColumnDescriptor() {
        testColumnDescriptorHelper()
    }

    private fun testColumnDescriptorHelper() {
        val tp =
            SchemaType
                .primitiveTypeBuilder("name", ParquetType.BYTE_ARRAY)
                .withConvertedType(ConvertedType.UTF8)
                .build()

        val descr = ColumnDescriptor.new(tp, 4, 1, ColumnPath.from("name"))
        assertEquals(ColumnPath.from("name"), descr.path())
        assertEquals(ConvertedType.UTF8, descr.convertedType())
        assertEquals(ParquetType.BYTE_ARRAY, descr.physicalType())
        assertEquals(4.toShort(), descr.maxDefLevel())
        assertEquals(1.toShort(), descr.maxRepLevel())
        assertEquals("name", descr.name())
        assertEquals(-1, descr.typeLength())
        assertEquals(-1, descr.typePrecision())
        assertEquals(-1, descr.typeScale())
    }

    @Test
    fun testSchemaDescriptor() {
        testSchemaDescriptorHelper()
    }

    private fun testSchemaDescriptorHelper() {
        val fields = ArrayList<SchemaType>()

        val inta =
            SchemaType
                .primitiveTypeBuilder("a", ParquetType.INT32)
                .withRepetition(FieldRepetitionType.REQUIRED)
                .withConvertedType(ConvertedType.INT_32)
                .build()
        fields.add(inta)

        val intb =
            SchemaType
                .primitiveTypeBuilder("b", ParquetType.INT64)
                .withConvertedType(ConvertedType.INT_64)
                .build()
        fields.add(intb)

        val intc =
            SchemaType
                .primitiveTypeBuilder("c", ParquetType.BYTE_ARRAY)
                .withRepetition(FieldRepetitionType.REPEATED)
                .withConvertedType(ConvertedType.UTF8)
                .build()
        fields.add(intc)

        // 3-level list encoding
        val item1 =
            SchemaType
                .primitiveTypeBuilder("item1", ParquetType.INT64)
                .withRepetition(FieldRepetitionType.REQUIRED)
                .withConvertedType(ConvertedType.INT_64)
                .build()
        val item2 =
            SchemaType.primitiveTypeBuilder("item2", ParquetType.BOOLEAN).build()
        val item3 =
            SchemaType
                .primitiveTypeBuilder("item3", ParquetType.INT32)
                .withRepetition(FieldRepetitionType.REPEATED)
                .withConvertedType(ConvertedType.INT_32)
                .build()
        val list =
            SchemaType
                .groupTypeBuilder("records")
                .withRepetition(FieldRepetitionType.REPEATED)
                .withConvertedType(ConvertedType.LIST)
                .withFields(listOf(item1, item2, item3))
                .build()
        val bag =
            SchemaType
                .groupTypeBuilder("bag")
                .withRepetition(FieldRepetitionType.OPTIONAL)
                .withFields(listOf(list))
                .build()
        fields.add(bag)

        val schema =
            SchemaType
                .groupTypeBuilder("schema")
                .withRepetition(FieldRepetitionType.REPEATED)
                .withFields(fields)
                .build()
        val descr = SchemaDescriptor.new(schema)

        val nleaves = 6
        assertEquals(nleaves, descr.numColumns())

        val exMaxDefLevels = listOf<Short>(0, 1, 1, 2, 3, 3)
        val exMaxRepLevels = listOf<Short>(0, 0, 1, 1, 1, 2)

        for (i in 0 until nleaves) {
            val col = descr.column(i)
            assertEquals(exMaxDefLevels[i], col.maxDefLevel())
            assertEquals(exMaxRepLevels[i], col.maxRepLevel())
        }

        assertEquals("a", descr.column(0).path().string())
        assertEquals("b", descr.column(1).path().string())
        assertEquals("c", descr.column(2).path().string())
        assertEquals("bag.records.item1", descr.column(3).path().string())
        assertEquals("bag.records.item2", descr.column(4).path().string())
        assertEquals("bag.records.item3", descr.column(5).path().string())

        assertEquals("a", descr.getColumnRoot(0).name())
        assertEquals("bag", descr.getColumnRoot(3).name())
        assertEquals("bag", descr.getColumnRoot(4).name())
        assertEquals("bag", descr.getColumnRoot(5).name())
    }

    @Test
    fun testGetPhysicalTypePanic() {
        val list =
            SchemaType
                .groupTypeBuilder("records")
                .withRepetition(FieldRepetitionType.REPEATED)
                .build()
        assertFailsWith<ParquetError> {
            list.getPhysicalType()
        }
    }

    @Test
    fun testGetPhysicalTypePrimitive() {
        val f1 = SchemaType.primitiveTypeBuilder("f", ParquetType.INT64).build()
        assertEquals(ParquetType.INT64, f1.getPhysicalType())

        val f2 = SchemaType.primitiveTypeBuilder("f", ParquetType.BYTE_ARRAY).build()
        assertEquals(ParquetType.BYTE_ARRAY, f2.getPhysicalType())
    }

    @Test
    fun testCheckContainsPrimitivePrimitive() {
        val f1 = SchemaType.primitiveTypeBuilder("f", ParquetType.INT32).build()
        val f2 = SchemaType.primitiveTypeBuilder("f", ParquetType.INT32).build()
        assertTrue(f1.checkContains(f2))

        val f3 =
            SchemaType
                .primitiveTypeBuilder("f", ParquetType.INT32)
                .withConvertedType(ConvertedType.UINT_8)
                .build()
        val f4 =
            SchemaType
                .primitiveTypeBuilder("f", ParquetType.INT32)
                .withConvertedType(ConvertedType.UINT_16)
                .build()
        assertTrue(f3.checkContains(f4))

        val f5 = SchemaType.primitiveTypeBuilder("f1", ParquetType.INT32).build()
        val f6 = SchemaType.primitiveTypeBuilder("f2", ParquetType.INT32).build()
        assertFalse(f5.checkContains(f6))

        val f7 = SchemaType.primitiveTypeBuilder("f", ParquetType.INT32).build()
        val f8 = SchemaType.primitiveTypeBuilder("f", ParquetType.INT64).build()
        assertFalse(f7.checkContains(f8))

        val f9 =
            SchemaType
                .primitiveTypeBuilder("f", ParquetType.INT32)
                .withRepetition(FieldRepetitionType.REQUIRED)
                .build()
        val f10 =
            SchemaType
                .primitiveTypeBuilder("f", ParquetType.INT32)
                .withRepetition(FieldRepetitionType.OPTIONAL)
                .build()
        assertFalse(f9.checkContains(f10))
    }

    private fun testNewGroupType(
        name: String,
        repetition: FieldRepetitionType,
        types: List<SchemaType>,
    ): SchemaType =
        SchemaType
            .groupTypeBuilder(name)
            .withRepetition(repetition)
            .withFields(types)
            .build()

    @Test
    fun testCheckContainsGroupGroup() {
        val f1 = SchemaType.groupTypeBuilder("f").build()
        val f2 = SchemaType.groupTypeBuilder("f").build()
        assertTrue(f1.checkContains(f2))
        assertFalse(f1.isOptional())

        val g1 =
            testNewGroupType(
                "f",
                FieldRepetitionType.REPEATED,
                listOf(
                    SchemaType.primitiveTypeBuilder("f1", ParquetType.INT32).build(),
                    SchemaType.primitiveTypeBuilder("f2", ParquetType.INT64).build(),
                ),
            )
        val g2 =
            testNewGroupType(
                "f",
                FieldRepetitionType.REPEATED,
                listOf(
                    SchemaType.primitiveTypeBuilder("f1", ParquetType.INT32).build(),
                    SchemaType.primitiveTypeBuilder("f2", ParquetType.INT64).build(),
                ),
            )
        assertTrue(g1.checkContains(g2))

        val g3 =
            testNewGroupType(
                "f",
                FieldRepetitionType.REPEATED,
                listOf(
                    SchemaType.primitiveTypeBuilder("f2", ParquetType.INT64).build(),
                ),
            )
        assertTrue(g1.checkContains(g3))

        val g4 = SchemaType.groupTypeBuilder("f1").build()
        val g5 = SchemaType.groupTypeBuilder("f2").build()
        assertFalse(g4.checkContains(g5))
    }

    @Test
    fun testCheckContainsGroupPrimitive() {
        // KO: should not match
        val f1 = SchemaType.groupTypeBuilder("f").build()
        val f2 = SchemaType.primitiveTypeBuilder("f", ParquetType.INT64).build()
        assertFalse(f1.checkContains(f2))
        assertFalse(f2.checkContains(f1))

        // KO: should not match when primitive field is part of group type
        val g1 =
            testNewGroupType(
                "f",
                FieldRepetitionType.REPEATED,
                listOf(SchemaType.primitiveTypeBuilder("f1", ParquetType.INT32).build()),
            )
        val g2 = SchemaType.primitiveTypeBuilder("f1", ParquetType.INT32).build()
        assertFalse(g1.checkContains(g2))
        assertFalse(g2.checkContains(g1))

        // OK: match nested types
        val h1 =
            testNewGroupType(
                "a",
                FieldRepetitionType.REPEATED,
                listOf(
                    testNewGroupType(
                        "b",
                        FieldRepetitionType.REPEATED,
                        listOf(SchemaType.primitiveTypeBuilder("c", ParquetType.INT32).build()),
                    ),
                    SchemaType.primitiveTypeBuilder("d", ParquetType.INT64).build(),
                    SchemaType.primitiveTypeBuilder("e", ParquetType.BOOLEAN).build(),
                ),
            )
        val h2 =
            testNewGroupType(
                "a",
                FieldRepetitionType.REPEATED,
                listOf(
                    testNewGroupType(
                        "b",
                        FieldRepetitionType.REPEATED,
                        listOf(SchemaType.primitiveTypeBuilder("c", ParquetType.INT32).build()),
                    ),
                ),
            )
        assertTrue(h1.checkContains(h2)) // should match
        assertFalse(h2.checkContains(h1)) // should fail
    }

    @Test
    fun testSchemaTypeThriftConversionErr() {
        val schema = SchemaType.primitiveTypeBuilder("col", ParquetType.INT32).build()
        val err =
            assertFailsWith<ParquetError> {
                schemaToBuf(schema)
            }
        assertEquals("Parquet error: Root schema must be Group type", err.message)
    }

    private fun fileField(
        name: String,
        physical: ParquetType,
        logical: LogicalType?,
    ): SchemaType =
        SchemaType
            .primitiveTypeBuilder(name, physical)
            .withRepetition(FieldRepetitionType.OPTIONAL)
            .withLogicalType(logical)
            .build()

    private fun allFileFields(): List<SchemaType> =
        listOf(
            fileField("uri", ParquetType.BYTE_ARRAY, LogicalType.String),
            fileField("offset", ParquetType.INT64, null),
            fileField("size", ParquetType.INT64, null),
            fileField("content_type", ParquetType.BYTE_ARRAY, LogicalType.String),
            fileField("checksum", ParquetType.BYTE_ARRAY, LogicalType.String),
            fileField("inline", ParquetType.BYTE_ARRAY, null),
        )

    @Test
    fun testFileLogicalTypeRoundtrip() {
        val fileGroup =
            SchemaType
                .groupTypeBuilder("f")
                .withRepetition(FieldRepetitionType.REQUIRED)
                .withLogicalType(LogicalType.File)
                .withFields(allFileFields())
                .build()
        val schema =
            SchemaType
                .groupTypeBuilder("example")
                .withFields(listOf(fileGroup))
                .build()

        val result = roundtripSchema(schema)
        assertEquals(schema, result)
        assertEquals(LogicalType.File, result.getFields()[0].getBasicInfo().logicalTypeRef())
    }

    @Test
    fun testFileLogicalTypeAllFields() {
        val result =
            SchemaType
                .groupTypeBuilder("file_field")
                .withRepetition(FieldRepetitionType.REQUIRED)
                .withLogicalType(LogicalType.File)
                .withFields(allFileFields())
                .build()
        assertEquals(6, result.getFields().size)
    }

    @Test
    fun testFileLogicalTypeUriOnly() {
        val result =
            SchemaType
                .groupTypeBuilder("file_field")
                .withRepetition(FieldRepetitionType.REQUIRED)
                .withLogicalType(LogicalType.File)
                .withFields(listOf(fileField("uri", ParquetType.BYTE_ARRAY, LogicalType.String)))
                .build()
        assertEquals(LogicalType.File, result.getBasicInfo().logicalTypeRef())
    }

    @Test
    fun testFileLogicalTypeInlineOnly() {
        val result =
            SchemaType
                .groupTypeBuilder("inline_file")
                .withRepetition(FieldRepetitionType.REQUIRED)
                .withLogicalType(LogicalType.File)
                .withFields(listOf(fileField("inline", ParquetType.BYTE_ARRAY, null)))
                .build()
        assertEquals(1, result.getFields().size)
    }

    @Test
    fun testFileLogicalTypeEmptyGroupIsAllowed() {
        val result =
            SchemaType
                .groupTypeBuilder("empty_file")
                .withRepetition(FieldRepetitionType.REQUIRED)
                .withLogicalType(LogicalType.File)
                .withFields(emptyList())
                .build()
        assertEquals(0, result.getFields().size)
    }

    @Test
    fun testFileLogicalTypeRejectsUnrecognizedField() {
        val unknownField = fileField("unknown_field", ParquetType.BYTE_ARRAY, null)
        val err =
            assertFailsWith<ParquetError> {
                SchemaType
                    .groupTypeBuilder("bad_file")
                    .withRepetition(FieldRepetitionType.REQUIRED)
                    .withLogicalType(LogicalType.File)
                    .withFields(
                        listOf(
                            fileField("uri", ParquetType.BYTE_ARRAY, LogicalType.String),
                            unknownField,
                        ),
                    ).build()
            }
        assertTrue(err.message!!.contains("contains unrecognized field 'unknown_field'"))
    }

    @Test
    fun testFileLogicalTypeRequiresOptionalFields() {
        val uriField =
            SchemaType
                .primitiveTypeBuilder("uri", ParquetType.BYTE_ARRAY)
                .withRepetition(FieldRepetitionType.REQUIRED)
                .withLogicalType(LogicalType.String)
                .build()
        val err =
            assertFailsWith<ParquetError> {
                SchemaType
                    .groupTypeBuilder("required_uri")
                    .withRepetition(FieldRepetitionType.REQUIRED)
                    .withLogicalType(LogicalType.File)
                    .withFields(listOf(uriField))
                    .build()
            }
        assertTrue(err.message!!.contains("must be OPTIONAL in group 'required_uri'"))
    }

    @Test
    fun testFileLogicalTypeRejectsWrongPhysicalType() {
        val badSize = fileField("size", ParquetType.BYTE_ARRAY, null)
        val err =
            assertFailsWith<ParquetError> {
                SchemaType
                    .groupTypeBuilder("bad_size")
                    .withRepetition(FieldRepetitionType.REQUIRED)
                    .withLogicalType(LogicalType.File)
                    .withFields(listOf(badSize))
                    .build()
            }
        assertTrue(err.message!!.contains("must have physical type INT64"))
    }

    @Test
    fun testFileLogicalTypeRejectsWrongLogicalType() {
        val badUri = fileField("uri", ParquetType.BYTE_ARRAY, null)
        val err =
            assertFailsWith<ParquetError> {
                SchemaType
                    .groupTypeBuilder("bad_uri")
                    .withRepetition(FieldRepetitionType.REQUIRED)
                    .withLogicalType(LogicalType.File)
                    .withFields(listOf(badUri))
                    .build()
            }
        assertTrue(err.message!!.contains("must have logical type STRING"))
    }

    @Test
    fun testFileLogicalTypeNotAllowedOnPrimitive() {
        assertFailsWith<ParquetError> {
            SchemaType
                .primitiveTypeBuilder("bad", ParquetType.BYTE_ARRAY)
                .withRepetition(FieldRepetitionType.REQUIRED)
                .withLogicalType(LogicalType.File)
                .build()
        }
    }

    @Test
    fun testParquetSchemaFromArrayRejectsNegativeNumChildren() {
        val elements =
            listOf(
                SchemaElement(
                    type = null,
                    typeLength = null,
                    repetitionType = FieldRepetitionType.REQUIRED,
                    name = "schema",
                    numChildren = -1,
                ),
            )
        val err =
            assertFailsWith<ParquetError> {
                parquetSchemaFromArray(elements)
            }
        assertTrue(err.message!!.contains("Integer overflow"))
    }
}
