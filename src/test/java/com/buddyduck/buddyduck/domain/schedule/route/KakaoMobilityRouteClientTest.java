package com.buddyduck.buddyduck.domain.schedule.route;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.buddyduck.buddyduck.domain.place.entity.Place;
import com.buddyduck.buddyduck.domain.place.enums.PlaceSource;
import com.buddyduck.buddyduck.domain.schedule.enums.RouteMode;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriUtils;

class KakaoMobilityRouteClientTest {

	private MockRestServiceServer server;
	private KakaoMobilityRouteClient client;

	@BeforeEach
	void setUp() {
		RestClient.Builder builder = RestClient.builder();
		server = MockRestServiceServer.bindTo(builder).build();

		KakaoMobilityProperties properties = new KakaoMobilityProperties();
		properties.setRestApiKey("test-rest-api-key");
		properties.setDrivingDirectionsUri("https://apis-navi.kakaomobility.com/v1/directions");
		client = new KakaoMobilityRouteClient(builder, properties);
	}

	@Test
	void CAR_TAXI는_Driving_Directions_거리_시간_택시요금을_매핑한다() {
		server.expect(requestTo(containsString("https://apis-navi.kakaomobility.com/v1/directions")))
			.andExpect(header(HttpHeaders.AUTHORIZATION, "KakaoAK test-rest-api-key"))
			.andExpect(queryParam("origin", encoded("127.1000000,37.5130000")))
			.andExpect(queryParam("destination", encoded("127.1020000,37.5150000")))
			.andExpect(queryParam("summary", "true"))
			.andRespond(withSuccess("""
				{
				  "routes": [
				    {
				      "result_code": 0,
				      "summary": {
				        "fare": {
				          "taxi": 3800,
				          "toll": 0
				        },
				        "distance": 1033,
				        "duration": 297
				      }
				    }
				  ]
				}
				""", MediaType.APPLICATION_JSON));

		Optional<RouteEstimate> estimate = client.estimate(
			RouteMode.CAR_TAXI,
			place("잠실역", "37.5130000", "127.1000000"),
			place("잠실 카페", "37.5150000", "127.1020000")
		);

		assertThat(estimate).isPresent();
		assertThat(estimate.get().provider()).isEqualTo(RouteEstimateProvider.KAKAO_DRIVING.name());
		assertThat(estimate.get().distanceMeters()).isEqualTo(1033);
		assertThat(estimate.get().durationMinutes()).isEqualTo(5);
		assertThat(estimate.get().taxiFareWon()).isEqualTo(3800);
		assertThat(estimate.get().tollFareWon()).isZero();
		server.verify();
	}

	@Test
	void WALK는_Driving_Directions_거리로_도보_시간을_추정한다() {
		server.expect(requestTo(containsString("https://apis-navi.kakaomobility.com/v1/directions")))
			.andExpect(header(HttpHeaders.AUTHORIZATION, "KakaoAK test-rest-api-key"))
			.andExpect(queryParam("origin", encoded("127.1000000,37.5130000")))
			.andExpect(queryParam("destination", encoded("127.1020000,37.5150000")))
			.andExpect(queryParam("priority", "RECOMMEND"))
			.andExpect(queryParam("summary", "true"))
			.andRespond(withSuccess("""
				{
				  "routes": [
				    {
				      "result_code": 0,
				      "summary": {
				        "fare": {
				          "taxi": 3800,
				          "toll": 0
				        },
				        "distance": 1261,
				        "duration": 300
				      }
				    }
				  ]
				}
				""", MediaType.APPLICATION_JSON));

		Optional<RouteEstimate> estimate = client.estimate(
			RouteMode.WALK,
			place("잠실역", "37.5130000", "127.1000000"),
			place("잠실 카페", "37.5150000", "127.1020000")
		);

		assertThat(estimate).isPresent();
		assertThat(estimate.get().mode()).isEqualTo(RouteMode.WALK);
		assertThat(estimate.get().provider()).isEqualTo(RouteEstimateProvider.DRIVING_DISTANCE_WALK_ESTIMATE.name());
		assertThat(estimate.get().distanceMeters()).isEqualTo(1261);
		assertThat(estimate.get().durationMinutes()).isEqualTo(22);
		assertThat(estimate.get().taxiFareWon()).isNull();
		assertThat(estimate.get().tollFareWon()).isNull();
		server.verify();
	}

	@Test
	void REST_API_KEY가_비어있으면_비활성화된다() {
		KakaoMobilityProperties properties = new KakaoMobilityProperties();
		properties.setRestApiKey(" ");
		KakaoMobilityRouteClient disabledClient = new KakaoMobilityRouteClient(RestClient.builder(), properties);

		assertThat(disabledClient.isEnabled()).isFalse();
		assertThat(disabledClient.estimate(
			RouteMode.WALK,
			place("잠실역", "37.5130000", "127.1000000"),
			place("잠실 카페", "37.5150000", "127.1020000")
		)).isEmpty();
	}

	private Place place(String name, String lat, String lng) {
		return Place.create(
			PlaceSource.KAKAO_ADDRESS,
			name,
			name,
			"서울 송파구",
			new BigDecimal(lat),
			new BigDecimal(lng)
		);
	}

	private String encoded(String value) {
		return UriUtils.encodeQueryParam(value, StandardCharsets.UTF_8).replace(",", "%2C");
	}
}
