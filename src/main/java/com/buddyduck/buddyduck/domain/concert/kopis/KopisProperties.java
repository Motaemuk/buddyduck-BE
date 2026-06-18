package com.buddyduck.buddyduck.domain.concert.kopis;

import java.time.LocalDate;
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
	private String baseUri = "https://www.kopis.or.kr/openApi/restful";
	private int maxSyncRows = 10;
	private boolean syncOnQuery = false;
	private InitialImport initialImport = new InitialImport();

	public boolean enabled() {
		return StringUtils.hasText(serviceKey);
	}

	@Getter
	@Setter
	public static class InitialImport {

		private boolean enabled = false;
		private LocalDate from;
		private int days = 30;
		private int rows = 100;
		private int maxPages = 100;
	}
}
