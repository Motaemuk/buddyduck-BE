package com.buddyduck.buddyduck.domain.room.service;

import com.buddyduck.buddyduck.domain.concert.entity.Concert;
import com.buddyduck.buddyduck.domain.concert.enums.InterestTag;
import com.buddyduck.buddyduck.domain.concert.repository.ConcertRepository;
import com.buddyduck.buddyduck.domain.place.enums.PlaceSource;
import com.buddyduck.buddyduck.domain.room.dto.CreateRoomRequest;
import com.buddyduck.buddyduck.domain.room.dto.CreateRoomResponse;
import com.buddyduck.buddyduck.domain.room.dto.DemoRoomSeedResponse;
import com.buddyduck.buddyduck.domain.room.dto.RoomPlaceRequest;
import com.buddyduck.buddyduck.domain.user.entity.User;
import com.buddyduck.buddyduck.domain.user.enums.AgeRange;
import com.buddyduck.buddyduck.domain.user.enums.UserGender;
import com.buddyduck.buddyduck.domain.user.repository.UserRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Profile({"local", "test"})
@RequiredArgsConstructor
public class DevRoomSeedService {

	private static final String DEMO_HOST_KAKAO_ID = "dev:demo-host";
	private static final String DEMO_CONCERT_SOURCE = "SEED";
	private static final String DEMO_CONCERT_EXTERNAL_ID = "seed-demo-concert";

	private final UserRepository userRepository;
	private final ConcertRepository concertRepository;
	private final RoomService roomService;

	@Transactional
	public DemoRoomSeedResponse seedDemoRoom() {
		LocalDate demoDate = LocalDate.now().plusDays(14);
		LocalDateTime concertStartAt = demoDate.atTime(19, 0);
		LocalDateTime concertEndAt = demoDate.atTime(21, 30);
		OffsetDateTime meetingAt = demoDate.atTime(14, 0).atOffset(ZoneOffset.ofHours(9));

		User host = userRepository.findByKakaoId(DEMO_HOST_KAKAO_ID)
			.orElseGet(() -> userRepository.save(createDemoHost()));
		Concert concert = concertRepository.findBySourceAndExternalId(DEMO_CONCERT_SOURCE, DEMO_CONCERT_EXTERNAL_ID)
			.orElseGet(() -> concertRepository.save(Concert.create(
				DEMO_CONCERT_EXTERNAL_ID,
				"AURORA LIVE",
				"KSPO Dome",
				concertStartAt,
				concertEndAt,
				new BigDecimal("37.5190000"),
				new BigDecimal("127.1270000"),
				DEMO_CONCERT_SOURCE
			)));

		CreateRoomResponse response = roomService.createRoom(host.getId(), new CreateRoomRequest(
			concert.getId(),
			"굿즈 구매 동선 같이 맞출 분",
			"데모용 방입니다.",
			4,
			List.of(InterestTag.GOODS_BUYING, InterestTag.CAFE_VISIT),
			meetingAt,
			new RoomPlaceRequest(
				PlaceSource.KAKAO_ADDRESS,
				"seed-meeting-place",
				"잠실역 5번 출구",
				"서울 송파구 잠실동",
				new BigDecimal("37.5130000"),
				new BigDecimal("127.1000000")
			),
			new RoomPlaceRequest(
				PlaceSource.CONCERT,
				"seed-event-place",
				"KSPO Dome",
				"서울 송파구 올림픽로 424",
				new BigDecimal("37.5190000"),
				new BigDecimal("127.1270000")
			),
			"https://open.kakao.com/o/test",
			"1234"
		));

		return new DemoRoomSeedResponse(response.roomId(), response.scheduleId());
	}

	private User createDemoHost() {
		User user = User.createKakao(DEMO_HOST_KAKAO_ID, "demo_host", AgeRange.PRIVATE, UserGender.PRIVATE);
		user.completeProfile("demo_host", AgeRange.PRIVATE, UserGender.PRIVATE, false, false);
		return user;
	}
}
