name := "tfexample-derive"
description := "Provides compile-time derivation of conversions between Scala case classes and " +
  "Tensorflow Example protcol buffers"

val magnoliaVersion = "0.9.1"
val scalaTestVersion = "3.0.5"
val tensorflowVersion = "1.7.0"

val commonSettings = Seq(
  organization := "com.github.andrewsmartin",
  scalaVersion := "2.12.7",
  crossScalaVersions := Seq("2.11.12", "2.12.7"),
  scalacOptions ++= Seq("-target:jvm-1.8", "-deprecation", "-feature", "-unchecked"),
)

lazy val root: Project = Project(
  "root",
  file(".")
).settings(
  commonSettings,
  libraryDependencies ++= Seq(
    "com.propensive" %% "magnolia" % magnoliaVersion,
    "org.tensorflow" % "proto" % tensorflowVersion,
    "org.scalatest" %% "scalatest" % scalaTestVersion % Test
  )
)
