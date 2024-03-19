package com._4point.aem.package_manager;

import static com._4point.aem.package_manager.FormsAndDocumentsClientTest.*;
import static org.hamcrest.MatcherAssert.assertThat; 
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Path;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com._4point.aem.package_manager.FormsAndDocumentsClient.FormsAndDocumentsException;
import com._4point.testing.matchers.javalang.ExceptionMatchers;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;

@WireMockTest
class FormsAndDocumentsClientExTest {
	private static final Path RESOURCES_DIR = Path.of("src", "test", "resources");
	private static final Path SAMPLE_DATA_DIR = RESOURCES_DIR.resolve("SampleData");

	private FormsAndDocumentsClientEx underTest;

	@BeforeEach
	void setup(WireMockRuntimeInfo wmRuntimeInfo) {
		 underTest = FormsAndDocumentsClient.builder().port(wmRuntimeInfo.getHttpPort()).buildEx();
	}

	@Test
	void testDelete_Success() {
		String targetFolder = "sample-of";
		String response = "{\"requestStatus\":\"success\"}";
		stubForDelete(targetFolder, response);
		underTest.delete(targetFolder);
	}

	@Test
	void testDelete_Failure() {
		// Given
		String targetFolder = "sample-of";
		String response = DELETE_FAILURE_RESPONSE;
		stubForDelete(targetFolder, response);
		
		FormsAndDocumentsException ex = assertThrows(FormsAndDocumentsException.class, ()->underTest.delete(targetFolder));
		
		assertThat(ex, ExceptionMatchers.exceptionMsgContainsAll("Error while performing", "delete", "No node exists at path : /content/dam/formsanddocuments/sample-of"));
	}

	@Test
	void testUploadPath_Success() {
		// Given
		String filename = "SampleForm.zip";
		stubForPreview(filename, PREVIEW_RESPONSE);
		String fileId = "30226661338789";
		stubForUpload(fileId, UPLOAD_RESPONSE);
		
		underTest.upload(SAMPLE_DATA_DIR.resolve(filename));
	}

	@Test
	void testUploadPath_FailureInPreview() {
		String filename = "SampleForm.zip";
		stubForPreview(filename, PREVIEW_FAILURE);
		
		FormsAndDocumentsException ex = assertThrows(FormsAndDocumentsException.class, ()->underTest.upload(SAMPLE_DATA_DIR.resolve(filename)));
		
		assertThat(ex, ExceptionMatchers.exceptionMsgContainsAll("Error while performing", "upload", "Unexpected record signature: 0x6469462f"));
	}

	@Test
	void testUploadPath_FailureInUpload() {
		String filename = "SampleForm.zip";
		stubForPreview(filename, PREVIEW_RESPONSE);
		String fileId = "30226661338789";
		stubForUpload(fileId, UPLOAD_FAILURE);
		
		FormsAndDocumentsException ex = assertThrows(FormsAndDocumentsException.class, ()->underTest.upload(SAMPLE_DATA_DIR.resolve(filename)));
		
		assertThat(ex, ExceptionMatchers.exceptionMsgContainsAll("Error while performing", "upload", "java.lang.NullPointerException"));
	}

	@Test
	void testUploadPathString_Success() {
		String filename = "SampleForm.zip";
		stubForPreview(filename, PREVIEW_RESPONSE);
		String fileId = "30226661338789";
		stubForUpload(fileId, UPLOAD_RESPONSE);
		
		underTest.upload(SAMPLE_DATA_DIR.resolve(filename), "");
	}

	@Test
	void testUploadPathString_FailureInPreview() {
		String filename = "SampleForm.zip";
		stubForPreview(filename, PREVIEW_FAILURE);
		
		FormsAndDocumentsException ex = assertThrows(FormsAndDocumentsException.class, ()->underTest.upload(SAMPLE_DATA_DIR.resolve(filename), ""));
		
		assertThat(ex, ExceptionMatchers.exceptionMsgContainsAll("Error while performing", "upload", "Unexpected record signature: 0x6469462f"));
	}

	@Test
	void testUploadPathString_FailureInUpload() {
		String filename = "SampleForm.zip";
		stubForPreview(filename, PREVIEW_RESPONSE);
		String fileId = "30226661338789";
		stubForUpload(fileId, UPLOAD_FAILURE);
		
		FormsAndDocumentsException ex = assertThrows(FormsAndDocumentsException.class, ()->underTest.upload(SAMPLE_DATA_DIR.resolve(filename), ""));
		
		assertThat(ex, ExceptionMatchers.exceptionMsgContainsAll("Error while performing", "upload", "java.lang.NullPointerException"));
	}
}
