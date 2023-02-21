// Queries

import cats.effect._
import skunk._
import skunk.implicits._
import skunk.codec.all._
import natchez.Trace.Implicits.noop // (1)                          // (1)

// Single-Column Query

val a: Query[Void, String] =
  sql"SELECT name FROM country".query(varchar)

// Multi-Column Query

val b: Query[Void, String ~ Int] =
  sql"SELECT name, population FROM country".query(varchar ~ int4)

// Mapping Query Results

case class Country(name: String, population: Int)

val c: Query[Void, Country] =
  sql"SELECT name, population FROM country"
    .query(varchar ~ int4) // (1)
    .map { case n ~ p => Country(n, p) } // (2)

// Mapping Decoder Results

val country: Decoder[Country] =
  (varchar ~ int4).map { case (n, p) => Country(n, p) } // (1)

val d: Query[Void, Country] =
  sql"SELECT name, population FROM country".query(country) // (2)

// Mapping Decoder Results Generically

val country2: Decoder[Country] =
  (varchar ~ int4).gmap[Country]

val c2: Query[Void, Country] =
  sql"SELECT name, population FROM country"
    .query(varchar ~ int4)
    .gmap[Country]

// Parameterized Query

val e: Query[String, Country] =
  sql"""
    SELECT name, population
    FROM   country
    WHERE  name LIKE $varchar
  """.query(country)

// Multi-Parameter Query

val f: Query[String ~ Int, Country] =
  sql"""
    SELECT name, population
    FROM   country
    WHERE  name LIKE $varchar
    AND    population < $int4
  """.query(country)

val session: Resource[IO, Session[IO]] =
  Session.single( // (2)
    host = "localhost",
    port = 5432,
    user = "jimmy",
    database = "world",
    password = Some("banana")
  )

session.use { s =>
  s.execute(a) // IO[List[String]]

  s.execute(b) // IO[List[String ~ Int]]

  s.execute(c) // IO[List[Country]]

  s.execute(d) // IO[List[Country]]

  s.execute(c2) // IO[List[Country]]

  s.prepare(e).flatMap { ps =>
    ps.stream("U%", 64)
      .evalMap(c => IO.println(c))
      .compile
      .drain
  } // IO[Unit]

  import fs2._
  val stream: Stream[IO, Unit] =
    for {
      ps <- Stream.eval(s.prepare(e))
      c  <- ps.stream("U%", 64)
      _  <- Stream.eval(IO.println(c))
    } yield ()
  stream.compile.drain // IO[Unit]

  s.prepare(f).flatMap { ps =>
    ps.stream("U%" ~ 2000000, 64)
      .evalMap(c => IO.println(c))
      .compile
      .drain
  } // IO[Unit]
}
