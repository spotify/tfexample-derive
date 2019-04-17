tfexample-derive
==================

[![Build Status](https://travis-ci.com/spotify/tfexample-derive.svg?token=RuxhZ5UxBe3qBBNtxKVz&branch=master)](https://github.com/spotify/tfexample-derive)
[![codecov.io](https://codecov.io/github/spotify/tfexample-derive/coverage.svg?branch=master)](https://codecov.io/github/spotify/tfexample-derive?branch=master)
[![GitHub license](https://img.shields.io/github/license/spotify/tfexample-derive.svg)](./LICENSE)
[![Maven Central](https://img.shields.io/maven-central/v/com.spotify/tfexample-derive_2.11.svg)](https://maven-badges.herokuapp.com/maven-central/com.spotify/tfexample-derive_2.11)

[Magnolia](https://github.com/propensive/magnolia)-based conversions between case classes and tensorflow Example protobufs.


```scala
libraryDependencies += "com.spotify" %% "tfexample-derive" % "0.2.2"
```

# Usage

`ExampleConverter[T]` is a typeclass that converts between case class `T` and [Tensorflow Example Protobuf](https://github.com/tensorflow/tensorflow/blob/master/tensorflow/core/example/example.proto) types:

```scala
import com.spotify.tfexample.derive._

case class Data(floats: Array[Float], longs: Array[Long], strings: List[String], label: String)

val converter = ExampleConverter[Data]
val data = Data(Array(1.5f, 2.5f), Array(1L, 2L), List("a", "b"), "x")
val example = converter.toExample(data)
val data2: Option[Data] = converter.fromExample(example)
```

The derivation makes use of [magnolia](https://github.com/propensive/magnolia), which provides a generic macro for materializing typeclasses
for case classes.

## Supported Types

Tensorflow Example is an inherently flat structure - essentially a map of `(String -> Feature)`, where `Feature` is one of:

- Int64List
- FloatList
- BytesList

A converter can be automatically derived for types which naturally correspond to these feature types - `Int, Long, Float, Double, ByteString, String` etc, and by extension
collections of these types including `Array, Seq, List`. `Option` is also supported, simply by not encoding `None` values in the resulting `Example`. See the below section on custom types for an example of how to add encodings for new types.

### Nesting

As mentioned above, Example is a flat structure, but a converter can be derived in certain cases even for nested case classes. Flattening is achieved by using the field name of the nested case
class as a namespace for the features belonging to that class. For example:

```scala
case class Record(xs: List[Int], inner: Inner)
case class Inner(ys: List[Float], labels: Option[List[String]])

val record = Record(List(1, 2, 3), Inner(List(1.0f, 2.0f), Some(List("hello"))))
ExampleConverter[Record].toExample(record)
```

```
features {
  feature {
    key: "xs"
    value {
      int64_list {
        value: 1
        value: 2
        value: 3
      }
    }
  }
  feature {
    key: "inner.ys"
    value {
      float_list {
        value: 1.0
        value: 2.0
      }
    }
  }
  feature {
    key: "inner.labels"
    value {
      bytes_list {
        value: "hello"
      }
    }
  }
}
```

However, the following will result in a compilation error:

```scala
case class Record(xs: List[Int], inners: List[Inner])
case class Inner(y: Float, label: Option[String])
ExampleConverter[Record]
```

```
Error: could not find implicit value for parameter converter: com.spotify.tfexample.derive.ExampleConverter[Record]
    ExampleConverter[Record]
```

To drill down and find the particular field that breaks the derivation, we can get more information from Magnolia by directly calling
the macro. Replace the call to `ExampleConverter` with a call to `FeatureBuilder.gen`:

```scala
import com.spotify.tfexample.derive.FeatureBuilder

case class Record(xs: List[Int], inners: List[Inner])
case class Inner(y: Float, label: Option[String])
FeatureBuilder.gen[Record]
```

```
Error: cannot derive FeatureBuilder for type List[Inner]
    FeatureBuilder.gen[Record]
```

Magnolia tells us we cannot derive a `FeatureBuilder` for `List[Inner]`, and this makes sense - for `List[T]`, `T` must correspond to one
of the three feature types, and in the case of a nested case class, there's no obvious mapping. A mapping can either be provided (see below for an example)
or the case class should be restructured. 

## Custom Types

In addition to the types supported out of the box, custom types are also supported by providing an implicit `TensorflowMapping`, which defines an
appropriate encoding of the type to one of the three possible feature types. In this example, we encode a `URI` as a `BytesList` feature (via `String`),
using some helper functions from `TensorflowMapping.scala`. We create a `TensorflowMapping` by supplying functions for converting to and from `Feature` from
type `T`.

```scala
import com.spotify.tfexample.derive.TensorflowMapping._
import java.net.URI

implicit val uriType: TensorflowMapping[URI] =
  TensorflowMapping[URI](toStrings(_).map(URI.create), xs => fromStrings(xs.map(_.toString)))

case class Record(uri: URI, uris: List[URI])
val converter = ExampleConverter[Record]
val record = Record(URI.create("www.google.com"), List(URI.create("www.foobar.com")))
val example = converter.toExample(record)
```

# Code of Conduct

This project adheres to the [Open Code of Conduct][code-of-conduct]. By participating, you are expected to honor this code.

[code-of-conduct]: https://github.com/spotify/code-of-conduct/blob/master/code-of-conduct.md
