package org.jetbrains.plugins.scala.lang.resolveSemanticDb

import com.intellij.openapi.util.text.StringUtil
import com.intellij.util.ThrowableRunnable
import org.jetbrains.plugins.scala.decompiler.scalasig.ScalaSigPrinter.StringFixes
import org.jetbrains.plugins.scala.lang.resolveSemanticDb.ReferenceComparisonTestBase.Result
import org.jetbrains.plugins.scala.util.TestUtils

import java.nio.file.{Files, Path, Paths}
import scala.collection.mutable
import scala.jdk.CollectionConverters.IteratorHasAsScala

object ReferenceComparisonTestsGenerator {
  val excluded: Set[String] = Set(
    "large", "large2" // they're just very large with ~10k references/definitions
  )

  val testOutputPath: Path =
    Paths.get(TestUtils.findCommunityRoot())
      .resolve("scala/scala-impl/test/org/jetbrains/plugins/scala/lang/resolveSemanticDb/generated/ReferenceComparisonTest.scala")

  def main(args: Array[String]): Unit = {
    run()
    System.exit(0)
  }

  def run(): Unit = {

    val builder = new StringBuilder

    builder ++=
      """
        |package org.jetbrains.plugins.scala.lang.resolveSemanticDb
        |package generated
        |
        |//noinspection NameBooleanParameters
        |class ReferenceComparisonTest extends ReferenceComparisonTestBase {
        |""".stripMargin

    var cases = 0
    var successes = 0
    var result = Result.empty


    val testOutPaths = Files.list(ComparisonTestBase.outPath).iterator().asScala.toSeq
      .sortBy(_.getFileName.toString)((x, y) => StringUtil.naturalCompare(x, y))

    val originalTestNames = testOutPaths.map(_.getFileName.toString).toSet
    val usedTestNames = mutable.Set.empty[String]

    for {
      testOutPath <- testOutPaths
      testName = testOutPath.getFileName.toString
      if !excluded(testName)
    } {

      val test: ReferenceComparisonTestBase = new ReferenceComparisonTestBase {
        override  def runTestRunnable(testRunnable: ThrowableRunnable[Throwable]): Unit = {
          cases += 1
          val res = runTestToResult(testName)
          val success = res.problems.isEmpty
          if (success)
            successes += 1
          result += res

          val normalizedTestName = testName.replace('-', '_')
          val finalTestName =
            if (usedTestNames.contains(normalizedTestName) || originalTestNames.contains(normalizedTestName))
              testName
            else normalizedTestName
          usedTestNames += finalTestName
          val testId = s"test_$finalTestName".escapeNonIdentifiers
          builder ++= raw"""  def $testId(): Unit = doTest("$testName", $success)"""
          builder += '\n'

          val progress = cases.toDouble / testOutPaths.size.toDouble * 100
          val successRate = (successes.toDouble / cases.toDouble) * 100
          println(
            s"(${progress.toInt}%) " +
              s"$testName: $success (${successRate.toInt}% $successes/$cases) " +
              s"| problems: ${result.problems.size} " +
              s"| refs: ${result.refCount} " +
              s"| failed to resolve: ${result.failedToResolve} (${(result.failedToResolve.toDouble / result.refCount * 100.0).toInt}%) " +
              s"| not tested: ${result.refCount - result.failedToResolve - result.testedRefs}) " +
              s"| complete correct: ${result.completeCorrect} " +
              s"| partial correct: ${result.partialCorrect} " +
              s"| incorrect resolve: ${result.incorrectResolves}"
          )
        }
      }

      test.run()
    }

    builder ++= "}\n"

    println("Print...")
    Files.writeString(testOutputPath, builder)
    println("Done.")
  }
}