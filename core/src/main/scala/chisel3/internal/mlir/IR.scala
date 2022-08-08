package chisel3.internal.mlir

import com.github.sbt.jni.nativeLoader

@nativeLoader("nativeMLIR0")
class IR {
  @native def mlirContextCreate(): Long
  @native def mlirLocationCreate(context: Long):     Long
  @native def mlirModuleCreate(loc:       Long):     Long
  @native def mlirModulePrint(module:     Long)
  @native def mlirOperationCreate(module: Long, loc: Long, name: String): Long
}
