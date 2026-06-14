package com.buddyduck.buddyduck.global.apiPayload.handler;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.buddyduck.buddyduck.BuddyduckApplication;
import com.buddyduck.buddyduck.global.apiPayload.code.GeneralErrorCode;
import com.buddyduck.buddyduck.global.apiPayload.exception.ProjectException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.stereotype.Controller;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@SpringBootTest(classes = {
	BuddyduckApplication.class,
	GeneralExceptionAdviceTest.ExceptionTestController.class
})
@AutoConfigureMockMvc
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
			.andExpect(jsonPath("$.result").doesNotExist());
	}

	@Controller
	static class ExceptionTestController {

		@ResponseBody
		@GetMapping("/test/project-exception")
		String fail() {
			throw new ProjectException(GeneralErrorCode.NOT_FOUND);
		}
	}
}
