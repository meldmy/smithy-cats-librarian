import smithy4s.codegen.Smithy4sCodegenPlugin

lazy val root = project
  .in(file("."))
  .settings(
    name         := "contentful-test",
    version      := "0.1.1-SNAPSHOT",
    scalaVersion := "3.3.1",
    libraryDependencies ++= Seq(
      "com.disneystreaming.smithy4s" %% "smithy4s-core" % smithy4sVersion.value,
      "com.disneystreaming.smithy4s" %% "smithy4s-http4s" % smithy4sVersion.value,
      "com.disneystreaming.smithy4s" %% "smithy4s-http4s-swagger" % smithy4sVersion.value,
      "org.http4s" %% "http4s-ember-server" % "0.23.26",
      "org.http4s" %% "http4s-ember-client" % "0.23.26",
      "org.typelevel" %% "cats-effect" % "3.5.4",
      "org.typelevel" %% "cats-effect-std" % "3.5.4",

      "com.disneystreaming" %% "weaver-cats" % "0.8.3" % Test,
      "com.disneystreaming" %% "weaver-scalacheck" % "0.8.3" % Test
    ),
    fork := true,
    testFrameworks += new TestFramework("weaver.framework.CatsEffect")
  )
  .enablePlugins(Smithy4sCodegenPlugin)