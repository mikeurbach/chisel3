package chisel3.internal.mlir
import chisel3._
import chisel3.internal.firrtl._
import firrtl.{ir => fir}

object Converter {
  def convert(circuit: Circuit): fir.Circuit = {
    println("MIKE IS HERE")
    val ir = new IR
    val ctx = ir.mlirContextCreate()
    val loc = ir.mlirLocationCreate(ctx)
    val mod = ir.mlirModuleCreate(loc)
    ir.mlirOperationCreate(mod, loc, "foo.a")
    ir.mlirOperationCreate(mod, loc, "foo.b")
    ir.mlirModulePrint(mod)
    fir.Circuit(fir.NoInfo, List.empty, circuit.name)
  }
}
