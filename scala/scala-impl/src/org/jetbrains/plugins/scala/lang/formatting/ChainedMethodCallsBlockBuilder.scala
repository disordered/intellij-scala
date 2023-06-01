package org.jetbrains.plugins.scala.lang.formatting

import com.intellij.formatting._
import com.intellij.lang.ASTNode
import com.intellij.psi._
import com.intellij.psi.codeStyle.{CodeStyleSettings, CommonCodeStyleSettings}
import org.jetbrains.plugins.scala.extensions._
import org.jetbrains.plugins.scala.lang.formatting.ChainedMethodCallsBlockBuilder._
import org.jetbrains.plugins.scala.lang.formatting.getDummyBlocksUtils._
import org.jetbrains.plugins.scala.lang.formatting.settings.ScalaCodeStyleSettings
import org.jetbrains.plugins.scala.lang.lexer.ScalaTokenTypes._
import org.jetbrains.plugins.scala.lang.parser.ScalaElementType
import org.jetbrains.plugins.scala.lang.psi.api.expr._

import java.util
import scala.annotation.{nowarn, tailrec}

private final class ChainedMethodCallsBlockBuilder(
  block: ScalaBlock,
  settings: CodeStyleSettings,
  commonSettings: CommonCodeStyleSettings,
  scalaSettings: ScalaCodeStyleSettings
) extends ScalaBlockBuilderBase(block, settings, commonSettings, scalaSettings) {

  private val chainAlignment = if (cs.ALIGN_MULTILINE_CHAINED_METHODS) Alignment.createAlignment() else null
  private val chainWrap = block.suggestedWrap
  private val smartIndent = Indent.getSmartIndent(Indent.Type.CONTINUATION, false)

  def buildSubBlocks(node: ASTNode): util.List[ScalaBlock] = {
    val result = new util.ArrayList[ScalaBlock]

    collectChainedMethodCalls(node)(result)

    //We need to sort blocks because we add them in arbitrary order
    util.Collections.sort(result, util.Comparator.comparingInt[ScalaBlock](_.node.getTextRange.getStartOffset))

    result
  }

  /**
   * @param delegatedChildren can contain arguments, type arguments and comments between them
   */
  @tailrec//TODO
  private def collectChainedMethodCalls(
    node: ASTNode,
    dotFollowedByNewLine: Boolean = false,
    delegatedChildren: List[ASTNode] = List(),
    delegatedContext: Map[ASTNode, SubBlocksContext] = Map(),
  )(implicit result: java.util.List[ScalaBlock]): Unit = {
    val psi = node.getPsi
    if (canContainMethodCallChain(psi) || psi.is[ScGenericCall]) {
      //continue
    }
    else {
      result.add(subBlock(node))
      for (child <- delegatedChildren.filter(isNotEmptyNode)) {
        result.add(subBlock(child))
      }
      return
    }

    val childrenAll = node.getChildren(null)
    val childrenNonEmpty = childrenAll.filter(isNotEmptyNode).toList

    /**
     * some edge cases with comments in the middle of a method call: {{{
     *   Seq(1) // comment
     *     .map(_ * 2)
     *
     *   foo // comment
     *   {}
     * }}}
     */
    val (comments, children) = childrenNonEmpty.partition(isComment)

    lazy val delegatedChildrenSorted = sortByStartOffset(delegatedChildren)

    lazy val delegatedChildrenNotAlreadyInSomeContext = {
      // using Set we imply that ASTNode equals and hashCode methods are lightweight (default implementation)
      val filterOutNodes = delegatedContext.values.flatMap(_.additionalNodes).toSet
      delegatedChildrenSorted.filterNot(filterOutNodes.contains)
    }

    //Example of recursive descent:
    //value.method1.method2[String](1, 2, 3)
    //|---------------------------||-------|
    //|-------------------||------||~~~~~~~| delegated: args `(1, 2, 3)`
    //|-----------|.|-----||~~~~~~||~~~~~~~| delegated: args `(1, 2, 3)` and type args `[String]`
    //|---|.|-----|.|-----||~~~~~~||~~~~~~~| delegated: args `(1, 2, 3)` and type args `[String]`
    children match {
      case expr :: Nil =>
        val actualAlignment = if (dotFollowedByNewLine) chainAlignment else null
        val context = SubBlocksContext.withChild(expr, delegatedChildrenNotAlreadyInSomeContext, None, delegatedContext)
        result.add(subBlock(expr, delegatedChildrenSorted.lastOption.orNull, actualAlignment, None, None, Some(context)))

      //caller(args)
      //expr.method1[String](1, 2, 3).method2[Int, String](4, 5, 6)
      //|--------------------caller----------------------||-args--|
      case caller :: args :: Nil if args.getElementType == ScalaElementType.ARG_EXPRS =>
        val delegatedChildrenNew = args :: delegatedChildren ++ comments
        collectChainedMethodCalls(caller, dotFollowedByNewLine, delegatedChildrenNew, delegatedContext)

      //caller[typeArgs]
      //expr.method1[String](1, 2, 3).method2[Int, String](4, 5, 6)
      //|--------------caller---------------||-typeArgs--|
      case caller :: typeArgs :: Nil if typeArgs.getElementType == ScalaElementType.TYPE_ARGS =>
        val delegatedChildrenNew = typeArgs :: delegatedChildren ++ comments
        collectChainedMethodCalls(caller, dotFollowedByNewLine, delegatedChildrenNew, Map(typeArgs -> new SubBlocksContext(sortByStartOffset(delegatedChildren))))

      //expr.method1[String](1, 2, 3).method2[Int, String](4, 5, 6)
      //|------------expr-----------|.|-id--|
      case expr :: dot :: id :: Nil if dot.getElementType == tDOT =>
        //NOTE: we shadow `dotFollowedByNewLine` parameter, cause here we are interested in the new dot
        val dotFollowedByNewLine = dot.getPsi.followedByNewLine()

        val splitAtNode = if (dotFollowedByNewLine) id else dot

        assert(childrenNonEmpty.head.eq(expr), "assuming that first child is expr and comments can't go before it")
        val (nodesOnPrevLine, nodesOnNextLine) = childrenNonEmpty.tail.span(c => {
          //if chain part starts with a comment, include it into block:
          //value
          //  /*comment*/.map(x => x)
          val split = (c eq splitAtNode) || c.getPsi.startsFromNewLine(false)
          !split
        })

        //add whatever goes on previous line to a separate block
        //Example 1 (here `//comment` goes to separate block)
        //  seq //comment
        //    .map(x => x)
        //Example 2 (here `. //comment` goes to separate block)
        //  seq.//comment
        //    map(x => x)
        val chainCallIsSplitToTwoBlocks = nodesOnPrevLine.nonEmpty && nodesOnNextLine.nonEmpty
        if (chainCallIsSplitToTwoBlocks) {
          result.add(subBlock(nodesOnPrevLine.head, nodesOnPrevLine.lastOption.orNull, null, Some(Indent.getContinuationIndent), Some(null), None))
        }

        val context = SubBlocksContext.withChild(id, delegatedChildrenNotAlreadyInSomeContext, None, delegatedContext)
        val nodes = (if (nodesOnNextLine.nonEmpty) nodesOnNextLine else nodesOnPrevLine) ++ delegatedChildrenSorted
        result.add(subBlock(nodes.head, nodes.lastOption.orNull, chainAlignment, Some(smartIndent), Some(chainWrap), Some(context)))

        collectChainedMethodCalls(expr, dotFollowedByNewLine)
      case _ =>
        val childrenWithDelegated = children ++ delegatedChildren
        for (child <- childrenWithDelegated.filter(isNotEmptyNode)) {
          result.add(subBlock(child))
        }
    }
  }
}


object ChainedMethodCallsBlockBuilder {
  @inline
  private def sortByStartOffset(nodes: Seq[ASTNode]): Seq[ASTNode] = nodes.sortBy(_.getTextRange.getStartOffset)

  def canContainMethodCallChain(psi: PsiElement): Boolean =
    psi match {
      case _: ScReferenceExpression |
           _: ScThisReference |
           _: ScSuperReference |
           _: ScMethodCall => true
      case _ => false
    }
}