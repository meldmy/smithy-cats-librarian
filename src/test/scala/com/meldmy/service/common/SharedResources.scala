package com.meldmy.service

import cats.effect.IO
import cats.effect.Resource
import weaver.GlobalResource
import weaver.GlobalWrite
import weaver.LowPriorityImplicits
import weaver.ResourceTag

object SharedResources extends GlobalResource with LowPriorityImplicits {

  def sharedResources(global: GlobalWrite): Resource[IO, Unit] =
    for {
      bookRepository <- Resource.eval(BookRepository[IO]())
      memberRepository <- Resource.eval(MemberRepository[IO]())
      _ <- global.putR(SharedRepositories(memberRepository, bookRepository))
    } yield ()

}

case class SharedRepositories(
  memberRepository: MemberRepository[IO],
  bookRepository: BookRepository[IO],
)
