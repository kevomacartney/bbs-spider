import sbt._

object Resolvers {
  val defaultResolvers = Seq(
    Resolver.defaultLocal,
    "Confluent" at "https://packages.confluent.io/maven/"
  )
}
