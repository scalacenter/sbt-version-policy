package librarytest

trait Foo {

  def bar(x: Int): Int = {
    println("bar")
    x
  }

  def baz: Int = 0

}
