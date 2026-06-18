package com.buddyduck.buddyduck.domain.concert.kopis;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class KopisRestClientTest {

	private MockRestServiceServer server;
	private KopisRestClient client;

	@BeforeEach
	void setUp() {
		RestClient.Builder builder = RestClient.builder();
		server = MockRestServiceServer.bindTo(builder).build();

		KopisProperties properties = new KopisProperties();
		properties.setServiceKey("test-service-key");
		properties.setBaseUri("https://kopis.test/openApi/restful");

		client = new KopisRestClient(builder, properties, new KopisXmlParser());
	}

	@Test
	void 공연_목록_상세_시설을_조회해_좌표가_있는_공연_후보로_매핑한다() {
		server.expect(requestTo(containsString("/openApi/restful/pblprfr?")))
			.andRespond(withSuccess("""
				<dbs>
				  <db>
				    <mt20id>PF178134</mt20id>
				    <prfnm>AURORA LIVE</prfnm>
				  </db>
				</dbs>
				""", MediaType.APPLICATION_XML));
		server.expect(requestTo(containsString("/openApi/restful/pblprfr/PF178134?")))
			.andRespond(withSuccess("""
				<dbs>
				  <db>
				    <mt20id>PF178134</mt20id>
				    <prfnm>AURORA LIVE</prfnm>
				    <mt10id>FC001247</mt10id>
				    <fcltynm>KSPO Dome</fcltynm>
				    <prfpdfrom>2026.06.20</prfpdfrom>
				    <prfpdto>2026.06.20</prfpdto>
				  </db>
				</dbs>
				""", MediaType.APPLICATION_XML));
		server.expect(requestTo(containsString("/openApi/restful/prfplc/FC001247?")))
			.andRespond(withSuccess("""
				<dbs>
				  <db>
				    <fcltynm>올림픽공원</fcltynm>
				    <la>37.52112</la>
				    <lo>127.1283636</lo>
				  </db>
				</dbs>
				""", MediaType.APPLICATION_XML));

		List<KopisConcertCandidate> candidates = client.fetchConcerts(
			LocalDate.of(2026, 6, 1),
			LocalDate.of(2026, 6, 30),
			0,
			10,
			"AURORA"
		);

		assertThat(candidates).hasSize(1);
		assertThat(candidates.get(0).externalId()).isEqualTo("PF178134");
		assertThat(candidates.get(0).title()).isEqualTo("AURORA LIVE");
		assertThat(candidates.get(0).venueName()).isEqualTo("KSPO Dome");
		assertThat(candidates.get(0).startAt()).isEqualTo(LocalDateTime.of(2026, 6, 20, 0, 0));
		assertThat(candidates.get(0).lat()).isEqualByComparingTo(new BigDecimal("37.52112"));
		assertThat(candidates.get(0).lng()).isEqualByComparingTo(new BigDecimal("127.1283636"));
		server.verify();
	}

	@Test
	void service_key가_없으면_비활성화된다() {
		KopisProperties properties = new KopisProperties();
		properties.setServiceKey(" ");
		KopisRestClient disabledClient = new KopisRestClient(RestClient.builder(), properties, new KopisXmlParser());

		assertThat(disabledClient.isEnabled()).isFalse();
		assertThat(disabledClient.fetchConcerts(LocalDate.now(), LocalDate.now(), 0, 10, null)).isEmpty();
	}
}
