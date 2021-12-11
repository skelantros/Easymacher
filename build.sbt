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
  "org.tpolecat" %% "doobie-scalatest"   % doobieVersion
)

lazy val scalatestVersion = "3.2.10"
lazy val scalatestDependencies = Seq(
  "org.scalactic" %% "scalactic" % scalatestVersion,
  "org.scalatest" %% "scalatest" % scalatestVersion % "test"
)

lazy val simpleAuthDependencies = Seq(
  "org.reactormonk" %% "cryptobits" % "1.3"
)

lazy val auth0Dependencies = Seq(
  "com.pauldijou" %% "jwt-play-json" % "5.0.0",
  "com.pauldijou" %% "jwt-core" % "5.0.0",
  "com.auth0" % "jwks-rsa" % "0.20.0"
)

libraryDependencies ++= http4sDependencies
libraryDependencies ++= doobieDependencies
libraryDependencies ++= scalatestDependencies
libraryDependencies ++= simpleAuthDependencies
libraryDependencies ++= auth0Dependencies

libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.2.3"