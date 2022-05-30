package org.jetbrains.plugins.scala.lang.formatter.tests.scala3

class Scala3FormatterCommentsTest extends Scala3FormatterBaseTest {

  private def withIndent(indent: String, indents: Int)(text: String): String =
    text.linesIterator.map(indent * indents + _).mkString("\n")

  private val allComments = Seq(
    "// foo",
    "/* foo */",
    "/** foo */",
    """// foo
      |// foo""".stripMargin,
    """/* foo */
      |/* foo */""".stripMargin,
    """/* foo
      |   foo */""".stripMargin,
    """/**
      |  * foo
      |  */""".stripMargin
  )
  private val allContexts = Seq(
    ("", "", 0),
    ("{", "}", 1),
    ("class A {", "}", 1),
    ("class A:", "", 1),
    ("object A {", "}", 1),
    ("object A:", "", 1),
    ("def a = {", "}", 1),
    ("def a =", "", 1)
  )
  private val allIndents = Seq("  ", "    ", "\t")
  private val allBodies = Seq(
    "???",
    """???
      |???""".stripMargin
  )

  private def doCommentsFormatTest(text: String): Unit =
    for {
      comment <- allComments
      (contextBefore, contextAfter, contextIndents) <- allContexts
      indent <- allIndents
      body <- allBodies
    } {
      val toContextIndent = withIndent(indent, contextIndents)(_)
      val toBodyIndent = withIndent(indent, contextIndents + 1)(_)
      val oldKeepFirstColumnCommentSetting: Boolean = getSettings.KEEP_FIRST_COLUMN_COMMENT
      doTextTest(
        s"""$contextBefore
           |${toContextIndent(comment)}
           |${toContextIndent(text)}
           |${toBodyIndent(body)}
           |$contextAfter
           |""".stripMargin)
      doTextTest(
        s"""$contextBefore
           |${toContextIndent(text)}
           |${toBodyIndent(comment)}
           |${toBodyIndent(body)}
           |$contextAfter
           |""".stripMargin)
      getSettings.KEEP_FIRST_COLUMN_COMMENT = false
      doTextTest(
        s"""$contextBefore
           |${toContextIndent(text)}
           |$comment
           |${toBodyIndent(body)}
           |$contextAfter
           |""".stripMargin,
        s"""$contextBefore
           |${toContextIndent(text)}
           |${toBodyIndent(comment)}
           |${toBodyIndent(body)}
           |$contextAfter
           |""".stripMargin
      )
      getSettings.KEEP_FIRST_COLUMN_COMMENT = true
      doTextTest(
        s"""$contextBefore
           |${toContextIndent(text)}
           |$comment
           |${toBodyIndent(body)}
           |$contextAfter
           |""".stripMargin)
      getSettings.KEEP_FIRST_COLUMN_COMMENT = oldKeepFirstColumnCommentSetting
    }

  // SCL-20166
  def testExtension(): Unit = doCommentsFormatTest("extension (c: Circle)")

  def testWith(): Unit = doCommentsFormatTest("class A extends B with")

  def testClass(): Unit = doCommentsFormatTest("class A:")

  def testClassWithParamenter(): Unit = doCommentsFormatTest("class C(x: Int):")

  def testClassExtends(): Unit = doCommentsFormatTest("class A extends B:")

  def testObject(): Unit = doCommentsFormatTest("object A:")

  def testEnum(): Unit = doCommentsFormatTest("enum Color:")

  def testNew(): Unit = doCommentsFormatTest("new A:")

  def testPackage(): Unit = doCommentsFormatTest("package p:")

  def testDef(): Unit = doCommentsFormatTest("def x =")

  def testVal(): Unit = doCommentsFormatTest("val x =")

  def testVar(): Unit = doCommentsFormatTest("var x =")

  def testLambda(): Unit = doCommentsFormatTest("var x = (a: Int) =>")

  def testContextFunction(): Unit = doCommentsFormatTest("type Executable[T] = ExecutionContext ?=>")

  def testGenerator(): Unit = doCommentsFormatTest("x <-")

  def testTry(): Unit = doCommentsFormatTest("try")

  def testCatch(): Unit = doCommentsFormatTest(
    """try
      |  ???
      |catch""".stripMargin)

  def testFinally(): Unit = doCommentsFormatTest(
    """try
      |  ???
      |finally""".stripMargin)

  def testWhile(): Unit = doCommentsFormatTest("while")

  def testWhileDo(): Unit = doCommentsFormatTest("while true do")

  def testWhileParens(): Unit = doCommentsFormatTest("while (true)")

  def testFor(): Unit = doCommentsFormatTest("for")

  def testForParens(): Unit = doCommentsFormatTest("for (x <- Seq(1,2))")

  def testForBraces(): Unit = doCommentsFormatTest("for {x <- Seq(1,2)}")

  def testForDo(): Unit = doCommentsFormatTest("for x <- Seq(1,2) do")

  def testForYield(): Unit = doCommentsFormatTest("for x <- Seq(1,2) yield")

  def testMatch(): Unit = doCommentsFormatTest("??? match")

  def testReturn(): Unit = doCommentsFormatTest("return")

  def testThrow(): Unit = doCommentsFormatTest("throw")

  def testIf(): Unit = doCommentsFormatTest("if")

  def testIfThen(): Unit = doCommentsFormatTest("if ??? then")

  def testIfParens(): Unit = doCommentsFormatTest("if (???)")

  def testElseIf(): Unit = doCommentsFormatTest(
    """if false then
      |  ???
      |else if""".stripMargin)

  def testElseIfThen(): Unit = doCommentsFormatTest(
    """if false then
      |  ???
      |else if true then""".stripMargin)

  def testElseIfParens(): Unit = doCommentsFormatTest(
    """if false then
      |  ???
      |else if (true)""".stripMargin)

  def testElse(): Unit = doCommentsFormatTest(
    """if false then
      |  ???
      |else""".stripMargin)
}