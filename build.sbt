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

val magnoliaVersion = "0.14.0"
val scalameterVersion = "0.19"
val scalaTestVersion = "3.1.1"
val tensorflowVersion = "1.15.0"

val commonSettings = Seq(
  organization := "com.spotify",
  scalaVersion := "2.13.1",
  crossScalaVersions := Seq("2.11.12", "2.12.10", scalaVersion.value),
  scalacOptions ++= Seq("-target:jvm-1.8", "-deprecation", "-feature", "-unchecked"),
  publishTo := Some(if (isSnapshot.value) {
    Opts.resolver.sonatypeSnapshots
  } else {
    Opts.resolver.sonatypeStaging
  }),
  releaseCrossBuild := true,
  releasePublishArtifactsAction := PgpKeys.publishSigned.value,
  publishMavenStyle := true,
  publishArtifact in Test := false,
  sonatypeProfileName := "com.spotify",
  licenses := Seq("Apache 2" -> url("http://www.apache.org/licenses/LICENSE-2.0.txt")),
  homepage := Some(url("https://github.com/spotify/tfexample-derive")),
  scmInfo := Some(
    ScmInfo(
      url("https://github.com/spotify/tfexample-derive.git"),
      "scm:git:git@github.com:spotify/tfexample-derive.git"
    )
  ),
  developers := List(
    Developer(
      id = "andrewsmartin",
      name = "Andrew Martin",
      email = "andrewsmartin.mg@gmail.com",
      url = url("https://twitter.com/andrew_martin92")
    ),
    Developer(
      id = "daikeshi",
      name = "Keshi Dai",
      email = "keshi.dai@gmail.com",
      url = url("https://twitter.com/daikeshi")
    )
  )
)

val magnoliaDependencies = Def.settings(
  libraryDependencies ++= Seq(
    if (scalaBinaryVersion.value == "2.11") {
      "me.lyh" %% "magnolia" % "0.10.1-jto"
    } else {
      "com.propensive" %% "magnolia" % magnoliaVersion
    }
  )
)

lazy val root: Project = project
  .in(file("."))
  .settings(commonSettings)
  .settings(
    name := "tfexample-derive",
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
      "org.tensorflow" % "proto" % tensorflowVersion,
      "org.scalatest" %% "scalatest" % scalaTestVersion % Test
    ),
    magnoliaDependencies
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
