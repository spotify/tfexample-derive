package tf.magnolia.core

import com.google.protobuf.ByteString
import org.tensorflow.example._
import magnolia._

import scala.language.experimental.macros
import scala.collection.JavaConverters._

object ExampleConverter {
  type Typeclass[T] = FeatureBuilder[T]

  def apply[T](implicit conv: ExampleConverter[T]): ExampleConverter[T] = conv

  trait FeatureBuilder[T] {
    def toFeatures(record: T): Features.Builder
  }
  object FeatureBuilder {
    def of[T]: FeatureBuilderFrom[T] = new FeatureBuilderFrom[T]
  }

  class FeatureBuilderFrom[T] {
    def apply[U](f: T => U)(implicit fb: FeatureBuilder[U]): FeatureBuilder[T] =
      new FeatureBuilder[T] {
        override def toFeatures(record: T): Features.Builder = fb.toFeatures(f(record))
      }
  }

  def combine[T](caseClass: CaseClass[FeatureBuilder, T]): FeatureBuilder[T] = (record: T) => {
    val features = caseClass.parameters.map { p =>
      p.repeated
      composeWithName(p.label, p.typeclass).toFeatures(p.dereference(record))
    }
    features.reduce { (fb1, fb2) =>
      fb1.putAllFeature(fb2.getFeatureMap)
      fb1
    }
  }

  def dispatch[T](sealedTrait: SealedTrait[Typeclass, T]): Typeclass[T] = ???

  implicit def gen[T]: Typeclass[T] = macro Magnolia.gen[T]

  private def composeWithName[T](name: String, instance: FeatureBuilder[T]): FeatureBuilder[T] =
    (record: T) => {
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
        // TODO: fix
        val fMap = features.getFeatureMap.asScala.map { case (fName, f) =>
          (s"${name}_$fName", f)
        }.asJava
        Features.newBuilder().putAllFeature(fMap)
      }
    }

  implicit def iterableLongConverter[T <: Iterable[Long]]: FeatureBuilder[T] = { longs =>
    val jLongs = longs.asJava.asInstanceOf[java.lang.Iterable[java.lang.Long]]
    featuresOf(Feature.newBuilder()
      .setInt64List(
        Int64List.newBuilder()
          .addAllValue(jLongs))
      .build)
  }

  implicit def iterableIntConverter[T <: Iterable[Int]]: FeatureBuilder[T] = ints =>
    iterableLongConverter.toFeatures(ints.map(_.toLong))

  implicit def iterableFloatConverter[T <: Iterable[Float]]: FeatureBuilder[T] = { floats =>
    val jFloats = floats.asJava.asInstanceOf[java.lang.Iterable[java.lang.Float]]
    featuresOf(Feature.newBuilder()
      .setFloatList(FloatList.newBuilder().addAllValue(jFloats))
      .build)
  }

  implicit def iterableBytesConverter[T <: Iterable[ByteString]]: FeatureBuilder[T] = { bytes =>
    val jBytes = bytes.asJava
    featuresOf(Feature.newBuilder()
      .setBytesList(BytesList.newBuilder().addAllValue(jBytes))
      .build)
  }

  implicit def iterableStringConverter[T <: Iterable[String]]: FeatureBuilder[T] = strings =>
    iterableBytesConverter.toFeatures(strings.map(ByteString.copyFromUtf8))

  def singletonConverter[T](implicit iterableConverter: FeatureBuilder[Iterable[T]])
  : FeatureBuilder[T] = (item: T) => iterableConverter.toFeatures(Seq(item))

  implicit def singletonLongConverter[T <: Long]: FeatureBuilder[T] = singletonConverter
  implicit def singletonIntConverter[T <: Int]: FeatureBuilder[T] = singletonConverter
  implicit def singletonFloatConverter[T <: Float]: FeatureBuilder[T] = singletonConverter
  implicit def singletonByteStringConverter[T <: ByteString]: FeatureBuilder[T] = singletonConverter
  implicit def singletonStringConverter[T <: String]: FeatureBuilder[T] = singletonConverter

  implicit def toExampleConverter[T](implicit fb: FeatureBuilder[T]): ExampleConverter[T] =
    new ExampleConverter[T] {
      override def toExample(record: T): Example = {
        Example.newBuilder()
          .setFeatures(fb.toFeatures(record))
          .build()
      }
    }

  private def featuresOf(feature: Feature): Features.Builder =
    Features.newBuilder().putFeature("", feature)

}

trait ExampleConverter[T] {
  def toExample(record: T): Example
//  def fromExample(example: Example): T
}
