package org.jetbrains.plugins.scala.lang.scaladoc.editor.enter

import com.intellij.openapi.actionSystem.IdeActions
import com.intellij.openapi.editor.actionSystem.EditorActionManager
import org.jetbrains.plugins.scala.ScalaFileType
import org.jetbrains.plugins.scala.base.ScalaLightCodeInsightFixtureTestCase
import org.jetbrains.plugins.scala.extensions.StringExt
import org.jetbrains.plugins.scala.lang.actions.ActionTestBase.MyDataContext
import org.jetbrains.plugins.scala.util.{ShortCaretMarker, WriteCommandActionEx}

// TODO: unify with org.jetbrains.plugins.scala.lang.actions.AbstractActionTestBase ?
abstract class ScalaDocEnterTestBase extends ScalaLightCodeInsightFixtureTestCase
  with ShortCaretMarker{

  protected def editor = getEditor
  protected def file = getFile

  override protected def setUp(): Unit = {
    super.setUp()
    val scalaSettings = getScalaCodeStyleSettings
    scalaSettings.USE_SCALADOC2_FORMATTING = false // some tests have intentionally broken scaladoc formatting
  }

  protected def doTest(before: String, expectedAfter: String, stripTrailingSpaces: Boolean = true): Unit = {
    configureFromFileText(ScalaFileType.INSTANCE, before.withNormalizedSeparator)

    val handler = EditorActionManager.getInstance.getActionHandler(IdeActions.ACTION_EDITOR_ENTER)
    WriteCommandActionEx.runWriteCommandAction(getProject, () => {
      handler.execute(editor, editor.getCaretModel.getCurrentCaret, new MyDataContext(file))
    })

    myFixture.checkResult(expectedAfter.withNormalizedSeparator, stripTrailingSpaces)
  }
}