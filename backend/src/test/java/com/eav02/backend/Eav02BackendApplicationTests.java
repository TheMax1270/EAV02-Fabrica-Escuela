package com.eav02.backend;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.eav02.backend.auth.repository.AuthSessionRepository;
import com.eav02.backend.user.repository.UserRepository;

@SpringBootTest
class Eav02BackendApplicationTests {

	@MockitoBean
	private DataSource dataSource;

	@MockitoBean
	private UserRepository userRepository;

	@MockitoBean
	private AuthSessionRepository authSessionRepository;

	@Test
	void contextLoads() {
	}

}
