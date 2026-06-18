package com.buddyduck.buddyduck.domain.concert.kopis;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "kopis")
public class KopisProperties {

	private String serviceKey = "";
	private String baseUri = "http://www.kopis.or.kr/openApi/restful";
	private int maxSyncRows = 10;

	public boolean enabled() {
		return StringUtils.hasText(serviceKey);
	}
}
