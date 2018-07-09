package tf.magnolia.core

import com.google.protobuf.ByteString
import org.tensorflow.example._
import magnolia._

import scala.language.experimental.macros
import scala.collection.JavaConverters._

object ExampleConverter {
  type Typeclass[T] = ExampleConverter[T]

  def combine[T](caseClass: CaseClass[ExampleConverter, T]): ExampleConverter[T] = (record: T) => {
    val examples = caseClass.parameters.map { p =>
      composeWithName(p.label, p.typeclass).toExample(p.dereference(record))
    }
    examples.foldLeft(Example.newBuilder())((e1, e2) => e1.mergeFrom(e2)).build()
  }

  def dispatch[T](sealedTrait: SealedTrait[Typeclass, T]): Typeclass[T] = ???

  implicit def gen[T]: Typeclass[T] = macro Magnolia.gen[T]

  private def composeWithName[T](name: String, instance: ExampleConverter[T]): ExampleConverter[T] =
    (record: T) => {
      val ex = instance.toExample(record)
      val features = ex.getFeatures
      // If the parameter is -not- a nested case class, it should be treated as an anonymous
      // (un-named) single feature. In this case, we name the feature using the param name.
      // Instead of checking whether the param is a case class directly (via reflection),
      // we can instead just check whether the generated features already have names or not.
      // If the parameter -is- a nested case class, we prepend its feature names with this parameter
      // name to prevent potential ambiguities.
      if (features.getFeatureCount == 1 && features.containsFeature("")) {
        val feature = features.getFeatureOrThrow("")
        Example.newBuilder().setFeatures(Features.newBuilder().putFeature(name, feature)).build()
      }
      else {
        val fMap = features.getFeatureMap.asScala.map { case (fName, f) =>
          (s"${name}_$fName", f)
        }.asJava
        Example.newBuilder().setFeatures(Features.newBuilder().putAllFeature(fMap)).build
      }
    }

  implicit val longConverter: ExampleConverter[Long] = { l =>
    Example.newBuilder().setFeatures(Features.newBuilder()
      .putFeature("", Feature.newBuilder()
        .setInt64List(Int64List.newBuilder().addValue(l))
        .build))
      .build
  }

  implicit val intConverter: ExampleConverter[Int] = (i: Int) => longConverter.toExample(i)

  implicit val floatConverter: ExampleConverter[Float] = { f =>
    Example.newBuilder().setFeatures(Features.newBuilder()
      .putFeature("", Feature.newBuilder()
        .setFloatList(FloatList.newBuilder().addValue(f))
        .build))
      .build
  }

  implicit val bytesConverter: ExampleConverter[ByteString] = { bytes =>
    Example.newBuilder().setFeatures(Features.newBuilder()
      .putFeature("", Feature.newBuilder()
        .setBytesList(BytesList.newBuilder().addValue(bytes))
        .build))
      .build
  }

  implicit val stringConverter: ExampleConverter[String] = s =>
    bytesConverter.toExample(ByteString.copyFromUtf8(s))
}

trait ExampleConverter[T] {
  def toExample(record: T): Example
  // TODO: requires schema
//  def fromExample(example: Example): T
}
