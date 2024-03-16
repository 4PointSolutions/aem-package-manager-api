package com._4point.aem.package_manager;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com._4point.aem.package_manager.FormsAndDocumentsClient.DeleteResponse;
import com._4point.aem.package_manager.FormsAndDocumentsClient.DeleteResponse.DeleteError;
import com._4point.aem.package_manager.FormsAndDocumentsClient.DeleteResponse.DeleteSuccess;
import com._4point.aem.package_manager.rest_client.RestClient.ContentType;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;

@WireMockTest
class FormsAndDocumentsClientTest {

	static final String DELETE_FAILURE_RESPONSE = """
			{
			    "code": "ALC-FMG-600-009",
			    "type": "error",
			    "title": "No node exists at path : /content/dam/formsanddocuments/fidelity-of",
			    "description": "No node exists at path : /content/dam/formsanddocuments/fidelity-of",
			    "unresolvedMessage": "No node exists at path : {0}",
			    "messageArgs": [
			        "/content/dam/formsanddocuments/fidelity-of"
			    ],
			    "rootCause": "com.adobe.livecycle.formsmanagement.common.FormsManagerException: No node exists at path : /content/dam/formsanddocuments/fidelity-of",
			    "extendedData": null
			}
			""";
	private FormsAndDocumentsClient underTest;
	
	@BeforeEach
	void setup(WireMockRuntimeInfo wmRuntimeInfo) {
		 underTest = FormsAndDocumentsClient.builder().port(wmRuntimeInfo.getHttpPort()).build();
	}

	@Test
	void testDelete_Success() {
		String targetFolder = "/content/dam/formsanddocuments/fidelity-of";
		String response = "{\"requestStatus\":\"success\"}";
		stubForDelete(targetFolder, response);
		assertThat(underTest.delete(targetFolder), instanceOf(DeleteSuccess.class));
	}

	@Test
	void testDelete_Failure() {
		String targetFolder = "/content/dam/formsanddocuments/fidelity-of";
		String response = DELETE_FAILURE_RESPONSE;
		stubForDelete(targetFolder, response);
		DeleteResponse result = underTest.delete(targetFolder);
		assertThat(result, instanceOf(DeleteError.class));
		DeleteError deleteError = (DeleteError) result;
		assertAll(
				()->assertEquals("ALC-FMG-600-009", deleteError.code()),
				()->assertEquals("error", deleteError.type()),
				()->assertEquals("No node exists at path : /content/dam/formsanddocuments/fidelity-of", deleteError.title()),
				()->assertEquals("No node exists at path : /content/dam/formsanddocuments/fidelity-of", deleteError.description()),
				()->assertEquals("No node exists at path : {0}", deleteError.unresolvedMessage()),
				()->assertEquals(List.of("/content/dam/formsanddocuments/fidelity-of"), deleteError.messageArgs()),
				()->assertEquals("com.adobe.livecycle.formsmanagement.common.FormsManagerException: No node exists at path : /content/dam/formsanddocuments/fidelity-of", deleteError.rootCause())
				);
	}

	@Test
	void testPreviewStringByteArrayString() {
		fail("Not yet implemented");
	}

	@Test
	void testPreviewPathString() {
		fail("Not yet implemented");
	}

	@Test
	void testUpload() {
		fail("Not yet implemented");
	}

	@Test
	void testCreateFolder() {
		fail("Not yet implemented");
	}

	void stubForDelete(String targetFolder, String response) {
		stubFor(post(urlPathEqualTo("/libs/fd/fm/content/manage.json"))
				.withQueryParam("func", equalTo("deleteAssets"))
				.withMultipartRequestBody(aMultipart("assetPaths").withBody(equalTo(targetFolder)))
			.willReturn(
					okForContentType(ContentType.APPLICATION_JSON.contentType(), response)
					));
	}

}
