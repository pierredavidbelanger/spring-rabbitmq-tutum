# Fun with Docker, Java, Spring Boot, Maven, RabbitMQ and Tutum

This is a log of my experimentation session playing with [Docker](https://www.docker.com/) to locally build with (containerized) [Maven](https://maven.apache.org/) a simple (also containerized) Java [Spring Boot](http://projects.spring.io/spring-boot/) application that send/receive to/from (also containerized) [RabbitMQ](https://www.rabbitmq.com/) before deploying the complete stack on [Tutum](https://www.tutum.co/) in a production like environment.

## We need

### For the local build part

* A UNIX like machine with:
* A shell
* Docker running (see Docker Machine)
* A browser
* GIT
* No, I did not forgot Java or Maven here, we do not need them.

### For the deployment part

* A UNIX like machine with:
* A shell
* Docker running
* A Tutum account
* A Tutum supported cloud hosting provider account (I will use DigitalOcean)

## Get the code

First, get the code. This will do the job:

```bash
$ git clone https://github.com/pierredavidbelanger/spring-rabbitmq-tutum
$ cd spring-rabbitmq-tutum
```

## Build the application

The application is a simple 2 classes Spring Boot web application that allows one to call URL to `/send` a message to RabbitMQ, and `/receive` it. The class [Application](src/main/java/ca/pjer/rabbitmqclient/Application.java) is the entry point, and [QueueController](src/main/java/ca/pjer/rabbitmqclient/QueueController.java) the REST controller.

### Create a data volume to store Maven dependencies

We create a data only container that will be used as a cache for the Maven build process:

```bash
$ docker run --name maven-data -it -v /root/.m2 maven:3-jdk-8 true
```

This part is easy: the new container is named `maven-data` so we can use it later. We declare a volume inside the container at `/root/.m2`, this is where Maven store its repository. We run the command `true` interactively, so the container will run and quit immediately after having created the volume.

You can see that the container is created:

```bash
$ docker ps -a
CONTAINER ID        IMAGE               COMMAND             CREATED             STATUS                     PORTS               NAMES
c2a339afe450        maven:3-jdk-8       "true"              2 seconds ago       Exited (0) 1 seconds ago                       maven-data
```

### Execute Maven

We run a new autoremove containerized Maven (linked to the previous data volume) to build the application:

```bash
$ docker run -it --rm --volumes-from maven-data -v "$PWD":/usr/src/maven -w /usr/src/maven maven:3-jdk-8 mvn clean package
```

This is the actual Java build part. We run interactively the command `mvn clean package` from the `maven:3-jdk-8` image. This command is run in the host current directory (where the [pom.xml](pom.xml) is), this is working because we tell docker to mount a volume from `"$PWD"` on the host to `/usr/src/maven` in the container, and we tell docker to use the container `/usr/src/maven` path as the working directory. Also, we mounted the volume from `maven-data`, this way Maven will reuse its cached dependencies the next time we compile.

### Create the container image

Now the containerization of our application:

```bash
$ docker build -t rabbitmq-client .
```

This process uses the [Dockerfile](Dockerfile) file. We named our image `rabbitmq-client`.

We can see our newly created image:

```bash
$ docker images
REPOSITORY                                     TAG                 IMAGE ID            CREATED             VIRTUAL SIZE
rabbitmq-client                                latest              883df650a28c        About an hour ago   779.8 MB
```

## Run the application locally

We will try our new application locally. But first we need to start a local RabbitMQ.

### Start RabbitMQ

```bash
$ docker run -d --name rabbitmq -e RABBITMQ_PASS="mypass" -p 5672:5672 -p 15672:15672 tutum/rabbitmq
```

This command started a single node RabbitMQ instance named `rabbitmq` from the image `tutum/rabbitmq`. We also published port `5672` and `15672` and set the `admin` password to `mypass`.

We can have a look at the logs to see it is running:

```bash
$ docker logs rabbitmq
=> Securing RabbitMQ with a preset password
=> Done!
========================================================================
You can now connect to this RabbitMQ server using, for example:

    curl --user admin:mypass http://<host>:<port>/api/vhosts

Please remember to change the above password as soon as possible!
========================================================================

              RabbitMQ 3.5.0. Copyright (C) 2007-2014 GoPivotal, Inc.
  ##  ##      Licensed under the MPL.  See http://www.rabbitmq.com/
  ##  ##
  ##########  Logs: /var/log/rabbitmq/rabbit@b34a7e8ec78d.log
  ######  ##        /var/log/rabbitmq/rabbit@b34a7e8ec78d-sasl.log
  ##########
              Starting broker... completed with 6 plugins.
```

### Start the application

```bash
$ docker run -it --rm --link rabbitmq:rabbitmq -e SPRING_RABBITMQ_ADDRESSES="amqp://admin:mypass@rabbitmq:5672" -p 8080:8080 rabbitmq-client
```

This command started a new autoremove interactive container from our `rabbitmq-client` image. We linked the container `rabbitmq` to create an `/etc/hosts` entry named `rabbitmq` resolving to the previous RabbitMQ container. We also published port `8080` and set an environment variable named `SPRING_RABBITMQ_ADDRESSES` with the value `"amqp://admin:mypass@rabbitmq:5672"`, this is actually the URI that our Spring Boot application will use to connect to RabbitMQ.

### Try it!

We can go to the `rabbitmq-client` container IP in our browser, on port `8080`. `/send` is used to send a message to RabbitMQ, and `/receive` is used to receive it.

![Send](assets/asset1.png)

We can see our message is in the queue, the RabbitMQ management interface is available if we go to the `rabbitmq` container IP in our browser, on port `15672`.

![Look](assets/asset2.png)

Now receive the message.

![Receive](assets/asset3.png)

## Deploy in a production like environment

In this part, we will deploy our new application (and RabbitMQ) in the cloud.

We will create a 3 machines cluster to run:

* a 2 nodes RabbitMQ cluster
* 2 instances of our application
* a load balancer to access RabbitMQ management interface
* a load balancer to access our application

### Create the cluster

We use the Tutum web interface to create our 3 machines cluster:

![Cluster](assets/asset4.png)

### Push our application

We push our application to Tutum's private Docker image registry.

*Note: In this part, you will have to replace `pierredavidbelanger` with your Tutum username.*

But we need to login first:

```bash
$ docker login tutum.co
```

Then we push:

```bash
$ docker tag -f rabbitmq-client tutum.co/pierredavidbelanger/rabbitmq-client
$ docker push tutum.co/pierredavidbelanger/rabbitmq-client
```

It should be available now:

![Image](assets/asset5.png)

### Create and deploy the Stack

Now we create the entire stack by uploading the [Stack.yml](Stack.yml) file using the Tutum web interface. *(Note: remember to replace `pierredavidbelanger` in this file with your Tutum username.)*

I named the stack `test`.

![Stack](assets/asset6.png)

Then, we hit start and wait for all services to be deployed:

![Services](assets/asset7.png)

At this point, our stack is working. But before we try it, we will scale our `rabbitmq` service to 2 and also our `rabbitmq-client` application to 2 using the scaler widget in each of those 2 services:

![Scale](assets/asset8.png)

### Try it!

We can go to the Endpoints tab in the `rabbitmq-client-lb` service to know the URL we need to use to access our application. It will be something like:

```
http://rabbitmq-client-lb.test.pierredavidbelanger.svc.tutum.io:80/
```

We send a message:

![Send](assets/asset9.png)

Also, we can go to the Endpoints tab in the `rabbitmq-admin` service to see how to access the RabbitMQ management interface.

![Send](assets/asset10.png)

We receive it:

![Send](assets/asset11.png)

Or we can use `curl` instead to send:

```bash
$ curl http://rabbitmq-client-lb.test.pierredavidbelanger.svc.tutum.io/send
```

And to receive:

```bash
$ curl http://rabbitmq-client-lb.test.pierredavidbelanger.svc.tutum.io/receive
rabbitmq-client-2> HelloWorld from rabbitmq-client-1 (8:40:48 PM)
```
