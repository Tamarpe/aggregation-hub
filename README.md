# Support Aggregation Hub

The project is designed to aggregate data from two different CRM systems, and provides a support aggregation system.


## Features
- Web-based user interface
  - An endpoint to view and perform a search on the data
- On-demand data aggregation
  - A refresh button to fetch the data on-demand
  - Throttling - we can define a time limit between the last refresh to avoid overload. 
- Offline data aggregation
  - Using cron, the data would be fetched every defined time period.
  - The data is saved to Redis (in-memory cache), so when a user login, he would see the most updated data without waiting online / on demand aggregation to complete
- Persistent storage
  - The data is saved to Redis, so no need to perform online/offline aggregation
- Highly configurable
  - CRM URLs to fetch the data from
  - Throttling - each API to a CRM can have a limitation of current sessions in the same time, and a time limit between two aggregations.
  - CRON - the crontab role is configurable and can update the data in any defined time period.


## Getting started

Run the docker container for Redis:
```
docker-compose up -d
```

Run the project:
```
mvn clean spring-boot:run
```

Using browser, navigate to:

```
http://localhost:8080/myAggregatedHub
```

Run the tests:

```
./mvnw test
```



## Screenshot
