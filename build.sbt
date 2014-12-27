name := """diff-sync-editor"""

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.1"

libraryDependencies ++= Seq(
  "org.clapper" %% "grizzled-slf4j" % "1.0.2",
  jdbc,
  anorm,
  cache,
  ws
)
