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

package com.spotify.tfexample.derive

import java.nio.ByteBuffer

import com.google.protobuf.ByteString
import org.tensorflow.example.{BytesList, Feature, FloatList, Int64List}

import scala.collection.JavaConverters._

trait TensorflowMapping[T] extends Serializable {
  def toFeature(value: T): Feature = toSeq(Seq(value))
  def fromFeature(value: Feature): T = fromSeq(value).head
  def fromSeq(value: Feature): Seq[T]
  def toSeq(value: Seq[T]): Feature
}

object TensorflowMapping {
  def apply[T](fromFn: Feature => Seq[T], toFn: Seq[T] => Feature.Builder): TensorflowMapping[T] =
    new TensorflowMapping[T] {
      override def fromSeq(value: Feature): Seq[T] = fromFn(value)
      override def toSeq(value: Seq[T]): Feature = toFn(value).build()
    }

  def fromBooleans(xs: Seq[Boolean]): Feature.Builder =
    Feature
      .newBuilder()
      .setInt64List(
        Int64List.newBuilder().addAllValue(xs.map(x => (if (x) 1L else 0L): java.lang.Long).asJava)
      )
  def toBooleans(f: Feature): Seq[Boolean] =
    f.getInt64List.getValueList.asScala.map(x => if (x > 0) true else false).toSeq

  def fromLongs(xs: Seq[Long]): Feature.Builder =
    Feature
      .newBuilder()
      .setInt64List(Int64List.newBuilder().addAllValue(xs.asInstanceOf[Seq[java.lang.Long]].asJava))
  def toLongs(f: Feature): Seq[Long] =
    f.getInt64List.getValueList.asScala.toSeq.asInstanceOf[Seq[Long]]

  def fromInts(xs: Seq[Int]): Feature.Builder = fromLongs(xs.map(_.toLong))
  def toInts(f: Feature): Seq[Int] = toLongs(f).map(_.toInt)

  def fromFloats(xs: Seq[Float]): Feature.Builder =
    Feature
      .newBuilder()
      .setFloatList(
        FloatList.newBuilder().addAllValue(xs.asInstanceOf[Seq[java.lang.Float]].asJava)
      )
  def toFloats(f: Feature): Seq[Float] =
    f.getFloatList.getValueList.asScala.toSeq.asInstanceOf[Seq[Float]]

  def fromDoubles(xs: Seq[Double]): Feature.Builder = fromFloats(xs.map(_.toFloat))
  def toDoubles(f: Feature): Seq[Double] = toFloats(f).map(_.toDouble)

  def fromByteStrings(xs: Seq[ByteString]): Feature.Builder =
    Feature.newBuilder().setBytesList(BytesList.newBuilder().addAllValue(xs.asJava))
  def toByteStrings(f: Feature): Seq[ByteString] = f.getBytesList.getValueList.asScala.toSeq

  def fromByteArrays(xs: Seq[Array[Byte]]): Feature.Builder =
    fromByteStrings(xs.map(ByteString.copyFrom))
  def toByteArrays(f: Feature): Seq[Array[Byte]] = toByteStrings(f).map(_.toByteArray)

  def fromStrings(xs: Seq[String]): Feature.Builder =
    fromByteStrings(xs.map(ByteString.copyFromUtf8))
  def toStrings(f: Feature): Seq[String] = toByteStrings(f).map(_.toStringUtf8)

  def fromByteBuffers(xs: Seq[ByteBuffer]): Feature.Builder =
    fromByteStrings(xs.map { buf =>
      val pos = buf.position()
      val byteStr = ByteString.copyFrom(buf)
      // Preserve the original state of the buffer
      buf.position(pos)
      byteStr
    })
  def toByteBuffers(f: Feature): Seq[ByteBuffer] = toByteArrays(f).map(ByteBuffer.wrap)
}
