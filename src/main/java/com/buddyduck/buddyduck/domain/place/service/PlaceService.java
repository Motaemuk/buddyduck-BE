package com.buddyduck.buddyduck.domain.place.service;

import com.buddyduck.buddyduck.domain.place.dto.GeocodeItemResponse;
import com.buddyduck.buddyduck.domain.place.dto.GeocodeResponse;
import com.buddyduck.buddyduck.domain.place.dto.PlaceSearchItemResponse;
import com.buddyduck.buddyduck.domain.place.dto.PlaceSearchResponse;
import com.buddyduck.buddyduck.domain.place.dto.PlaceUpsertRequest;
import com.buddyduck.buddyduck.domain.place.dto.PlaceUpsertResponse;
import com.buddyduck.buddyduck.domain.place.entity.Place;
import com.buddyduck.buddyduck.domain.place.kakao.KakaoLocalClient;
import com.buddyduck.buddyduck.domain.place.repository.PlaceRepository;
import com.buddyduck.buddyduck.global.apiPayload.code.GeneralErrorCode;
import com.buddyduck.buddyduck.global.apiPayload.exception.ProjectException;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class PlaceService {

	private final PlaceRepository placeRepository;
	private final KakaoLocalClient kakaoLocalClient;

	@Transactional(readOnly = true)
	public PlaceSearchResponse searchPlaces(String keyword, Long concertId, Long roomId) {
		String normalizedKeyword = requireText(keyword);
		if (kakaoLocalClient.isEnabled()) {
			return new PlaceSearchResponse(kakaoLocalClient.searchKeyword(normalizedKeyword).stream()
				.map(PlaceSearchItemResponse::from)
				.toList());
		}

		List<PlaceSearchItemResponse> items = placeRepository
			.findTop10ByNameContainingIgnoreCaseOrAddressContainingIgnoreCaseOrderByIdAsc(
				normalizedKeyword,
				normalizedKeyword
			)
			.stream()
			.map(PlaceSearchItemResponse::from)
			.toList();

		return new PlaceSearchResponse(items);
	}

	@Transactional(readOnly = true)
	public GeocodeResponse geocode(String address) {
		String normalizedAddress = requireText(address);
		if (kakaoLocalClient.isEnabled()) {
			return new GeocodeResponse(kakaoLocalClient.searchAddress(normalizedAddress).stream()
				.map(GeocodeItemResponse::from)
				.toList());
		}

		List<GeocodeItemResponse> items = placeRepository
			.findTop10ByAddressContainingIgnoreCaseOrderByIdAsc(normalizedAddress)
			.stream()
			.map(GeocodeItemResponse::from)
			.toList();

		return new GeocodeResponse(items);
	}

	@Transactional
	public PlaceUpsertResponse upsertPlace(PlaceUpsertRequest request) {
		Place place = findReusablePlace(request)
			.map(existingPlace -> updatePlace(existingPlace, request))
			.orElseGet(() -> createPlace(request));

		Place savedPlace = placeRepository.save(place);
		return new PlaceUpsertResponse(savedPlace.getId());
	}

	private Optional<Place> findReusablePlace(PlaceUpsertRequest request) {
		if (StringUtils.hasText(request.providerPlaceId())) {
			return placeRepository.findByProviderAndProviderPlaceId(request.provider(), request.providerPlaceId());
		}
		return placeRepository.findFirstByProviderAndNameAndAddressOrderByIdAsc(
			request.provider(),
			request.name(),
			request.address()
		);
	}

	private Place updatePlace(Place place, PlaceUpsertRequest request) {
		place.update(
			request.name(),
			request.address(),
			request.lat(),
			request.lng()
		);
		return place;
	}

	private Place createPlace(PlaceUpsertRequest request) {
		return Place.create(
			request.provider(),
			request.providerPlaceId(),
			request.name(),
			request.address(),
			request.lat(),
			request.lng()
		);
	}

	private String requireText(String value) {
		if (!StringUtils.hasText(value)) {
			throw new ProjectException(GeneralErrorCode.BAD_REQUEST);
		}
		return value.trim();
	}
}
