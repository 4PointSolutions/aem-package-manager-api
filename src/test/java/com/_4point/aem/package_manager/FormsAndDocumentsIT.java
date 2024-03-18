package com._4point.aem.package_manager;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Path;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import com._4point.aem.package_manager.FormsAndDocumentsClient.AemError;
import com._4point.aem.package_manager.FormsAndDocumentsClient.DeleteResponse;
import com._4point.aem.package_manager.FormsAndDocumentsClient.DeleteResponse.DeleteSuccess;
import com._4point.aem.package_manager.FormsAndDocumentsClient.UploadResponse.UploadSuccess;
import com._4point.aem.package_manager.FormsAndDocumentsClient.UploadResponse;

@Disabled("This is an Integration test.  It assumes that AEM is running on localhost:4502 with default credentials.")
@TestMethodOrder(OrderAnnotation.class)
class FormsAndDocumentsIT {
	private static final Path RESOURCES_DIR = Path.of("src", "test", "resources");
	private static final Path SAMPLE_DATA_DIR = RESOURCES_DIR.resolve("SampleData");

	private final FormsAndDocumentsClient underTest = FormsAndDocumentsClient.builder().build();

	@Test
	@Order(1)
	void testUpload() {
		// When
		UploadResponse result = underTest.upload(SAMPLE_DATA_DIR.resolve("SampleForm.zip"));
	
		// Then
		assertThat(result, instanceOf(UploadSuccess.class));
	}

	@Test
	@Order(2)
	void testDelete() {
		// Given
		String targetLocation = "SampleForm.xdp";
		
		// When
		DeleteResponse result = underTest.delete(targetLocation);
	
		// Then
		assertThat(result, instanceOf(DeleteSuccess.class));
	}

	@Test
	@Order(3)
	void testDeleteAgain() {
		// Given
		String targetLocation = "SampleForm.xdp";
		
		// When
		DeleteResponse result = underTest.delete(targetLocation);
	
		// Then
		assertThat(result, instanceOf(AemError.class));
	}

}

