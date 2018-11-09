package tf.magnolia

import org.tensorflow.example.Example

trait ExampleConverter[T] {
  def toExample(record: T): Example
  def fromExample(example: Example): T
}

object ExampleConverter {
  def apply[T](implicit converter: ExampleConverter[T]): ExampleConverter[T] = converter
}
