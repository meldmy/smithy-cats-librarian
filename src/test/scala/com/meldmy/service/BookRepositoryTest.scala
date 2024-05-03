package com.meldmy.service

import cats.Show
import cats.effect.*
import com.meldmy.service.common.CommonTestTrait
import library.Book
import library.BookNotFoundException
import library.Genre
import org.scalacheck.Gen
import smithy4s.Timestamp
import weaver.*
import weaver.scalacheck.*

import java.util.Calendar

object BookRepositoryTest extends SimpleIOSuite with Checkers with CommonTestTrait {

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

  test("queryBooks should return the correct list of books") {
    forall(bookDataGen) { case (_, title, author, publishedDate, genre, coverImageUrl) =>
      for {
        repo <- BookRepository[IO]()
        _ <- repo.addBook(title, author, publishedDate, genre, coverImageUrl)
        books <- repo.queryBooks()
      } yield expect(books.nonEmpty)
    }
  }
}
