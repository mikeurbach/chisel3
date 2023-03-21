// SPDX-License-Identifier: Apache-2.0

package chiselTests.naming

import chisel3._
import chisel3.aop.Select
import chisel3.experimental.prefix
import chiselTests.{ChiselFlatSpec, Utils}
import circt.stage.ChiselStage

class NamePluginSpec extends ChiselFlatSpec with Utils {
  implicit val minimumScalaVersion: Int = 12

  "Scala plugin" should "name internally scoped components for val" in {
    class Test extends Module {
      { val mywire = Wire(UInt(3.W)) }
    }
    aspectTest(() => new Test) { top: Test =>
      Select.wires(top).head.toTarget.ref should be("mywire")
    }
  }

  "Scala plugin" should "name internally scoped instances for val" in {
    class Inner extends Module {}
    class Test extends Module {
      { val myinstance = Module(new Inner) }
    }
    aspectTest(() => new Test) { top: Test =>
      Select.instances(top).head.instanceName should be("myinstance")
    }
  }

  "Scala plugin" should "interact with prefixing for val" in {
    class Test extends Module {
      def builder() = {
        val wire = Wire(UInt(3.W))
      }
      prefix("first") {
        builder()
      }
      prefix("second") {
        builder()
      }
    }
    aspectTest(() => new Test) { top: Test =>
      Select.wires(top).map(_.instanceName) should be(List("first_wire", "second_wire"))
    }
  }

  "Scala plugin" should "interact with prefixing so last val name wins for val" in {
    class Test extends Module {
      def builder() = {
        val wire1 = Wire(UInt(3.W))
        val wire2 = Wire(UInt(3.W))
        wire2
      }
      {
        val x1 = prefix("first") {
          builder()
        }
      }
      {
        val x2 = prefix("second") {
          builder()
        }
      }
    }
    aspectTest(() => new Test) { top: Test =>
      Select.wires(top).map(_.instanceName) should be(List("x1_first_wire1", "x1", "x2_second_wire1", "x2"))
    }
  }

  "Scala plugin" should "name verification ops for val" in {
    class Test extends Module {
      val foo, bar = IO(Input(UInt(8.W)))

      {
        val x1 = chisel3.assert(1.U === 1.U)
        val x2 = cover(foo =/= bar)
        val x3 = chisel3.assume(foo =/= 123.U)
        val x4 = printf("foo = %d\n", foo)
      }
    }
    val chirrtl = ChiselStage.emitCHIRRTL(new Test)
    (chirrtl should include).regex("assert.*: x1")
    (chirrtl should include).regex("cover.*: x2")
    (chirrtl should include).regex("assume.*: x3")
    (chirrtl should include).regex("printf.*: x4")
  }

  "Naming on option" should "work for val" in {

    class Test extends Module {
      def builder(): Option[UInt] = {
        val a = Wire(UInt(3.W))
        Some(a)
      }

      { val blah = builder() }
    }
    aspectTest(() => new Test) { top: Test =>
      Select.wires(top).map(_.instanceName) should be(List("blah"))
    }
  }

  "Naming on iterables" should "work for val" in {

    class Test extends Module {
      def builder(): Seq[UInt] = {
        val a = Wire(UInt(3.W))
        val b = Wire(UInt(3.W))
        Seq(a, b)
      }
      {
        val blah = {
          builder()
        }
      }
    }
    aspectTest(() => new Test) { top: Test =>
      Select.wires(top).map(_.instanceName) should be(List("blah_0", "blah_1"))
    }
  }

  "Naming on nested iterables" should "work for val" in {

    class Test extends Module {
      def builder(): Seq[Seq[UInt]] = {
        val a = Wire(UInt(3.W))
        val b = Wire(UInt(3.W))
        val c = Wire(UInt(3.W))
        val d = Wire(UInt(3.W))
        Seq(Seq(a, b), Seq(c, d))
      }
      {
        val blah = {
          builder()
        }
      }
    }
    aspectTest(() => new Test) { top: Test =>
      Select.wires(top).map(_.instanceName) should be(
        List(
          "blah_0_0",
          "blah_0_1",
          "blah_1_0",
          "blah_1_1"
        )
      )
    }
  }

  "Naming on custom case classes" should "not work for val" in {
    case class Container(a: UInt, b: UInt)

    class Test extends Module {
      def builder(): Container = {
        val a = Wire(UInt(3.W))
        val b = Wire(UInt(3.W))
        Container(a, b)
      }

      { val blah = builder() }
    }
    aspectTest(() => new Test) { top: Test =>
      Select.wires(top).map(_.instanceName) should be(List("a", "b"))
    }
  }

  "Multiple names on an IO within a module" should "get the first name for val" in {
    class Test extends RawModule {
      {
        val a = IO(Output(UInt(3.W)))
        val b = a
      }
    }

    aspectTest(() => new Test) { top: Test =>
      Select.ios(top).map(_.instanceName) should be(List("a"))
    }
  }

  "Multiple names on a non-IO" should "get the first name for val" in {
    class Test extends Module {
      {
        val a = Wire(UInt(3.W))
        val b = a
      }
    }

    aspectTest(() => new Test) { top: Test =>
      Select.wires(top).map(_.instanceName) should be(List("a"))
    }
  }

  "Outer Expression, First Statement naming" should "apply to IO for val" in {
    class Test extends RawModule {
      {
        val widthOpt: Option[Int] = Some(4)
        val out = widthOpt.map { w =>
          val port = IO(Output(UInt(w.W)))
          port
        }
        val foo = out
        val bar = out.get
      }
    }

    aspectTest(() => new Test) { top: Test =>
      Select.ios(top).map(_.instanceName) should be(List("out"))
    }
  }

  "Outer Expression, First Statement naming" should "apply to non-IO for val" in {
    class Test extends RawModule {
      {
        val widthOpt: Option[Int] = Some(4)
        val fizz = widthOpt.map { w =>
          val wire = Wire(UInt(w.W))
          wire
        }
        val foo = fizz
        val bar = fizz.get
      }
    }

    aspectTest(() => new Test) { top: Test =>
      Select.wires(top).map(_.instanceName) should be(List("fizz"))
    }
  }

  "autoSeed" should "NOT override automatic naming for IO for val" in {
    class Test extends RawModule {
      {
        val a = IO(Output(UInt(3.W)))
        a.autoSeed("b")
      }
    }

    aspectTest(() => new Test) { top: Test =>
      Select.ios(top).map(_.instanceName) should be(List("a"))
    }
  }

  "autoSeed" should "override automatic naming for non-IO for val" in {
    class Test extends Module {
      {
        val a = Wire(UInt(3.W))
        a.autoSeed("b")
      }
    }

    aspectTest(() => new Test) { top: Test =>
      Select.wires(top).map(_.instanceName) should be(List("b"))
    }
  }

  "Unapply assignments" should "still be named for val" in {
    class Test extends Module {
      {
        val (a, b) = (Wire(UInt(3.W)), Wire(UInt(3.W)))
      }
    }

    aspectTest(() => new Test) { top: Test =>
      Select.wires(top).map(_.instanceName) should be(List("a", "b"))
    }
  }

  "Unapply assignments" should "not override already named things for val" in {
    class Test extends Module {
      {
        val x = Wire(UInt(3.W))
        val (a, b) = (x, Wire(UInt(3.W)))
      }
    }

    aspectTest(() => new Test) { top: Test =>
      Select.wires(top).map(_.instanceName) should be(List("x", "b"))
    }
  }

  "Case class unapply assignments" should "be named for val" in {
    case class Foo(x: UInt, y: UInt)
    class Test extends Module {
      {
        def func() = Foo(Wire(UInt(3.W)), Wire(UInt(3.W)))
        val Foo(a, b) = func()
      }
    }

    aspectTest(() => new Test) { top: Test =>
      Select.wires(top).map(_.instanceName) should be(List("a", "b"))
    }
  }

  "Complex unapply assignments" should "be named for val" in {
    case class Foo(x: UInt, y: UInt)
    class Test extends Module {
      {
        val w = Wire(UInt(3.W))
        def func() = {
          val x = Foo(Wire(UInt(3.W)), Wire(UInt(3.W)))
          (x, w) :: Nil
        }
        val ((Foo(a, _), c) :: Nil) = func()
      }
    }

    aspectTest(() => new Test) { top: Test =>
      Select.wires(top).map(_.instanceName) should be(List("w", "a", "_WIRE"))
    }
  }

  "Recursive types" should "not infinitely loop for val" in {
    // When this fails, it causes a StackOverflow when compiling the tests
    // Unfortunately, this doesn't seem to work with assertCompiles(...), it probably ignores the
    // custom project scalacOptions
    def func(x: String) = {
      // We only check types of vals, we don't actually want to run this code though
      val y = scala.xml.XML.loadFile(x)
      y
    }
  }

  "Nested val declarations" should "all be named" in {
    class Test extends Module {
      {
        val a = {
          val b = {
            val c = Wire(UInt(3.W))
            Wire(UInt(3.W))
          }
          Wire(UInt(3.W))
        }
      }
    }

    aspectTest(() => new Test) { top: Test =>
      Select.wires(top).map(_.instanceName) should be(List("a_b_c", "a_b", "a"))
    }
  }

  behavior.of("Unnamed values (aka \"Temporaries\")")

  they should "be declared by starting the name with '_' for val" in {
    class Test extends Module {
      {
        val a = {
          val b = {
            val _c = Wire(UInt(3.W))
            4.U // literal so there is no name
          }
          b
        }
      }
    }
    aspectTest(() => new Test) { top: Test =>
      Select.wires(top).map(_.instanceName) should be(List("_a_b_c"))
    }
  }

  "Scala plugin" should "name internally scoped components for def" in {
    class Test extends Module {
      def mywire = Wire(UInt(3.W))
      mywire
    }
    aspectTest(() => new Test) { top: Test =>
      Select.wires(top).head.toTarget.ref should be("mywire")
    }
  }

  "Scala plugin" should "name internally scoped components used multiple times for def" in {
    class Test extends Module {
      def mywire = Wire(UInt(3.W))
      Seq(mywire, mywire)
    }
    aspectTest(() => new Test) { top: Test =>
      Select.wires(top).map(_.toTarget.ref) should be(List("mywire", "mywire"))
    }
  }

  "Scala plugin" should "name internally scoped instances for def" in {
    class Inner extends Module {}
    class Test extends Module {
      def myinstance = Module(new Inner)
      myinstance
    }
    aspectTest(() => new Test) { top: Test =>
      Select.instances(top).head.instanceName should be("myinstance")
    }
  }

  "Scala plugin" should "interact with prefixing for def" in {
    class Test extends Module {
      def builder() = {
        def wire = Wire(UInt(3.W))
        wire
      }
      prefix("first") {
        builder()
      }
      prefix("second") {
        builder()
      }
    }
    aspectTest(() => new Test) { top: Test =>
      Select.wires(top).map(_.instanceName) should be(List("first_wire", "second_wire"))
    }
  }

  "Scala plugin" should "interact with prefixing so last def name wins for def" in {
    class Test extends Module {
      def builder() = {
        def wire1 = Wire(UInt(3.W))
        wire1
        def wire2 = Wire(UInt(3.W))
        wire2
      }
      def x1 = prefix("first") {
        builder()
      }
      x1
      def x2 = prefix("second") {
        builder()
      }
      x2
    }
    aspectTest(() => new Test) { top: Test =>
      Select.wires(top).map(_.instanceName) should be(List("x1_first_wire1", "x1", "x2_second_wire1", "x2"))
    }
  }

  "Scala plugin" should "name verification ops for def" in {
    class Test extends Module {
      val foo, bar = IO(Input(UInt(8.W)))

      def x1 = chisel3.assert(1.U === 1.U)
      x1
      def x2 = cover(foo =/= bar)
      x2
      def x3 = chisel3.assume(foo =/= 123.U)
      x3
      def x4 = printf("foo = %d\n", foo)
      x4
    }
    val chirrtl = ChiselStage.emitCHIRRTL(new Test)
    (chirrtl should include).regex("assert.*: x1")
    (chirrtl should include).regex("cover.*: x2")
    (chirrtl should include).regex("assume.*: x3")
    (chirrtl should include).regex("printf.*: x4")
  }

  "Naming on option" should "work for def" in {

    class Test extends Module {
      def builder(): Option[UInt] = {
        def a = Wire(UInt(3.W))
        Some(a)
      }

      val blah = builder()
    }
    aspectTest(() => new Test) { top: Test =>
      Select.wires(top).map(_.instanceName) should be(List("blah"))
    }
  }

  "Naming on iterables" should "work for def" in {

    class Test extends Module {
      def builder(): Seq[UInt] = {
        def a = Wire(UInt(3.W))
        def b = Wire(UInt(3.W))
        Seq(a, b)
      }
      val blah = {
        builder()
      }
    }
    aspectTest(() => new Test) { top: Test =>
      Select.wires(top).map(_.instanceName) should be(List("blah_0", "blah_1"))
    }
  }

  "Naming on nested iterables" should "work for def" in {

    class Test extends Module {
      def builder(): Seq[Seq[UInt]] = {
        def a = Wire(UInt(3.W))
        def b = Wire(UInt(3.W))
        def c = Wire(UInt(3.W))
        def d = Wire(UInt(3.W))
        Seq(Seq(a, b), Seq(c, d))
      }
      val blah = {
        builder()
      }
    }
    aspectTest(() => new Test) { top: Test =>
      Select.wires(top).map(_.instanceName) should be(
        List(
          "blah_0_0",
          "blah_0_1",
          "blah_1_0",
          "blah_1_1"
        )
      )
    }
  }

  "Naming on custom case classes" should "not work for def" in {
    case class Container(a: UInt, b: UInt)

    class Test extends Module {
      def builder(): Container = {
        def a = Wire(UInt(3.W))
        def b = Wire(UInt(3.W))
        Container(a, b)
      }

      val blah = builder()
    }
    aspectTest(() => new Test) { top: Test =>
      Select.wires(top).map(_.instanceName) should be(List("a", "b"))
    }
  }

  "Multiple names on an IO within a module" should "get the first name for def" in {
    class Test extends RawModule {
      def a = IO(Output(UInt(3.W)))
      def b = a
      Seq(a, b)
    }

    aspectTest(() => new Test) { top: Test =>
      Select.ios(top).map(_.instanceName) should be(List("a"))
    }
  }

  "Multiple names on a non-IO" should "get the first name for def" in {
    class Test extends Module {
      def a = Wire(UInt(3.W))
      def b = a
      Seq(a, b)
    }

    aspectTest(() => new Test) { top: Test =>
      Select.wires(top).map(_.instanceName) should be(List("a"))
    }
  }

  "Outer Expression, First Statement naming" should "apply to IO for def" in {
    class Test extends RawModule {
      def widthOpt: Option[Int] = Some(4)
      def out = widthOpt.map { w =>
        def port = IO(Output(UInt(w.W)))
        port
      }
      def foo = out
      foo
      def bar = out.get
      bar
    }

    aspectTest(() => new Test) { top: Test =>
      Select.ios(top).map(_.instanceName) should be(List("out"))
    }
  }

  "Outer Expression, First Statement naming" should "apply to non-IO for def" in {
    class Test extends RawModule {
      def widthOpt: Option[Int] = Some(4)
      def fizz = widthOpt.map { w =>
        def wire = Wire(UInt(w.W))
        wire
      }
      def foo = fizz
      foo
      def bar = fizz.get
      bar
    }

    aspectTest(() => new Test) { top: Test =>
      Select.wires(top).map(_.instanceName) should be(List("fizz"))
    }
  }

  "autoSeed" should "NOT override automatic naming for IO for def" in {
    class Test extends RawModule {
      def a = IO(Output(UInt(3.W)))
      a.autoSeed("b")
    }

    aspectTest(() => new Test) { top: Test =>
      Select.ios(top).map(_.instanceName) should be(List("a"))
    }
  }

  "autoSeed" should "override automatic naming for non-IO for def" in {
    class Test extends Module {
      def a = Wire(UInt(3.W))
      a.autoSeed("b")
    }

    aspectTest(() => new Test) { top: Test =>
      Select.wires(top).map(_.instanceName) should be(List("b"))
    }
  }

  // "Unapply assignments" should "still be named for def" in {
  //   class Test extends Module {
  //     {
  //       def (a, b) = (Wire(UInt(3.W)), Wire(UInt(3.W)))
  //     }
  //   }

  //   aspectTest(() => new Test) { top: Test =>
  //     Select.wires(top).map(_.instanceName) should be(List("a", "b"))
  //   }
  // }

  // "Unapply assignments" should "not override already named things for def" in {
  //   class Test extends Module {
  //     {
  //       def x = Wire(UInt(3.W))
  //       def (a, b) = (x, Wire(UInt(3.W)))
  //     }
  //   }

  //   aspectTest(() => new Test) { top: Test =>
  //     Select.wires(top).map(_.instanceName) should be(List("x", "b"))
  //   }
  // }

  // "Case class unapply assignments" should "be named for def" in {
  //   case class Foo(x: UInt, y: UInt)
  //   class Test extends Module {
  //     {
  //       def func() = Foo(Wire(UInt(3.W)), Wire(UInt(3.W)))
  //       def Foo(a, b) = func()
  //     }
  //   }

  //   aspectTest(() => new Test) { top: Test =>
  //     Select.wires(top).map(_.instanceName) should be(List("a", "b"))
  //   }
  // }

  // "Complex unapply assignments" should "be named for def" in {
  //   case class Foo(x: UInt, y: UInt)
  //   class Test extends Module {
  //     {
  //       def w = Wire(UInt(3.W))
  //       def func() = {
  //         val x = Foo(Wire(UInt(3.W)), Wire(UInt(3.W)))
  //         (x, w) :: Nil
  //       }
  //       def ((Foo(a, _), c) :: Nil) = func()
  //     }
  //   }

  //   aspectTest(() => new Test) { top: Test =>
  //     Select.wires(top).map(_.instanceName) should be(List("w", "a", "_WIRE"))
  //   }
  // }

  "Recursive types" should "not infinitely loop for def" in {
    // When this fails, it causes a StackOverflow when compiling the tests
    // Unfortunately, this doesn't seem to work with assertCompiles(...), it probably ignores the
    // custom project scalacOptions
    def func(x: String) = {
      // We only check types of vals, we don't actually want to run this code though
      def y = scala.xml.XML.loadFile(x)
      y
    }
  }

  "Nested def declarations" should "all be named" in {
    class Test extends Module {
      def a = {
        def b = {
          def c = Wire(UInt(3.W))
          c
          Wire(UInt(3.W))
        }
        b
        Wire(UInt(3.W))
      }
      a
    }

    aspectTest(() => new Test) { top: Test =>
      Select.wires(top).map(_.instanceName) should be(List("a_b_c", "a_b", "a"))
    }
  }

  behavior.of("Unnamed values (aka \"Temporaries\")")

  they should "be declared by starting the name with '_' for def" in {
    class Test extends Module {
      def a = {
        def b = {
          def _c = Wire(UInt(3.W))
          _c
          4.U // literal so there is no name
        }
        b
      }
      a
    }
    aspectTest(() => new Test) { top: Test =>
      Select.wires(top).map(_.instanceName) should be(List("_a_b_c"))
    }
  }
}
