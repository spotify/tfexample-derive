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

import org.tensorflow.example._
import magnolia._

import scala.annotation.implicitNotFound
import scala.language.experimental.macros

trait FeatureBuilder[T] {
  def toFeatures(record: T, nameOrPrefix: Option[String]): Features.Builder
  def fromFeatures(features: Features, nameOrPrefix: Option[String]): T
}

object FeatureBuilder {
  type Typeclass[T] = FeatureBuilder[T]

  def combine[T](caseClass: CaseClass[FeatureBuilder, T]): FeatureBuilder[T] =
    new FeatureBuilder[T] {
      override def toFeatures(record: T, nameOrPrefix: Option[String]): Features.Builder = {
        caseClass.parameters.foldLeft(Features.newBuilder()) { (fb, param) =>
          val newPrefix = nameOrPrefix.fold(param.label)(prefix => s"$prefix.${param.label}")
          fb.putAllFeature(
            param.typeclass.toFeatures(param.dereference(record), Some(newPrefix)).getFeatureMap)
          fb
        }
      }

      override def fromFeatures(features: Features, nameOrPrefix: Option[String]): T = {
        caseClass.construct { param =>
          val newPrefix = nameOrPrefix.fold(param.label)(prefix => s"$prefix.${param.label}")
          param.typeclass.fromFeatures(features, Some(newPrefix))
        }
      }
    }

  // Hack to force compiler errors for List[T] where T is another case class
  @implicitNotFound("cannot derive FeatureBuilder for type ${T}")
  private trait Dispatchable[T]
  def dispatch[T: Dispatchable](sealedTrait: SealedTrait[Typeclass, T]): FeatureBuilder[T] = ???

  implicit def gen[T]: Typeclass[T] = macro Magnolia.gen[T]
}
