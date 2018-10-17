package tf.magnolia.core

import com.google.protobuf.ByteString
import org.tensorflow.example._
import magnolia._

import scala.language.experimental.macros
import scala.collection.JavaConverters._

object ExampleConverter {
  type Typeclass[T] = ExampleConverter[T]

  def apply[T](implicit conv: ExampleConverter[T]): ExampleConverter[T] = conv

  def combine[T](caseClass: CaseClass[ExampleConverter, T]): ExampleConverter[T] = (record: T) => {
    val examples = caseClass.parameters.map { p =>
      composeWithName(p.label, p.typeclass).toExample(p.dereference(record))
    }
    examples.foldLeft(Example.newBuilder())((e1, e2) => e1.mergeFrom(e2)).build()
  }

  def dispatch[T](sealedTrait: SealedTrait[Typeclass, T]): Typeclass[T] = ???

  implicit def gen[T]: Typeclass[T] = macro Magnolia.gen[T]

  private def composeWithName[T](name: String, instance: ExampleConverter[T]): ExampleConverter[T] =
    (record: T) => {
      val ex = instance.toExample(record)
      val features = ex.getFeatures
      // If the parameter is -not- a nested case class, it should be treated as an anonymous
      // (un-named) single feature. In this case, we name the feature using the param name.
      // Instead of checking whether the param is a case class directly (via reflection),
      // we can instead just check whether the generated features already have names or not.
      // If the parameter -is- a nested case class, we prepend its feature names with this parameter
      // name to prevent potential ambiguities.
      if (features.getFeatureCount == 1 && features.containsFeature("")) {
        val feature = features.getFeatureOrThrow("")
        Example.newBuilder().setFeatures(Features.newBuilder().putFeature(name, feature)).build()
      }
      else {
        val fMap = features.getFeatureMap.asScala.map { case (fName, f) =>
          (s"${name}_$fName", f)
        }.asJava
        Example.newBuilder().setFeatures(Features.newBuilder().putAllFeature(fMap)).build
      }
    }

  implicit def iterableLongConverter[T <: Iterable[Long]]: ExampleConverter[T] = { longs =>
    val jLongs = longs.asJava.asInstanceOf[java.lang.Iterable[java.lang.Long]]
    Example.newBuilder().setFeatures(Features.newBuilder()
      .putFeature("", Feature.newBuilder()
        .setInt64List(Int64List.newBuilder().addAllValue(jLongs))
        .build))
      .build
  }

  implicit def iterableIntConverter[T <: Iterable[Int]]: ExampleConverter[T] = ints =>
    iterableLongConverter.toExample(ints.map(_.toLong))

  implicit def iterableFloatConverter[T <: Iterable[Float]]: ExampleConverter[T] = { floats =>
    val jFloats = floats.asJava.asInstanceOf[java.lang.Iterable[java.lang.Float]]
    Example.newBuilder().setFeatures(Features.newBuilder()
      .putFeature("", Feature.newBuilder()
        .setFloatList(FloatList.newBuilder().addAllValue(jFloats))
        .build))
      .build
  }

  implicit def iterableBytesConverter[T <: Iterable[ByteString]]: ExampleConverter[T] = { bytes =>
    val jBytes = bytes.asJava
    Example.newBuilder().setFeatures(Features.newBuilder()
      .putFeature("", Feature.newBuilder()
        .setBytesList(BytesList.newBuilder().addAllValue(jBytes))
        .build))
      .build
  }

  implicit def iterableStringConverter[T <: Iterable[String]]: ExampleConverter[T] = strings =>
    iterableBytesConverter.toExample(strings.map(ByteString.copyFromUtf8))

  def singletonConverter[T](implicit iterableConverter: ExampleConverter[Iterable[T]])
  : ExampleConverter[T] = (item: T) => iterableConverter.toExample(Seq(item))

  implicit def singletonLongConverter[T <: Long]: ExampleConverter[T] = singletonConverter
  implicit def singletonIntConverter[T <: Int]: ExampleConverter[T]  = singletonConverter
  implicit def singletonFloatConverter[T <: Float]: ExampleConverter[T]  = singletonConverter
  implicit def singletonByteStringConverter[T <: ByteString]: ExampleConverter[T]  = singletonConverter
  implicit def singletonStringConverter[T <: String]: ExampleConverter[T]  = singletonConverter
}

trait ExampleConverter[T] extends Serializable {
  def toExample(record: T): Example
  // TODO: requires schema
//  def fromExample(example: Example): T
}
