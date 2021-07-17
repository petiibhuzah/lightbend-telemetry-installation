package com.reactivebbq.customers

//#json-support

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import spray.json.DefaultJsonProtocol

trait JsonSupport extends SprayJsonSupport {
  // import the default encoders for primitive types (Int, String, Lists etc)
  import DefaultJsonProtocol._

  implicit val createCustomerJsonFormat = jsonFormat5(CreateCustomerDTO)
  implicit val customerCreatedJsonFormat = jsonFormat2(CustomerCreatedDTO.apply)
  implicit val customerAlreadyExistsJsonFormat = jsonFormat2(CustomerAlreadyExistsDTO.apply)
  implicit val addOrderJsonFormat = jsonFormat1(AddOrderDTO)
  implicit val orderAddedJsonFormat = jsonFormat3(OrderAddedDTO.apply)
  implicit val getCustomerResponseDTOJsonFormat = jsonFormat5(GetCustomerResponseDTO.apply)
  implicit val ordersDTO = jsonFormat2(OrdersDTO.apply)

}
//#json-support
