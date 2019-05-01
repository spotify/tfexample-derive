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

package com.spotify.tf.example.derive.bench

import java.nio.ByteBuffer

import com.spotify.tfexample.derive.ExampleConverter
import org.scalameter.api._
import org.tensorflow.example.Example

object ExampleConverterBenchmark extends Bench.LocalTime {
  case class Record(int: Option[Int], middle: Middle)
  case class Middle(floats: Option[List[Float]], inner: Option[Inner])
  case class Inner(bool: Option[Boolean], labels: List[String], bytes: ByteBuffer)

  val testRecord: Record = Record(
    Some(1000),
    Middle(
      Some(List.fill(40)(1.0f)),
      Some(
        Inner(
          Some(true),
          List.fill(10)("label"),
          ByteBuffer.wrap("bytes".getBytes))
      )))

  val converter: ExampleConverter[Record] = ExampleConverter[Record]
  val testExample: Example = converter.toExample(testRecord)

  val sizes: Gen[Int] = Gen.range("size")(20000, 100000, 20000)
  val recordGen: Gen[List[Record]] = for {
    size <- sizes
  } yield List.fill(size)(testRecord)
  val exampleGen: Gen[List[Example]] = for {
    size <- sizes
  } yield List.fill(size)(testExample)

  performance of "conversion" in {
    measure method "toExample" in {
      using(recordGen) in { records =>
        records.foreach(r => converter.toExample(r))
      }
    }

    measure method "fromExample" in {
      using(exampleGen) in { examples =>
        examples.foreach(e => converter.fromExample(e))
      }
    }
  }
}
