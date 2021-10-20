package org.jetbrains.plugins.scala.lang.dfa.analysis.tests

import org.jetbrains.plugins.scala.lang.dfa.Messages.ConditionAlwaysTrue
import org.jetbrains.plugins.scala.lang.dfa.analysis.ScalaDfaTestBase

class IgnoredConstructsDfaTest extends ScalaDfaTestBase {

  def testDefinitionsNotAffectingRestOfAnalysis(): Unit = test(codeFromMethodBody(returnType = "Boolean") {
    """
      |val x = 2
      |
      |
      |val y1 = s"Interpolated string with $x"
      |
      |private def nestedMethod(x: Int, y: Int): Int = {
      |  val z = x * y
      |  throw new IllegalStateException
      |}
      |
      |class NestedClass(x: Int) {
      |  final def someMethod(y: Int): Int = x + y
      |}
      |
      |trait NestedTrait
      |case class One() extends NestedTrait
      |case object Two extends NestedTrait
      |
      |object NestedObject {
      |  private def something: String = "Hi"
      |}
      |
      |
      |x == 2
      |""".stripMargin
  })(
    "x == 2" -> ConditionAlwaysTrue
  )

  def testIgnoringImplicitConversions(): Unit = test(codeFromMethodBody(returnType = "Boolean") {
    """
      |val x = 2
      |val y = x.timesAndPlus(5)
      |y == 13
      |
      |x == 2
      |""".stripMargin
  })(
    "x == 2" -> ConditionAlwaysTrue
  )
}