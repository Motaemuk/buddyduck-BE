package com.buddyduck.buddyduck.global.apiPayload.handler;

import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.buddyduck.buddyduck.BuddyduckApplication;
import com.buddyduck.buddyduck.global.apiPayload.code.GeneralErrorCode;
import com.buddyduck.buddyduck.global.apiPayload.exception.ProjectException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;

@SpringBootTest(classes = {
	BuddyduckApplication.class,
	GeneralExceptionAdviceTest.ExceptionTestController.class
})
@AutoConfigureMockMvc(addFilters = false)
class GeneralExceptionAdviceTest {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void ProjectException이_발생하면_공통_실패_응답을_반환한다() throws Exception {
		mockMvc.perform(get("/test/project-exception"))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.isSuccess").value(false))
			.andExpect(jsonPath("$.code").value("COMMON404"))
			.andExpect(jsonPath("$.message").value("요청한 리소스를 찾을 수 없습니다."))
			.andExpect(jsonPath("$.result").value(nullValue()));
	}

	@Test
	void ValidationException이_발생하면_공통_실패_응답을_반환한다() throws Exception {
		mockMvc.perform(post("/test/validation")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{}"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.isSuccess").value(false))
			.andExpect(jsonPath("$.code").value("COMMON400"))
			.andExpect(jsonPath("$.message").value("잘못된 요청입니다."))
			.andExpect(jsonPath("$.result").value(nullValue()));
	}

	@Test
	void 처리하지_않은_Exception은_내부_메시지를_노출하지_않는다() throws Exception {
		mockMvc.perform(get("/test/unexpected-exception"))
			.andExpect(status().isInternalServerError())
			.andExpect(jsonPath("$.isSuccess").value(false))
			.andExpect(jsonPath("$.code").value("COMMON500"))
			.andExpect(jsonPath("$.message").value("서버 내부 오류가 발생했습니다."))
			.andExpect(jsonPath("$.result").value(nullValue()));
	}

	@Controller
	static class ExceptionTestController {

		@ResponseBody
		@GetMapping("/test/project-exception")
		String fail() {
			throw new ProjectException(GeneralErrorCode.NOT_FOUND);
		}

		@ResponseBody
		@PostMapping("/test/validation")
		String validate(@Valid @RequestBody ValidationRequest request) {
			return request.name();
		}

		@ResponseBody
		@GetMapping("/test/unexpected-exception")
		String unexpectedFail() {
			throw new IllegalStateException("internal-secret");
		}
	}

	record ValidationRequest(@NotBlank String name) {
	}
}
