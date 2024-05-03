package com.meldmy.service

import cats.effect.Sync
import cats.effect.std.Console
import cats.implicits.*
import library.*
import smithy4s.Timestamp

import java.util.UUID

//TODO: add tests
object DefaultLibraryService {

  def apply[F[_]: Sync: Console](
    bookRepository: BookRepository[F],
    memberRepository: MemberRepository[F],
  ): LibraryService[F] =
    new LibraryService[F] {
      override def createBook(
        title: String,
        author: String,
        publishedDate: Timestamp,
        genre: Genre,
        coverImageUrl: Option[String] = None,
      ): F[Book] = bookRepository.addBook(title, author, publishedDate, genre, coverImageUrl)

      override def updateBook(
        id: UUID,
        title: String,
        author: String,
        publishedDate: Timestamp,
        genre: Genre,
        coverImageUrl: Option[String],
      ): F[Book] = bookRepository.updateBook(id, title, author, publishedDate, genre, coverImageUrl)

      override def getBook(id: UUID): F[Book] = bookRepository.getBook(id)

      override def getAllBooks(
        maxPageSize: Int = 20,
        pageToken: Option[String] = None,
        sortOrder: Option[String] = None,
      ): F[GetAllBooksOutput] = {
        val content: F[List[Book]] = bookRepository.queryBooks(maxPageSize, pageToken, sortOrder)

        val token = nextPageToken(pageToken)
        // TODO: implement fetching host
        val host = "localhost:9077/books"
        val nextPage = Some(s"$host?pageSize=$maxPageSize&pageToken=$token&sortOrder=$sortOrder")

        content.map(GetAllBooksOutput(_, PaginationMetadata(sortOrder, token, nextPage)))
      }

      // TODO: Implement pagination with DynamoDB
      private def nextPageToken(pageToken: Option[String]): Option[String] =
        pageToken match {
          case Some(token) => Some((token.toInt + 1).toString)
          case None        => Some("1")
        }

      override def createMember(
        name: String,
        membershipNumber: String,
        membershipStartDate: Timestamp,
      ): F[Member] = memberRepository.addMember(name, membershipNumber, membershipStartDate)

      override def getMember(id: UUID): F[Member] = memberRepository.getMember(id)

      override def getAllMembers(
        maxPageSize: Int = 20,
        pageToken: Option[String] = None,
        sortOrder: Option[String] = None,
      ): F[GetAllMembersOutput] = {
        val members: F[List[Member]] = memberRepository.queryMembers(
          maxPageSize,
          pageToken,
          sortOrder,
        )

        val token = nextPageToken(pageToken)
        // TODO; implement fetching host
        val host = "localhost:9077/members"
        val nextPage = Some(s"$host?pageSize=$maxPageSize&pageToken=$token&sortOrder=$sortOrder")

        members.map(GetAllMembersOutput(_, PaginationMetadata(sortOrder, token, nextPage)))
      }
    }

}
