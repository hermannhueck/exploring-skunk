package tutorial._00setup

import cats.effect._
import skunk._
import skunk.implicits._
import skunk.codec.all._
import natchez.Trace.Implicits.noop // (1)                          // (1)
import _root_.util._

object Hello extends IOApp.Simple {

  val session: Resource[IO, Session[IO]] =
    Session.single( // (2)
      host = "localhost",
      port = 5432,
      user = "jimmy",
      database = "world",
      password = Some("banana")
    )

  val run: IO[Unit] =
    session.use { s => // (3)
      for {
        _ <- IO.println(line80.green)
        d <- s.unique(sql"select current_date".query(date)) // (4)
        _ <- IO.println(s"The current date is $d.")
        _ <- IO.println(line80.green)
      } yield ()
    }
}
