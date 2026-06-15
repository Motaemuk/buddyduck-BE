package com.buddyduck.buddyduck.domain.place.kakao;

import com.buddyduck.buddyduck.domain.place.enums.PlaceSource;
import com.buddyduck.buddyduck.domain.place.kakao.dto.KakaoAddressDocument;
import com.buddyduck.buddyduck.domain.place.kakao.dto.KakaoAddressSearchResponse;
import com.buddyduck.buddyduck.domain.place.kakao.dto.KakaoKeywordDocument;
import com.buddyduck.buddyduck.domain.place.kakao.dto.KakaoKeywordSearchResponse;
import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

@Component
public class KakaoLocalRestClient implements KakaoLocalClient {

	private static final int KEYWORD_RESULT_SIZE = 15;
	private static final int ADDRESS_RESULT_SIZE = 10;
	private static final String AUTHORIZATION_PREFIX = "KakaoAK ";

	private final RestClient restClient;
	private final KakaoLocalProperties properties;

	public KakaoLocalRestClient(RestClient.Builder restClientBuilder, KakaoLocalProperties properties) {
		this.restClient = restClientBuilder.build();
		this.properties = properties;
	}

	@Override
	public boolean isEnabled() {
		return properties.enabled();
	}

	@Override
	public List<KakaoLocalPlaceCandidate> searchKeyword(String keyword) {
		if (!isEnabled()) {
			return List.of();
		}

		KakaoKeywordSearchResponse response = restClient.get()
			.uri(properties.getKeywordSearchUri() + "?query={query}&size={size}", keyword, KEYWORD_RESULT_SIZE)
			.header(HttpHeaders.AUTHORIZATION, AUTHORIZATION_PREFIX + properties.getRestApiKey())
			.retrieve()
			.body(KakaoKeywordSearchResponse.class);

		return documents(response).stream()
			.map(this::toKeywordCandidate)
			.toList();
	}

	@Override
	public List<KakaoLocalPlaceCandidate> searchAddress(String address) {
		if (!isEnabled()) {
			return List.of();
		}

		KakaoAddressSearchResponse response = restClient.get()
			.uri(properties.getAddressSearchUri() + "?query={query}&size={size}", address, ADDRESS_RESULT_SIZE)
			.header(HttpHeaders.AUTHORIZATION, AUTHORIZATION_PREFIX + properties.getRestApiKey())
			.retrieve()
			.body(KakaoAddressSearchResponse.class);

		return documents(response).stream()
			.map(this::toAddressCandidate)
			.toList();
	}

	private KakaoLocalPlaceCandidate toKeywordCandidate(KakaoKeywordDocument document) {
		return new KakaoLocalPlaceCandidate(
			PlaceSource.KAKAO_KEYWORD,
			document.id(),
			document.placeName(),
			preferredAddress(document.roadAddressName(), document.addressName()),
			new BigDecimal(document.y()),
			new BigDecimal(document.x())
		);
	}

	private KakaoLocalPlaceCandidate toAddressCandidate(KakaoAddressDocument document) {
		return new KakaoLocalPlaceCandidate(
			PlaceSource.KAKAO_ADDRESS,
			null,
			document.addressName(),
			document.addressName(),
			new BigDecimal(document.y()),
			new BigDecimal(document.x())
		);
	}

	private String preferredAddress(String roadAddressName, String addressName) {
		if (StringUtils.hasText(roadAddressName)) {
			return roadAddressName;
		}
		return addressName;
	}

	private List<KakaoKeywordDocument> documents(KakaoKeywordSearchResponse response) {
		if (response == null || response.documents() == null) {
			return List.of();
		}
		return response.documents().stream()
			.filter(Objects::nonNull)
			.toList();
	}

	private List<KakaoAddressDocument> documents(KakaoAddressSearchResponse response) {
		if (response == null || response.documents() == null) {
			return List.of();
		}
		return response.documents().stream()
			.filter(Objects::nonNull)
			.toList();
	}
}
