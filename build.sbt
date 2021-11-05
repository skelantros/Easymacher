name := "Easymacher"

version := "0.1"

scalaVersion := "2.13.7"

val http4sVersion = "0.23.0-RC1"
lazy val http4sDependencies = Seq(
  "org.http4s" %% "http4s-dsl" % http4sVersion,
  "org.http4s" %% "http4s-blaze-server" % http4sVersion,
  "org.http4s" %% "http4s-blaze-client" % http4sVersion,
  "org.http4s" %% "http4s-circe" % http4sVersion,
  // Optional for auto-derivation of JSON codecs
  "io.circe" %% "circe-generic" % "0.14.1",
  // Optional for string interpolation to JSON model
  "io.circe" %% "circe-literal" % "0.14.1"
)

lazy val doobieVersion = "1.0.0-RC1"
lazy val doobieDependencies = Seq(
  "org.tpolecat" %% "doobie-core"     % doobieVersion,
  "org.tpolecat" %% "doobie-postgres" % doobieVersion,
  "org.tpolecat" %% "doobie-specs2"   % doobieVersion
)

lazy val scalatestVersion = "3.2.10"
lazy val scalatestDependencies = Seq(
  "org.scalactic" %% "scalactic" % scalatestVersion,
  "org.scalatest" %% "scalatest" % scalatestVersion % "test"
)

libraryDependencies ++= http4sDependencies
libraryDependencies ++= doobieDependencies
libraryDependencies ++= scalatestDependencies