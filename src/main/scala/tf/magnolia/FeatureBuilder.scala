package tf.magnolia

import java.util.function.Predicate

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

  def dispatch[T](sealedTrait: SealedTrait[Typeclass, T]): FeatureBuilder[T] =
    new FeatureBuilder[T] {
    override def toFeatures(record: T, nameOrPrefix: Option[String]): Features.Builder =
      sealedTrait.dispatch(record) { subtype =>
        val name = s"${subtype.typeName.short}"
        val newPrefix = nameOrPrefix.fold(name)(n => s"$name#$n")
        subtype.typeclass.toFeatures(subtype.cast(record), Some(newPrefix))
      }

    override def fromFeatures(features: Features, nameOrPrefix: Option[String]): T = {
      // Note: this is hardcoded to only support Option for now, to minimize complexity. This
      // may change in the future.
      val subtypes = sealedTrait.subtypes.map(tpe => tpe.typeName.full -> tpe).toMap
      val namePrefix = s"Some#${nameOrPrefix.getOrElse("")}"
      if (subtypes.contains("scala.Some") && subtypes.contains("scala.None")) {
        val keys = features.getFeatureMap.keySet()
        if (keys.stream().anyMatch(new Predicate[String] {
          override def test(t: String): Boolean = t.startsWith(namePrefix)
        })) {
          subtypes("scala.Some").typeclass.fromFeatures(features, Some(namePrefix))
        }
        else {
          None.asInstanceOf[T]
        }
      }
      else ???
    }
  }

  implicit def gen[T]: Typeclass[T] = macro Magnolia.gen[T]
}
