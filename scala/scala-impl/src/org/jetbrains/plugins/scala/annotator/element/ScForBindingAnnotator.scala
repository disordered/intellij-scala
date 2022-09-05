package org.jetbrains.plugins.scala
package annotator
package element

import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.codeInsight.intention.preview.IntentionPreviewInfo
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.plugins.scala.ScalaBundle
import org.jetbrains.plugins.scala.codeInspection.caseClassParamInspection.RemoveValFromForBindingIntentionAction
import org.jetbrains.plugins.scala.lang.lexer.ScalaTokenTypes
import org.jetbrains.plugins.scala.lang.psi.api.expr.{ScForBinding, ScPatternedEnumerator}

object ScForBindingAnnotator extends ElementAnnotator[ScForBinding] {

  override def annotate(element: ScForBinding, typeAware: Boolean)
                       (implicit holder: ScalaAnnotationHolder): Unit = {
    element.valKeyword.foreach { valKeyword =>
      holder.createWarningAnnotation(
        valKeyword,
        ScalaBundle.message("enumerators.binding.val.keyword.deprecated"),
        ProblemHighlightType.LIKE_DEPRECATED,
        new RemoveValFromForBindingIntentionAction(element)
      )
    }

    // TODO: this is quite the same as ScGeneratorAnnotator.annotate has
    //  looks like the presentation (message and style) of these two errors is not the best, maybe rethink?
    element.caseKeyword.foreach { caseKeyword =>
      holder.createWarningAnnotation(
        caseKeyword,
        ScalaBundle.message("enumerators.binding.case.keyword.found"),
        ProblemHighlightType.GENERIC_ERROR,
        new RemoveCaseFromPatternedEnumeratorFix(element)
      )
    }
  }

  class RemoveCaseFromPatternedEnumeratorFix(enumerator: ScPatternedEnumerator) extends IntentionAction {

    override def getFamilyName: String = ScalaBundle.message("family.name.remove.case.from.enumerator")

    override def getText: String = ScalaBundle.message("remove.case")

    override def isAvailable(project: Project, editor: Editor, file: PsiFile): Boolean = true

    override def invoke(project: Project, editor: Editor, file: PsiFile): Unit = {
      if (!enumerator.isValid) return
      findCases(enumerator).foreach(_.delete())
    }

    override def startInWriteAction(): Boolean = true

    override def generatePreview(project: Project, editor: Editor, file: PsiFile): IntentionPreviewInfo = {
      val cases = findCases(PsiTreeUtil.findSameElementInCopy(enumerator, file))
      if (cases.isEmpty) IntentionPreviewInfo.EMPTY
      else {
        cases.foreach(_.delete())
        IntentionPreviewInfo.DIFF
      }
    }

    private def findCases(enumerator: ScPatternedEnumerator) = enumerator.findChildrenByType(ScalaTokenTypes.kCASE)
  }
}
