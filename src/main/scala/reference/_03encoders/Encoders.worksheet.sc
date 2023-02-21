// Encoders

import skunk._
import skunk.implicits._
import skunk.codec.all._

sql"SELECT name FROM country WHERE code = $varchar"

sql"SELECT name FROM country WHERE code = $varchar AND population < $int8"

// Composite Encoders

sql"INSERT INTO person (name, age) VALUES (${varchar ~ int4})"

val enc = varchar ~ int4 ~ float4

sql"INSERT INTO person (comment, name, age, weight, comment) VALUES ($text, $enc)"

// Combinators

val enc2 = (varchar ~ int4).values

sql"INSERT INTO person (name, age) VALUES $enc2"

val enc3 = (varchar ~ int4).values

sql"INSERT INTO person (name, age) VALUES ${enc3.list(3)}"

// Transforming the Input Type

case class Person(name: String, age: Int)

val encPerson = (varchar ~ int4).values.contramap((p: Person) => p.name ~ p.age)

sql"INSERT INTO person (name, age) VALUES $encPerson"

val encPerson2 = (varchar ~ int4).values.gcontramap[Person]

sql"INSERT INTO person (name, age) VALUES $encPerson2"
