package com.buddyduck.buddyduck.global.security;

import com.buddyduck.buddyduck.domain.auth.exception.AuthErrorCode;
import com.buddyduck.buddyduck.domain.user.repository.UserRepository;
import com.buddyduck.buddyduck.global.apiPayload.ApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@RequiredArgsConstructor
public class ProfileCompletionFilter extends OncePerRequestFilter {

	private static final String CONCERT_DETAIL_PREFIX = "/api/concerts/";

	private final UserRepository userRepository;
	private final ObjectMapper objectMapper;

	@Override
	protected boolean shouldNotFilter(HttpServletRequest request) {
		String method = request.getMethod();
		String path = request.getRequestURI();

		return HttpMethod.OPTIONS.matches(method)
			|| isPath(method, path, HttpMethod.GET, "/api/health")
			|| isPath(method, path, HttpMethod.POST, "/api/auth/kakao/login")
			|| isPath(method, path, HttpMethod.GET, "/api/users/me")
			|| isPath(method, path, HttpMethod.PATCH, "/api/users/me/profile")
			|| isPath(method, path, HttpMethod.GET, "/api/concerts")
			|| isConcertDetailPath(method, path)
			|| isPath(method, path, HttpMethod.POST, "/api/dev/seed/concerts")
			|| isPath(method, path, HttpMethod.POST, "/api/dev/seed/demo-room")
			|| path.equals("/swagger-ui.html")
			|| path.startsWith("/swagger-ui/")
			|| path.startsWith("/v3/api-docs/")
			|| path.equals("/error");
	}

	@Override
	protected void doFilterInternal(
		HttpServletRequest request,
		HttpServletResponse response,
		FilterChain filterChain
	) throws ServletException, IOException {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (!(authentication != null && authentication.getPrincipal() instanceof UserPrincipal principal)) {
			filterChain.doFilter(request, response);
			return;
		}

		boolean profileCompleted = userRepository.findById(principal.userId())
			.map(user -> user.isProfileCompleted())
			.orElse(false);

		if (!profileCompleted) {
			writeRequiredProfileResponse(response);
			return;
		}

		filterChain.doFilter(request, response);
	}

	private void writeRequiredProfileResponse(HttpServletResponse response) throws IOException {
		response.setStatus(AuthErrorCode.REQUIRED_PROFILE_INFO.getStatus().value());
		response.setCharacterEncoding(StandardCharsets.UTF_8.name());
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		objectMapper.writeValue(response.getWriter(), ApiResponse.onFailure(AuthErrorCode.REQUIRED_PROFILE_INFO, null));
	}

	private boolean isPath(String method, String path, HttpMethod httpMethod, String expectedPath) {
		return httpMethod.matches(method) && path.equals(expectedPath);
	}

	private boolean isConcertDetailPath(String method, String path) {
		if (!HttpMethod.GET.matches(method) || !path.startsWith(CONCERT_DETAIL_PREFIX)) {
			return false;
		}

		String concertId = path.substring(CONCERT_DETAIL_PREFIX.length());
		return !concertId.isBlank() && !concertId.contains("/");
	}
}
