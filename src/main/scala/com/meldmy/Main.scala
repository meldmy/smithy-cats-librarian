package com.meldmy

import cats.effect.IO
import cats.effect.IOApp
import cats.effect.Resource
import com.comcast.ip4s.host
import com.comcast.ip4s.port
import com.meldmy.Swagger.swaggerRoutes
import com.meldmy.service.BookRepository
import com.meldmy.service.DefaultLibraryService
import com.meldmy.service.MemberRepository
import library.Genre
import library.Genre.BIOGRAPHY
import library.LibraryServiceGen
import org.http4s.HttpRoutes
import org.http4s.Uri
import org.http4s.ember.client.EmberClientBuilder
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.server.Server
import smithy4s.Timestamp
import smithy4s.http4s
import smithy4s.http4s.SimpleRestJsonBuilder
import smithy4s.http4s.swagger.docs

//TODO: add more tests
object ServerMain extends IOApp.Simple {
  import cats.implicits.*

  def run: IO[Unit] =
    for {
      bookRepository <- BookRepository.apply[IO]()
      memberRepository <- MemberRepository.apply[IO]()
      libraryRoutes = SimpleRestJsonBuilder.routes(
        DefaultLibraryService.apply(bookRepository, memberRepository)
      )
      server <-
        createServer(libraryRoutes.resource)
          .evalMap((srv: Server) => IO.println(srv.addressIp4s))
          .useForever
    } yield server

  private def createServer(routes: Resource[IO, HttpRoutes[IO]]) = routes
    .map((serviceRoutes: HttpRoutes[IO]) => swaggerRoutes <+> serviceRoutes)
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
  val swaggerRoutes = docs[IO](LibraryServiceGen)
}

object ClientMain extends IOApp.Simple {

  override def run: IO[Unit] = EmberClientBuilder.default[IO].build.use { c =>
    SimpleRestJsonBuilder(LibraryServiceGen)
      .client(c)
      .uri(Uri.unsafeFromString("http://localhost:9077"))
      .resource
      .use { client =>
        for {
          _ <- client.createBook(
            "The Snowball: Warren Buffett and the Business of Life",
            "Alice Schroeder",
            Timestamp.fromEpochMilli(System.currentTimeMillis()),
            BIOGRAPHY,
            Some("https://m.media-amazon.com/images/I/71EsoCQzRtL._SL1500_.jpg"),
          )
          _ <- client.createBook(
            "Ray Dalio: Principles for Success",
            "Ray Dalio",
            Timestamp.fromEpochMilli(System.currentTimeMillis()),
            BIOGRAPHY,
            Some("https://m.media-amazon.com/images/I/61QX7I6f+cL._SL1500_.jpg"),
          )
          lastBook <- client.createBook(
            "The Changing World Order: Why Nations Succeed and Fail",
            "Ray Dalio",
            Timestamp.fromEpochMilli(System.currentTimeMillis()),
            BIOGRAPHY,
            Some("https://m.media-amazon.com/images/I/61b6uC-m2OL._SL1500_.jpg"),
          )
          receivedBook <- client.getBook(lastBook.id)
          _ <- IO.println(s"last book created: $receivedBook")
          firstBatch <- client.getAllBooks(maxPageSize = 2)
          _ <- IO.println(s"first batch: ${firstBatch.content.map(_.title)}")
          secondBatch <- client
            .getAllBooks(maxPageSize = 2, pageToken = firstBatch.metadata.nextPageToken)
          _ <- IO.println(s"second batch: ${secondBatch.content.map(_.title)}")
        } yield ()
      }
  }

}
