import smithy4s.codegen.Smithy4sCodegenPlugin

lazy val root = project
  .in(file("."))
  .settings(
    name         := "smithy-cats-librarian",
    version      := "0.1.1-SNAPSHOT",
    scalaVersion := "3.6.1",
    libraryDependencies ++= Seq(
      "com.disneystreaming.smithy4s" %% "smithy4s-core" % smithy4sVersion.value,
      "com.disneystreaming.smithy4s" %% "smithy4s-http4s" % smithy4sVersion.value,
      "com.disneystreaming.smithy4s" %% "smithy4s-http4s-swagger" % smithy4sVersion.value,
      "org.http4s" %% "http4s-ember-server" % "0.23.29",
      "org.http4s" %% "http4s-ember-client" % "0.23.30",
      "org.typelevel" %% "cats-effect" % "3.6-623178c",
      "org.typelevel" %% "cats-effect-std" % "3.6-623178c",

      "com.disneystreaming" %% "weaver-cats" % "0.8.4" % Test,
      "com.disneystreaming" %% "weaver-scalacheck" % "0.8.4" % Test
    ),
    fork := true,
    testFrameworks += new TestFramework("weaver.framework.CatsEffect")
  )
  .enablePlugins(Smithy4sCodegenPlugin)