package com.reactivebbq.customers

//#customer-routes-spec
//#test-top
import java.util.concurrent.TimeUnit

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.cluster.sharding.{ClusterSharding, ClusterShardingSettings}
import akka.http.scaladsl.marshalling.Marshal
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.testkit.{RouteTestTimeout, ScalatestRouteTest}
import akka.testkit.TestKit
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import scala.concurrent.duration.FiniteDuration

//#set-up
class CustomerRoutesSpec extends AnyWordSpec with Matchers with ScalaFutures with ScalatestRouteTest
    with CustomerRoutes {
  //#test-top

  // Here we need to implement all the abstract members of CustomerRoutes.
  // We use the real CustomerRegistryActor to test it while we hit the Routes,
  // but we could "mock" it by implementing it in-place or by using a TestProbe()

  override def createActorSystem(): ActorSystem =
    ActorSystem("CustomerCluster", testConfig)

  val customerRegistryActor: ActorRef = ClusterSharding(system).start(
    typeName = "Customer",
    entityProps = Props[CustomerActor](),
    settings = ClusterShardingSettings(system),
    extractEntityId = CustomerActor.extractEntityId,
    extractShardId = CustomerActor.extractShardId
  )
  lazy val routes = customerRoutes

  implicit def default(implicit system: ActorSystem) = RouteTestTimeout(new FiniteDuration(5, TimeUnit.SECONDS))

  override def afterAll() = {
    TestKit.shutdownActorSystem(system)
  }

  val joshCustomer = CreateCustomerDTO(
    username = "jajo",
    fullname = "Josh Jo",
    phone = "1-613-454-1212",
    email = "josh123@lightbend.com",
    address = "625 Market St"
  )

  val joshCustomerResponse = GetCustomerResponseDTO(
    username = "jajo",
    fullname = "Josh Jo",
    phone = "1-613-454-1212",
    email = "josh123@lightbend.com",
    address = "625 Market St"
  )

  //#actual-test
  "CustomerRoutes" should {

    "be able to add customers (POST /customers)" in {
      val customerEntity = Marshal(joshCustomer).to[MessageEntity].futureValue // futureValue is from ScalaFutures

      val request = Post("/customers").withEntity(customerEntity)

      request ~> routes ~> check {
        status should ===(StatusCodes.Created)

        contentType should ===(ContentTypes.`application/json`)

        entityAs[CustomerCreatedDTO] should ===(CustomerCreatedDTO(customerId = joshCustomer.username))
      }
    }

    "be able to retrieve a customer" in {

      // using the RequestBuilding DSL:
      val request = Get("/customers/jajo")

      request ~> routes ~> check {
        status should ===(StatusCodes.OK)

        contentType should ===(ContentTypes.`application/json`)

        entityAs[GetCustomerResponseDTO] should ===(joshCustomerResponse)
      }
    }

    "be able to return not found for a non-existent customer" in {
      val request = Get("/customers/Idontexist")

      request ~> Route.seal(routes) ~> check {
        response.status should ===(StatusCodes.NotFound)
      }
    }

    "be able to add an order to a customer" in {
      val addOrderDTO = AddOrderDTO("1234")

      val orderEntity = Marshal(addOrderDTO).to[MessageEntity].futureValue

      val request = Put("/customers/jajo/addorder").withEntity(orderEntity)

      request ~> routes ~> check {
        status should ===(StatusCodes.OK)

        contentType should ===(ContentTypes.`application/json`)

        entityAs[OrderAddedDTO] should ===(OrderAddedDTO(customerId = "jajo", orderId = "1234"))
      }
    }

    "return not found when adding an order to a non existent user" in {
      val addOrderDTO = AddOrderDTO("1234")

      val orderEntity = Marshal(addOrderDTO).to[MessageEntity].futureValue

      val request = Put("/customers/doesntexist/addorder").withEntity(orderEntity)

      request ~> Route.seal(routes) ~> check {
        response.status should ===(StatusCodes.NotFound)

      }
    }

    "retrieve all orders" in {
      val request = Get("/customers/jajo/orders")

      request ~> routes ~> check {
        status should ===(StatusCodes.OK)

        contentType should ===(ContentTypes.`application/json`)

        entityAs[OrdersDTO] should ===(OrdersDTO("jajo", Seq("1234")))
      }
    }

    "retrieve all orders where customer doesn't exist" in {
      val request = Get("/customers/doesntexit/orders")

      request ~> Route.seal(routes) ~> check {
        response.status should ===(StatusCodes.NotFound)

      }
    }

  }
}
