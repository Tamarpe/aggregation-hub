# Support Aggregation Hub

The goal of the project is to aggregate data from different CRMs systems, and provides a support aggregation platform.

## Features
- Web-based user interface
  - An endpoint to view and perform a search on the data
  
- On-demand data aggregation
  - A refresh button to fetch the data on-demand
  - Throttling - we can define a time limit between the last refresh to avoid overload
  
- Offline data aggregation
  - Using cron, the data would be fetched every defined time period
  - The data is saved to Redis (in-memory cache), so when a user login, he would see the most updated data without waiting online / on demand aggregation to complete

- Persistent storage
  - The data is saved to Redis, so no need to perform online/offline aggregation

- Highly configurable
  - CRM URLs to fetch the data from
  - Throttling - each API to a CRM can have a limitation of current sessions in the same time, and a time limit between two aggregations
  - Cron - the crontab role is configurable and can update the data in any defined time period


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

To run tests only:

```
mvn test
```



## Screenshot

![image](https://user-images.githubusercontent.com/7335049/87868995-10134180-c99c-11ea-9156-86cba03420bb.png)
