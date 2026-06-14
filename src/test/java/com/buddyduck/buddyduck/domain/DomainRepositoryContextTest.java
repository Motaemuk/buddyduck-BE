package com.buddyduck.buddyduck.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.buddyduck.buddyduck.domain.concert.repository.ConcertInterestTagRepository;
import com.buddyduck.buddyduck.domain.concert.repository.ConcertRepository;
import com.buddyduck.buddyduck.domain.place.repository.PlaceRepository;
import com.buddyduck.buddyduck.domain.room.repository.JoinRequestRepository;
import com.buddyduck.buddyduck.domain.room.repository.RoomMemberRepository;
import com.buddyduck.buddyduck.domain.room.repository.RoomRepository;
import com.buddyduck.buddyduck.domain.room.repository.RoomTagRepository;
import com.buddyduck.buddyduck.domain.schedule.repository.RouteSegmentRepository;
import com.buddyduck.buddyduck.domain.schedule.repository.ScheduleRepository;
import com.buddyduck.buddyduck.domain.schedule.repository.ScheduleSlotRepository;
import com.buddyduck.buddyduck.domain.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class DomainRepositoryContextTest {

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private ConcertRepository concertRepository;

	@Autowired
	private ConcertInterestTagRepository concertInterestTagRepository;

	@Autowired
	private PlaceRepository placeRepository;

	@Autowired
	private RoomRepository roomRepository;

	@Autowired
	private RoomTagRepository roomTagRepository;

	@Autowired
	private RoomMemberRepository roomMemberRepository;

	@Autowired
	private JoinRequestRepository joinRequestRepository;

	@Autowired
	private ScheduleRepository scheduleRepository;

	@Autowired
	private ScheduleSlotRepository scheduleSlotRepository;

	@Autowired
	private RouteSegmentRepository routeSegmentRepository;

	@Test
	void ERD_기반_repository_bean이_등록된다() {
		assertThat(userRepository).isNotNull();
		assertThat(concertRepository).isNotNull();
		assertThat(concertInterestTagRepository).isNotNull();
		assertThat(placeRepository).isNotNull();
		assertThat(roomRepository).isNotNull();
		assertThat(roomTagRepository).isNotNull();
		assertThat(roomMemberRepository).isNotNull();
		assertThat(joinRequestRepository).isNotNull();
		assertThat(scheduleRepository).isNotNull();
		assertThat(scheduleSlotRepository).isNotNull();
		assertThat(routeSegmentRepository).isNotNull();
	}
}
