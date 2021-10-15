## Antaeus

Antaeus (/ænˈtiːəs/), in Greek mythology, a giant of Libya, the son of the sea god Poseidon and the Earth goddess Gaia. He compelled all strangers who were passing through the country to wrestle with him. Whenever Antaeus touched the Earth (his mother), his strength was renewed, so that even if thrown to the ground, he was invincible. Heracles, in combat with him, discovered the source of his strength and, lifting him up from Earth, crushed him to death.

Welcome to our challenge.

## The challenge

As most "Software as a Service" (SaaS) companies, Pleo needs to charge a subscription fee every month. Our database contains a few invoices for the different markets in which we operate. Your task is to build the logic that will schedule payment of those invoices on the first of the month. While this may seem simple, there is space for some decisions to be taken and you will be expected to justify them.

## Instructions

Fork this repo with your solution. Ideally, we'd like to see your progression through commits, and don't forget to update the README.md to explain your thought process.

Please let us know how long the challenge takes you. We're not looking for how speedy or lengthy you are. It's just really to give us a clearer idea of what you've produced in the time you decided to take. Feel free to go as big or as small as you want.

## Developing

Requirements:
- \>= Java 11 environment

Open the project using your favorite text editor. If you are using IntelliJ, you can open the `build.gradle.kts` file and it is gonna setup the project in the IDE for you.

### Building

```
./gradlew build
```

### Running

There are 2 options for running Anteus. You either need libsqlite3 or docker. Docker is easier but requires some docker knowledge. We do recommend docker though.

*Running Natively*

Native java with sqlite (requires libsqlite3):

If you use homebrew on MacOS `brew install sqlite`.

```
./gradlew run
```

*Running through docker*

Install docker for your platform

```
docker build -t antaeus
docker run antaeus
```

### App Structure
The code given is structured as follows. Feel free however to modify the structure to fit your needs.
```
├── buildSrc
|  | gradle build scripts and project wide dependency declarations
|  └ src/main/kotlin/utils.kt 
|      Dependencies
|
├── pleo-antaeus-app
|       main() & initialization
|
├── pleo-antaeus-core
|       This is probably where you will introduce most of your new code.
|       Pay attention to the PaymentProvider and BillingService class.
|
├── pleo-antaeus-data
|       Module interfacing with the database. Contains the database 
|       models, mappings and access layer.
|
├── pleo-antaeus-models
|       Definition of the Internal and API models used throughout the
|       application.
|
└── pleo-antaeus-rest
        Entry point for HTTP REST API. This is where the routes are defined.
```

### Main Libraries and dependencies
* [Exposed](https://github.com/JetBrains/Exposed) - DSL for type-safe SQL
* [Javalin](https://javalin.io/) - Simple web framework (for REST)
* [kotlin-logging](https://github.com/MicroUtils/kotlin-logging) - Simple logging framework for Kotlin
* [JUnit 5](https://junit.org/junit5/) - Testing framework
* [Mockk](https://mockk.io/) - Mocking library
* [Sqlite3](https://sqlite.org/index.html) - Database storage engine

Happy hacking 😁!


## Implementation Observations and Decisions

- First session with the code used in reviewing it, understanding all the parts and polish my Kotlin. Start thinking on 
the problem, solution and architecture needed. 

- There are two main parts / problems: 
  1) The logic of the call to the paymentProvider and the invoice lifecycle.
  2) The scheduling part of payments on the first of each month
 
### 1 PaymenProvider and Invoice Lifecycle
#### Payment provider and status sync

Once we made the charge of an invoice using the PaymentProvider, and if it sucessfull, we have to update the invoice
status and this could be a problem if there is any problem updating the status in the database, we could face two 
scenarios:
 
 - the payment is made and the status is not updated, so it will eventually be charged again (bad for the client and 
our claims department) 
 - the payment is not made and the status is updated to paid, so it is not charged (bad for us as we are not getting paid)

Both scenarios are bad for the business and have to be taken care of properly by avoiding them. 

We can try to solve the first scenario by updating the status after calling the billing service. If the update fails, the
invoice with be still "pending" so it maybe be sent to the paymentservice to be charged again.
However as we know that the paymentservice is an external service, for example a restfull service, we are going to 
assume that it is protected to several calls to the same endpoint with the same data (i.e. is idempotent).
If not we should raise a bug to the maintainers of the service to make it idempotent :)

This is also an enforcement of the single responsibility principle.

#### who is responsible marks as unpaid the invoices for the next month?

Once all invoices are paid and the month is "closed", the invoices to the next month should be "created". 
The billingService should not be worried about that, it only has one task: to charge unpaid invoices.

We can assume that there is another service that generates the new invoices to be paid for the next month for each of the
customers.
This service should:
- Have a log of when each invoice has been charged, this way it could track which invoices generate for the next month.
- Implement the logic for the invoices that haven't been paid: notify the customer?, remove it? try again? how many times?
- Generate the new invoices to be paid the next month.
In order to implement such service changes to the DB structure must be done: A table for the historic payments.

#### Changes
Creation of a method to update the invoice status. We only expose to change the status instead of all object as the
invoices should not be changed once they are emitted (they are a sort of immutable object)