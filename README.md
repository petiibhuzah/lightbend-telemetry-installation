# Customers Service 

This is a simple Akka based application for managing customers as part of the Reactive BBQ microservice system. It is built using Akka HTTP, Akka Cluster Sharding and Akka Persistence.

## Running the Application

> sbt run

To run another instance on a different port:

> sbt "run 2552"

Akka remoting will run on port 2552, the Customers service HTTP port will run on 9551 and Akka Management will runo n 8551.

> sbt "run 2553"

Akka remoting will run on port 2553, the Customers service HTTP port will run on 9553 and Akka Management will runo n 8553.

And so on...

Note that node 2551 must be running to form a cluster as it is the first seed node.

## Using the Application

All of the commands below assume default ports (no arguments specified).

### Create a Customer

```
 curl -H "Content-type: application/json" -X POST -d '{"username": "MrX", "fullname": "John Smith", "email": "john.smith@example.com", "phone": "613-555-1212", "address": "123 Fake St." }' http://localhost:9551/customers
```

### Get a Customer

This assumes the customer from the last command was created:
```
 curl -X GET http://localhost:9551/customers/MrX
```

### Add Orders to the Customer

```
 curl -H "Content-type: application/json" -X PUT -d '{"orderId": "12345" }' http://localhost:9551/customers/MrX/addorder
```

### Get all Orders for a Customer

```
 curl -X GET http://localhost:9551/customers/MrX/orders
```

## Running Tests

> sbt test