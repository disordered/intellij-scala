package org.jetbrains.plugins.scala.lang.surroundWith.surrounders.expression

import com.intellij.lang.ASTNode
import com.intellij.lang.surroundWith.Surrounder
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.psi.{PsiDocumentManager, PsiElement, PsiWhiteSpace}
import org.jetbrains.plugins.scala.editor.DocumentExt
import org.jetbrains.plugins.scala.extensions.{PsiElementExt, StringExt}
import org.jetbrains.plugins.scala.lang.lexer.ScalaTokenTypes
import org.jetbrains.plugins.scala.lang.psi.ScalaPsiUtil
import org.jetbrains.plugins.scala.lang.psi.api.expr._
import org.jetbrains.plugins.scala.lang.psi.api.statements._
import org.jetbrains.plugins.scala.lang.psi.impl.ScalaPsiElementFactory
import org.jetbrains.plugins.scala.project.ProjectContext

/**
 * Surrounds an expression and return an expression
 */
abstract class ScalaExpressionSurrounder extends Surrounder {

  import ScalaTokenTypes.{COMMENTS_TOKEN_SET, tSEMICOLON => Semicolon}

  def isApplicable(element: PsiElement): Boolean = element match {
    case _: ScExpression |
         _: PsiWhiteSpace |
         _: ScValue |
         _: ScVariable |
         _: ScFunction |
         _: ScTypeAlias => true
    case _ if ScalaPsiUtil.isLineTerminator(element) => true
    case _ =>
      element.getNode.getElementType match {
        case Semicolon => true
        case elementType => COMMENTS_TOKEN_SET.contains(elementType)
      }
  }

  override def isApplicable(elements: Array[PsiElement]): Boolean =
    elements.forall(isApplicable)

  override def surroundElements(project: Project, editor: Editor, elements: Array[PsiElement]): TextRange =
    getSurroundSelectionRange(editor, surroundedNode(elements))

  def surroundedNode(elements: Array[PsiElement]): ASTNode = {
    val result = surroundPsi(elements).getNode
    var childNode: ASTNode = null

    for {
      child <- elements
      nextNode = child.getNode
      parentNode = nextNode.getTreeParent
    } {
      val flag = childNode == null
      childNode = nextNode

      if (flag) parentNode.replaceChild(childNode, result)
      else parentNode.removeChild(childNode)
    }

    result
  }

  protected def needParenthesis(element: PsiElement): Boolean = element.getParent match {
    case _: ScSugarCallExpr |
         _: ScReferenceExpression => true
    case _ => false
  }

  protected final def surroundPsi(elements: Array[PsiElement]): ScExpression = {
    val element = elements.head

    val text = getTemplateAsString(elements)
      .parenthesize(needParenthesis = elements.length == 1 && this.needParenthesis(element))

    implicit val context: ProjectContext = element.projectContext
    ScalaPsiElementFactory.createExpressionFromText(text, element)
  }

  def getTemplateAsString(elements: Array[PsiElement]): String =
    elements.map(_.getNode.getText).mkString

  def getSurroundSelectionRange(editor: Editor, node: ASTNode): TextRange

  protected def unwrapParenthesis(node: ASTNode): Option[PsiElement] = node.getPsi match {
    case p: ScParenthesisedExpr => p.innerElement
    case e => Option(e)
  }

  protected def unblockDocument(editor: Editor): Unit =
    PsiDocumentManager.getInstance(editor.getProject)
      .doPostponedOperationsAndUnblockDocument(editor.getDocument)

  protected def deleteText(editor: Editor, range: TextRange): Unit = {
    val document = editor.getDocument
    document.deleteString(range.getStartOffset, range.getEndOffset)
    document.commit(editor.getProject)
  }

  protected def deleteText(editor: Editor, node: ASTNode): Unit = {
    unblockDocument(editor)
    deleteText(editor, node.getTextRange)
  }
}
