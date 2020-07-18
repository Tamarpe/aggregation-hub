package com.tamar.support;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import com.tamar.support.controller.AggregationHubController;
import com.tamar.support.model.Case;

import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

@SpringBootTest(webEnvironment= SpringBootTest.WebEnvironment.RANDOM_PORT)
class AggregationHubApplicationTests {

	@Autowired
	private TestRestTemplate restTemplate;

	@InjectMocks
	private AggregationHubController aggregationHubController = new AggregationHubController();

	@Mock
	RedisTemplate<String, String> redisTemplate;

	@Mock
	private com.tamar.support.repository.RedisRepository RedisRepository;

	@LocalServerPort
	private int port;

	/** The URL for the aggregation of the CRM 1. */
	@Value("${aggregations.crm1.url}")
	private String aggregationsCrm1Url;

	/** The URL for the aggregation of the CRM 2. */
	@Value("${aggregations.crm2.url}")
	private String aggregationsCrm2Url;

	@Test
	void contextLoads() {
	}

	@Test
	public void projectPathShouldReturnProjectHeader() throws Exception {
		assertThat(this.restTemplate.getForObject("http://localhost:" + port + "/myAggregatedHub",
			String.class)).contains("My aggregated hub");
	}


	@Test
	public void getAllCases() {
		Case mockCase = new Case("7;test", 7, 11234, 42, 101, "Open",
			new Date(), new Date(), "Test Product", "Test");
		Map<Object, Object> allMockedCases = new LinkedHashMap<>();

		allMockedCases.put(mockCase.getId(), mockCase);
		ArrayList<Map<String, Object>> result = new ArrayList<>();
		DateFormat dateFormat = new SimpleDateFormat("dd/MM/YYYY");
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("productName", mockCase.getProductName());
		map.put("provider", mockCase.getProvider());
		map.put("caseId", mockCase.getCaseId());
		map.put("lastModifiedDate",  dateFormat.format(mockCase.getLastModifiedDate()));
		map.put("status", mockCase.getStatus());
		map.put("customerId", mockCase.getCustomerId());
		map.put("creationDate", dateFormat.format(mockCase.getCreationDate()));
		map.put("errorCode", mockCase.getErrorCode());
		map.put("resourceName", mockCase.getResourceName());
		result.add(map);

		when(RedisRepository.findAllCases()).thenReturn(allMockedCases);

		assertEquals(result, aggregationHubController.searchCases(null, null,null,null,null,null,null,null,null));
	}

	@Test
	public void searchCasesByProductName() {
		Map<Object, Object> allMockedCases = new LinkedHashMap<>();
		Case mockCase = new Case("7;test", 7, 11234, 42, 101, "Open",
			new Date(), new Date(), "Test Product", "Test");

		Case mockCase2 = new Case("8;test", 99, 11234, 888, 102, "Open",
			new Date(), new Date(), "Test Product 2", "Test");

		allMockedCases.put(mockCase.getId(), mockCase);
		allMockedCases.put(mockCase2.getId(), mockCase2);

		ArrayList<Map<String, Object>> result = new ArrayList<>();
		DateFormat dateFormat = new SimpleDateFormat("dd/MM/YYYY");
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("productName", mockCase2.getProductName());
		map.put("provider", mockCase2.getProvider());
		map.put("caseId", mockCase2.getCaseId());
		map.put("lastModifiedDate",  dateFormat.format(mockCase2.getLastModifiedDate()));
		map.put("status", mockCase2.getStatus());
		map.put("customerId", mockCase2.getCustomerId());
		map.put("creationDate", dateFormat.format(mockCase2.getCreationDate()));
		map.put("errorCode", mockCase2.getErrorCode());
		map.put("resourceName", mockCase2.getResourceName());
		result.add(map);

		when(RedisRepository.findAllCases()).thenReturn(allMockedCases);

		assertEquals(result, aggregationHubController.searchCases("Test Product 2", null,null,null,null,null,null,null,null));
	}

	@Test
	public void getOkResponseFromResources() {
		String[] resourcesUrls = {aggregationsCrm1Url, aggregationsCrm2Url};
		for (String resourcesUrl: resourcesUrls) {
			ResponseEntity<List<Object>> res = restTemplate.exchange(
				aggregationsCrm1Url,
				HttpMethod.GET,
				null,
				new ParameterizedTypeReference<>() {
				});

			assertEquals(HttpStatus.OK,
				res.getStatusCode());
		}
	}

}
