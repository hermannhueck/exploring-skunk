// Twiddle Lists

val x1: (((Int, String), Boolean), Double) =
  (((42, "hi"), true), 1.23)

type ~[+A, +B] = (A, B)

implicit final class IdOps[A](a: A) {
  def ~[B](b: B): A ~ B = (a, b)
}

// ~ is left-associative so this is exactly the same as `x` above.
val x2: Int ~ String ~ Boolean ~ Double =
  42 ~ "hi" ~ true ~ 1.23

object ~ {
  def unapply[A, B](t: A ~ B): Some[A ~ B] = Some(t)
}

// Deconstruct `x1/x2`
x1 match {
  case n ~ s ~ b ~ d =>
    println(s"n = $n, s = $s, b = $b, d = $d")
}
x2 match {
  case n ~ s ~ b ~ d =>
    println(s"n = $n, s = $s, b = $b, d = $d")
}

// Isomorphism with Case Classes

case class Person(name: String, age: Int, active: Boolean)

def fromTwiddle(t: String ~ Int ~ Boolean): Person =
  t match {
    case s ~ n ~ b => Person(s, n, b)
  }

def toTwiddle(p: Person): String ~ Int ~ Boolean =
  p.name ~ p.age ~ p.active

val p  = Person("Bob", 42, true)
val t  = toTwiddle(p)
val p2 = fromTwiddle(t)
p2 == p

val bob = Person("Bob", 42, true)

val tw = skunk.util.Twiddler[Person]

val twiddled: String ~ Int ~ Boolean = tw.to(bob)
val untwiddled                       = tw.from(twiddled)
untwiddled == bob

import skunk.codec.all._
import skunk._

val codec  = varchar ~ int4 ~ bool
val pCodec = codec.gimap[Person]
