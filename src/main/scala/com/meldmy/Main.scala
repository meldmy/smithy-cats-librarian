package com.meldmy

import cats.effect.IO
import cats.effect.IOApp
import com.comcast.ip4s.host
import com.comcast.ip4s.port
import com.meldmy.Swagger.swaggerRoutes
import com.meldmy.service.ContentRepository
import com.meldmy.service.ContentService
import content.ContentGroup.*
import content.ContentId
import content.ContentServiceGen
import content.ContentTitle
import content.ContentType.*
import org.http4s.Uri
import org.http4s.ember.client.EmberClientBuilder
import org.http4s.ember.server.EmberServerBuilder
import smithy4s.http4s
import smithy4s.http4s.SimpleRestJsonBuilder
import smithy4s.http4s.SimpleRestJsonBuilder.RouterBuilder
import smithy4s.http4s.swagger.docs

object Main extends IOApp.Simple {
  import cats.implicits.*

  def run: IO[Unit] =
    for {
      repo <- ContentRepository.apply[IO]()
      routerBuilder = SimpleRestJsonBuilder.routes(ContentService.apply(repo))
      server <- createServer(routerBuilder).evalMap(srv => IO.println(srv.addressIp4s)).useForever
    } yield server

  private def createServer(routerBuilder: RouterBuilder[ContentServiceGen, IO]) = routerBuilder
    .resource
    .map(serviceRoutes => swaggerRoutes <+> serviceRoutes)
    .flatMap { routes =>
      EmberServerBuilder
        .default[IO]
        .withPort(port"9077")
        .withHost(host"localhost")
        .withHttpApp(routes.orNotFound)
        .build
    }

}

object Swagger {
  val swaggerRoutes = docs[IO](ContentServiceGen)
}

object ClientMain extends IOApp.Simple {

  override def run: IO[Unit] = EmberClientBuilder.default[IO].build.use { c =>
    SimpleRestJsonBuilder(ContentServiceGen)
      .client(c)
      .uri(Uri.unsafeFromString("http://localhost:9077"))
      .resource
      .use { client =>
        for {
          _ <- client.createContent(
            ContentId("1"),
            CHANNEL,
            PLAYABLE,
            ContentTitle("Howard Stern 24/7"),
          )
          _ <- client.createContent(
            ContentId("2"),
            EPISODE,
            PLAYABLE,
            ContentTitle("Howard Stern - Interview with Dave Grohl"),
          )
          _ <- client.createContent(
            ContentId("3"),
            EPISODE,
            PLAYABLE,
            ContentTitle("Howard Stern - Metallica, Miley Cyrus, and Elton\nJohn"),
          )
          _ <- client.createContent(
            ContentId("4"),
            CHANNEL,
            PLAYABLE,
            ContentTitle("SiriusXM NFL Radio"),
          )
          _ <- client.createContent(
            ContentId("5"),
            SHOW,
            CONTAINER,
            ContentTitle("Howard Stern"),
          )
          _ <- client.createContent(
            ContentId("6"),
            CATEGORY,
            CONTAINER,
            ContentTitle("Sports"),
          )
          content <- client.queryContents("Alexa, play Howard Stern on SiriusXM")
          _ <- IO.println(content)
        } yield ()
      }
  }

}
