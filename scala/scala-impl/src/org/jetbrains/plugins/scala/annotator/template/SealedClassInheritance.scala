package org.jetbrains.plugins.scala
package annotator
package template

import com.intellij.lang.annotation.AnnotationHolder
import org.jetbrains.plugins.scala.extensions.PsiClassExt
import org.jetbrains.plugins.scala.lang.psi.api.ScalaFile
import org.jetbrains.plugins.scala.lang.psi.api.expr.ScNewTemplateDefinition
import org.jetbrains.plugins.scala.lang.psi.api.toplevel.typedef.{ScTemplateDefinition, ScTypeDefinition}

/**
  * Pavel Fatin
  */
object SealedClassInheritance extends TemplateDefinitionAnnotatorPart {

  def annotate(definition: ScTemplateDefinition,
               holder: AnnotationHolder,
               typeAware: Boolean): Unit = definition.getContainingFile match {
    case file: ScalaFile if !file.isCompiled =>
      val references = definition match {
        case templateDefinition: ScNewTemplateDefinition if templateDefinition.extendsBlock.templateBody.isEmpty => Nil
        case _ => superRefs(definition)
      }
      val fileNavigationElement = file.getNavigationElement

      references.collect {
        case (range, definition: ScTypeDefinition)
          if definition.isSealed && definition.getContainingFile.getNavigationElement != fileNavigationElement =>
          (range, ScalaBundle.message("illegal.inheritance.from.sealed.kind", kindOf(definition, toLowerCase = true), definition.name))
      }.foreach {
        case (range, message) =>
          holder.createErrorAnnotation(range, message)
      }
    case _ =>
  }
}