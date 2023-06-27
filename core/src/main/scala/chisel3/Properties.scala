// SPDX-License-Identifier: Apache-2.0

package chisel3

import chisel3.internal.{throwException, ElementLitBinding}
import chisel3.internal.firrtl.Width
import chisel3.experimental.SourceInfo

/** Properties are like normal Elements in that they can be used in ports,
  * connected to, etc. However, they are used to describe a hierarchy of
  * non-hardware types, so they have no width, and cannot be connected to
  * hardware types.
  */
sealed abstract trait PropertyType extends Element {
  private[chisel3] override def connectFromBits(
    that: Bits
  )(implicit sourceInfo: SourceInfo): Unit = {
    throwException("Property cannot be connected from Bits")
  }

  override def do_asUInt(implicit sourceInfo: SourceInfo): UInt = {
    throwException("Property cannot be converted to UInt")
  }

  private[chisel3] override def width: Width = Width()

  override def isLit: Boolean = topBindingOpt match {
    case Some(ElementLitBinding(_)) => true
    case _                          => false
  }

  override def litOption: Option[BigInt] = {
    throwException("Property literals cannot be accessed")
  }

  override def litValue: BigInt = {
    throwException("Property literals cannot be accessed")
  }
}

private[chisel3] sealed class IntegerProp extends PropertyType {
  override def cloneType: this.type = new IntegerProp().asInstanceOf[this.type]
  override def toPrintable: Printable = PString("IntegerProp")
}
