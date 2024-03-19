package com._4point.aem.package_manager;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com._4point.aem.package_manager.FormsAndDocumentsClient.AemError;
import com._4point.aem.package_manager.FormsAndDocumentsClient.DeleteResponse;
import com._4point.aem.package_manager.FormsAndDocumentsClient.DeleteResponse.DeleteSuccess;
import com._4point.aem.package_manager.FormsAndDocumentsClient.FormsAndDocumentsException;
import com._4point.aem.package_manager.FormsAndDocumentsClient.PreviewResponse;
import com._4point.aem.package_manager.FormsAndDocumentsClient.PreviewResponse.PreviewSuccess;
import com._4point.aem.package_manager.FormsAndDocumentsClient.UploadResponse;
import com._4point.aem.package_manager.FormsAndDocumentsClient.UploadResponse.UploadSuccess;
import com._4point.aem.package_manager.rest_client.RestClient.ContentType;
import com._4point.testing.matchers.javalang.ExceptionMatchers;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;

@WireMockTest
class FormsAndDocumentsClientTest {
	private static final Path RESOURCES_DIR = Path.of("src", "test", "resources");
	private static final Path SAMPLE_DATA_DIR = RESOURCES_DIR.resolve("SampleData");

	static final String DELETE_FAILURE_RESPONSE = """
			{
			    "code": "ALC-FMG-600-009",
			    "type": "error",
			    "title": "No node exists at path : /content/dam/formsanddocuments/sample-of",
			    "description": "No node exists at path : /content/dam/formsanddocuments/sample-of",
			    "unresolvedMessage": "No node exists at path : {0}",
			    "messageArgs": [
			        "/content/dam/formsanddocuments/sample-of"
			    ],
			    "rootCause": "com.adobe.livecycle.formsmanagement.common.FormsManagerException: No node exists at path : /content/dam/formsanddocuments/sample-of",
			    "extendedData": null
			}\
			""";
	
	static final String PREVIEW_RESPONSE = """
			{
				"fileId": "30226661338789",
				"fileName": "sample-of-0.0.1-SNAPSHOT.zip",
				"uploadType": "assets",
				"changes": [{
					"path": "/content/dam/formsanddocuments/sample-of/Images/sampleImage1.jpg",
					"create": true,
					"nameValid": true,
					"name": "sampleImage1.jpg",
					"section": "Forms & Documents",
					"relativeLocation": "/sample-of/Images"
				}, {
					"path": "/content/dam/formsanddocuments/sample-of/Images/sampleImage2.jpg",
					"create": true,
					"nameValid": true,
					"name": "sampleImage2.jpg",
					"section": "Forms & Documents",
					"relativeLocation": "/sample-of/Images"
				}, {
					"path": "/content/dam/formsanddocuments/sample-of/sample_form.xdp",
					"create": true,
					"nameValid": true,
					"name": "sample_form.xdp",
					"section": "Forms & Documents",
					"relativeLocation": "/sample-of"
				}]
			}\
			""";

	static final String PREVIEW_FAILURE = """
			{
			    "code": "ALC-FMG-001-001",
			    "type": "error",
			    "title": "Unexpected record signature: 0x6469462f",
			    "description": "Unexpected record signature: 0x6469462f",
			    "messageArgs": [],
			    "rootCause": "com.adobe.livecycle.formsmanagement.common.FormsManagerException: Unexpected record signature: 0x6469462f",
			    "extendedData": null
			}\
			""";
	
	static final String UPLOAD_RESPONSE = """
			{
				"lastUploadedAssetPath": "/content/dam/formsanddocuments/fidelity-of/resp_family_fr.xdp"
			}\
			""";
	
	static final String UPLOAD_FAILURE = """
			{
			    "code": "ALC-FMG-001-001",
			    "type": "error",
			    "title": "java.lang.NullPointerException",
			    "description": "java.lang.NullPointerException",
			    "messageArgs": [],
			    "rootCause": "com.adobe.livecycle.formsmanagement.common.FormsManagerException: java.lang.NullPointerException",
			    "extendedData": null
			}\
			""";
	
	private FormsAndDocumentsClient underTest;
	
	@BeforeEach
	void setup(WireMockRuntimeInfo wmRuntimeInfo) {
		 underTest = FormsAndDocumentsClient.builder().port(wmRuntimeInfo.getHttpPort()).build();
	}

	@ParameterizedTest
	@ValueSource(strings = {DELETE_FAILURE_RESPONSE, PREVIEW_FAILURE, UPLOAD_FAILURE})
	void testAemError_From_Success(String errorResponse) {
		JsonData errorJson = JsonData.from(errorResponse); 
		AemError aemError = AemError.from(errorJson).orElseThrow();
		
		assertAll(
				()->assertThat(aemError.code(), startsWith("ALC-FMG-")),
				()->assertEquals(aemError.type(), "error"),
				()->assertThat(aemError.title(), not(emptyOrNullString())),
				()->assertThat(aemError.description(), not(emptyOrNullString())),
				()->assertThat(aemError.rootCause(), not(emptyOrNullString()))
				);
	}

	@Test
	void testDelete_Success() {
		String targetFolder = "sample-of";
		String response = "{\"requestStatus\":\"success\"}";
		stubForDelete(targetFolder, response);
		assertThat(underTest.delete(targetFolder), instanceOf(DeleteSuccess.class));
	}

	@Test
	void testDelete_Failure() {
		// Given
		String targetFolder = "sample-of";
		String response = DELETE_FAILURE_RESPONSE;
		stubForDelete(targetFolder, response);

		// When
		DeleteResponse result = underTest.delete(targetFolder);

		// Then - should contain that error was returned and text from the response.
		assertThat(result, instanceOf(AemError.class));
		AemError aemError = (AemError) result;
		assertAll(
				()->assertEquals("ALC-FMG-600-009", aemError.code()),
				()->assertEquals("error", aemError.type()),
				()->assertEquals("No node exists at path : /content/dam/formsanddocuments/sample-of", aemError.title()),
				()->assertEquals("No node exists at path : /content/dam/formsanddocuments/sample-of", aemError.description()),
				()->assertEquals("No node exists at path : {0}", aemError.unresolvedMessage().orElseThrow()),
				()->assertEquals(List.of("/content/dam/formsanddocuments/sample-of"), aemError.messageArgs()),
				()->assertEquals("com.adobe.livecycle.formsmanagement.common.FormsManagerException: No node exists at path : /content/dam/formsanddocuments/sample-of", aemError.rootCause())
				);
	}


	@Test
	void testDelete_UnexpectedRequestStatus() {
		// Given
		String unexpectedStatus = "unexpected";
		String targetFolder = "sample-of";
		String response = "{\"requestStatus\":\"" + unexpectedStatus + "\"}";
		stubForDelete(targetFolder, response);
		
		// When
		FormsAndDocumentsException ex = assertThrows(FormsAndDocumentsException.class, ()->underTest.delete(targetFolder));

		// Then - should contain that error was returned and text from the response.
		assertAll(
				()->assertThat(ex, ExceptionMatchers.exceptionMsgContainsAll("Error while deleting folder", targetFolder)),
				()->assertThat(ex.getCause(), ExceptionMatchers.exceptionMsgContainsAll("Unexpected requestStatus returned from AEM", unexpectedStatus))
				);
	}

	@Test
	void testDelete_UnexpectedResponse() {
		// Given
		String targetFolder = "sample-of";
		String response = "{ \"foo\": \"bar\" }";
		stubForDelete(targetFolder, response);
		
		// When
		FormsAndDocumentsException ex = assertThrows(FormsAndDocumentsException.class, ()->underTest.delete(targetFolder));
		
		// Then - should contain that error was returned and text from the response.
		assertAll(
				()->assertThat(ex, ExceptionMatchers.exceptionMsgContainsAll("Error while deleting folder", targetFolder)),
				()->assertThat(ex.getCause(), ExceptionMatchers.exceptionMsgContainsAll("Unexpected response returned from AEM", response))
				);

	}
	
	@Test
	void testPreviewStringByteArrayString_Success() {
		// Given
		String filename = "sample-of-0.0.1-SNAPSHOT.zip";
		byte[] content = "Sample Bytes".getBytes();
		String targetLocation = "";
		stubForPreview(filename, PREVIEW_RESPONSE);
		
		// When
		PreviewResponse result = underTest.preview(filename, content, targetLocation);
	
		// Then
		assertThat(result, instanceOf(PreviewSuccess.class));
		assertEquals("30226661338789", ((PreviewSuccess)result).fileId());
	}

	@Test
	void testPreviewStringByteArrayString_Failure() {
		// Given
		String filename = "sample-of-0.0.1-SNAPSHOT.zip";
		byte[] content = "Sample Bytes".getBytes();
		String targetLocation = "";
		stubForPreview(filename, PREVIEW_FAILURE);
		
		// When
		PreviewResponse result = underTest.preview(filename, content, targetLocation);

		// Then
		assertThat(result, instanceOf(AemError.class));
		AemError aemError = (AemError) result;
		assertAll(
				()->assertEquals("ALC-FMG-001-001", aemError.code()),
				()->assertEquals("error", aemError.type()),
				()->assertEquals("Unexpected record signature: 0x6469462f", aemError.title()),
				()->assertEquals("Unexpected record signature: 0x6469462f", aemError.description()),
				()->assertTrue(aemError.unresolvedMessage().isEmpty()),
				()->assertThat(aemError.messageArgs(), empty()),
				()->assertEquals("com.adobe.livecycle.formsmanagement.common.FormsManagerException: Unexpected record signature: 0x6469462f", aemError.rootCause())
				);
	}

	@Test
	void testPreviewPathString() {
		// Given
		String filename = "SampleForm.zip";
		stubForPreview(filename, PREVIEW_RESPONSE);
		
		// When
		PreviewResponse result = underTest.preview(SAMPLE_DATA_DIR.resolve(filename), "");
	
		// Then
		assertThat(result, instanceOf(PreviewSuccess.class));
		assertEquals("30226661338789", ((PreviewSuccess)result).fileId());
	}

	@Test
	void testUpload_Success() {
		String fileId = "30226661338789";
		stubForUpload(fileId, UPLOAD_RESPONSE);
		
		UploadResponse result = underTest.upload(fileId, "");
		
		assertThat(result, instanceOf(UploadSuccess.class));
	}

	@Test
	void testUpload_Failure() {
		String fileId = "30226661338789";
		stubForUpload(fileId, UPLOAD_FAILURE);
		
		UploadResponse result = underTest.upload(fileId, "");
		
		assertThat(result, instanceOf(AemError.class));
		AemError aemError = (AemError)result;
		assertAll(
				()->assertEquals("ALC-FMG-001-001", aemError.code()),
				()->assertEquals("error", aemError.type()),
				()->assertEquals("java.lang.NullPointerException", aemError.title()),
				()->assertEquals("java.lang.NullPointerException", aemError.description()),
				()->assertTrue(aemError.unresolvedMessage().isEmpty()),
				()->assertThat(aemError.messageArgs(), empty()),
				()->assertEquals("com.adobe.livecycle.formsmanagement.common.FormsManagerException: java.lang.NullPointerException", aemError.rootCause())
				);
	}

	@Test
	void testUploadPathString() {
		// Given
		String filename = "SampleForm.zip";
		stubForPreview(filename, PREVIEW_RESPONSE);
		String fileId = "30226661338789";
		stubForUpload(fileId, UPLOAD_RESPONSE);
		
		// When
		UploadResponse result = underTest.upload(SAMPLE_DATA_DIR.resolve(filename), "");
	
		// Then
		assertThat(result, instanceOf(UploadSuccess.class));
	}

	@Disabled("Not yet implemented")
	@Test
	void testCreateFolder() {
		fail("Not yet implemented");
	}

	static void stubForDelete(String targetFolder, String response) {
		stubFor(post(urlPathEqualTo("/libs/fd/fm/content/manage.json"))
				.withQueryParam("func", equalTo("deleteAssets"))
				.withMultipartRequestBody(aMultipart("assetPaths").withBody(equalTo("/content/dam/formsanddocuments/" + targetFolder)))
			.willReturn(
					okForContentType(ContentType.APPLICATION_JSON.contentType(), response)
					));
	}

	static void stubForPreview(String filename, String response) {
		stubFor(post(urlPathEqualTo("/libs/fd/fm/content/manage.json"))
				.withQueryParam("func", equalTo("uploadFormsPreview"))
				.withQueryParam("folderPath", containing("/content/dam/formsanddocuments"))
				.withQueryParam("isIE", equalTo("false"))
				.withMultipartRequestBody(aMultipart("filename").withBody(equalTo(filename)))
				.withMultipartRequestBody(aMultipart("_charset_").withBody(equalTo("UTF-8")))
				.withMultipartRequestBody(aMultipart("file").withBody(containing("")))
			.willReturn(
					okForContentType(ContentType.APPLICATION_JSON.contentType(), response)
					));
	}

	static void stubForUpload(String fileId, String response) {
		stubFor(post(urlPathEqualTo("/libs/fd/fm/content/manage.json"))
				.withQueryParam("func", equalTo("uploadForms"))
				.withQueryParam("folderPath", containing("/content/dam/formsanddocuments"))
				.withQueryParam("fileId", equalTo(fileId))
				.withQueryParam("uploadType", equalTo("assets"))
				.withMultipartRequestBody(aMultipart("_charset_").withBody(equalTo("UTF-8")))
			.willReturn(
					okForContentType(ContentType.APPLICATION_JSON.contentType(), response)
					));
	}

}
