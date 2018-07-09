package tf.magnolia.core

import com.google.protobuf.ByteString
import org.scalatest.{FlatSpec, Matchers}
import org.tensorflow.example._

import scala.collection.JavaConverters._

class ExampleConverterTest extends FlatSpec with Matchers {
  "ExampleConversion" should "support basic types" in {
    case class BasicRecord(int: Int, long: Long, float: Float, bytes: ByteString, string: String)
    val actual = implicitly[ExampleConverter[BasicRecord]].toExample(
      BasicRecord(1, 2, 3.0f, ByteString.copyFromUtf8("hello"), "world"))
    val expected = Example.newBuilder()
        .setFeatures(Features.newBuilder()
          .putFeature("int", Feature.newBuilder()
            .setInt64List(Int64List.newBuilder().addValue(1)).build)
          .putFeature("long", Feature.newBuilder()
            .setInt64List(Int64List.newBuilder().addValue(2)).build)
          .putFeature("float", Feature.newBuilder()
            .setFloatList(FloatList.newBuilder().addValue(3.0f)).build)
          .putFeature("bytes", Feature.newBuilder()
            .setBytesList(BytesList.newBuilder().addValue(ByteString.copyFromUtf8("hello")))
            .build)
          .putFeature("string", Feature.newBuilder()
            .setBytesList(BytesList.newBuilder().addValue(ByteString.copyFromUtf8("world")))
            .build)
        .build)
    actual.getFeatures.getFeatureMap shouldEqual expected.getFeatures.getFeatureMap
  }

  it should "support Int" in {
    val example = implicitly[ExampleConverter[Int]].toExample(42)
    val actual = example.getFeatures.getFeatureMap.get("").getInt64List
    val expected = Int64List.newBuilder().addValue(42).build
    actual shouldEqual expected
  }

  it should "support nested case class" in {
    case class Record(f1: Int, f2: Long, inner: Inner)
    case class Inner(f3: Long)
    val example = implicitly[ExampleConverter[Record]].toExample(Record(1, 2l, Inner(3l)))
    example.getFeatures.getFeatureCount shouldEqual 3
    val features = example.getFeatures.getFeatureMap
    features.get("f1").getInt64List shouldEqual Int64List.newBuilder().addValue(1).build
    features.get("f2").getInt64List shouldEqual Int64List.newBuilder().addValue(2).build
    features.get("inner_f3").getInt64List shouldEqual Int64List.newBuilder().addValue(3).build
  }

  it should "handle duplicate feature names" in {
    case class Outer(f: Int, middle: Middle)
    case class Middle(f: Int, inner: Inner)
    case class Inner(f: Int)
    val example = implicitly[ExampleConverter[Outer]].toExample(Outer(1, Middle(2, Inner(3))))
    example.getFeatures.getFeatureCount shouldEqual 3
    val expectedFeatures = Map[String, Feature](
      "f" -> Feature.newBuilder()
        .setInt64List(Int64List.newBuilder().addValue(1)).build,
      "middle_f" -> Feature.newBuilder()
        .setInt64List(Int64List.newBuilder().addValue(2)).build,
      "middle_inner_f" -> Feature.newBuilder()
        .setInt64List(Int64List.newBuilder().addValue(3)).build
    ).asJava
    example.getFeatures.getFeatureMap shouldEqual expectedFeatures
  }
}
