package com.buddyduck.buddyduck.domain.place.kakao;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "kakao.local")
public class KakaoLocalProperties {

	private String restApiKey = "";
	private String keywordSearchUri = "https://dapi.kakao.com/v2/local/search/keyword.json";
	private String addressSearchUri = "https://dapi.kakao.com/v2/local/search/address.json";

	public boolean enabled() {
		return StringUtils.hasText(restApiKey);
	}
}
