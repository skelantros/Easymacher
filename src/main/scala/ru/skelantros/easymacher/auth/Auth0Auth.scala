package ru.skelantros.easymacher.auth

import java.time.Clock

import cats.data.{EitherT, Kleisli, OptionT}
import cats.effect.Sync
import com.auth0.jwk.{Jwk, UrlJwkProvider}
import org.http4s.{AuthedRoutes, HttpRoutes, Request}
import org.http4s.dsl.Http4sDsl
import pdi.jwt.{JwtAlgorithm, JwtBase64, JwtClaim, JwtJson}
import ru.skelantros.easymacher.db.{Mistake, Thr, UserDb}
import ru.skelantros.easymacher.entities.User
import cats.implicits._
import org.http4s.headers.Cookie
import org.http4s.server.AuthMiddleware
import org.typelevel.ci.CIString

import scala.util.{Failure, Success, Try}

class Auth0Auth[F[_] : Sync](config: Auth0Auth.Config)(implicit db: UserDb.Auth0[F]) extends AuthWare[F] {
  import Auth0Auth._

  val dsl = new Http4sDsl[F] {}
  import dsl._

  private val Config(domain, audience) = config
  private val issuer = s"https://$domain"

  type OrThrowable[A] = Either[Throwable, A]
  type OrThrT[A] = EitherT[F, Throwable, A]

  def apply(service: UserRoutes[F]): HttpRoutes[F] = middleware(service)

  private val authUser: Kleisli[F, Request[F], Either[String, User]] = Kleisli { request =>
    val res = for {
      authHeader <- EitherT(request.headers.get(CIString("authorization")).toRight("No authorization header found").pure[F])
      token <- authHeader.head.value match {
        case headerTokenRegex(t) => EitherT((Right(t) : Either[String, String]).pure[F])
        case _ => EitherT((Left("Invalid authorization token") : Either[String, String]).pure[F])
      }
      user <- EitherT(userFromJwt(token))
    } yield user
    res.value
  }
  private val onFailure: AuthedRoutes[String, F] = Kleisli(req => OptionT.liftF(Forbidden(req.context)))

  private val middleware: AuthMiddleware[F, User] = AuthMiddleware(authUser, onFailure)

  private implicit def clock = Clock.systemUTC()

  private def userFromJwt(token: String): F[Either[String, User]] =
    for {
      jwtClaims <- Sync[F].blocking(validateJwt(token))
      claimsEither = jwtClaims.toEither.leftMap(_.getMessage)
      res <- claimsEither match {
        case Right(claims) if claims.subject.nonEmpty =>
          val sub = claims.subject.get
          val dbRes = db.findByAuth0Id(sub)
          dbRes.flatMap {
            case Right(Some(u)) => (Right(u) : Either[String, User]).pure[F]
            case Right(None) => db.addByAuth0Id(sub) map {
              case Right(u) => Right(u)
              case Left(Mistake(msg)) => Left(msg)
              case Left(Thr(t)) => Left("Internal server error")
            }
            case Left(Mistake(msg)) => (Left(msg) : Either[String, User]).pure[F]
            case Left(Thr(t)) => (Left("Internal server error") : Either[String, User]).pure[F]
          }
        case Right(_) => (Left("Your jwt doesn't have subject claims") : Either[String, User]).pure[F]
        case Left(m) => (Left(m) : Either[String, User]).pure[F]
      }
    } yield res

  private def validateJwt(token: String): Try[JwtClaim] = for {
    jwk <- getJwk(token)           // Get the secret key for this token
    claims <- JwtJson.decode(token, jwk.getPublicKey, Seq(JwtAlgorithm.RS256)) // Decode the token using the secret key
    _ <- validateClaims(claims)     // validate the data stored inside the token
  } yield claims


  private def getJwk(token: String) =
    (splitToken andThen decodeElements) (token) flatMap {
      case (header, _, _) =>
        val jwtHeader = JwtJson.parseHeader(header)     // extract the header
        val jwkProvider = new UrlJwkProvider(s"https://$domain")

        // Use jwkProvider to load the JWKS data and return the JWK
        jwtHeader.keyId.map { k =>
          Try(jwkProvider.get(k))
        } getOrElse Failure(new Exception("Unable to retrieve kid"))
    }

  private val validateClaims = (claims: JwtClaim) =>
    if (claims.isValid(issuer, audience)) {
      Success(claims)
    } else {
      Failure(new Exception("The JWT did not pass validation"))
    }


}

object Auth0Auth {
  case class Config(domain: String, audience: String)
  val jwtRegex = """(.+?)\.(.+?)\.(.+?)""".r
  val headerTokenRegex = """Bearer (.+?)""".r

  // Splits a JWT into three components
  private val splitToken = (jwt: String) => jwt match {
    case jwtRegex(header, body, sig) => Success((header, body, sig))
    case _ => Failure(new Exception("Token does not match the correct pattern"))
  }

  // Takes three components of JWT wrapped into Try, returns decoded counterpart
  private val decodeElements = (data: Try[(String, String, String)]) => data map {
    case (header, body, sig) =>
      (JwtBase64.decodeString(header), JwtBase64.decodeString(body), sig)
  }
}