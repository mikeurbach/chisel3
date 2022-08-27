package example

import chisel3._
import chisel3.stage.ChiselStage
import chisel3.experimental.hierarchy._
import chisel3.experimental.ExtModule

case class Thingy(width: Int) extends IsLookupable

@instantiable
class Passthrough extends Module {
  @public val foo = 15.U(8.W)
  @public val in = IO(Input(UInt(8.W)))
  @public val out = IO(Output(UInt(8.W)))
  out := in
}

class Top extends Module {
  val definition = Definition(new Passthrough)
  val instance = Instance(definition)
  instance.in := instance.out
}

object Main extends App {
  println((new ChiselStage).emitFirrtl(new Top()))
}
