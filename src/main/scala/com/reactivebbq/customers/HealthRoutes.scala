package com.reactivebbq.customers

import akka.actor.ActorSystem
import akka.cluster.{Cluster, MemberStatus}
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.directives.MethodDirectives.get
import akka.http.scaladsl.server.directives.RouteDirectives.complete

trait HealthRoutes {

  // we leave these abstract, since they will be provided by the App
  implicit def system: ActorSystem

  /**
    * Checks if we're part of an Akka Cluster.
    * These methods could equally be impleme
    */
  def getMemberStatus() =  Cluster(system).selfMember.status

  def joinedCluster: Boolean = getMemberStatus() == MemberStatus.up

  def leftCluster: Boolean = {
    val myMemberStatus = getMemberStatus()
    myMemberStatus == MemberStatus.down ||
    myMemberStatus == MemberStatus.removed
  }


  lazy val healthRoutes: Route =
    concat (
      path("alive") {
          get {
            if (joinedCluster)
              complete(StatusCodes.OK) //Other custom checks?
            else
              complete(StatusCodes.ServiceUnavailable)
          }
      },
      path("ready") {
        get {
         if (leftCluster)
           complete(StatusCodes.ServiceUnavailable)
         else
           complete(StatusCodes.OK) //Other custom checks?
        }
      }
    )


}
