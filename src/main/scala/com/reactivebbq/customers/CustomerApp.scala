package com.reactivebbq.customers

//#quick-start-server
import akka.actor.{ActorRef, ActorSystem, Props}
import akka.cluster.sharding.{ClusterSharding, ClusterShardingSettings}
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.RouteConcatenation._
import akka.management.scaladsl.AkkaManagement
import akka.stream.ActorMaterializer
import com.typesafe.config.{Config, ConfigFactory}

import scala.concurrent.Await
import scala.concurrent.duration.Duration

//#main-class
object CustomerApp extends App with CustomerRoutes with HealthRoutes {

  var config: Config = ConfigFactory.load

  //This block of code allows us to run multiple instances of this cluster member
  //locally and avoid port clashes.
  if (args.length != 0) {

    if (args.length != 1) {
      println("Format: $ sbt \"run <akka remoting port>\"")
      println("Example: $ sbt \"run 2551\"")
      System.exit(1)
    }

    val akkaRemotingPort = args(0)
    val persistenceFolder = s"journal$akkaRemotingPort"

    //Create a port number for Akka Management
    val akkaManagementPort: String = (akkaRemotingPort.toInt + 6000).asInstanceOf[Integer].toString

    //Create a port number for Akka Management
    val httpPort: String = (akkaRemotingPort.toInt + 7000).asInstanceOf[Integer].toString

    // Create a port number for Prometheus
    val prometheusPort = (akkaRemotingPort.toInt + 6450)

    //Override configuration for the Akka Remoting port, and Akka Management HTTP port.
    //Normally, all of these would be defined in application.conf
    //akka.extensions = ["akka.persistence.journal.PersistencePluginProxyExtension"]
    config = ConfigFactory
      .parseString(
        "reactivebbq.customers.http.hostname=127.0.0.1" + "\n" +
          "reactivebbq.customers.http.port=" + httpPort + "\n" +
          "akka.remote.artery.canonical.port=" + akkaRemotingPort + "\n" +
          "akka.management.http.hostname=127.0.0.1" + "\n" +
          "akka.management.http.port=" + akkaManagementPort + "\n" +
          "cinnamon.prometheus.http-server.port=" + prometheusPort + "\n" +
          "akka.persistence.journal.leveldb.dir=target/" + persistenceFolder + "\n"
      )
      .withFallback(ConfigFactory.load)

  }

  // set up ActorSystem and other dependencies here
  implicit val system: ActorSystem = ActorSystem("CustomerCluster", config)

  //Start the Http Server when we've joined the cluster.
  //Cluster.get(system).registerOnMemberUp(startHttpServer)
  startHttpServer
  //Start the customer registry actor - a cluster sharded system.
  val customerRegistryActor: ActorRef = ClusterSharding(system).start(
    typeName = "customers",
    entityProps = Props[CustomerActor](),
    settings = ClusterShardingSettings(system),
    extractEntityId = CustomerActor.extractEntityId,
    extractShardId = CustomerActor.extractShardId
  )

  //The set of routes to be used by Akka Http
  lazy val routes: Route = concat(customerRoutes, healthRoutes)

  //Start Akka Management
  AkkaManagement.get(system).start()

  Await.result(system.whenTerminated, Duration.Inf)

  def startHttpServer = {
    val httpAddress = config.getString("reactivebbq.customers.http.hostname")
    val httpPort = config.getInt("reactivebbq.customers.http.port")

    //#http-server
    Http().newServerAt(httpAddress, httpPort).bindFlow(routes)
    log.info(s"Customer service online at http://{}:{}/", httpAddress, httpPort)

  }

}
