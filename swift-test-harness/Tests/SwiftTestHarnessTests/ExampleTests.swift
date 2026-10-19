#if canImport(Testing)
import Testing
import Parquet

@Suite("Parquet Swift Export Tests")
struct ParquetExportTests {
    @Test("Parquet swift module imported cleanly")
    func testSwiftModuleLoads() throws {
        #expect(Bool(true), "Parquet swift module imported cleanly")
    }
}
#elseif canImport(XCTest)
import XCTest
import Parquet

final class ParquetExportTests: XCTestCase {
    func testSwiftModuleLoads() throws {
        XCTAssertTrue(true, "Parquet swift module imported cleanly")
    }
}
#endif
