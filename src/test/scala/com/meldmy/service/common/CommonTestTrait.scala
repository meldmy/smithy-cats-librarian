package com.meldmy.service.common

import cats.Show
import cats.effect.IO
import library.Book
import library.BookNotFoundException
import library.Genre
import library.Member
import library.MemberNotFoundException
import org.scalacheck.Gen
import smithy4s.Timestamp
import weaver.*
import weaver.scalacheck.*

import java.util.Calendar

trait CommonTestTrait extends IOSuite with Checkers {

  val timestampGen: Gen[Timestamp] = Gen.calendar.map { cal =>
    cal.set(Calendar.YEAR, cal.get(Calendar.YEAR) % 10000) // Limit the year to the range 0-9999
    Timestamp.fromEpochMilli(cal.getTimeInMillis)
  }

  val bookDataGen =
    for {
      id <- Gen.uuid
      title <- Gen.alphaStr
      author <- Gen.alphaStr
      publishedDate <- timestampGen
      genre <- Gen.oneOf(Genre.values)
      coverImageUrl <- Gen.alphaStr
    } yield (id, title, author, publishedDate, genre, Some(coverImageUrl))

  val memberDataGen =
    for {
      id <- Gen.uuid
      name <- Gen.alphaStr
      membershipNumber <- Gen.alphaStr
      membershipStartDate <- timestampGen
    } yield (id, name, membershipNumber, membershipStartDate)

  given Show[Timestamp] = Show.show(_ => s"Timestamp")

  given Show[Some[String]] = Show.show(_ => s"Some[String]")

  given Show[Genre] = Show.show(_ => s"Genre")

  val failWithMemberNotFoundException =
    (ex: Either[Throwable, Member]) =>
      ex match
        case Left(_: MemberNotFoundException) => success
        case _                                => failure("Expected MemberNotFoundException")

  val failWithBookNotFoundException =
    (ex: Either[Throwable, Book]) =>
      ex match
        case Left(_: BookNotFoundException) => success
        case _                              => failure("Expected BookNotFoundException")

}
