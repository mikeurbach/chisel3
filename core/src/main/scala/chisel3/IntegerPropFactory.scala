// SPDX-License-Identifier: Apache-2.0

package chisel3

import chisel3.internal.firrtl.IntegerPropLit

trait IntegerPropFactory {

  /** Creates an empty IntegerProp.
    */
  def apply(): IntegerProp = new IntegerProp()

  /** Creates an Integer literal.
    */
  def apply(n: BigInt): IntegerProp = {
    val lit = IntegerPropLit(n)
    val result = new IntegerProp()
    // Bind result to being a Literal
    lit.bindLitArg(result)
  }
}
