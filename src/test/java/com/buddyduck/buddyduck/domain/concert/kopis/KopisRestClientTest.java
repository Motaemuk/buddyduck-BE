package com.buddyduck.buddyduck.domain.concert.kopis;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class KopisRestClientTest {

	private static final MediaType XML_UTF8 = new MediaType("application", "xml", StandardCharsets.UTF_8);

	private MockRestServiceServer server;
	private KopisRestClient client;

	@BeforeEach
	void setUp() {
		RestClient.Builder builder = RestClient.builder();
		server = MockRestServiceServer.bindTo(builder).build();

		KopisProperties properties = new KopisProperties();
		properties.setServiceKey("test-service-key");
		properties.setBaseUri("https://kopis.test/openApi/restful");

		client = new KopisRestClient(builder.build(), properties, new KopisXmlParser());
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
				""", XML_UTF8));
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
				    <poster>http://www.kopis.or.kr/upload/pfmPoster/PF178134.gif</poster>
				    <area>서울특별시</area>
				    <genrenm>대중음악</genrenm>
				    <dtguidance>토요일(19:00)</dtguidance>
				  </db>
				</dbs>
				""", XML_UTF8));
		server.expect(requestTo(containsString("/openApi/restful/prfplc/FC001247?")))
			.andRespond(withSuccess("""
				<dbs>
				  <db>
				    <fcltynm>올림픽공원</fcltynm>
				    <la>37.52112</la>
				    <lo>127.1283636</lo>
				  </db>
				</dbs>
				""", XML_UTF8));

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
		assertThat(candidates.get(0).startAt()).isEqualTo(LocalDateTime.of(2026, 6, 20, 19, 0));
		assertThat(candidates.get(0).lat()).isEqualByComparingTo(new BigDecimal("37.52112"));
		assertThat(candidates.get(0).lng()).isEqualByComparingTo(new BigDecimal("127.1283636"));
		assertThat(candidates.get(0).posterUrl()).isEqualTo("https://www.kopis.or.kr/upload/pfmPoster/PF178134.gif");
		assertThat(candidates.get(0).area()).isEqualTo("서울특별시");
		assertThat(candidates.get(0).genre()).isEqualTo("대중음악");
		assertThat(candidates.get(0).timeGuidance()).isEqualTo("토요일(19:00)");
		server.verify();
	}

	@Test
	void 공연시간에_시간이_여러개면_시작일_자정으로_매핑한다() {
		server.expect(requestTo(containsString("/openApi/restful/pblprfr?")))
			.andRespond(withSuccess("""
				<dbs>
				  <db>
				    <mt20id>PF178135</mt20id>
				    <prfnm>AURORA LIVE DAY2</prfnm>
				  </db>
				</dbs>
				""", XML_UTF8));
		server.expect(requestTo(containsString("/openApi/restful/pblprfr/PF178135?")))
			.andRespond(withSuccess("""
				<dbs>
				  <db>
				    <mt20id>PF178135</mt20id>
				    <prfnm>AURORA LIVE DAY2</prfnm>
				    <mt10id>FC001247</mt10id>
				    <fcltynm>KSPO Dome</fcltynm>
				    <prfpdfrom>2026.06.21</prfpdfrom>
				    <prfpdto>2026.06.21</prfpdto>
				    <dtguidance>일요일(15:00,18:00)</dtguidance>
				  </db>
				</dbs>
				""", XML_UTF8));
		server.expect(requestTo(containsString("/openApi/restful/prfplc/FC001247?")))
			.andRespond(withSuccess("""
				<dbs>
				  <db>
				    <fcltynm>올림픽공원</fcltynm>
				    <la>37.52112</la>
				    <lo>127.1283636</lo>
				  </db>
				</dbs>
				""", XML_UTF8));

		List<KopisConcertCandidate> candidates = client.fetchConcerts(
			LocalDate.of(2026, 6, 1),
			LocalDate.of(2026, 6, 30),
			0,
			10,
			null
		);

		assertThat(candidates).hasSize(1);
		assertThat(candidates.get(0).startAt()).isEqualTo(LocalDateTime.of(2026, 6, 21, 0, 0));
		assertThat(candidates.get(0).timeGuidance()).isEqualTo("일요일(15:00,18:00)");
		server.verify();
	}

	@Test
	void service_key가_없으면_비활성화된다() {
		KopisProperties properties = new KopisProperties();
		properties.setServiceKey(" ");
		KopisRestClient disabledClient = new KopisRestClient(
			RestClient.builder().build(),
			properties,
			new KopisXmlParser()
		);

		assertThat(disabledClient.isEnabled()).isFalse();
		assertThat(disabledClient.fetchConcerts(LocalDate.now(), LocalDate.now(), 0, 10, null)).isEmpty();
	}

	@Test
	void KOPIS_기본_base_uri는_공식_HTTP_endpoint를_사용한다() {
		KopisProperties properties = new KopisProperties();

		assertThat(properties.getBaseUri()).isEqualTo("http://www.kopis.or.kr/openApi/restful");
	}
}
