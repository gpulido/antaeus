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


#### Currency mismatch management

When an invoice has a different currency than the customer the paymentService refuses to make the charge.
What to do next is up to the business logic, some options:
- Don't force the payment and just let it as unpaid. Notify it so it can be handled externally.
- Use a currency conversion provider to calculate the amount of money from the invoice currency to the customer currency,
update the invoice with the new data and try to charge it again. We can do this conversion before calling the PaymentProvider
to avoid the double call to the PaymentProvider. This CurrencyProvider could be an external api as the exact amount 
probably depends on the exchange values for the day. Also, should it be changed for every month for such invoice
currency exchange rates are not the same from month to month.

As this requires to be discussed with another "part" (payments), I consider also that in real life this kind of situation
for a subscription is rare, I will implement the first option. In real life a conversation to clarify requirements 
is needed.

#### Missing customer management

Again we can decide just to notify the problem. Externally to the BillingService should be decided if the invoice has 
to be deleted or fixed.

#### Changes made outside the BillingService
- Creation of a method to update the invoice status.
- Adding a new dal method to retrieve the invoices by state. This allows to filter them using the db

### 2 Scheduling 

With the BillingService taking care of charging the pending invoices, now is time to decide how to expose and call the
service monthly.
We need to call it at least for the first day of the month. There are several options:

- Execute a timer in a separate thread  running inside the same application. This timer could be set to execute a 
function each day that would check if the day is the first of a month: if it is the first of the month it should call the
chargePendingInvoices method from the BillingService; otherwise it will set another execution the next day. It can make the
right calculations to avoid drifting. This option is easy to implement, but it would need to keep a long-running thread
alongside the application. Also, it would couple the "scheduler" with the code, so if we decide to change the charge interval
to charge each 4 months or yearly, we need to change the code and redeploy the entire app.

- As we have a rest service already running, add an endpoint to allow to execute the method. 
Then we can use an external scheduler like setting a cron job that just make the call to the endpoint each month (that is very easy to configure in 
cron) or we can develop a service similar to the timer on the first option but instead of calling the function it can call
the service, so it is decoupled allowing changes just to the logic of "scheduling" without the need of deploying again.
It has one con: if we want to implement retries or manage the different scenarios of a failing charge, we will need to
expand the endpoint to return more information about the "charge invoices" result (not just the number of invoices
charged for example) or it has to have access to the logging / historical information from the billing service.

- There are more complex solutions using queues or brokers that could be discussed if needed.

I will implement the endpoint solution, returning the number of failing invoices (this way the calling process could at least
know that something went wrong directly) and adding a NotificationProvider.

As an example a Schedule independent aplication has been added that will check every day if it is the first of the
month and use the provided enpoint to make all the charges. This app is very basic and could be also replaced with a 
monthly cron job with a curl call (for example).
It has been implemented this way as a starting point to add more logic to the management of failing charges. However the
that is completely isolated from the billing application

#### To retry or not retry (and when)

Every invoice that could not be charged is keep as "pending". So after the first attempt of charging all invoices at the
first of the month, some of them would be still pending. For this challenge, we are only going to try to
retry the ones that failed with the NetworkException, and is going to be done immediately following the usual pattern 
of waiting an incremental amount of time between each call.

#### Generating the new invoices from the subscriptions

For this code, the invoices to be paid each month are already generated, so this code doesn't create new invoices for the
subscriptions but this is something that has to be done somewhere: every month a new set of invoices for each of the customers
subscriptions must be generated and store into the db with the "pending" status, so the scheduler could try to charge them.
Also, any invoice that could not be paid for any reason need to be managed and probably removed from the automatic 
system, so they don't keep piling up.

The scenario and code presented for the challenge is a very simplify process of a whole customer contract/subscription 
billing process.

Diary
- Billing Services
- Endpoint
- Parallel and retries
- Schedule app

Improvements
Use coroutines, ktor for the client, use environment variables for the api instead of hardcoded
Error management in client. Add mechanism to ensure that the scheduler is running in case of anormal exit.
More advanced scheduler using Quartz for example that provides cancellation, persistence, etc...  
