// Fragments

import skunk._
import skunk.implicits._
import skunk.codec.all._

// A fragment with no interpolated encoders.
val f1 = sql"SELECT 42"

// A fragment with an interpolated encoder.
val f2 = sql"SELECT foo FROM bar WHERE baz = $int8"

// Interpolating Parameter Encoders

val f3 = sql"foo $int4 bar $varchar bar"

sql"foo ${int4 ~ varchar} bar".sql

sql"foo ${(int4 ~ varchar).values} bar".sql

sql"foo ${int4.list(4)} bar".sql

sql"INSERT ... VALUES ${(int4 ~ varchar).values.list(3)}".sql

// Interpolating Literal Strings

val table = "my_table"
val frag  = sql"SELECT foo, bar FROM #$table where foo = $int4"
frag.sql

// Composing Fragments

val f4 = sql"SELECT $int4, foo FROM blah WHERE "
f4.sql
val f5 = sql"bar = $varchar"
f5.sql
val f6 = f4 ~ f5
f6.sql

val f7 = sql"bar = $varchar"
f7.sql
val f8 = sql"SELECT $int4, foo FROM blah WHERE $f7 AND x = $int2"
f8.sql

// Contramapping Fragments

case class Person(name: String, age: Int)

val f9  = sql"INSERT ... VALUES ($varchar, $int4)"
f9.sql
val f10 = f9.contramap[Person](p => (p.name, p.age))
f10.sql

def countryQuery(name: Option[String], pop: Option[Int]): AppliedFragment = {

  // Our base query
  val base = sql"SELECT code FROM country"

  // Some filter conditions
  val nameLike       = sql"name LIKE $varchar"
  val popGreaterThan = sql"population > $int4"

  // Applied fragments for conditions, if any.
  val conds: List[AppliedFragment] =
    List(
      name.map(nameLike),
      pop.map(popGreaterThan)
    ).flatten

  import cats.syntax.foldable._

  // The composed filter.
  val filter =
    if (conds.isEmpty)
      AppliedFragment.empty
    else
      conds.foldSmash(void" WHERE ", void" AND ", AppliedFragment.empty)

  // Prepend the base query and we're done.
  base(Void) |+| filter
}

countryQuery(Some("Un%"), Some(99999)).fragment.sql

countryQuery(Some("Un%"), None).fragment.sql

countryQuery(None, Some(99999)).fragment.sql

countryQuery(None, None).fragment.sql

import cats.effect.IO

def usage(s: Session[IO]): IO[List[String]] = {
  val af: AppliedFragment    = countryQuery(Some("Un%"), None)
  val q: Query[af.A, String] = af.fragment.query(varchar)
  s.prepare(q)
    .flatMap(_.stream(af.argument, 64).compile.to(List))
}
