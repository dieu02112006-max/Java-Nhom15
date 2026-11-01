package com.example.demo;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

import com.example.demo.entity.User;
import com.example.demo.entity.enums.UserRole;
import com.example.demo.security.AppUserPrincipal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

@SpringBootTest
@AutoConfigureMockMvc
class DemoApplicationTests {

	@Autowired
	private MockMvc mockMvc;

	private final AppUserPrincipal admin = new AppUserPrincipal(User.builder()
			.id(10L)
			.email("admin@example.com")
			.passwordHash("unused")
			.fullName("Test Admin")
			.role(UserRole.ADMIN)
			.build());

	@Test
	void contextLoads() {
	}

	@Test
	void managesTodoResourcesAndReturnsTaskLinks() throws Exception {
		mockMvc.perform(post("/api/task-lists")
						.with(adminRequest())
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"userId":10,"name":"Work","color":"blue","position":0}
								"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.id").value(1));

		mockMvc.perform(post("/api/tasks")
						.with(adminRequest())
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"userId":10,"listId":1,"title":"Prepare report","status":"todo","priority":"high"}
								"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.id").value(1));

		mockMvc.perform(get("/api/tasks/1").with(adminRequest()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$._links.self.href").value(containsString("/api/tasks/1")))
				.andExpect(jsonPath("$._links.tasks.href").value(containsString("/api/tasks")));

		mockMvc.perform(post("/api/tags")
						.with(adminRequest())
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"userId":10,"name":"Urgent","color":"red"}
								"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.id").value(1));

		mockMvc.perform(post("/api/task-tags")
						.with(adminRequest())
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"taskId":1,"tagId":1}
								"""))
				.andExpect(status().isCreated());

		mockMvc.perform(post("/api/reminders")
						.with(adminRequest())
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"userId":10,"taskId":1,"remindAt":"2026-10-25T14:00:00","sent":false}
								"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.taskId").value(1));
	}

	private RequestPostProcessor adminRequest() {
		return user(admin);
	}
}
