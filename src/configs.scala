package njumips
package configs

import chisel3._

object conf {
  val xprlen = 32
  val addr_width = 32
  val data_width = 32
  val xprbyte = xprlen / 8
  val start_addr = "hbfc00000".U
  val axi_data_width = 32
  val axi_id_width = 4
  val memio_cycles = 2
  val INSTR_ID_SZ = 6
}
