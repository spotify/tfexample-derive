package tf.magnolia.core

import com.google.protobuf.ByteString
import org.scalatest.{FlatSpec, Matchers}
import org.tensorflow.example._
import tf.magnolia.core.TensorflowMapping._

import java.lang.{Float => JFloat, Iterable => JIterable, Long => JLong}
import scala.collection.JavaConverters._

class ExampleConverterTest extends FlatSpec with Matchers {
  "ExampleConversion" should "support basic types" in {
    case class BasicRecord(int: Int, long: Long, float: Float, bytes: ByteString, string: String)
    val actual = ExampleConverter[BasicRecord].toExample(
      BasicRecord(1, 2, 3.0f, ByteString.copyFromUtf8("hello"), "world"))
    val expected = Example.newBuilder()
        .setFeatures(Features.newBuilder()
          .putFeature("int", longFeat(1L))
          .putFeature("long", longFeat(2L))
          .putFeature("float", floatFeat(3.0f))
          .putFeature("bytes", stringFeat("hello"))
          .putFeature("string", stringFeat("world"))
        .build)
    actual.getFeatures.getFeatureMap shouldEqual expected.getFeatures.getFeatureMap
  }

  it should "support nested case class" in {
    case class Record(f1: Int, f2: Long, inner: Inner)
    case class Inner(f3: Long)
    val example = ExampleConverter[Record].toExample(Record(1, 2l, Inner(3l)))
    example.getFeatures.getFeatureCount shouldEqual 3
    val features = example.getFeatures.getFeatureMap
    features.get("f1").getInt64List shouldEqual Int64List.newBuilder().addValue(1).build
    features.get("f2").getInt64List shouldEqual Int64List.newBuilder().addValue(2).build
    features.get("inner.f3").getInt64List shouldEqual Int64List.newBuilder().addValue(3).build
  }

  it should "handle duplicate feature names" in {
    case class Outer(f: Int, middle: Middle)
    case class Middle(f: Int, inner: Inner)
    case class Inner(f: Int)
    val example = ExampleConverter[Outer].toExample(Outer(1, Middle(2, Inner(3))))
    example.getFeatures.getFeatureCount shouldEqual 3
    val expectedFeatures = Map[String, Feature](
      "f" -> longFeat(1L),
      "middle.f" -> longFeat(2L),
      "middle.inner.f" -> longFeat(3L)
    ).asJava
    example.getFeatures.getFeatureMap shouldEqual expectedFeatures
  }

  it should "support collection types" in {
    case class Record(int: Int, ints: List[Int], inner: Inner)
    case class Inner(floats: Seq[Float], bools: Array[Boolean])
    FeatureBuilder.gen[Record]
    val example = ExampleConverter[Record].toExample(
      Record(1, List(1, 2, 3), Inner(Seq(1.0f, 2.0f), Array(true, false))))
    val expected = Example.newBuilder()
      .setFeatures(Features.newBuilder()
        .putFeature("int", longFeat(1))
        .putFeature("ints", longFeat(1L, 2L, 3L))
        .putFeature("inner.floats", floatFeat(1.0f, 2.0f))
        .putFeature("inner.bools", longFeat(1L, 0L))
        .build)
    example.getFeatures.getFeatureMap shouldEqual expected.getFeatures.getFeatureMap
  }

  it should "support custom types" in {
    class MyInt(val int: Int)

    implicit val myIntType: TensorflowMapping[MyInt] =
      TensorflowMapping[MyInt](
        f => toInts(f).map(n => new MyInt(n)),
        myInts => fromInts(myInts.map(_.int)))

    case class Record(id: String, myInt: MyInt, myInts: List[MyInt])
    val conv = ExampleConverter[Record]
    val example = conv.toExample(
      Record("1", new MyInt(1), List(new MyInt(2), new MyInt(3))))
    val expected = Example.newBuilder()
      .setFeatures(Features.newBuilder()
        .putFeature("id", stringFeat("1"))
        .putFeature("myInt", longFeat(1L))
        .putFeature("myInts", longFeat(2L, 3L))
      )
      .build()
    example.getFeatures.getFeatureMap shouldEqual expected.getFeatures.getFeatureMap
  }

  it should "support basic types from examples" in {
    case class BasicRecord(int: Int, long: Long, float: Float, bytes: ByteString, string: String)
    val example = Example.newBuilder()
      .setFeatures(Features.newBuilder()
        .putFeature("int", longFeat(1L))
        .putFeature("long", longFeat(2L))
        .putFeature("float", floatFeat(3.0f))
        .putFeature("bytes", stringFeat("hello"))
        .putFeature("string", stringFeat("world"))
        .build)
      .build()
    val expected = BasicRecord(1, 2, 3.0f, ByteString.copyFromUtf8("hello"), "world")
    val actual = ExampleConverter[BasicRecord].fromExample(example)

    actual shouldEqual expected
  }

  private def longFeat(longs: Long*): Feature = {
    val jLongs = longs.asJava.asInstanceOf[JIterable[JLong]]
    Feature.newBuilder().setInt64List(Int64List.newBuilder().addAllValue(jLongs)).build()
  }

  private def floatFeat(floats: Float*): Feature = {
    val jFloats = floats.asJava.asInstanceOf[JIterable[JFloat]]
    Feature.newBuilder().setFloatList(FloatList.newBuilder().addAllValue(jFloats)).build()
  }

  private def stringFeat(str: String): Feature = {
    Feature.newBuilder().setBytesList(BytesList.newBuilder().addValue(ByteString.copyFromUtf8(str)))
      .build()
  }
}
