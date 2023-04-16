# Big-Data-Tech-Demo
DEMO 1 ~ 3 for INFO 7255

## Related Software and Versions

`Redis`: latest version up to March 2023

Install on Mac: `brew install redis`
Run on Mac: `brew services start redis`

`ElasticSearch`: version 7.17.9

run in your command line: `/elasticsearch-7-17-9/bin/elasticsearch`

`Kibana`: version 7.17.9 (has to be the same as ES)

do it the same way with your ES

`RabbitMQ`: latest version up to March 2023

Install on Mac: `brew install rabbitmq`

(You can also choose to use `Docker` to run rabbit mq)

`SpringBoot`:

My configuration of running Spring Boot:

https://start.spring.io/#!type=maven-project&language=java&platformVersion=3.0.5&packaging=jar&jvmVersion=17&groupId=com.example&artifactId=demo&name=demo&description=Demo%20project%20for%20Spring%20Boot&packageName=com.example.demo&dependencies=web,devtools,amqp,data-redis

Other dependencies:

```maven
<!-- https://mvnrepository.com/artifact/commons-codec/commons-codec -->
<dependency>
  <groupId>commons-codec</groupId>
  <artifactId>commons-codec</artifactId>
  <version>1.15</version>
</dependency>
<dependency>
  <groupId>com.github.erosb</groupId>
  <artifactId>everit-json-schema</artifactId>
  <version>1.14.1</version>
</dependency>

<dependency>
  <groupId>com.google.api-client</groupId>
  <artifactId>google-api-client</artifactId>
  <version>1.30.11</version>
</dependency>
<!-- https://mvnrepository.com/artifact/com.google.http-client/google-http-client-apache-v2 -->
<dependency>
  <groupId>com.google.http-client</groupId>
  <artifactId>google-http-client-apache-v2</artifactId>
  <version>1.43.1</version>
</dependency>
```

For other configurations please look into the source project

## How to run?

Examples for running:

1. Make sure your `Redis` and `RabbitMQ` are running in the background:

`brew services list` and check related status are `started`.

2. Run ES and Kibana in your command line

3. Run the DEMO3 Project in Eclipse (If you want to run it in IDEA, just migrate it yourself)

4. Create Mapping in ElasticSearch and create Index

(Go to http://localhost:5601/app/dev_tools#/console to view them)
