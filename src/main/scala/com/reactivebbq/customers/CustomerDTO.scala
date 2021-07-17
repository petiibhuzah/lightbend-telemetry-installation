package com.reactivebbq.customers

import com.reactivebbq.customers.CustomerProtocol._

trait CustomerDTO

case class CreateCustomerDTO(
    username: String,
    fullname: String,
    email: String,
    phone: String,
    address: String
) extends CustomerDTO {

  def toCustomer: Customer = Customer(
    username,
    fullname,
    email,
    phone,
    address
  )

}

case class GetCustomerResponseDTO(
  username: String,
  fullname: String,
  email: String,
  phone: String,
  address: String
)

object GetCustomerResponseDTO {
  def fromGetCustomerResponse(customerResponse: Option[GetCustomerResponse]): Option[GetCustomerResponseDTO] = {
    customerResponse.map { cr =>
      GetCustomerResponseDTO(
        username = cr.getCustomer.username,
        fullname = cr.getCustomer.fullname,
        email = cr.getCustomer.email,
        phone = cr.getCustomer.phone,
        address = cr.getCustomer.address
      )
    }
  }

}

case class CustomerCreatedDTO(
  msg: String = "Customer successfully created.",
  customerId: String
) extends CustomerDTO
object CustomerCreatedDTO {

  def fromCustomerCreated(customerCreated: CustomerCreated): CustomerCreatedDTO =
    CustomerCreatedDTO(
      customerId = customerCreated.customerId
    )

}

case class CustomerAlreadyExistsDTO(
  msg: String = "Customer Already Exists!",
  customerId: String
) extends CustomerDTO

object CustomerAlreadyExistsDTO {
  def fromCAE(customerAlreadyExists: CustomerAlreadyExists): CustomerAlreadyExistsDTO = {
    CustomerAlreadyExistsDTO(customerId = customerAlreadyExists.customerId)
  }
}

case class AddOrderDTO(orderId: String) extends CustomerDTO

case class OrderAddedDTO(
  msg: String = "Order successfully added.",
  customerId: String,
  orderId: String
) extends CustomerDTO

object OrderAddedDTO {
  def fromOrderAdded(maybeOrderAdded: Option[OrderAdded]): Option[OrderAddedDTO] = {
    maybeOrderAdded.map(orderAdded => OrderAddedDTO(
      customerId = orderAdded.customerId,
      orderId = orderAdded.orderId
    ))
  }
}

case class OrdersDTO(customerId: String, orders: Seq[String])
object OrdersDTO {
  def fromOrdersResponse(maybeOrders: Option[GetOrdersResponse]): Option[OrdersDTO] = {
    maybeOrders.map(gor => OrdersDTO(gor.customerId, gor.orders))
  }
}