package com.meldmy.service

import cats.Show
import cats.effect.IO
import cats.effect.Resource
import com.meldmy.service.common.CommonTestTrait
import library.*
import smithy4s.Timestamp
import smithy4s.Timestamp.fromEpochMilli
import weaver.*
import weaver.scalacheck.*

import java.util.UUID

class DefaultLibraryServiceTest(global: GlobalRead)
  extends IOSuite
  with Checkers
  with CommonTestTrait {

  type Res = SharedRepositories
  def sharedResource: Resource[IO, SharedRepositories] = global.getOrFailR[SharedRepositories]()

  test("createBook should create a book correctly") { sharedRepositories =>
    val libraryService = DefaultLibraryService[IO](
      sharedRepositories.bookRepository,
      sharedRepositories.memberRepository,
    )

    forall(bookDataGen) { case (_, title, author, publishedDate, genre, coverImageUrl) =>
      for {
        book <- libraryService.createBook(title, author, publishedDate, genre, coverImageUrl)
        _ <- expect(book.title == title).failFast
        _ <- expect(book.author == author).failFast
        _ <- expect(book.publishedDate == publishedDate).failFast
        _ <- expect(book.genre == genre).failFast
        _ <- expect(book.coverImageUrl == coverImageUrl).failFast
      } yield success
    }
  }

  test("getBook should retrieve a book correctly") { sharedRepositories =>
    val libraryService = DefaultLibraryService[IO](
      sharedRepositories.bookRepository,
      sharedRepositories.memberRepository,
    )

    forall(bookDataGen) { case (_, title, author, publishedDate, genre, coverImageUrl) =>
      for {
        book <- libraryService.createBook(title, author, publishedDate, genre, coverImageUrl)
        retrievedBook <- libraryService.getBook(book.id)
        _ <- expect(retrievedBook == book).failFast
      } yield success
    }
  }

  test("updateBook should update a book correctly") { sharedRepositories =>
    val libraryService = DefaultLibraryService[IO](
      sharedRepositories.bookRepository,
      sharedRepositories.memberRepository,
    )

    forall(bookDataGen) { case (_, title, author, publishedDate, genre, coverImageUrl) =>
      for {
        originalBook <- libraryService.createBook(
          title,
          author,
          publishedDate,
          genre,
          coverImageUrl,
        )

        updatedTitle = "Updated Title"
        updatedAuthor = "Updated Author"
        updatedPublishedDate = Timestamp.fromEpochMilli(System.currentTimeMillis())
        updatedGenre = Genre.values((genre.intValue + 1) % Genre.values.size)
        updatedCoverImageUrl = Some("Updated Cover Image URL")

        _ <- libraryService.updateBook(
          originalBook.id,
          updatedTitle,
          updatedAuthor,
          updatedPublishedDate,
          updatedGenre,
          updatedCoverImageUrl,
        )

        updatedBook <- libraryService.getBook(originalBook.id)

        _ <- expect(updatedBook.title == updatedTitle).failFast
        _ <- expect(updatedBook.author == updatedAuthor).failFast
        _ <- expect(updatedBook.publishedDate == updatedPublishedDate).failFast
        _ <- expect(updatedBook.genre == updatedGenre).failFast
        _ <- expect(updatedBook.coverImageUrl == updatedCoverImageUrl).failFast
      } yield success
    }
  }

  test("getAllBooks should retrieve all books correctly") { sharedRepositories =>
    import cats.implicits.toTraverseOps

    val bookDataList = List
      .fill(3)(bookDataGen.sample.get)
      .map(gen => Book(UUID.randomUUID(), gen._2, gen._3, gen._4, gen._5, gen._6))

    for {
      bookRepository <- BookRepository[IO]()
      libraryService = DefaultLibraryService[IO](
        bookRepository,
        sharedRepositories.memberRepository,
      )
      createdBooks <- bookDataList.traverse(book =>
        libraryService.createBook(
          book.title,
          book.author,
          book.publishedDate,
          book.genre,
          book.coverImageUrl,
        )
      )

      retrievedBooks <- libraryService.getAllBooks().map(_.content)

      _ <- expect(retrievedBooks == createdBooks).failFast
    } yield success
  }

}
