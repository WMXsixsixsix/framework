package njumips
package core

import chisel3._
import chisel3.util._
import njumips.configs._
import njumips.utils._
import njumips.consts._

class AddrSpace(start:UInt, end:UInt) {
  val st = start
  val ed = end
}

class AXI42SRAM extends Module {
  val io = IO(new Bundle {
    val in = Flipped(new AXI4IO(4, conf.xprlen))
    val out = new MemIO
  })
  io.in <> DontCare
  io.out <> DontCare
}

class MemMux(name:String) extends Module {
  val io = IO(new Bundle {
    val in = Flipped(new MemIO)
    val cached = new MemIO
    val uncached = new MemIO
  })

  io.in.req.ready := Mux(io.in.req.bits.is_cached, io.cached.req.ready, io.uncached.req.ready)
  io.in.resp.valid := io.cached.resp.valid || io.uncached.resp.valid
  io.in.resp.bits := Mux1H(Array(
    io.cached.resp.valid -> io.cached.resp.bits,
    io.uncached.resp.valid -> io.uncached.resp.bits))
  assert (!io.cached.resp.valid || !io.uncached.resp.valid)

  io.cached.req.valid := io.in.req.valid && io.in.req.bits.is_cached
  io.cached.req.bits := io.in.req.bits
  io.cached.resp.ready := io.in.resp.ready

  io.uncached.req.valid := io.in.req.valid && !io.in.req.bits.is_cached
  io.uncached.req.bits := io.in.req.bits
  io.uncached.resp.ready := io.in.resp.ready
}

/* assume memory request reach in order */
class CrossbarNx1(m:Int) extends Module {
  val io = IO(new Bundle {
    val in = Vec(m, Flipped(new MemIO))
    val out = new MemIO
  })

  val in_valids = Reverse(Cat(for (i <- 0 until m) yield io.in(i).req.valid))
  val in_readys = Reverse(Cat(for (i <- 0 until m) yield io.in(i).req.ready))
  val in_resp_readys = Reverse(Cat(for (i <- 0 until m) yield io.in(i).resp.ready))
  val in_valids_1H = Mux(GTimer()(0),
    BitsOneWay(in_valids),
    Reverse(BitsOneWay(Reverse(in_valids))))
  val in_req = Mux1H(for (i <- 0 until m) yield in_valids_1H(i) -> io.in(i).req.bits)

  /* q_datas [0:head, ..., nstages-1:tail] */
  val q_data_sz = RegInit(~(0.U(log2Ceil(m+1).W)))
  val q_datas = Mem(conf.memio_cycles, UInt(m.W))
  val q_out = q_datas(q_data_sz)
  when (io.out.req.fire()) {
    for (i <- 1 until conf.memio_cycles) {
      q_datas(i) := q_datas(i - 1)
    }
    q_datas(0) := in_valids_1H
  }
  q_data_sz := q_data_sz + io.out.req.fire() - io.out.resp.fire()

  for (i <- 0 until m) {
    io.in(i).req.ready := io.out.req.ready && in_valids_1H(i)
    io.in(i).resp.valid := io.out.resp.valid && q_out(i)
    io.in(i).resp.bits := io.out.resp.bits
  }

  io.out.resp.ready := (q_out & in_resp_readys).orR
  io.out.req.valid := in_valids.orR
  io.out.req.bits := in_req

  assert ((~q_data_sz).orR =/= 0.U || !io.out.resp.valid)
}

class MemCrossbar(m:Int, nAddrSpace:Array[AddrSpace]) extends Module {
  val n = nAddrSpace.length
  val io = IO(new Bundle {
    val in = Vec(m, Flipped(new MemIO))
    val out = Vec(n, new MemIO)
  })

  val in_valids = Reverse(Cat(for (i <- 0 until m) yield io.in(i).req.valid))
  val in_readys = Reverse(Cat(for (i <- 0 until m) yield io.in(i).req.ready))
  val in_valids_1H = BitsOneWay(in_valids)
  val has_req = (in_valids_1H & in_readys).orR

  val reqing = RegEnable(next=Y, enable=has_req, init=N)
  val resping = RegInit(N)
  val working = reqing || resping

  val cached_in_valids_1H = RegEnable(next=in_valids_1H, enable=has_req)
  val cached_in_req = RegEnable(next=Mux1H(for (i <- 0 until m) yield in_valids_1H(i) -> io.in(i).req.bits), enable=has_req)
  val cached_out_valids = RegEnable(next=Reverse(Cat(for (i <- 0 until n) yield
      nAddrSpace(i).st <= cached_in_req.addr &&
      cached_in_req.addr < nAddrSpace(i).ed
    )), enable=has_req)

  val has_resp = Cat(for (i <- 0 until n) yield io.out(i).resp.valid).orR

  for (i <- 0 until m) {
    io.in(i).req.ready := !working && in_valids_1H(i)
    io.in(i).resp.valid := has_resp && resping && cached_in_valids_1H(i)
    io.in(i).resp.bits := Mux1H(for (i <- 0 until n) yield
      cached_out_valids(i) -> io.out(i).resp.bits)

    when (io.in(i).resp.fire()) { resping := N }
  }

  val out_req_fire = for (i <- 0 until n) yield io.out(i).req.fire()
  assert (AtMost1H(out_req_fire:_*))
  for (i <- 0 until n) {
    io.out(i).resp.ready := resping
    io.out(i).req.valid := reqing && cached_out_valids(i)
    io.out(i).req.bits  := cached_in_req
    when (io.out(i).req.fire()) {
      reqing := N
      resping := Y
    }
  }

  /* no matched output */
  when (reqing && !cached_out_valids.orR) { reqing := N }
}

