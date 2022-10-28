// SPDX-License-Identifier: Apache-2.0

package chisel3

import chisel3.internal.Builder

object VerbatimMode {
  def enable() = {
    Builder.setVerbatimMode(true)
  }
}
