addSbtPlugin("io.spray" % "sbt-revolver" % "0.9.1")

addSbtPlugin("com.typesafe.sbt" % "sbt-native-packager" % "1.7.6")

addSbtPlugin("com.thesamet" % "sbt-protoc" % "0.99.25")

libraryDependencies += "com.thesamet.scalapb" %% "compilerplugin" % "0.10.0"

addSbtPlugin("com.lightbend.cinnamon" % "sbt-cinnamon" % "2.16.1") // Do not show actor processing details

//addSbtPlugin("com.lightbend.cinnamon" % "sbt-cinnamon" % "2.13.1") //recommended