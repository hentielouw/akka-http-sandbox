name          := "akka-http-sandbox"
organization  := "hlouw"
version       := "1.0"
scalaVersion  := "2.11.7"

scalacOptions := Seq("-unchecked", "-deprecation", "-encoding", "utf8")

libraryDependencies ++= {
  val akkaV       = "2.4.2"
  val scalaTestV  = "2.2.6"
  Seq(
    "com.typesafe.akka" %% "akka-actor"                           % akkaV,
    "com.typesafe.akka" %% "akka-stream"                          % akkaV,
    "com.typesafe.akka" %% "akka-http-core"                       % akkaV,
    "com.typesafe.akka" %% "akka-http-experimental"               % akkaV,
    "com.typesafe.akka" %% "akka-http-spray-json-experimental"    % akkaV,
    "com.typesafe.akka" %% "akka-http-testkit"                    % akkaV % "test",
    "org.scalatest"     %% "scalatest"                            % scalaTestV % "test",
    "io.scalac"         %% "reactive-rabbit"                      % "1.0.3",
    "org.mongodb.scala" %% "mongo-scala-driver"                   % "1.1.0"
  )
}

Revolver.settings