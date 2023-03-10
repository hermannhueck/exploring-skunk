package tutorial._01queries

import cats.effect._
import skunk._
import skunk.implicits._
import skunk.codec.all._
import java.time.OffsetDateTime
import natchez.Trace.Implicits.noop
import _root_.util._

object QueryExample extends IOApp.Simple {

  // a source of sessions
  val session: Resource[IO, Session[IO]] =
    Session.single(
      host = "localhost",
      user = "jimmy",
      database = "world",
      password = Some("banana")
    )

  // a data model
  case class Country(name: String, code: String, population: Int)

  // a simple query
  val simple: Query[Void, OffsetDateTime] =
    sql"select current_timestamp".query(timestamptz)

  // an extended query
  val extended: Query[String, Country] =
    sql"""
      SELECT name, code, population
      FROM   country
      WHERE  name like $text
    """
      .query(varchar ~ bpchar(3) ~ int4)
      .gmap[Country]

  // run our simple query
  def doSimple(s: Session[IO]): IO[Unit] =
    for {
      ts <- s.unique(simple) // we expect exactly one row
      _  <- IO.println(s"timestamp is $ts")
    } yield ()

  // run our extended query
  def doExtended(s: Session[IO]): IO[Unit] =
    s.prepare(extended).flatMap { ps =>
      ps.stream("U%", 32)
        .evalMap(c => IO.println(c))
        .compile
        .drain
    }

  // our entry point
  val run: IO[Unit] =
    session.use { s =>
      for {
        _ <- IO.println(line80.green)
        _ <- doSimple(s)
        _ <- doExtended(s)
        _ <- IO.println(line80.green)
      } yield ()
    }
}
