package tf.magnolia.core

import java.net.URI
import java.util

import com.google.protobuf.ByteString
import org.tensorflow.example._
import magnolia._
import java.util.{Map => JMap}

import scala.language.experimental.macros
import scala.collection.JavaConverters._

object ExampleConverter {
  type Typeclass[T] = FeaturesBuilder[T]


  trait FeaturesBuilder[T] {
    def toFeatures(record: T): JMap[String, Feature]
  }

  def apply[T](implicit conv: ExampleConverter[T]): ExampleConverter[T] = conv

  def combine[T](caseClass: CaseClass[FeaturesBuilder, T]): FeaturesBuilder[T] = (record: T) => {
    val examples = caseClass.parameters.map { p =>
      composeWithName(p.label, p.typeclass).toFeatures(p.dereference(record))
    }
    // TODO: check size and optimize?
    examples.reduce((m1, m2) => { m1.putAll(m2); m1 })
  }

  def dispatch[T](sealedTrait: SealedTrait[Typeclass, T]): Typeclass[T] = ???

  implicit def gen[T]: Typeclass[T] = macro Magnolia.gen[T]

  private def composeWithName[T](name: String, instance: FeaturesBuilder[T]): FeaturesBuilder[T] =
    (record: T) => {
      val features = instance.toFeatures(record)
      // If the parameter is -not- a nested case class, it should be treated as an anonymous
      // (un-named) single feature. In this case, we name the feature using the param name.
      // Instead of checking whether the param is a case class directly (via reflection),
      // we can instead just check whether the generated features already have names or not.
      // If the parameter -is- a nested case class, we prepend its feature names with this parameter
      // name to prevent potential ambiguities.
      if (features.size() == 1 && features.containsKey("")) {
        val feature = features.remove("")
        features.put(name, feature)
        features
      }
      else {
        // TODO: fix
        val fMap = features.asScala.map { case (fName, f) =>
          (s"${name}_$fName", f)
        }.asJava
        fMap
      }
    }

  implicit def iterableLongConverter[T <: Iterable[Long]]: FeaturesBuilder[T] = { longs =>
    val jLongs = longs.asJava.asInstanceOf[java.lang.Iterable[java.lang.Long]]
    anonMapOf(Feature.newBuilder()
      .setInt64List(
        Int64List.newBuilder()
          .addAllValue(jLongs))
      .build)
  }

  implicit def iterableIntConverter[T <: Iterable[Int]]: FeaturesBuilder[T] = ints =>
    iterableLongConverter.toFeatures(ints.map(_.toLong))

  implicit def iterableFloatConverter[T <: Iterable[Float]]: FeaturesBuilder[T] = { floats =>
    val jFloats = floats.asJava.asInstanceOf[java.lang.Iterable[java.lang.Float]]
    anonMapOf(Feature.newBuilder()
      .setFloatList(FloatList.newBuilder().addAllValue(jFloats))
      .build)
  }

  implicit def iterableBytesConverter[T <: Iterable[ByteString]]: FeaturesBuilder[T] = { bytes =>
    val jBytes = bytes.asJava
    anonMapOf(Feature.newBuilder()
      .setBytesList(BytesList.newBuilder().addAllValue(jBytes))
      .build)
  }

  implicit def iterableStringConverter[T <: Iterable[String]]: FeaturesBuilder[T] = strings =>
    iterableBytesConverter.toFeatures(strings.map(ByteString.copyFromUtf8))

  def singletonConverter[T](implicit iterableConverter: FeaturesBuilder[Iterable[T]])
  : FeaturesBuilder[T] = (item: T) => iterableConverter.toFeatures(Seq(item))

  implicit def singletonLongConverter[T <: Long]: FeaturesBuilder[T] = singletonConverter
  implicit def singletonIntConverter[T <: Int]: FeaturesBuilder[T]  = singletonConverter
  implicit def singletonFloatConverter[T <: Float]: FeaturesBuilder[T]  = singletonConverter
  implicit def singletonByteStringConverter[T <: ByteString]: FeaturesBuilder[T]  = singletonConverter
  implicit def singletonStringConverter[T <: String]: FeaturesBuilder[T]  = singletonConverter

  implicit def toExampleConverter[T](implicit fb: FeaturesBuilder[T]): ExampleConverter[T] =
  new ExampleConverter[T] {
    override def toExample(record: T): Example = {
      Example.newBuilder()
        .setFeatures(
          Features.newBuilder()
            .putAllFeature(fb.toFeatures(record)))
        .build()
    }
  }

  private def anonMapOf(feature: Feature): JMap[String, Feature] = {
    //TODO: guava?
    val map = new util.HashMap[String, Feature]()
    map.put("", feature)
    map
  }
}

trait ExampleConverter[T] {
  def toExample(record: T): Example
//  def fromExample(example: Example): T
}
