package tf.magnolia.core

import com.google.protobuf.ByteString
import org.tensorflow.example._

import scala.collection.JavaConverters._

trait Implicits {
  implicit def iterableLongConverter: FeatureBuilder[Iterable[Long]] =
    new FeatureBuilder[Iterable[Long]] {
      override def toFeatures(longs: Iterable[Long]): Features.Builder = {
        val jLongs = longs.asJava.asInstanceOf[java.lang.Iterable[java.lang.Long]]
        featuresOf(Feature.newBuilder()
          .setInt64List(
            Int64List.newBuilder()
              .addAllValue(jLongs))
          .build)
      }

      override def fromFeatures(features: Features): Iterable[Long] =
        features.getFeatureMap.entrySet()
          .asScala.head.getValue.getInt64List.getValueList.asScala.map(_.toLong)
    }

  implicit def iterableIntConverter: FeatureBuilder[Iterable[Int]] =
    FeatureBuilder.of[Iterable[Int]](_.map(_.toLong))(_.map(_.toInt))

  implicit def iterableFloatConverter: FeatureBuilder[Iterable[Float]] =
    new FeatureBuilder[Iterable[Float]] {
      override def toFeatures(floats: Iterable[Float]): Features.Builder = {
        val jFloats = floats.asJava.asInstanceOf[java.lang.Iterable[java.lang.Float]]
        featuresOf(Feature.newBuilder()
          .setFloatList(FloatList.newBuilder().addAllValue(jFloats))
          .build)
      }

      override def fromFeatures(features: Features): Iterable[Float] =
        features.getFeatureMap.entrySet()
          .asScala.head.getValue.getFloatList.getValueList.asScala.map(_.toFloat)
    }

  implicit def iterableDoubleConverter: FeatureBuilder[Iterable[Double]] =
    FeatureBuilder.of[Iterable[Double]](_.map(_.toFloat))(_.map(_.toDouble))

  implicit def iterableBytesConverter: FeatureBuilder[Iterable[ByteString]] =
    new FeatureBuilder[Iterable[ByteString]] {
      override def toFeatures(bytes: Iterable[ByteString]): Features.Builder = {
        val jBytes = bytes.asJava
        featuresOf(Feature.newBuilder()
          .setBytesList(BytesList.newBuilder().addAllValue(jBytes))
          .build)
      }

      override def fromFeatures(features: Features): Iterable[ByteString] =
        features.getFeatureMap.entrySet()
          .asScala.head.getValue.getBytesList.getValueList.asScala
    }

  implicit def iterableStringConverter: FeatureBuilder[Iterable[String]] =
    FeatureBuilder.of[Iterable[String]](_.map(ByteString.copyFromUtf8))(_.map(_.toStringUtf8))

  def singletonConverter[T](implicit fb: FeatureBuilder[Iterable[T]]): FeatureBuilder[T] =
    FeatureBuilder.of[T](Seq(_).asInstanceOf[Iterable[T]])(_.head)

  implicit def singletonLongConverter: FeatureBuilder[Long] = singletonConverter
  implicit def singletonIntConverter: FeatureBuilder[Int] = singletonConverter
  implicit def singletonFloatConverter: FeatureBuilder[Float] = singletonConverter
  implicit def singletonByteStringConverter: FeatureBuilder[ByteString] = singletonConverter
  implicit def singletonStringConverter: FeatureBuilder[String] = singletonConverter

  implicit def toExampleConverter[T](implicit fb: FeatureBuilder[T]): ExampleConverter[T] =
    new ExampleConverter[T] {
      override def toExample(record: T): Example = {
        Example.newBuilder()
          .setFeatures(fb.toFeatures(record))
          .build()
      }

      override def fromExample(example: Example): T = fb.fromFeatures(example.getFeatures)
    }


  private def featuresOf(feature: Feature): Features.Builder =
    Features.newBuilder().putFeature("", feature)
}
