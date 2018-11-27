tfexample-derive
==================

[![Build Status](https://travis-ci.org/andrewsmartin/tfexample-derive.svg?branch=master)](https://travis-ci.org/andrewsmartin/tfexample-derive)
[![codecov.io](https://codecov.io/github/andrewsmartin/tfexample-derive/coverage.svg?branch=master)](https://codecov.io/github/andrewsmartin/tfexample-derive?branch=master)
[![GitHub license](https://img.shields.io/github/license/andrewsmartin/tfexample-derive.svg)](./LICENSE)

[magnolia](https://github.com/propensive/magnolia)-based conversions between case classes and tensorflow Example protobufs.


```scala
libraryDependencies += "com.github.andrewsmartin" %% "tfexample-derive" % "0.1.0"
```

# Usage

`ExampleConverter[T]` is a typeclass that converts between case class `T` and Tensorflow Example Protobuf types:

```scala
import com.spotify.tfexample.derive._

case class Data(floats: Array[Float], longs: Array[Long], strings: List[String], label: String)

val converter = ExampleConverter[Data]
val data = Data(Array(1.5f, 2.5f), Array(1L, 2L), List("a", "b"), "x")
val example = converter.toExample(data)
val data2 = converter.fromExample(example)
```

## Custom Types

In addition to the types supported out of the box, custom types are also supported by providing an implicit `TensorflowMapping`:

```scala
import com.spotify.tfexample.derive.TensorflowMapping._

implicit val uriType: TensorflowMapping[URI] =
  TensorflowMapping[URI](toStrings(_).map(URI.create), xs => fromStrings(xs.map(_.toString)))

case class Record(uri: URI, uris: List[URI])
val converter = ExampleConverter[Record]
val record = Record(URI.create("www.google.com"), List(URI.create("www.foobar.com")))
val example = converter.toExample(record)
```
