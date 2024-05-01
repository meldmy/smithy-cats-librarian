package com.meldmy.service

import cats.effect.Sync
import cats.effect.kernel.Ref
import cats.implicits.*
import content.Content
import content.ContentGroup.CONTAINER
import content.ContentId

trait ContentRepository[F[_]] {
  def addContent(content: Content): F[List[Content]]

  def updateContent(content: Content): F[Content]

  def getContent(id: ContentId): F[Content]

  def queryContents(input: String): F[List[Content]]
}

object ContentRepository {

  def apply[F[_]: Sync](): F[ContentRepository[F]] = Ref
    .of[F, List[Content]](List.empty[Content])
    .map { ref =>
      new ContentRepository[F] {

        def addContent(content: Content): F[List[Content]] =
          // TODO: check if content id is already present in state
          content match {
            case Content(_, _, CONTAINER, _, None) =>
              throw new Exception("Playable ids must be defined")
            case Content(_, _, _, _, Some(playableIds)) =>
              throw new Exception("Playable ids must not be defined for playable")
            case _ => ref.updateAndGet(_ :+ content)
          }

        def updateContent(content: Content): F[Content] = ref.modify { actual =>
          // TODO: check container and playable ids like above
          val updated = actual.map(c =>
            if (c.id == content.id)
              content
            else
              c
          )
          (updated, content)
        }

        def getContent(id: ContentId): F[Content] = ref
          .get
          .map(_.find(x => x.id == id) match {
            case Some(value) => value
            case None        => throw new Exception(s"Content with id $id not found")
          })

        def queryContents(input: String): F[List[Content]] = ref
          .get
          .map(_.filter(content => calculateSimilarity(content.title.toString, input) >= 2))
      }
    }

  private def calculateSimilarity(s1: String, s2: String): Int = {
    val words1 = s1.split("\\s+")
    val words2 = s2.split("\\s+")
    words1.intersect(words2).length
  }

}
