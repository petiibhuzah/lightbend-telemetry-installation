package com.reactivebbq.customers

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.cluster.sharding.{ClusterSharding, ClusterShardingSettings}
import akka.testkit.{ImplicitSender, TestKit}
import com.reactivebbq.customers.CustomerProtocol._
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatest.{BeforeAndAfter, BeforeAndAfterAll}

class CustomerActorSpec extends TestKit(ActorSystem("CustomerCluster")) with ImplicitSender
    with AnyWordSpecLike with Matchers with BeforeAndAfterAll with BeforeAndAfter {

  val customerRegistryActor: ActorRef = ClusterSharding(system).start(
    typeName = "Customer",
    entityProps = Props[CustomerActor](),
    settings = ClusterShardingSettings(system),
    extractEntityId = CustomerActor.extractEntityId,
    extractShardId = CustomerActor.extractShardId
  )

  override def afterAll() = {
    TestKit.shutdownActorSystem(system)
  }

  val billyCustomer = Customer(
    username = "Billy",
    fullname = "Billy Bob",
    phone = "1-613-555-1212",
    email = "billy123@lightbend.com",
    address = "625 Market St"
  )

  val billyId = "Billy"

  val joshCustomer = Customer(
    username = "Josh",
    fullname = "Josh Jo",
    phone = "1-613-454-1212",
    email = "josh123@lightbend.com",
    address = "625 Market St"
  )

  "Customer registry actor" must {
    /** Note Tests are not distinct in favour of execution time, thus order matters **/

    "return None for a customer that does not exist" in {
      customerRegistryActor ! GetCustomer("1234")
      expectMsg(None)
    }

    "create a new customer and return it" in {
      createAndValidateCustomer(billyCustomer)
    }

    "create multiple customers and retrieve each" in {
      val joshID = createAndValidateCustomer(joshCustomer)

      customerRegistryActor ! GetCustomer(joshID)
      expectMsg(Some(GetCustomerResponse(joshID, Some(joshCustomer))))

      customerRegistryActor ! GetCustomer(billyId)
      expectMsg(Some(GetCustomerResponse(billyId, Some(billyCustomer))))
    }

    "add an order to a customer" in {
      customerRegistryActor ! AddOrder(billyId, "12345")
      expectMsg(Some(OrderAdded(billyId, "12345")))
    }

    "add and retrieve multiple orders from a customer" in {
      customerRegistryActor ! AddOrder(billyId, "789")
      expectMsg(Some(OrderAdded(billyId, "789")))

      customerRegistryActor ! GetAllOrders(billyId)
      expectMsg(Some(GetOrdersResponse(billyId, Seq("12345", "789"))))

    }

  }

  private def createAndValidateCustomer(customer: Customer): String = {

    //Create the customer
    customerRegistryActor ! NewCustomer(Some(customer))

    val cc: CustomerCreated =
      expectMsgPF() {
        case custCreated @ CustomerCreated(id, msg, _) =>
          //Retrieve the customer (and any existing customers)
          customerRegistryActor ! GetCustomer(id)
          expectMsgType[Option[GetCustomerResponse]]
          custCreated
      }
    cc.customerId
  }

}
