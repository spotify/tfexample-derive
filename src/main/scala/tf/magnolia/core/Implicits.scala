package tf.magnolia.core

import com.google.protobuf.ByteString
import org.tensorflow.example._

import scala.collection.JavaConverters._

trait Implicits {
  implicit def iterableLongConverter[T <: Iterable[Long]]: FeatureBuilder[T] = { longs =>
    val jLongs = longs.asJava.asInstanceOf[java.lang.Iterable[java.lang.Long]]
    featuresOf(Feature.newBuilder()
      .setInt64List(
        Int64List.newBuilder()
          .addAllValue(jLongs))
      .build)
  }
  implicit def iterableIntConverter[T <: Iterable[Int]]: FeatureBuilder[T] =
    FeatureBuilder.of[T](_.map(_.toLong))

  implicit def iterableFloatConverter[T <: Iterable[Float]]: FeatureBuilder[T] = { floats =>
    val jFloats = floats.asJava.asInstanceOf[java.lang.Iterable[java.lang.Float]]
    featuresOf(Feature.newBuilder()
      .setFloatList(FloatList.newBuilder().addAllValue(jFloats))
      .build)
  }
  implicit def iterableDoubleConverter[T <: Iterable[Double]]: FeatureBuilder[T] =
    FeatureBuilder.of[T](_.map(_.toFloat))

  implicit def iterableBytesConverter[T <: Iterable[ByteString]]: FeatureBuilder[T] = { bytes =>
    val jBytes = bytes.asJava
    featuresOf(Feature.newBuilder()
      .setBytesList(BytesList.newBuilder().addAllValue(jBytes))
      .build)
  }
  implicit def iterableStringConverter[T <: Iterable[String]]: FeatureBuilder[T] =
    FeatureBuilder.of[T](_.map(ByteString.copyFromUtf8))

  def singletonConverter[T](implicit fb: FeatureBuilder[Iterable[T]]): FeatureBuilder[T] =
    FeatureBuilder.of[T](Seq(_).asInstanceOf[Iterable[T]])

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
