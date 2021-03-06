import Dependencies._
import sbt.Keys._
import sbt._

name := "ton-api"

organization := "io.broxus"

scalaVersion := "2.12.8"

lazy val `ton-client` = RootProject(uri("https://github.com/broxus/ton-client.git#8962af32b1cef888df4fc2430cb9fa1a55c4f32a"))

lazy val `ton-api` = (project in file("."))
    .enablePlugins(PlayScala)
    .settings(
        libraryDependencies ++= Seq(
            guice
        )
    )
    .dependsOn(
        `ton-client`
    )

sources in (Compile, doc) := Seq.empty
publishArtifact in (Compile, packageDoc) := false
publishArtifact in (Compile, packageSrc) := false

sources in (Production,doc) := Seq.empty
publishArtifact in (Production, packageDoc) := false
publishArtifact in (Production, packageSrc) := false
