package com.buddyduck.buddyduck.domain.concert.kopis;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class KopisXmlParserTest {

	private final KopisXmlParser parser = new KopisXmlParser();

	@Test
	void 공연_목록_XML을_KOPIS_목록_항목으로_파싱한다() {
		List<KopisConcertListItem> items = parser.parseConcertList("""
			<dbs>
			  <db>
			    <mt20id>PF178134</mt20id>
			    <prfnm>반짝반짝 인어공주</prfnm>
			  </db>
			</dbs>
			""");

		assertThat(items).hasSize(1);
		assertThat(items.get(0).externalId()).isEqualTo("PF178134");
		assertThat(items.get(0).title()).isEqualTo("반짝반짝 인어공주");
	}

	@Test
	void 공연_상세_XML을_공연_상세로_파싱한다() {
		Optional<KopisConcertDetail> detail = parser.parseConcertDetail("""
			<dbs>
			  <db>
			    <mt20id>PF132236</mt20id>
			    <prfnm>우리연애할까</prfnm>
			    <mt10id>FC001431</mt10id>
			    <fcltynm>피가로아트홀</fcltynm>
			    <prfpdfrom>2016.05.12</prfpdfrom>
			    <prfpdto>2016.06.30</prfpdto>
			  </db>
			</dbs>
			""");

		assertThat(detail).isPresent();
		assertThat(detail.get().externalId()).isEqualTo("PF132236");
		assertThat(detail.get().facilityId()).isEqualTo("FC001431");
		assertThat(detail.get().startDate()).isEqualTo(LocalDate.of(2016, 5, 12));
		assertThat(detail.get().endDate()).isEqualTo(LocalDate.of(2016, 6, 30));
	}

	@Test
	void 시설_상세_XML을_좌표로_파싱한다() {
		Optional<KopisFacilityDetail> facility = parser.parseFacilityDetail("""
			<dbs>
			  <db>
			    <fcltynm>올림픽공원</fcltynm>
			    <la>37.52112</la>
			    <lo>127.12836360000005</lo>
			  </db>
			</dbs>
			""");

		assertThat(facility).isPresent();
		assertThat(facility.get().venueName()).isEqualTo("올림픽공원");
		assertThat(facility.get().lat()).isEqualByComparingTo(new BigDecimal("37.52112"));
		assertThat(facility.get().lng()).isEqualByComparingTo(new BigDecimal("127.12836360000005"));
	}
}
