package com.meldmy.service

import cats.effect.Sync
import cats.effect.kernel.Ref
import cats.implicits.*
import library.Book
import library.BookNotFoundException
import library.Genre
import smithy4s.Timestamp

import java.util.UUID

trait BookRepository[F[_]] {

  def addBook(
    title: String,
    author: String,
    publishedDate: Timestamp,
    genre: Genre,
    coverImageUrl: Option[String] = None,
  ): F[Book]

  def updateBook(
    id: UUID,
    title: String,
    author: String,
    publishedDate: Timestamp,
    genre: Genre,
    coverImageUrl: Option[String],
  ): F[Book]

  def getBook(id: UUID): F[Book]

  def queryBooks(
    maxPageSize: Int = 20,
    pageToken: Option[String] = None,
    sortOrder: Option[String] = None,
  ): F[List[Book]]

}

def storeBook[F[_]: Sync](booksRef: Ref[F, Map[UUID, Book]], book: Book) = booksRef
  .update(_ + (book.id -> book))
  .as(book)

object BookRepository {

  def apply[F[_]: Sync](): F[BookRepository[F]] = Ref
    .of[F, Map[UUID, Book]](Map.empty)
    .map { booksRef =>
      new BookRepository[F] {
        override def addBook(
          title: String,
          author: String,
          publishedDate: Timestamp,
          genre: Genre,
          coverImageUrl: Option[String] = None,
        ): F[Book] = {
          val book = Book(
            UUID.randomUUID(),
            title,
            author,
            publishedDate,
            genre,
            coverImageUrl,
          )
          storeBook(booksRef, book)
        }

        override def updateBook(
          id: UUID,
          title: String,
          author: String,
          publishedDate: Timestamp,
          genre: Genre,
          coverImageUrl: Option[String],
        ): F[Book] =
          for {
            books <- booksRef.get
            _ <- raiseErrorWhenBookNotFound(id, books)
            newBook = Book(id, title, author, publishedDate, genre, coverImageUrl)
            result <- storeBook(booksRef, newBook)
          } yield result

        override def getBook(id: UUID): F[Book] = booksRef.get.map(_.get(id)).flatMap {
          case Some(book) => Sync[F].pure(book)
          case None => Sync[F].raiseError(new BookNotFoundException(s"Book with id $id not found"))
        }

        override def queryBooks(
          maxPageSize: Int,
          pageToken: Option[String],
          sortOrder: Option[String],
        ): F[List[Book]] = booksRef.get.map { books =>
          val sortedBooks =
            sortOrder match {
              case Some("asc")  => books.values.toList.sortBy(_.title)
              case Some("desc") => books.values.toList.sortBy(_.title).reverse
              case _            => books.values.toList
            }
          sortedBooks
            .slice(pageToken.getOrElse("0").toInt, pageToken.getOrElse("0").toInt + maxPageSize)
        }
      }
    }

  private def raiseErrorWhenBookNotFound[F[_]: Sync](id: UUID, books: Map[UUID, Book]) =
    books
      .get(id)
      .fold[F[Book]](
        Sync[F].raiseError(new BookNotFoundException(s"Book with id $id not found"))
      )(Sync[F].pure)

}
