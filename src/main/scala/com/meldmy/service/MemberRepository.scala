package com.meldmy.service

import cats.effect.Sync
import cats.effect.kernel.Ref
import cats.implicits.*
import library.Member
import library.MemberNotFoundException
import smithy4s.Timestamp

import java.util.UUID

trait MemberRepository[F[_]] {
  def addMember(name: String, membershipNumber: String, membershipStartDate: Timestamp): F[Member]

  def updateMember(id: UUID, name: String, membershipNumber: String, membershipStartDate: Timestamp)
    : F[Member]

  def getMember(id: UUID): F[Member]

  def queryMembers(
    maxPageSize: Int = 20,
    pageToken: Option[String] = None,
    sortOrder: Option[String] = None,
  ): F[List[Member]]

}

object MemberRepository {

  def apply[F[_]: Sync](): F[MemberRepository[F]] = Ref
    .of[F, Map[UUID, Member]](Map.empty)
    .map { membersRef =>
      new MemberRepository[F] with InMemoryRepository[F, Member] {
        override def id(member: Member): UUID = member.id

        override def notFoundError(id: UUID): Throwable =
          new MemberNotFoundException(s"Member with id $id not found")

        override def addMember(
          name: String,
          membershipNumber: String,
          membershipStartDate: Timestamp,
        ): F[Member] = {
          val member = Member(
            UUID.randomUUID(),
            name,
            membershipNumber,
            membershipStartDate,
          )
          storeEntity(membersRef, member)
        }

        override def updateMember(
          id: UUID,
          name: String,
          membershipNumber: String,
          membershipStartDate: Timestamp,
        ): F[Member] = {
          val member = Member(id, name, membershipNumber, membershipStartDate)
          updateEntity(membersRef, member)
        }

        override def getMember(id: UUID): F[Member] = getEntity(membersRef, id)

        override def queryMembers(
          maxPageSize: Int,
          pageToken: Option[String],
          sortOrder: Option[String],
        ): F[List[Member]] = queryEntities(membersRef, maxPageSize, pageToken, sortOrder, _.name)
      }
    }

}
