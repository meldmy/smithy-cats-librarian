package com.meldmy.service

import cats.Show
import cats.effect.*
import library.Book
import library.BookNotFoundException
//import org.scalacheck.Arbitrary.arbitrary
import library.Genre
import org.scalacheck.Gen
import smithy4s.Timestamp
import weaver.*
import weaver.scalacheck.*

import java.util.Calendar

object BookRepositoryTest extends SimpleIOSuite with Checkers {

  private val timestampGen: Gen[Timestamp] = Gen.calendar.map { cal =>
    cal.set(Calendar.YEAR, cal.get(Calendar.YEAR) % 10000) // Limit the year to the range 0-9999
    Timestamp.fromEpochMilli(cal.getTimeInMillis)
  }

  private val bookDataGen =
    for {
      id <- Gen.uuid
      title <- Gen.alphaStr
      author <- Gen.alphaStr
      publishedDate <- timestampGen
      genre <- Gen.oneOf(Genre.values)
      coverImageUrl <- Gen.alphaStr
    } yield (id, title, author, publishedDate, genre, Some(coverImageUrl))

  test("adding and retrieving books should be consistent") {
    forall(bookDataGen) { case (_, title, author, publishedDate, genre, coverImageUrl) =>
      for {
        repo <- BookRepository[IO]()
        book <- repo.addBook(title, author, publishedDate, genre, coverImageUrl)
        _ <- expect(book.title.equals(title)).failFast
        retrievedBook <- repo.getBook(book.id)
      } yield expect.same(book, retrievedBook)
    }
  }

  test("updateBook should update a book correctly") {
    forall(bookDataGen) { case (_, title, author, publishedDate, genre, coverImageUrl) =>
      for {
        repo <- BookRepository[IO]()
        book <- repo.addBook(title, author, publishedDate, genre, coverImageUrl)
        updatedBook = book.copy(title = "Updated Title")
        _ <- repo.updateBook(
          book.id,
          updatedBook.title,
          updatedBook.author,
          updatedBook.publishedDate,
          updatedBook.genre,
          updatedBook.coverImageUrl,
        )
        retrievedBook <- repo.getBook(book.id)
      } yield expect.same(updatedBook, retrievedBook)
    }
  }

  test("should fail when book is not found") {
    forall(bookDataGen) {
      case (nonExistentId, title, author, publishedDate, genre, coverImageUrl) =>
        for {
          repo <- BookRepository[IO]()
          exGet <- repo.getBook(nonExistentId).attempt
          _ <- failWithBookNotFoundException(exGet).failFast
          ex <-
            repo
              .updateBook(nonExistentId, title, author, publishedDate, genre, coverImageUrl)
              .attempt
          _ <- failWithBookNotFoundException(ex).failFast
        } yield success
    }
  }

  private val failWithBookNotFoundException =
    (ex: Either[Throwable, Book]) =>
      ex match
        case Left(_: BookNotFoundException) => success
        case _                              => failure("Expected BookNotFoundException")

  test("queryBooks should return the correct list of books") {
    forall(bookDataGen) { case (_, title, author, publishedDate, genre, coverImageUrl) =>
      for {
        repo <- BookRepository[IO]()
        _ <- repo.addBook(title, author, publishedDate, genre, coverImageUrl)
        books <- repo.queryBooks()
      } yield expect(books.nonEmpty)
    }
  }

  private given Show[Timestamp] = Show.show(_ => s"Timestamp")

  private given Show[Genre] = Show.show(_ => s"Genre")

  private given Show[Some[String]] = Show.show(_ => s"Some[String]")
}
