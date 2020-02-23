package org.jetbrains.plugins.scala.lang.resolve2


/**
 * Pavel.Fatin, 02.02.2010
 */

class FunctionCountPriorityTest extends ResolveTestBase {
  override def folderPath: String = {
    super.folderPath + "function/count/priority/"
  }

  def testEmptyToAll(): Unit = doTest()
  def testNoneToAll(): Unit = doTest()
  def testOneToAll(): Unit = doTest()
  def testTwoToAll(): Unit = doTest()
}