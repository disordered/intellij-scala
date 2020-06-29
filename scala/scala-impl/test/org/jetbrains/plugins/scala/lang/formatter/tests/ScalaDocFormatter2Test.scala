package org.jetbrains.plugins.scala.lang.formatter.tests

import org.jetbrains.plugins.scala.lang.formatter.AbstractScalaFormatterTestBase

class ScalaDocFormatter2Test extends AbstractScalaFormatterTestBase {

  // SCL-6599
  def testListsMix(): Unit = {
    scalaSettings.ENABLE_SCALADOC_FORMATTING = true
    val before =
      """/**
        | * description
        | *
        | *  1. item text
        | *item text
        | * item text
        | *          item text
        | *    I.      item text
        | * item text
        | *
        | *
        | *    I.    item
        | *  1. item
        | *      - item
        | *      -     item text
        | * item text
        | *
        | *  1.    item
        | */
        |class DocWithLists
        |""".stripMargin

    val after =
      """/**
        | * description
        | *
        | *  1. item text
        | *     item text
        | *     item text
        | *     item text
        | *    I. item text
        | *       item text
        | *
        | *
        | *    I. item
        | *  1. item
        | *      - item
        | *      - item text
        | *        item text
        | *
        | *  1. item
        | */
        |class DocWithLists
        |""".stripMargin

    doTextTest(before, after)
  }

  def testLists_WithMultilineNonLeafNodesInItemsContent(): Unit = {
    scalaSettings.ENABLE_SCALADOC_FORMATTING = true
    val before =
      """/**
        | *  - item 1 {{{some code}}}
        | *  - item 2 {{{
        | *     some code
        | *}}}
        | *  - item 3
        | *          {{{
        | *       multiline
        | *       code
        | *          }}}
        | *  - item 4 `
        | * multiline
        | *      monospace text
        | *       `
        | *  - item 4
        | *       - inner item
        | *         `
        | * multiline
        | *      monospace text
        | *       `
        | *         {{{
        | *     some code
        | * }}}
        | *         {{{
        | *       multiline
        | *       code
        | *    }}}
        | */
        |class DocWithLists""".stripMargin

    val after =
      """/**
        | *  - item 1 {{{some code}}}
        | *  - item 2 {{{
        | *     some code
        | * }}}
        | *  - item 3
        | * {{{
        | *       multiline
        | *       code
        | * }}}
        | *  - item 4 `
        | *    multiline
        | *    monospace text
        | *    `
        | *  - item 4
        | *       - inner item
        | *         `
        | *         multiline
        | *         monospace text
        | *         `
        | * {{{
        | *     some code
        | * }}}
        | * {{{
        | *       multiline
        | *       code
        | * }}}
        | */
        |class DocWithLists""".stripMargin

    doTextTest(before, after)
  }

  def testListsMix_ItemsFormattingDisabled(): Unit = {
    scalaSettings.SD_ALIGN_LIST_ITEM_CONTENT = false

    val before =
      """"
        |/**
        | * Description
        | *
        | * == header ==
        | *
        | * - list item 1
        | *   line 2
        | *  - list item 1.1
        | *    line 2
        | *  - list item 1.2
        | *    line 2
        | * 1. 1
        | * line 2
        | *  1.1 1.1 // not a list item (no space after)
        | *  line 2
        | * 2. 2
        | * i. 1
        | *    line 2
        | *  i. 1.1
        | * ii. 2
        | * A. 1
        | * A. 2
        | *   A. 3
        | *   A.A.A.A // not a list item (no space after)
        | *  line 2
        | * a. 1
        | *   a. 1.1
        | * a. 2
        | */
        |val a = 42
      """.stripMargin

    val after =
      """"
        |/**
        | * Description
        | *
        | * == header ==
        | *
        | * - list item 1
        | * line 2
        | *  - list item 1.1
        | * line 2
        | *  - list item 1.2
        | * line 2
        | * 1. 1
        | * line 2
        | * 1.1 1.1 // not a list item (no space after)
        | * line 2
        | * 2. 2
        | * i. 1
        | * line 2
        | *  i. 1.1
        | * ii. 2
        | * A. 1
        | * A. 2
        | *   A. 3
        | * A.A.A.A // not a list item (no space after)
        | * line 2
        | * a. 1
        | *   a. 1.1
        | * a. 2
        | */
        |val a = 42
      """.stripMargin

    doTextTest(before, after)
  }

  def testListsMix_ScalaDocFormattingDisabled(): Unit = {
    scalaSettings.ENABLE_SCALADOC_FORMATTING = false

    val before =
      """"
        |/**
        | *      Description
        | *
        | * == header ==
        | *
        | * - list item 1
        | *   line 2
        | *  - list item 1.1
        | *    line 2
        | *  - list item 1.2
        | *    line 2
        | * 1. 1
        | * line 2
        | *  1.1 1.1 // not a list item (no space after)
        | *  line 2
        | * 2. 2
        | * i. 1
        | *    line 2
        | *  i. 1.1
        | * ii. 2
        | * A. 1
        | * A. 2
        | *   A. 3
        | *   A.A.A.A // not a list item (no space after)
        | *  line 2
        | * a. 1
        | *   a. 1.1
        | * a. 2
        | */
        |val a = 42
      """.stripMargin

    doTextTest(before, before)
  }

  def testParamTag(): Unit =
    doTextTest(
      """/**
        | *   @param      x      description
        | */""".stripMargin,
      """/**
        | * @param x description
        | */""".stripMargin
    )

  def testParamTag_1(): Unit =
    doTextTest(
      """/**
        | *   @param      x      description 1
        | *   @param    xxxxx      description 2
        | */""".stripMargin,
      """/**
        | * @param x     description 1
        | * @param xxxxx description 2
        | */""".stripMargin
    )


  def testListMix_InsideTags(): Unit =
    doTextTest(
      """/**
        | * @param x some text
        | *          - item
        | *          - item
        | *               i. item
        | *               i. item
        | *
        | *          another text
        | * @define macro1 some text
        | *                - item
        | *                - item
        | *                  i. item
        | *                  i. item
        | *
        | *                another text
        | */
        |class Parent""".stripMargin
    )

  def testParamTag_WithParagraphsAndLists(): Unit =
    doTextTest(
      """/**
        | * @param x description 1
        | *          description 2
        | *       description 3
        | *                   description 4
        | *
        | *                   description 5
        | *                   description 6
        | *          - item 1
        | *          - item 2
        | *            - item inner 1
        | *
        | */""".stripMargin,
      """/**
        | * @param x description 1
        | *          description 2
        | *          description 3
        | *          description 4
        | *
        | *          description 5
        | *          description 6
        | *          - item 1
        | *          - item 2
        | *            - item inner 1
        | *
        | */""".stripMargin
    )

  def testFixCommentEndIndentAfterEmptyTag(): Unit =
    doTextTest(
      """/**
        | * @note
        |   */
        |class A {
        |
        |  /**
        |   * @note
        |     */
        |  class B
        |
        |}""".stripMargin,
      """/**
        | * @note
        | */
        |class A {
        |
        |  /**
        |   * @note
        |   */
        |  class B
        |
        |}""".stripMargin
    )


}