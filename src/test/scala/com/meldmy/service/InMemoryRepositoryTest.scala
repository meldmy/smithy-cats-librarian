package com.meldmy.service

import cats.Show
import cats.effect.IO
import cats.effect.kernel.Ref
import cats.effect.unsafe.implicits.global
import cats.implicits.toTraverseOps
import org.scalacheck.Gen
import weaver.*
import weaver.scalacheck.*

import java.util.UUID

object InMemoryRepositoryTest extends SimpleIOSuite with Checkers {

  test("storeEntity should store an entity correctly") {
    forall(entityDataGen) { case entity: TestEntity =>
      for {
        entitiesRef <- Ref.of[IO, Map[UUID, TestEntity]](Map.empty)
        repo = createRepo()
        _ <- repo.storeEntity(entitiesRef, entity)
        entities <- entitiesRef.get
      } yield expect(entities.contains(entity.id))
    }
  }

  test("updateEntity should update an entity correctly") {
    forall(entityDataGen) { case entity: TestEntity =>
      for {
        entitiesRef <- Ref.of[IO, Map[UUID, TestEntity]](Map.empty)
        repo = createRepo()
        _ <- repo.storeEntity(entitiesRef, entity)
        newEntity = entity.copy(name = "Updated Name")
        _ <- repo.updateEntity(entitiesRef, newEntity)
        entities <- entitiesRef.get
      } yield expect(entities(entity.id) == newEntity)
    }
  }

  test("getEntity should retrieve an entity correctly") {
    forall(entityDataGen) { case entity: TestEntity =>
      for {
        entitiesRef <- Ref.of[IO, Map[UUID, TestEntity]](Map.empty)
        repo = createRepo()
        _ <- repo.storeEntity(entitiesRef, entity)
        retrievedEntity <- repo.getEntity(entitiesRef, entity.id)
      } yield expect(retrievedEntity == entity)
    }
  }

  // check overlapped pages
  test("queryEntities should return the correct list of entities with pagination") {
    val pageSize = 2
    val numberOfEntities = 5 // create 100 entities

    forall(Gen.listOfN(numberOfEntities, entityDataGen)) { entitiesData =>
      for {
        entitiesRef <- Ref.of[IO, Map[UUID, TestEntity]](Map.empty)
        repo = createRepo(_ => "Not found err")
        _ <- entitiesData.traverse { entity =>
          repo.storeEntity(entitiesRef, entity)
        }
        pages <- (0 until numberOfEntities by pageSize).toList.traverse { pageNumber =>
          repo.queryEntities(
            entitiesRef,
            maxPageSize = pageSize,
            pageToken = Some(pageNumber.toString),
            sortOrder = None,
            _.name,
          )
        }
      } yield {
        val flattenedPages = pages.flatten
        val noOverlap = pages.zipWithIndex.forall { case (page, index) =>
          val nextIndex = index + 1
          nextIndex >= pages.size || !pages(nextIndex).exists(page.contains)
        }
        expect(flattenedPages.size == numberOfEntities) and expect(noOverlap)
      }
    }
  }

  test("raiseErrorWhenEntityNotFound should raise an error when an entity is not found") {
    val expectedExceptionMsg = (id: UUID) => s"Entity with id $id not found"

    forall(entityDataGen) { case entity: TestEntity =>
      for {
        entitiesRef <- Ref.of[IO, Map[UUID, TestEntity]](Map.empty)
        repo = createRepo(expectedExceptionMsg)
        error <- repo.raiseErrorWhenEntityNotFound(entity.id, Map.empty).attempt
      } yield expect(error match {
        case Left(e: CustomNotFoundException) => e.getMessage == expectedExceptionMsg(entity.id)
        case _                                => false
      })
    }
  }

  private def createRepo(expectedExceptionMsg: UUID => String = _ => "Not found err") =
    new InMemoryRepository[IO, TestEntity] {
      override def id(entity: TestEntity): UUID = entity.id

      override def notFoundError(id: UUID): Throwable =
        new CustomNotFoundException(expectedExceptionMsg(id))
    }

  private case class TestEntity(id: UUID, name: String)

  private given Show[TestEntity] = Show.show { case TestEntity(id, name) =>
    s"TestEntity(id = $id, name = $name)"
  }

  private val entityDataGen =
    for {
      id <- Gen.uuid
      name <- Gen.alphaStr
    } yield TestEntity(id, name)

  class CustomNotFoundException(msg: String) extends Exception(msg)
}
