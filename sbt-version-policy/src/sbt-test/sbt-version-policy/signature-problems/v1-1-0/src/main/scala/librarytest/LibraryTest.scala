package librarytest

abstract class Base[A]

class Foo extends Base[String] {
  def xs: List[String] = Nil
}
