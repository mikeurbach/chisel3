// SPDX-License-Identifier: Apache-2.0

package chisel3

import chisel3.internal.throwException
import chisel3.internal.firrtl.Width
import chisel3.experimental.SourceInfo

/** Properties are like normal Elements in that they can be used in ports,
  * connected to, etc. However, they are used to describe a hierarchy of
  * non-hardware types, so they have no width, and cannot be connected to
  * hardware types.
  */
private[chisel3] sealed abstract trait Property extends Element {
  private[chisel3] override def connectFromBits(
    that: Bits
  )(implicit sourceInfo: SourceInfo): Unit = {
    throwException("Integer cannot be connected from Bits")
  }

  override def do_asUInt(implicit sourceInfo: SourceInfo): UInt = {
    throwException("Integer cannot be converted to UInt")
  }

  private[chisel3] override def width: Width = Width()
}

private[chisel3] sealed class IntegerProp extends Property {
  override def cloneType: this.type = new IntegerProp().asInstanceOf[this.type]
  override def toPrintable: Printable = PString("IntegerProp")
}
