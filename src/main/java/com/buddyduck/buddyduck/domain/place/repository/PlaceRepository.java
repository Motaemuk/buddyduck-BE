package com.buddyduck.buddyduck.domain.place.repository;

import com.buddyduck.buddyduck.domain.place.entity.Place;
import com.buddyduck.buddyduck.domain.place.enums.PlaceSource;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlaceRepository extends JpaRepository<Place, Long> {

	List<Place> findTop10ByNameContainingIgnoreCaseOrAddressContainingIgnoreCaseOrderByIdAsc(
		String name,
		String address
	);

	List<Place> findTop10ByAddressContainingIgnoreCaseOrderByIdAsc(String address);

	Optional<Place> findByProviderAndProviderPlaceId(PlaceSource provider, String providerPlaceId);

	Optional<Place> findFirstByProviderAndNameAndAddressOrderByIdAsc(
		PlaceSource provider,
		String name,
		String address
	);
}
