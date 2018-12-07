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

val magnoliaVersion = "0.9.1"
val scalameterVersion = "0.10"
val scalaTestVersion = "3.0.5"
val tensorflowVersion = "1.12.0"

val commonSettings = Seq(
  organization := "com.spotify",
  scalaVersion := "2.12.8",
  crossScalaVersions := Seq("2.11.12", "2.12.8"),
  scalacOptions ++= Seq("-target:jvm-1.8", "-deprecation", "-feature", "-unchecked"),
)

lazy val root: Project = project
  .in(file("."))
  .settings(commonSettings)
  .settings(
    publish / skip := true
  )
  .aggregate(core, bench)

lazy val core: Project = project
  .in(file("core"))
  .settings(commonSettings)
  .settings(
    name := "core",
    moduleName := "tfexample-derive",
    description := "Provides compile-time derivation of conversions between Scala case classes and " +
      "Tensorflow Example protcol buffers",
    libraryDependencies ++= Seq(
      "com.propensive" %% "magnolia" % magnoliaVersion,
      "org.tensorflow" % "proto" % tensorflowVersion,
      "org.scalatest" %% "scalatest" % scalaTestVersion % Test
    )
  )

lazy val Benchmark = config("bench") extend Test
lazy val bench: Project = project
  .in(file("bench"))
  .configs(Benchmark)
  .settings(commonSettings)
  .settings(
    publish / skip := true,
    libraryDependencies ++= Seq(
      "com.storm-enroute" %% "scalameter" % scalameterVersion % "bench"
    ),
    testFrameworks += new TestFramework("org.scalameter.ScalaMeterFramework"),
    logBuffered := false,
    parallelExecution in Benchmark := false,
    inConfig(Benchmark)(Defaults.testSettings)
  )
  .dependsOn(core)
