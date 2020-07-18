package com.tamar.support;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.tamar.support.controller.AggregationHubController;
import com.tamar.support.model.Case;

import com.tamar.support.repository.RedisRepository;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

@SpringBootTest(webEnvironment= SpringBootTest.WebEnvironment.RANDOM_PORT)
class AggregationHubApplicationTests {

	@Autowired
	private TestRestTemplate restTemplate;

	@InjectMocks
	private AggregationHubController aggregationHubController = new AggregationHubController();

	@Mock
	private RedisRepository RedisRepository;

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
		ArrayList<Map<String, Object>> result = parseCaseAsResult(mockCase);

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

		ArrayList<Map<String, Object>> result = parseCaseAsResult(mockCase2);
		when(RedisRepository.findAllCases()).thenReturn(allMockedCases);

		assertEquals(result, aggregationHubController.searchCases("Test Product 2", null,null,null,null,null,null,null,null));
	}

	@Test
	public void searchCasesByRangeDate() throws ParseException {
		Map<Object, Object> allMockedCases = new LinkedHashMap<>();
		DateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");

		Case mockCase = new Case("7;test", 7, 11234, 42, 101, "Open",
			dateFormat.parse("22/10/2020"), dateFormat.parse("22/11/2020"), "Test Product", "Test");

		Case mockCase2 = new Case("8;test", 99, 11234, 888, 102, "Open",
			dateFormat.parse("25/12/2020"), dateFormat.parse("26/12/2020"), "Test Product 2", "Test");

		allMockedCases.put(mockCase.getId(), mockCase);
		allMockedCases.put(mockCase2.getId(), mockCase2);

		ArrayList<Map<String, Object>> result = parseCaseAsResult(mockCase);
		ArrayList<Map<String, Object>> result2 = parseCaseAsResult(mockCase2);

		when(RedisRepository.findAllCases()).thenReturn(allMockedCases);

		assertEquals(result, aggregationHubController.searchCases(null, null,null,null,"2020-10-21","2020-10-22",null,null,null));
		assertEquals(result2, aggregationHubController.searchCases(null, null,null,null, null,null,"2020-12-26","2020-12-26", null));
	}

	@Test
	public void searchWithoutResults() {
		Map<Object, Object> allMockedCases = new LinkedHashMap<>();
		Case mockCase = new Case("7;test", 7, 11234, 42, 101, "Open",
			new Date(), new Date(), "Test Product", "Test");

		allMockedCases.put(mockCase.getId(), mockCase);
		when(RedisRepository.findAllCases()).thenReturn(allMockedCases);

		assertEquals(0, aggregationHubController.searchCases("Some text", null,null,null,null,null,null,null, null).size());
	}

	@Test
	public void searchCasesMultipleFilters() {
		Map<Object, Object> allMockedCases = new LinkedHashMap<>();
		Case mockCase = new Case("7;test", 7, 11234, 42, 101, "Open",
			new Date(), new Date(), "Test Product", "Test");

		allMockedCases.put(mockCase.getId(), mockCase);

		ArrayList<Map<String, Object>> result = parseCaseAsResult(mockCase);
		when(RedisRepository.findAllCases()).thenReturn(allMockedCases);

		assertEquals(result, aggregationHubController.searchCases("Test Product", "42","101","11234",null,null,null,null,"Open"));
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

	@Test
	public void refreshTimeLimit() {
		when(RedisRepository.getLastExecutionResource(anyString())).thenReturn(new Date());
		assertEquals(false, RedisRepository.refresh());
	}

	/**
	 * Helper function to parse a case as displayed as a search result.
	 *
	 * @param caseObj the case to parse.
	 * @return the parsed case as a search result.
	 */
	public ArrayList<Map<String, Object>> parseCaseAsResult(Case caseObj) {
		ArrayList<Map<String, Object>> result = new ArrayList<>();
		DateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");

		Map<String, Object> map = new HashMap<String, Object>();
		map.put("productName", caseObj.getProductName());
		map.put("provider", caseObj.getProvider());
		map.put("caseId", caseObj.getCaseId());
		map.put("lastModifiedDate",  dateFormat.format(caseObj.getLastModifiedDate()));
		map.put("status", caseObj.getStatus());
		map.put("customerId", caseObj.getCustomerId());
		map.put("creationDate", dateFormat.format(caseObj.getCreationDate()));
		map.put("errorCode", caseObj.getErrorCode());
		map.put("resourceName", caseObj.getResourceName());
		result.add(map);

		return result;
	}

}
