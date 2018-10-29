package tf.magnolia.core

import org.tensorflow.example._
import magnolia._

import scala.language.experimental.macros
import scala.collection.JavaConverters._

trait FeatureBuilder[T] {
  def toFeatures(record: T): Features.Builder
  def fromFeatures(features: Features): T
}

object FeatureBuilder {
  type Typeclass[T] = FeatureBuilder[T]

  def combine[T](caseClass: CaseClass[FeatureBuilder, T]): FeatureBuilder[T] =
    new FeatureBuilder[T] {
      override def toFeatures(record: T): Features.Builder = {
        val features = caseClass.parameters.map { p =>
          composeWithName(p.label, p.typeclass).toFeatures(p.dereference(record))
        }
        features.reduce { (fb1, fb2) =>
          fb1.putAllFeature(fb2.getFeatureMap)
          fb1
        }
      }

      override def fromFeatures(fs: Features): T = {
        val fsMap = fs.getFeatureMap

        caseClass.construct { param =>
          val key = param.label
          val fb = Features.newBuilder()
          if (fsMap.containsKey(key)) {
            // this is a primitive field
            fb.putAllFeature(Map(key -> fsMap.get(key)).asJava)
            param.typeclass.fromFeatures(fb.build())
          } else {
            // this is a nested field
            // construct a new features map with keys starting with param.label
            val keys = fsMap.keySet().asScala.filter(_.startsWith(key))
            keys.foreach { key =>
              val newKey = key.substring(key.length + 1)
              fb.putFeature(newKey, fsMap.get(key))
            }
            param.typeclass.fromFeatures(fb.build())
          }
        }
      }
    }

  def dispatch[T](sealedTrait: SealedTrait[Typeclass, T]): Typeclass[T] = ???

  implicit def gen[T]: Typeclass[T] = macro Magnolia.gen[T]

  private def composeWithName[T](name: String, instance: FeatureBuilder[T]): FeatureBuilder[T] =
    new FeatureBuilder[T] {
      override def toFeatures(record: T): Features.Builder = {
        val features = instance.toFeatures(record)
        // If the parameter is -not- a nested case class, it should be treated as an anonymous
        // (un-named) single feature. In this case, we name the feature using the param name.
        // Instead of checking whether the param is a case class directly (via reflection),
        // we can instead just check whether the generated features already have names or not.
        // If the parameter -is- a nested case class, we prepend its feature names with this parameter
        // name to prevent potential ambiguities.
        if (features.getFeatureCount == 1 && features.containsFeature("")) {
          val feature = features.getFeatureOrThrow("")
          features.removeFeature("")
          features.putFeature(name, feature)
          features
        }
        else {
          // TODO: do we really need scala conversion?
          val fMap = features.getFeatureMap.asScala.map { case (fName, f) =>
            (s"${name}.$fName", f)
          }.asJava
          Features.newBuilder().putAllFeature(fMap)
        }
      }

      override def fromFeatures(features: Features): T = instance.fromFeatures(features)
    }
}
