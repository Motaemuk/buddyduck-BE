package com.buddyduck.buddyduck.domain.schedule.route;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "kakao.mobility")
public class KakaoMobilityProperties {

	private String restApiKey = "";
	private String drivingDirectionsUri = "https://apis-navi.kakaomobility.com/v1/directions";

	public boolean enabled() {
		return StringUtils.hasText(restApiKey);
	}
}
