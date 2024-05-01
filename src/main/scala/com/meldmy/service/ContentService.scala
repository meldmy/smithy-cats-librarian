package com.meldmy.service

import cats.effect.Sync
import cats.effect.std.Console
import cats.implicits.*
import content.*

object ContentService {

  def apply[F[_]: Sync: Console](repo: ContentRepository[F]): ContentService[F] =
    new ContentService[F] {

      override def getContent(id: ContentId): F[Content] =
        for {
          _ <- Console[F].println(s"getContent($id)")
          response <- repo.getContent(id)
        } yield response

      override def createContent(
        id: ContentId,
        typeC: ContentType,
        group: ContentGroup,
        title: ContentTitle,
        containerPlayableIds: Option[List[ContentId]] = None,
      ): F[CreateContentOutput] =
        for {
          _ <- Console[F].println(s"createContent($id, $typeC, $title)")
          _ <- repo.addContent(Content(id, typeC, group, title, containerPlayableIds))
        } yield CreateContentOutput(id)

      override def updateContent(
        id: ContentId,
        typeC: ContentType,
        group: ContentGroup,
        title: ContentTitle,
        containerPlayableIds: Option[List[ContentId]] = None,
      ): F[Content] =
        for {
          _ <- Console[F].println(s"updateContent($id, $typeC, $group, $title)")
          updatedContent <- repo.updateContent(
            Content(id, typeC, group, title, containerPlayableIds)
          )
        } yield updatedContent

      override def queryContents(input: String): F[QueryContentsOutput] =
        for {
          _ <- Console[F].println(s"queryContents($input)")
          response <- repo.queryContents(input)
        } yield QueryContentsOutput(response)
    }

}
