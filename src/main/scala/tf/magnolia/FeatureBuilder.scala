package tf.magnolia

import org.tensorflow.example._
import magnolia._

import scala.language.experimental.macros

trait FeatureBuilder[T] {
  def toFeatures(record: T, nameOrPrefix: Option[String]): Features.Builder
  def fromFeatures(features: Features, nameOrPrefix: Option[String]): T
}

object FeatureBuilder {
  type Typeclass[T] = FeatureBuilder[T]

  def combine[T](caseClass: CaseClass[FeatureBuilder, T]): FeatureBuilder[T] =
    new FeatureBuilder[T] {
      override def toFeatures(record: T, nameOrPrefix: Option[String]): Features.Builder = {
        caseClass.parameters.foldLeft(Features.newBuilder()) { (fb, param) =>
          val newPrefix = nameOrPrefix.fold(param.label)(prefix => s"$prefix.${param.label}")
          fb.putAllFeature(param.typeclass.toFeatures(param.dereference(record), Some(newPrefix))
            .getFeatureMap)
          fb
        }
      }

      override def fromFeatures(features: Features, nameOrPrefix: Option[String]): T = {
        caseClass.construct { param =>
          val newPrefix = nameOrPrefix.fold(param.label)(prefix => s"$prefix.${param.label}")
          param.typeclass.fromFeatures(features, Some(newPrefix))
        }
      }
    }

  def dispatch[T](sealedTrait: SealedTrait[Typeclass, T]): FeatureBuilder[T] = ???

  implicit def gen[T]: Typeclass[T] = macro Magnolia.gen[T]
}
