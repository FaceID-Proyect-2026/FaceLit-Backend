package com.FaceLit.backend;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.lang.reflect.Field;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import com.FaceLit.backend.academic.model.audit.ChangeHistory;

import jakarta.persistence.Column;

@SpringBootTest
class BackendApplicationTests {

	@Test
	void contextLoads() {
	}

	@Test
	void changeHistoryEntityShouldMapCreatedByColumn() throws NoSuchFieldException {
		Field field = ChangeHistory.class.getDeclaredField("createdBy");
		Column column = field.getAnnotation(Column.class);
		assertNotNull(column);
		assertEquals("created_by", column.name());
		assertEquals(100, column.length());
	}

}
