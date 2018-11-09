package tf.magnolia

import com.google.protobuf.ByteString
import org.tensorflow.example._
import TensorflowMapping._

import scala.collection.JavaConverters._
import scala.reflect.ClassTag

trait Implicits {
  implicit val booleanTensorflowMapping: TensorflowMapping[Boolean] =
    TensorflowMapping[Boolean](toBooleans, fromBooleans)
  implicit val intTensorflowMapping: TensorflowMapping[Int] =
    TensorflowMapping[Int](toInts, fromInts)
  implicit val longTensorflowMapping: TensorflowMapping[Long] =
    TensorflowMapping[Long](toLongs, fromLongs)
  implicit val floatTensorflowMapping: TensorflowMapping[Float] =
    TensorflowMapping[Float](toFloats, fromFloats)
  implicit val doubleTensorflowMapping: TensorflowMapping[Double] =
    TensorflowMapping[Double](toDoubles, fromDoubles)
  implicit val byteStringTensorflowMapping: TensorflowMapping[ByteString] =
    TensorflowMapping[ByteString](toByteStrings, fromByteStrings)
  implicit val byteArrayTensorflowMapping: TensorflowMapping[Array[Byte]] =
    TensorflowMapping[Array[Byte]](toByteArrays, fromByteArrays)
  implicit val stringTensorflowMapping: TensorflowMapping[String] =
    TensorflowMapping[String](toStrings, fromStrings)

  implicit def singletonFeatureBuilder[T](implicit mapping: TensorflowMapping[T])
  : FeatureBuilder[T] =
    new FeatureBuilder[T] {
      override def toFeatures(record: T): Features.Builder = featuresOf(mapping.toFeature(record))
      override def fromFeatures(features: Features): T = {
        mapping.fromFeature(features.getFeatureMap.values().asScala.head)
      }
    }

  // Collections (TODO: can these be refactored?)
  implicit def iterableFb[T](implicit mapping: TensorflowMapping[T])
  : FeatureBuilder[Iterable[T]] = new FeatureBuilder[Iterable[T]] {
    override def toFeatures(record: Iterable[T]): Features.Builder =
      featuresOf(mapping.toSeq(record.toSeq))
    override def fromFeatures(features: Features): Iterable[T] =
      mapping.fromSeq(features.getFeatureMap.values().asScala.head)
  }

  implicit def seqFb[T](implicit mapping: TensorflowMapping[T])
  : FeatureBuilder[Seq[T]] = new FeatureBuilder[Seq[T]] {
    override def toFeatures(record: Seq[T]): Features.Builder =
      featuresOf(mapping.toSeq(record))
    override def fromFeatures(features: Features): Seq[T] =
      mapping.fromSeq(features.getFeatureMap.values().asScala.head)
  }

  implicit def arrFb[T: ClassTag](implicit mapping: TensorflowMapping[T])
  : FeatureBuilder[Array[T]] = new FeatureBuilder[Array[T]] {
    override def toFeatures(record: Array[T]): Features.Builder =
      featuresOf(mapping.toSeq(record))
    override def fromFeatures(features: Features): Array[T] =
      mapping.fromSeq(features.getFeatureMap.values().asScala.head).toArray
  }

  implicit def listFb[T](implicit mapping: TensorflowMapping[T])
  : FeatureBuilder[List[T]] = new FeatureBuilder[List[T]] {
    override def toFeatures(record: List[T]): Features.Builder =
      featuresOf(mapping.toSeq(record))
    override def fromFeatures(features: Features): List[T] =
      mapping.fromSeq(features.getFeatureMap.values().asScala.head).toList
  }

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
