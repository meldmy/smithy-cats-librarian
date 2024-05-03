package com.meldmy.service

import cats.effect.Sync
import cats.effect.kernel.Ref
import cats.implicits.*

import java.util.UUID

trait InMemoryRepository[F[_]: Sync, E] {

  def id(entity: E): UUID

  def notFoundError(id: UUID): Throwable

  def storeEntity(entitiesRef: Ref[F, Map[UUID, E]], entity: E): F[E] = entitiesRef
    .update(_ + (id(entity) -> entity))
    .as(entity)

  def updateEntity(entitiesRef: Ref[F, Map[UUID, E]], entityToUpdate: E): F[E] =
    for {
      entities <- entitiesRef.get
      entity <- Sync[F].fromOption(
        entities.get(id(entityToUpdate)),
        notFoundError(id(entityToUpdate)),
      )
      _ <- storeEntity(entitiesRef, entityToUpdate)
    } yield entityToUpdate

  def getEntity(entitiesRef: Ref[F, Map[UUID, E]], id: UUID)
    : F[E] = entitiesRef.get.map(_.get(id)).flatMap {
    case Some(entity) => Sync[F].pure(entity)
    case None         => Sync[F].raiseError(notFoundError(id))
  }

  def queryEntities(
    entitiesRef: Ref[F, Map[UUID, E]],
    maxPageSize: Int = 20,
    pageToken: Option[String] = None,
    sortOrder: Option[String] = None,
    sortFunction: E => String,
  ): F[List[E]] = entitiesRef.get.map { entities =>
    val sortedEntities =
      sortOrder match {
        case Some("asc")  => entities.values.toList.sortBy(sortFunction)
        case Some("desc") => entities.values.toList.sortBy(sortFunction).reverse
        case _            => entities.values.toList
      }
    sortedEntities
      .slice(pageToken.getOrElse("0").toInt, pageToken.getOrElse("0").toInt + maxPageSize)
  }

  def raiseErrorWhenEntityNotFound(id: UUID, entities: Map[UUID, E]): F[Unit] =
    Sync[F].raiseWhen(!entities.contains(id))(notFoundError(id))
}
