package librarytest

trait Foo {

  def bar[A](x: A): A = {
    println("bar")
    x
  }

  def baz: Int = 0

}
