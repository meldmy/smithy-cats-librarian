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

object BookRepository {

  def apply[F[_]: Sync](): F[BookRepository[F]] = Ref
    .of[F, Map[UUID, Book]](Map.empty)
    .map { booksRef =>
      new BookRepository[F] with InMemoryRepository[F, Book] {
        override def id(book: Book): UUID = book.id

        override def notFoundError(id: UUID): Throwable =
          new BookNotFoundException(s"Book with id $id not found")

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
          storeEntity(booksRef, book)
        }

        override def updateBook(
          id: UUID,
          title: String,
          author: String,
          publishedDate: Timestamp,
          genre: Genre,
          coverImageUrl: Option[String],
        ): F[Book] = {
          val book = Book(id, title, author, publishedDate, genre, coverImageUrl)
          updateEntity(booksRef, book)
        }

        override def getBook(id: UUID): F[Book] = getEntity(booksRef, id)

        override def queryBooks(
          maxPageSize: Int,
          pageToken: Option[String],
          sortOrder: Option[String],
        ): F[List[Book]] = queryEntities(booksRef, maxPageSize, pageToken, sortOrder, _.title)
      }
    }

}
