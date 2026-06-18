package com.buddyduck.buddyduck.domain.concert.kopis;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.LongConsumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;

@Slf4j
@Component
public class KopisRestClient implements KopisConcertClient {

	private static final DateTimeFormatter REQUEST_DATE_FORMATTER = DateTimeFormatter.BASIC_ISO_DATE;
	private static final Pattern TIME_PATTERN = Pattern.compile("(?<!\\d)([01]?\\d|2[0-3]):([0-5]\\d)(?!\\d)");
	private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(3);
	private static final Duration READ_TIMEOUT = Duration.ofSeconds(5);

	private final RestClient restClient;
	private final KopisProperties properties;
	private final KopisXmlParser parser;
	private final LongConsumer requestDelay;

	@Autowired
	public KopisRestClient(RestClient.Builder restClientBuilder, KopisProperties properties, KopisXmlParser parser) {
		this(
			restClientBuilder
				.requestFactory(requestFactory())
				.build(),
			properties,
			parser,
			KopisRestClient::sleep
		);
	}

	KopisRestClient(RestClient restClient, KopisProperties properties, KopisXmlParser parser) {
		this(restClient, properties, parser, KopisRestClient::sleep);
	}

	KopisRestClient(
		RestClient restClient,
		KopisProperties properties,
		KopisXmlParser parser,
		LongConsumer requestDelay
	) {
		this.restClient = restClient;
		this.properties = properties;
		this.parser = parser;
		this.requestDelay = requestDelay;
	}

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
			log.debug("Failed to fetch KOPIS concert detail. externalId={}", item.externalId());
			return Optional.empty();
		}
		Optional<KopisFacilityDetail> facility = fetchFacilityDetail(detail.get().facilityId());
		if (facility.isEmpty()) {
			log.debug("Failed to fetch KOPIS facility detail. facilityId={}", detail.get().facilityId());
			return Optional.empty();
		}

		KopisConcertDetail concert = detail.get();
		KopisFacilityDetail venue = facility.get();
		return Optional.of(new KopisConcertCandidate(
			concert.externalId(),
			concert.title(),
			preferredVenueName(concert.venueName(), venue.venueName()),
			startAt(concert),
			concert.endDate().atTime(LocalTime.MAX).withNano(0),
			venue.lat(),
			venue.lng(),
			normalizePosterUrl(concert.posterUrl()),
			concert.area(),
			concert.genre(),
			concert.timeGuidance()
		));
	}

	private LocalDateTime startAt(KopisConcertDetail concert) {
		return singleTime(concert.timeGuidance())
			.map(time -> concert.startDate().atTime(time))
			.orElseGet(() -> concert.startDate().atStartOfDay());
	}

	private Optional<LocalTime> singleTime(String timeGuidance) {
		if (!StringUtils.hasText(timeGuidance)) {
			return Optional.empty();
		}
		Matcher matcher = TIME_PATTERN.matcher(timeGuidance);
		Set<LocalTime> times = new LinkedHashSet<>();
		while (matcher.find()) {
			times.add(LocalTime.of(
				Integer.parseInt(matcher.group(1)),
				Integer.parseInt(matcher.group(2))
			));
		}
		return times.size() == 1 ? Optional.of(times.iterator().next()) : Optional.empty();
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
		delayRequest();
		return restClient.get()
			.uri(uri)
			.retrieve()
			.body(String.class);
	}

	private void delayRequest() {
		long delayMillis = properties.getRequestDelayMillis();
		if (delayMillis > 0) {
			requestDelay.accept(delayMillis);
		}
	}

	private static void sleep(long delayMillis) {
		try {
			Thread.sleep(delayMillis);
		} catch (InterruptedException exception) {
			Thread.currentThread().interrupt();
			throw new RestClientException("Interrupted while waiting before KOPIS request", exception);
		}
	}

	private static SimpleClientHttpRequestFactory requestFactory() {
		SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
		requestFactory.setConnectTimeout(CONNECT_TIMEOUT);
		requestFactory.setReadTimeout(READ_TIMEOUT);
		return requestFactory;
	}

	private String preferredVenueName(String concertVenueName, String facilityVenueName) {
		return StringUtils.hasText(concertVenueName) ? concertVenueName : facilityVenueName;
	}

	private String normalizePosterUrl(String posterUrl) {
		if (!StringUtils.hasText(posterUrl)) {
			return null;
		}
		return posterUrl.trim().replaceFirst("^http://", "https://");
	}
}
