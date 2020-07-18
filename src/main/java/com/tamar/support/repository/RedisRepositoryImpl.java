package com.tamar.support.repository;

import com.google.common.util.concurrent.RateLimiter;
import com.tamar.support.model.Case;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Repository;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

@Repository
public class RedisRepositoryImpl implements RedisRepository {
    /** The key to be used in Redis for a case. */
    private static final String KEY = "case";

    /** The Redis template. */
    private RedisTemplate<String, Object> redisTemplate;

    /** The Hash operations. */
    private HashOperations hashOperations;

    /** The logger. */
    private Logger logger;

    /** The URL for the aggregation of the CRM 1. */
    @Value("${aggregations.crm1.url}")
    private String aggregationsCrm1Url;

    /** The URL for the aggregation of the CRM 2. */
    @Value("${aggregations.crm2.url}")
    private String aggregationsCrm2Url;

    /** The resource name of the CRM 1. */
    @Value("${aggregations.crm1.resourcename}")
    private String aggregationsCrm1Resource;

    /** The resource name of the CRM 2. */
    @Value("${aggregations.crm2.resourcename}")
    private String aggregationsCrm2Resource;

    /** The rate limit for the CRM 1. */
    @Value("${aggregations.crm1.ratelimit}")
    private Integer aggregationsCrm1RateLimit;

    /** The rate limit for the CRM 2. */
    @Value("${aggregations.crm2.ratelimit}")
    private Integer aggregationsCrm2RateLimit;

    /**
     * The period time that should passed to be able to execute
     * another call to the CRM 1.
     */
    @Value("${aggregations.crm1.periodtime}")
    private Integer aggregationsCrm1PeriodTime;

    /**
     * The period time that should passed to be able to execute
     * another call to the CRM 2.
     */
    @Value("${aggregations.crm2.periodtime}")
    private Integer aggregationsCrm2PeriodTime;

    /**
     * The period time that should passed to be able to refresh.
     */
    @Value("${aggregations.refresh.ratelimit}")
    private Integer aggregationsRefreshPeriodTime;

    /**
     * Create a new RedisRepositoryImpl.
     *
     * @param redisTemplate the redis template.
     */
    @Autowired
    public RedisRepositoryImpl(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @PostConstruct
    private void init() {
        hashOperations = redisTemplate.opsForHash();
        logger = LoggerFactory.getLogger(RedisRepositoryImpl.class);
    }

    /**
     * Refresh the fetched data.
     *
     * @return true if a refresh is allowed, false otherwise.
     */
    @Scheduled(cron = "${aggregations.auto.refresh}")
    public boolean refresh() {
        Date currentDate = new Date();
        Date lastExecutionCrm1 = getLastExecutionResource(aggregationsCrm1Resource);
        Date lastExecutionCrm2 = getLastExecutionResource(aggregationsCrm2Resource);

        if (lastExecutionCrm1 != null && lastExecutionCrm2 != null) {
            // If the data from the 2 CRMs are fetched less than the
            // aggregation refresh period time, we do nothing.
            if ((currentDate.getTime() - lastExecutionCrm1.getTime() < aggregationsRefreshPeriodTime)
              && (currentDate.getTime() - lastExecutionCrm2.getTime() < aggregationsRefreshPeriodTime)) {
                logger.warn("The data is already fetched less than the time limit.");
                return false;
            }
        }

        refreshCrm(aggregationsCrm1Url, true, aggregationsCrm1Resource, aggregationsCrm1RateLimit, aggregationsCrm1PeriodTime);
        refreshCrm(aggregationsCrm2Url, false, aggregationsCrm2Resource, aggregationsCrm2RateLimit, aggregationsCrm2PeriodTime);
        return true;
    }

    /**
     * Refresh the data from a CRM.
     *
     * @param crmUrl the URL of the CRM.
     * @param getFirstElement a boolean value if the data is in a nested array.
     * @param resource the CRM resource.
     * @param rateLimit the allowed number to execute an API call to the CRM.
     * @param periodTime The period time that should passed to be able to
     *   execute a call to the CRM.
     */
    public void refreshCrm(String crmUrl, boolean getFirstElement, String resource, Integer rateLimit, Integer periodTime) {
        Date currentDate = new Date();
        RestTemplate restTemplate = new RestTemplate();
        RateLimiter rateLimiter = RateLimiter.create(rateLimit);

        if (hashOperations.hasKey("last_execution", resource)) {
            Date lastExecutionCrm = (Date) hashOperations.get("last_execution", resource);
            if (currentDate.getTime() - lastExecutionCrm.getTime() < periodTime) {
                logger.warn("The data is already fetched less than the time limit.");
                return;
            }
        }
        if (rateLimiter.tryAcquire()) {
            try {
                logger.info("Fetching data from CRM: " + resource);
                ResponseEntity<List<Object>> res = restTemplate.exchange(
                    crmUrl,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<>() {
                    });

                List<Object> result = res.getBody();
                ArrayList<LinkedHashMap> fetchedCases =
                  getFirstElement ? (ArrayList) result.get(0) : (ArrayList) result;

                for (LinkedHashMap fetchedCase : fetchedCases) {
                    String id = fetchedCase.get("CaseID") + ";resource:" + resource;
                    SimpleDateFormat fromApi = new SimpleDateFormat("M/d/yyyyh:mm");

                    try {
                        Date creationDate = fromApi.parse((String) fetchedCase.get("TICKET_CREATION_DATE"));
                        Date lastModifiedDate = fromApi.parse((String) fetchedCase.get("LAST_MODIFIED_DATE"));
                        Case caseTmp = new Case(id,
                          (Integer) fetchedCase.get("CaseID"),
                          (Integer) fetchedCase.get("CustomerID"),
                          (Integer) fetchedCase.get("Provider"),
                          (Integer) fetchedCase.get("CREATED_ERROR_CODE"),
                          (String) fetchedCase.get("STATUS"),
                          creationDate,
                          lastModifiedDate,
                          (String) fetchedCase.get("PRODUCT_NAME"),
                          resource);

                        add(caseTmp);

                    } catch (ParseException e) {
                        e.printStackTrace();
                        logger.error("Error found on parsing the date format", e);
                    }
                }
                hashOperations.put("last_execution", resource, currentDate);
            } catch (HttpStatusCodeException e) {
                    e.printStackTrace();
                    logger.error("Error found on calling to the API", e);
            }
        }
    }

    /**
     * Add a case to Redis.
     *
     * @param caseToAdd the case that should be added.
     */
    public void add(Case caseToAdd) {
        hashOperations.put(KEY, caseToAdd.getId(), caseToAdd);
    }

    /**
     * Delete all the data from Redis.
     */
    public void delete() {
        redisTemplate.delete(KEY);
        redisTemplate.delete("last_execution");
    }

    /**
     * Return all found cases.
     *
     * @return all the cases from Redis.
     */
    public Map<Object, Object> findAllCases() {
        return hashOperations.entries(KEY);
    }

    /**
     * Return last execution of fetching a resource.
     *
     * @param aggregationsCrmResource the resource.
     * @return the date of the last execution.
     */
    public Date getLastExecutionResource(String aggregationsCrmResource) {
        if (hashOperations.hasKey("last_execution", aggregationsCrmResource)) {
            return (Date) hashOperations.get("last_execution", aggregationsCrmResource);
        }
        return null;
    }

}
