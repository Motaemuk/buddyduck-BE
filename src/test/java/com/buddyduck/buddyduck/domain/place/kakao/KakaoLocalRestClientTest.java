package com.buddyduck.buddyduck.domain.place.kakao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.buddyduck.buddyduck.domain.place.enums.PlaceSource;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.util.UriUtils;
import org.springframework.web.client.RestClient;

class KakaoLocalRestClientTest {

	private MockRestServiceServer server;
	private KakaoLocalRestClient client;

	@BeforeEach
	void setUp() {
		RestClient.Builder builder = RestClient.builder();
		server = MockRestServiceServer.bindTo(builder).build();
		KakaoLocalProperties properties = new KakaoLocalProperties();
		properties.setRestApiKey("test-rest-api-key");
		properties.setKeywordSearchUri("https://dapi.kakao.com/v2/local/search/keyword.json");
		properties.setAddressSearchUri("https://dapi.kakao.com/v2/local/search/address.json");
		client = new KakaoLocalRestClient(builder, properties);
	}

	@Test
	void keyword_검색은_KakaoAK_헤더를_보내고_장소_후보로_매핑한다() {
		server.expect(requestTo(containsString("/v2/local/search/keyword.json")))
			.andExpect(header(HttpHeaders.AUTHORIZATION, "KakaoAK test-rest-api-key"))
			.andExpect(queryParam("query", encoded("잠실 카페")))
			.andExpect(queryParam("size", "15"))
			.andRespond(withSuccess("""
				{
				  "documents": [
				    {
				      "id": "26338954",
				      "place_name": "잠실 카페 무드",
				      "address_name": "서울 송파구 잠실동 1",
				      "road_address_name": "서울 송파구 올림픽로 300",
				      "x": "127.1020000",
				      "y": "37.5150000"
				    }
				  ]
				}
				""", MediaType.APPLICATION_JSON));

		List<KakaoLocalPlaceCandidate> candidates = client.searchKeyword("잠실 카페");

		assertThat(candidates).hasSize(1);
		assertThat(candidates.get(0).provider()).isEqualTo(PlaceSource.KAKAO_KEYWORD);
		assertThat(candidates.get(0).providerPlaceId()).isEqualTo("26338954");
		assertThat(candidates.get(0).name()).isEqualTo("잠실 카페 무드");
		assertThat(candidates.get(0).address()).isEqualTo("서울 송파구 올림픽로 300");
		assertThat(candidates.get(0).lat()).isEqualByComparingTo(new BigDecimal("37.5150000"));
		assertThat(candidates.get(0).lng()).isEqualByComparingTo(new BigDecimal("127.1020000"));
		server.verify();
	}

	@Test
	void address_검색은_주소_후보로_매핑한다() {
		server.expect(requestTo(containsString("/v2/local/search/address.json")))
			.andExpect(header(HttpHeaders.AUTHORIZATION, "KakaoAK test-rest-api-key"))
			.andExpect(queryParam("query", encoded("서울 송파구 올림픽로 424")))
			.andExpect(queryParam("size", "10"))
			.andRespond(withSuccess("""
				{
				  "documents": [
				    {
				      "address_name": "서울 송파구 올림픽로 424",
				      "x": "127.1270000",
				      "y": "37.5190000"
				    }
				  ]
				}
				""", MediaType.APPLICATION_JSON));

		List<KakaoLocalPlaceCandidate> candidates = client.searchAddress("서울 송파구 올림픽로 424");

		assertThat(candidates).hasSize(1);
		assertThat(candidates.get(0).provider()).isEqualTo(PlaceSource.KAKAO_ADDRESS);
		assertThat(candidates.get(0).providerPlaceId()).isNull();
		assertThat(candidates.get(0).name()).isEqualTo("서울 송파구 올림픽로 424");
		assertThat(candidates.get(0).address()).isEqualTo("서울 송파구 올림픽로 424");
		assertThat(candidates.get(0).lat()).isEqualByComparingTo(new BigDecimal("37.5190000"));
		assertThat(candidates.get(0).lng()).isEqualByComparingTo(new BigDecimal("127.1270000"));
		server.verify();
	}

	@Test
	void rest_api_key가_비어있으면_비활성화된다() {
		KakaoLocalProperties properties = new KakaoLocalProperties();
		properties.setRestApiKey(" ");
		KakaoLocalRestClient disabledClient = new KakaoLocalRestClient(RestClient.builder(), properties);

		assertThat(disabledClient.isEnabled()).isFalse();
		assertThat(disabledClient.searchKeyword("잠실")).isEmpty();
		assertThat(disabledClient.searchAddress("서울")).isEmpty();
	}

	private String encoded(String value) {
		return UriUtils.encodeQueryParam(value, StandardCharsets.UTF_8);
	}
}
