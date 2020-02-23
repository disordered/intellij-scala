package org.jetbrains.plugins.scala.lang.resolve2


/**
 * Pavel.Fatin, 02.02.2010
 */

class ImportScopeTest extends ResolveTestBase {
  override def folderPath: String = {
    super.folderPath + "import/scope/"
  }

  def testBlock(): Unit = doTest()
  def testInnerBlock(): Unit = doTest()
  def testOuterBlockNested(): Unit = doTest()
  def testOuterBlock(): Unit = doTest()
  def testTwoBlocks(): Unit = doTest()
}