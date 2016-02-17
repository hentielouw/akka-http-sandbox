# akka-http-sandbox
Playing around with akka-http and akka-streams

To start up the dependencies using Docker Compose, simply run `docker-compose up -d`. This will start a 
MongoDB instance and a RabbitMQ instance with management console on port 8080.

RabbitMQ events are routed via the `sandbox` topic exchange. This should be created manually using the management console.
The application will create the necessary queues on this exchange itself.
 
Persistence in MongoDB will be in the `events` db, in the `events` collection.