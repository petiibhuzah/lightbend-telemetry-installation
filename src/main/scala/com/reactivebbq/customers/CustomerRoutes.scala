package com.reactivebbq.customers

import akka.actor.{ ActorRef, ActorSystem }
import akka.event.Logging

import scala.concurrent.duration._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.directives.MethodDirectives.get
import akka.http.scaladsl.server.directives.MethodDirectives.post
import akka.http.scaladsl.server.directives.RouteDirectives.complete
import akka.http.scaladsl.server.directives.PathDirectives.path

import scala.concurrent.Future
import akka.pattern.ask
import akka.util.Timeout
import com.reactivebbq.customers.CustomerProtocol._

import scala.util.{ Failure, Success }

//Use default execution context - tune later
import scala.concurrent.ExecutionContext.Implicits.global

//#customer-routes-class
trait CustomerRoutes extends JsonSupport {

  // we leave these abstract, since they will be provided by the App
  implicit def system: ActorSystem

  lazy val log = Logging(system, classOf[CustomerRoutes])

  // other dependencies that CustomerRoutes use
  def customerRegistryActor: ActorRef

  // Required by the `ask` (?) method below
  implicit lazy val timeout = Timeout(60.seconds) // usually we'd obtain the timeout from the system's configuration

  //#all-routes
  //#customers-get-post
  //#customers-get-delete
  lazy val customerRoutes: Route =
  pathPrefix("customers") {
    //POST /customers/
    post {
      entity(as[CreateCustomerDTO]) { customerDTO =>
        val newCustomer = customerDTO.toCustomer
        onComplete(customerRegistryActor ? NewCustomer(Some(newCustomer))) {
          case Success(response) => response match {
            case cc: CustomerCreated =>
              complete(StatusCodes.Created, Some(CustomerCreatedDTO.fromCustomerCreated(cc)))
            case cae: CustomerAlreadyExists =>
              complete(StatusCodes.BadRequest, Some(CustomerAlreadyExistsDTO.fromCAE(cae)))
          }
          case Failure(err) =>
            err.printStackTrace()
            complete(StatusCodes.InternalServerError, "Message: " + err.getMessage)
        }
      }
    } ~
    pathPrefix(Segment) { customerId =>
      pathEnd {
        //GET /customers/(customerID/username)
        get {
          //#retrieve-customer-info
          val futureMaybeCustomer: Future[Option[GetCustomerResponse]] =
            (customerRegistryActor ? GetCustomer(customerId))
              .mapTo[Option[GetCustomerResponse]]

          rejectEmptyResponse {
            complete(
              futureMaybeCustomer
                .map(GetCustomerResponseDTO.fromGetCustomerResponse))
          }
          //#retrieve-customer-info
        }
      } ~
      path("addorder") {
        //PUT /customers/(customerID/username)/addorder
        put {
          entity(as[AddOrderDTO]) { addOrder =>
            val futureMaybeOrderAdded =
              (customerRegistryActor ? AddOrder(customerId, addOrder.orderId)).mapTo[Option[OrderAdded]]

            rejectEmptyResponse {
              complete(futureMaybeOrderAdded.map(OrderAddedDTO.fromOrderAdded))
            }

          }
        }
      } ~
      //GET /customers/(customerID/username)/orders
      path("orders") {
        get {
          val futureMaybeOrders =
            (customerRegistryActor ? GetAllOrders(customerId)).mapTo[Option[GetOrdersResponse]]

          rejectEmptyResponse {
            complete(futureMaybeOrders.map(OrdersDTO.fromOrdersResponse))
          }

        }
      }
    } //GET /customers/(customerID/username)

  }
}
