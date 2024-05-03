package com.meldmy.service

import cats.Show
import cats.effect.*
import com.meldmy.service.common.CommonTestTrait
import library.Member
import library.MemberNotFoundException
import org.scalacheck.Gen
import smithy4s.Timestamp
import weaver.*
import weaver.scalacheck.*

import java.util.Calendar

object MemberRepositoryTest extends SimpleIOSuite with Checkers with CommonTestTrait {

  test("adding and retrieving members should be consistent") {
    forall(memberDataGen) { case (_, name, membershipNumber, membershipStartDate) =>
      for {
        repo <- MemberRepository[IO]()
        member <- repo.addMember(name, membershipNumber, membershipStartDate)
        _ <- expect(member.name.equals(name)).failFast
        retrievedMember <- repo.getMember(member.id)
      } yield expect.same(member, retrievedMember)
    }
  }

  test("updateMember should update a member correctly") {
    forall(memberDataGen) { case (_, name, membershipNumber, membershipStartDate) =>
      for {
        repo <- MemberRepository[IO]()
        member <- repo.addMember(name, membershipNumber, membershipStartDate)
        updatedMember = member.copy(name = "Updated Name")
        _ <- repo.updateMember(
          member.id,
          updatedMember.name,
          updatedMember.membershipNumber,
          updatedMember.membershipStartDate,
        )
        retrievedMember <- repo.getMember(member.id)
      } yield expect.same(updatedMember, retrievedMember)
    }
  }

  test("should fail when member is not found") {
    forall(memberDataGen) { case (nonExistentId, name, membershipNumber, membershipStartDate) =>
      for {
        repo <- MemberRepository[IO]()
        exGet <- repo.getMember(nonExistentId).attempt
        _ <- failWithMemberNotFoundException(exGet).failFast
        ex <-
          repo
            .updateMember(nonExistentId, name, membershipNumber, membershipStartDate)
            .attempt
        _ <- failWithMemberNotFoundException(ex).failFast
      } yield success
    }
  }

  test("queryMembers should return the correct list of members") {
    forall(memberDataGen) { case (_, name, membershipNumber, membershipStartDate) =>
      for {
        repo <- MemberRepository[IO]()
        _ <- repo.addMember(name, membershipNumber, membershipStartDate)
        members <- repo.queryMembers()
      } yield expect(members.nonEmpty)
    }
  }
}
