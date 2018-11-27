/*
 * Copyright 2018 Spotify AB.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package tf.magnolia

import com.google.protobuf.ByteString
import org.tensorflow.example._
import TensorflowMapping._

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

  implicit def singletonFeatureBuilder[T](
    implicit mapping: TensorflowMapping[T]): FeatureBuilder[T] =
    new FeatureBuilder[T] {
      override def toFeatures(record: T, nameOrPrefix: Option[String]): Features.Builder =
        featuresOf(nameOrPrefix, mapping.toFeature(record))
      override def fromFeatures(features: Features, nameOrPrefix: Option[String]): T =
        mapping.fromFeature(getFeature(nameOrPrefix, features))
    }

  // Collections (TODO: can these be refactored?)
  implicit def iterableFb[T](implicit mapping: TensorflowMapping[T]): FeatureBuilder[Iterable[T]] =
    new FeatureBuilder[Iterable[T]] {
      override def toFeatures(record: Iterable[T], nameOrPrefix: Option[String]): Features.Builder =
        featuresOf(nameOrPrefix, mapping.toSeq(record.toSeq))
      override def fromFeatures(features: Features, nameOrPrefix: Option[String]): Iterable[T] =
        mapping.fromSeq(getFeature(nameOrPrefix, features))
    }

  implicit def seqFb[T](implicit mapping: TensorflowMapping[T]): FeatureBuilder[Seq[T]] =
    new FeatureBuilder[Seq[T]] {
      override def toFeatures(record: Seq[T], nameOrPrefix: Option[String]): Features.Builder =
        featuresOf(nameOrPrefix, mapping.toSeq(record))
      override def fromFeatures(features: Features, nameOrPrefix: Option[String]): Seq[T] =
        mapping.fromSeq(getFeature(nameOrPrefix, features))
    }

  implicit def arrFb[T: ClassTag](
    implicit mapping: TensorflowMapping[T]): FeatureBuilder[Array[T]] =
    new FeatureBuilder[Array[T]] {
      override def toFeatures(record: Array[T], nameOrPrefix: Option[String]): Features.Builder =
        featuresOf(nameOrPrefix, mapping.toSeq(record))
      override def fromFeatures(features: Features, nameOrPrefix: Option[String]): Array[T] =
        mapping.fromSeq(getFeature(nameOrPrefix, features)).toArray
    }

  implicit def listFb[T](implicit mapping: TensorflowMapping[T]): FeatureBuilder[List[T]] =
    new FeatureBuilder[List[T]] {
      override def toFeatures(record: List[T], nameOrPrefix: Option[String]): Features.Builder =
        featuresOf(nameOrPrefix, mapping.toSeq(record))
      override def fromFeatures(features: Features, nameOrPrefix: Option[String]): List[T] =
        mapping.fromSeq(getFeature(nameOrPrefix, features)).toList
    }

  implicit def toExampleConverter[T](implicit fb: FeatureBuilder[T]): ExampleConverter[T] =
    new ExampleConverter[T] {
      override def toExample(record: T): Example = {
        Example
          .newBuilder()
          .setFeatures(fb.toFeatures(record, None))
          .build()
      }

      override def fromExample(example: Example): T = fb.fromFeatures(example.getFeatures, None)
    }

  private def featuresOf(name: Option[String], feature: Feature): Features.Builder =
    Features.newBuilder().putFeature(name.getOrElse(""), feature)

  private def getFeature(name: Option[String], features: Features): Feature =
    features.getFeatureOrThrow(name.getOrElse(""))
}
