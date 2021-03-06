package njumips

import chisel3._
import chisel3.util._
import njumips.configs._

trait MemConsts {
  // MX
  val MX_SZ = 1
  val MX_X  = 0.U(MX_SZ.W)
  val MX_RD = 0.U(MX_SZ.W)
  val MX_WR = 1.U(MX_SZ.W)

  val ML_SZ = 2
  val ML_X  = 0.U(ML_SZ.W)
  val ML_1  = 0.U(ML_SZ.W)
  val ML_2  = 1.U(ML_SZ.W)
  val ML_3  = 2.U(ML_SZ.W)
  val ML_4  = 3.U(ML_SZ.W)
}

trait CP0Consts {
  val AM_EVL = 0x10000020.U // exception vector location

  val EXC_WIDTH = 5
  // EXC code
  val EXC_INTR    = 0.U(EXC_WIDTH.W)   // - Interrupt
  val EXC_Mod     = 1.U(EXC_WIDTH.W)   // * TLB modification
  val EXC_TLBL    = 2.U(EXC_WIDTH.W)   // * TLB load
  val EXC_TLBS    = 3.U(EXC_WIDTH.W)   // * TLB store
  val EXC_AdEL    = 4.U(EXC_WIDTH.W)   // * Address Load
  val EXC_AdES    = 5.U(EXC_WIDTH.W)   // * Address Store
  val EXC_IBE     = 6.U(EXC_WIDTH.W)   //   Bus error(IF)
  val EXC_DBE     = 7.U(EXC_WIDTH.W)   //   Bus error(data)
  val EXC_SYSCALL = 8.U(EXC_WIDTH.W)   // * Syscall
  val EXC_BP      = 9.U(EXC_WIDTH.W)   //   Break Point
  val EXC_RI      = 10.U(EXC_WIDTH.W)  // * Reserved instruction
  val EXC_CPU     = 11.U(EXC_WIDTH.W)  // * Coprocessor unusable
  val EXC_OV      = 12.U(EXC_WIDTH.W)  // * Arithmetic overflow
  val EXC_TRAP    = 13.U(EXC_WIDTH.W)  // * Trap

  // exception type
  val ETW_WIDTH = 32
  val ET_NONE     =  0x0.U(ETW_WIDTH.W)    // no excepttion
  val ET_INT      =  0x1.U(ETW_WIDTH.W)    // interruptions
  val ET_AdEL_IF  =  0x2.U(ETW_WIDTH.W)    // pc[1:0]  != 2'b00, AdEl occurs in if stage 
  val ET_SYSCALL  =  0x4.U(ETW_WIDTH.W)    // syscall instruction
  val ET_INVOP    =  0x8.U(ETW_WIDTH.W)    // invalid opcode
  val ET_OV       =  0x10.U(ETW_WIDTH.W)   // overflow
  val ET_TRAP     =  0x20.U(ETW_WIDTH.W)   // trap instruction     
  val ET_BP       =  0x40.U(ETW_WIDTH.W)   // breakpoint
  val ET_AdEL_LD  =  0x80.U(ETW_WIDTH.W)   // unaligned address when loading
  val ET_AdES     =  0x100.U(ETW_WIDTH.W)  // unaligned address when storing
  val ET_eret     =  0x200.U(ETW_WIDTH.W)  // eret
  val ET_Mod      =  0x400.U(ETW_WIDTH.W)  // TLB modification
  val ET_TLBL     =  0x800.U(ETW_WIDTH.W)  // TLB load/IF
  val ET_TLBS     =  0x1000.U(ETW_WIDTH.W) // TLB store

  // exception bit
  val ETB_INT     =  0         // interruptions
  val ETB_AdEL_IF =  1         // pc[1:0]  != 2'b00, AdEl occurs in if stage 
  val ETB_SYSCALL =  2         // syscall instruction
  val ETB_INVOP   =  3         // invalid opcode
  val ETB_OV      =  4         // overflow
  val ETB_TRAP    =  5         // trap instruction     
  val ETB_BP      =  6         // breakpoint
  val ETB_AdEL_LD =  7         // unaligned address when loading
  val ETB_AdES    =  8         // unaligned address when storing
  val ETB_eret    =  9         // eret
  val ETB_Mod     =  10        // TLB modification
  val ETB_TLBL    =  11        // TLB load/IF
  val ETB_TLBS    =  12        // TLB store
}

trait InstrConsts {
  val REG_SZ    = 5;
}

trait InstrPattern {
  val LUI   = BitPat("b00111100000?????????????????????")
  val ADD   = BitPat("b000000???????????????00000100000")
  val ADDU  = BitPat("b000000???????????????00000100001")
  val SUB   = BitPat("b000000???????????????00000100010")
  val SUBU  = BitPat("b000000???????????????00000100011")
  val SLT   = BitPat("b000000???????????????00000101010")
  val SLTU  = BitPat("b000000???????????????00000101011")
  val AND   = BitPat("b000000???????????????00000100100")
  val OR    = BitPat("b000000???????????????00000100101")
  val XOR   = BitPat("b000000???????????????00000100110")
  val NOR   = BitPat("b000000???????????????00000100111")
  val SLTI  = BitPat("b001010??????????????????????????")
  val SLTIU = BitPat("b001011??????????????????????????")
  val SRA   = BitPat("b00000000000???????????????000011")
  val SRL   = BitPat("b00000000000???????????????000010")
  val SLL   = BitPat("b00000000000???????????????000000")
  val SRAV  = BitPat("b000000???????????????00000000111")
  val SRLV  = BitPat("b000000???????????????00000000110")
  val SLLV  = BitPat("b000000???????????????00000000100")

  val ADDI  = BitPat("b001000??????????????????????????")
  val ADDIU = BitPat("b001001??????????????????????????")
  val ANDI  = BitPat("b001100??????????????????????????")
  val ORI   = BitPat("b001101??????????????????????????")
  val XORI  = BitPat("b001110??????????????????????????")
  val MOVN  = BitPat("b000000???????????????00000001011")
  val MOVZ  = BitPat("b000000???????????????00000001010")

  val BEQ   = BitPat("b000100??????????????????????????")
  val BNE   = BitPat("b000101??????????????????????????")
  val BLEZ  = BitPat("b000110?????00000????????????????")
  val BGTZ  = BitPat("b000111?????00000????????????????")
  val BLTZ  = BitPat("b000001?????00000????????????????")
  val J     = BitPat("b000010??????????????????????????")
  val JAL   = BitPat("b000011??????????????????????????")
  val JR    = BitPat("b000000?????000000000000000001000")
  val JALR  = BitPat("b000000???????????????00000001001")

  val LW    = BitPat("b100011??????????????????????????")
  val LH    = BitPat("b100001??????????????????????????")
  val LHU   = BitPat("b100101??????????????????????????")
  val LB    = BitPat("b100000??????????????????????????")
  val LBU   = BitPat("b100100??????????????????????????")
  val LWL   = BitPat("b100010??????????????????????????")
  val LWR   = BitPat("b100110??????????????????????????")
  val SW    = BitPat("b101011??????????????????????????")
  val SH    = BitPat("b101001??????????????????????????")
  val SB    = BitPat("b101000??????????????????????????")
  val SWL   = BitPat("b101010??????????????????????????")
  val SWR   = BitPat("b101110??????????????????????????")

  val MFHI  = BitPat("b0000000000000000?????00000010000")
  val MFLO  = BitPat("b0000000000000000?????00000010010")
  val MUL   = BitPat("b011100???????????????00000000010")
  val MULT  = BitPat("b000000??????????0000000000011000")
  val MULTU = BitPat("b000000??????????0000000000011001")
  val DIV   = BitPat("b000000??????????0000000000011010")
  val DIVU  = BitPat("b000000??????????0000000000011011")
}

object consts extends InstrPattern
  with MemConsts
  with CP0Consts
  with InstrConsts
{
  val Y = true.B
  val N = false.B
}
