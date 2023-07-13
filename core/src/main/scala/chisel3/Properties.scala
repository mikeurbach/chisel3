// SPDX-License-Identifier: Apache-2.0

package chisel3

import chisel3.internal.{throwException, ElementLitBinding}
import chisel3.internal.firrtl.{Command, Definition, DefClass, DefObject, PropLit, PropAssign, Width}
import chisel3.experimental.SourceInfo

import scala.reflect.runtime.universe.{TypeTag, typeOf, typeTag}

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

class PropertyBase[T]

object PropertyBaseInstances {
  implicit val BigIntPropertyBaseInstance = new PropertyBase[BigInt]
}

import PropertyBaseInstances._

class Prop[T : PropertyBase : TypeTag] extends Element {
  val tpe = typeOf[T]

  private[chisel3] override def connectFromBits(
    that: Bits
  )(implicit sourceInfo: SourceInfo): Unit = {
    throwException("Property cannot be connected from Bits")
  }

  override def do_asUInt(implicit sourceInfo: SourceInfo): UInt = {
    throwException("Property cannot be converted to UInt")
  }

  private[chisel3] override def width: Width = Width()

  override def cloneType: this.type = new Prop[T]().asInstanceOf[this.type]

  override def toPrintable: Printable = PString("Prop")
}

object Prop {
  def apply[T : PropertyBase : TypeTag](): Prop[T] = {
    new Prop[T]
  }

  def apply[T : PropertyBase : TypeTag](lit: T): Prop[T] = {
    val literal = PropLit[T](lit)
    val result = new Prop[T]
    literal.bindLitArg(result)
  }
}

class ClassDef extends RawModule {
  private[chisel3] override def addCommand(c: Command): Unit = c match {
    case (_: DefObject) | (_: PropAssign) => super.addCommand(c)
    case _ => throwException(s"only objects and property assignment allowed, found $c")
  }

  override def IO[T <: Data](iodef: => T)(implicit sourceInfo: SourceInfo): T = {
    val data = iodef // evaluate once (passed by name)
    data match {
      case (_: PropertyType) => super.IO(iodef)(sourceInfo)
      case _ => throwException(s"only property ports allowed, found ${data}")
    }
  }

  override def getComponent(): DefClass = {
    DefClass(this, name, _firrtlPorts.get, _commands.result())
  }

  private[chisel3] override def getInstantiateCommand(sourceInfo: SourceInfo): Definition = {
    DefObject(sourceInfo, this, _component.get.ports)
  }
}
