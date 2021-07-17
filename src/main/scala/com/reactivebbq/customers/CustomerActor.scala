package com.reactivebbq.customers

import akka.actor._
import akka.cluster.sharding.ShardRegion
import akka.http.javadsl.model.DateTime
import akka.persistence.{PersistentActor, RecoveryCompleted}
import com.reactivebbq.customers.CustomerActor._
import com.reactivebbq.customers.CustomerProtocol._

/**
 * This is just an example: cluster sharding would be overkill for just keeping a small amount of data,
 * but becomes useful when you have a collection of 'heavy' actors (in terms of processing or state)
 * so you need to distribute them across several nodes.
 */
object CustomerActor {

  final case class CustomerState(
    customerId: String,
    customer: Customer,
    lastModified: String,
    orders: Seq[String]
  )

  def getCustomerId(customer: Customer): String = customer.username

  //Rule of thumb: # of Shards should be 10 times greater than the maximum
  //number of cluster nodes.
  val numberOfShards = 30

  val extractEntityId: ShardRegion.ExtractEntityId = {
    case nc: NewCustomer => (getCustomerId(nc.getCustomer), nc)
    case gc: GetCustomer => (gc.customerId, gc)
    case ano: AddOrder => (ano.customerId, ano)
    case gao: GetAllOrders => (gao.customerId, gao)
  }

  val extractShardId: ShardRegion.ExtractShardId = {
    case nc: NewCustomer => (Math.abs(getCustomerId(nc.getCustomer).hashCode) % numberOfShards).toString
    case gc: GetCustomer => (Math.abs(gc.customerId.hashCode) % numberOfShards).toString
    case ano: AddOrder => (Math.abs(ano.customerId.hashCode) % numberOfShards).toString
    case gao: GetAllOrders => (Math.abs(gao.customerId.hashCode) % numberOfShards).toString
  }

}

class CustomerActor extends PersistentActor with ActorLogging {

  override def persistenceId: String = "Customer-" + self.path.name

  private var maybeCustomerData: Option[CustomerState] = None

  override def receiveRecover: Receive = {
    case customerCreated: CustomerCreated =>
      log.debug("Recovering Customer: {}", customerCreated.customerId)
      setInitialCustomerData(customerCreated.customerId, customerCreated.getCustomer)
    case AddOrder(customerId, orderId, _) => addOrder(customerId, orderId)
    case RecoveryCompleted => log.debug("Completed recovery of {}", persistenceId)
    case msg => log.warning("Unhandled recovery message: {}", msg)
  }

  override def receiveCommand: Receive = {

    case newCustomer: NewCustomer =>
      createCustomer(newCustomer, sender()) match {
        case Some(customerCreated) => persist(customerCreated)(cc => sender() ! cc)
        case None => sender() ! CustomerAlreadyExists(getCustomerId(newCustomer.getCustomer))
      }

    case GetCustomer(customerId, _) => sender() ! getCustomer(customerId)

    case orderToAdd: AddOrder =>
      addOrder(orderToAdd.customerId, orderToAdd.orderId) match {
        case Some(orderAdded) => persist(orderAdded)(oa => sender() ! Some(oa))
        case None => sender() ! None
      }

    case GetAllOrders(customerId, _) => sender() ! getOrders(customerId)

    case msg => log.error("Received unhandled message: " + msg.toString)

  }

  private def createCustomer(newCustomer: NewCustomer, sender: ActorRef): Option[CustomerCreated] = {
    val newCustomerId = getCustomerId(newCustomer.getCustomer)
    maybeCustomerData match {
      case Some(_) => None
      case None =>
        setInitialCustomerData(newCustomerId, newCustomer.getCustomer)
        Some(CustomerCreated(newCustomerId, Some(newCustomer.getCustomer)))

    }
  }

  private def setInitialCustomerData(customerId: String, customer: Customer): Unit = {
    maybeCustomerData = Some(
      CustomerState(
        customerId,
        customer,
        DateTime.now().toIsoDateTimeString,
        Seq.empty[String]
      )
    )

  }

  private def getCustomer(customerId: String): Option[GetCustomerResponse] = {
    getCustomerDataOrShutdown.map { customerData =>
      GetCustomerResponse(customerId, Some(customerData.customer))
    }
  }

  private def addOrder(customerId: String, orderId: String): Option[OrderAdded] = {
    getCustomerDataOrShutdown.map { customerData =>
      maybeCustomerData = Some(customerData.copy(orders = (customerData.orders :+ orderId).distinct))
      OrderAdded(customerId, orderId)
    }
  }

  private def getOrders(customerId: String): Option[GetOrdersResponse] = {
    maybeCustomerData.map { customerData =>
      GetOrdersResponse(customerId, customerData.orders)
    }
  }

  //If we receive a message that expects that we have populated user data (e.g. get orders),
  //and we do not, we will need to shut ourselves down as we represent a non-existent entity.
  private def getCustomerDataOrShutdown: Option[CustomerState] = {
    maybeCustomerData match {
      case None =>
        context.stop(self)
        None
      case Some(state) => Some(state)
    }
  }

}
