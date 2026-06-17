package com.buddyduck.buddyduck.domain.place.repository;

import com.buddyduck.buddyduck.domain.place.entity.Place;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlaceRepository extends JpaRepository<Place, Long> {
}
