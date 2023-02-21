// Commands

import cats.effect._
import skunk._
import skunk.implicits._
import skunk.codec.all._
import natchez.Trace.Implicits.noop // (1)                          // (1)

// Simple Command

val a: Command[Void] =
  sql"SET SEED TO 0.123".command

// Parameterized Command

val c: Command[String] =
  sql"DELETE FROM country WHERE name = $varchar".command

// Contramapping Commands

case class Info(code: String, hos: String)

val update2: Command[Info] =
  sql"""
    UPDATE country
    SET    headofstate = $varchar
    WHERE  code = ${bpchar(3)}
  """
    .command // Command[String ~ String]
    .contramap { case Info(code, hos) => code ~ hos } // Command[Info]

val update3: Command[Info] =
  sql"""
    UPDATE country
    SET    headofstate = $varchar
    WHERE  code = ${bpchar(3)}
  """
    .command // Command[String ~ String]
    .gcontramap[Info] // Command[Info]

// List Parameters

def deleteMany(n: Int): Command[List[String]] =
  sql"DELETE FROM country WHERE name IN (${varchar.list(n)})".command

val delete3 = deleteMany(3) // takes a list of size 3

def insertMany(n: Int): Command[List[(String, Short)]] = {
  val enc = (varchar ~ int2).values.list(n)
  sql"INSERT INTO pets VALUES $enc".command
}

val insert3 = insertMany(3)

def insertExactly(ps: List[(String, Short)]): Command[ps.type] = {
  val enc = (varchar ~ int2).values.list(ps)
  sql"INSERT INTO pets VALUES $enc".command
}

val pairs = List[(String, Short)](("Bob", 3), ("Alice", 6))
// pairs: List[(String, Short)] = List(("Bob", 3), ("Alice", 6))

// Note the type!
val insertPairs = insertExactly(pairs)

val session: Resource[IO, Session[IO]] =
  Session.single( // (2)
    host = "localhost",
    port = 5432,
    user = "jimmy",
    database = "world",
    password = Some("banana")
  )

session.use { s =>
  s.execute(a) // IO[Completion]

  s.prepare(c).flatMap { pc =>
    pc.execute("xyzzy") *>
      pc.execute("fnord") *>
      pc.execute("blech")
  } // IO[Completion]

  import cats.syntax.traverse._
  s.prepare(c).flatMap { pc =>
    List("xyzzy", "fnord", "blech").traverse(s => pc.execute(s))
  } // IO[List[Completion]]

  import fs2.Stream
  import skunk.data.Completion
  val stream: Stream[IO, Completion] = Stream.eval(s.prepare(c)).flatMap { pc =>
    Stream("xyzzy", "fnord", "blech").through(pc.pipe)
  }                     // Stream[IO, Completion]
  stream.compile.toList // IO[List[Completion]]

  s.prepare(update2).flatMap { pc =>
    pc.execute(Info("USA", "George W. Bush")) *>
      pc.execute(Info("CAN", "Jean Chretien"))
  } // IO[Completion]

  s.prepare(update3).flatMap { pc =>
    pc.execute(Info("USA", "George W. Bush")) *>
      pc.execute(Info("CAN", "Jean Chretien"))
  } // IO[Completion]

  s.prepare(delete3).flatMap { pc =>
    pc.execute(List("United States", "Canada", "Mexico"))
  } // IO[Completion]

  s.prepare(insert3).flatMap { pc =>
    pc.execute(List(("Fido", 3), ("Rover", 5), ("Spot", 7)))
  } // IO[Completion]

  s.prepare(insertPairs).flatMap { pc =>
    pc.execute(pairs)
  } // IO[Completion]

// s.prepare(insertPairs).flatMap { pc => pc.execute(pairs.drop(1)) }
// error: type mismatch;
//  found   : List[(String, Short)]
//  required: repl.MdocSession.MdocApp.pairs.type
// s.prepare(insertPairs).flatMap { pc => pc.execute(pairs.drop(1)) }
//                                                   ^^^^^^^^^^^^^
}
