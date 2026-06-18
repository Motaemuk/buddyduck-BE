package com.buddyduck.buddyduck.domain.concert.kopis;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;

@Component
@RequiredArgsConstructor
public class KopisRestClient implements KopisConcertClient {

	private static final DateTimeFormatter REQUEST_DATE_FORMATTER = DateTimeFormatter.BASIC_ISO_DATE;

	private final RestClient.Builder restClientBuilder;
	private final KopisProperties properties;
	private final KopisXmlParser parser;

	@Override
	public boolean isEnabled() {
		return properties.enabled();
	}

	@Override
	public List<KopisConcertCandidate> fetchConcerts(
		LocalDate from,
		LocalDate to,
		int page,
		int rows,
		String keyword
	) {
		if (!isEnabled()) {
			return List.of();
		}

		String listXml = get(buildListUri(from, to, page, rows, keyword));
		return parser.parseConcertList(listXml)
			.stream()
			.map(this::fetchCandidate)
			.flatMap(Optional::stream)
			.toList();
	}

	private Optional<KopisConcertCandidate> fetchCandidate(KopisConcertListItem item) {
		Optional<KopisConcertDetail> detail = fetchConcertDetail(item.externalId());
		if (detail.isEmpty()) {
			return Optional.empty();
		}
		Optional<KopisFacilityDetail> facility = fetchFacilityDetail(detail.get().facilityId());
		if (facility.isEmpty()) {
			return Optional.empty();
		}

		KopisConcertDetail concert = detail.get();
		KopisFacilityDetail venue = facility.get();
		return Optional.of(new KopisConcertCandidate(
			concert.externalId(),
			concert.title(),
			preferredVenueName(concert.venueName(), venue.venueName()),
			concert.startDate().atStartOfDay(),
			concert.endDate().atTime(LocalTime.MAX).withNano(0),
			venue.lat(),
			venue.lng()
		));
	}

	private Optional<KopisConcertDetail> fetchConcertDetail(String externalId) {
		try {
			return parser.parseConcertDetail(get(buildDetailUri("/pblprfr/" + externalId)));
		} catch (IllegalArgumentException | RestClientException exception) {
			return Optional.empty();
		}
	}

	private Optional<KopisFacilityDetail> fetchFacilityDetail(String facilityId) {
		try {
			return parser.parseFacilityDetail(get(buildDetailUri("/prfplc/" + facilityId)));
		} catch (IllegalArgumentException | RestClientException exception) {
			return Optional.empty();
		}
	}

	private URI buildListUri(LocalDate from, LocalDate to, int page, int rows, String keyword) {
		UriComponentsBuilder builder = UriComponentsBuilder
			.fromUriString(properties.getBaseUri())
			.path("/pblprfr")
			.queryParam("service", properties.getServiceKey())
			.queryParam("stdate", from.format(REQUEST_DATE_FORMATTER))
			.queryParam("eddate", to.format(REQUEST_DATE_FORMATTER))
			.queryParam("cpage", page + 1)
			.queryParam("rows", rows);

		if (StringUtils.hasText(keyword)) {
			builder.queryParam("shprfnm", keyword.trim());
		}
		return builder.encode(StandardCharsets.UTF_8).build().toUri();
	}

	private URI buildDetailUri(String path) {
		return UriComponentsBuilder
			.fromUriString(properties.getBaseUri())
			.path(path)
			.queryParam("service", properties.getServiceKey())
			.encode(StandardCharsets.UTF_8)
			.build()
			.toUri();
	}

	private String get(URI uri) {
		return restClientBuilder.build()
			.get()
			.uri(uri)
			.retrieve()
			.body(String.class);
	}

	private String preferredVenueName(String concertVenueName, String facilityVenueName) {
		return StringUtils.hasText(concertVenueName) ? concertVenueName : facilityVenueName;
	}
}
