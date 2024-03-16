package com._4point.aem.package_manager;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com._4point.aem.package_manager.PackageManagerClient.PackageManagerException;
import com._4point.aem.package_manager.rest_client.RestClient.ContentType;
import com._4point.testing.matchers.javalang.ExceptionMatchers;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;

@WireMockTest
class PackageManagerClientTest {
	static final String SAMPLE_PACKAGE_FILENAME = "sample00002test.zip";
	static final Path RESOURCES_DIR = Path.of("src", "test", "resources");
	static final Path SAMPLE_DATA_DIR = RESOURCES_DIR.resolve("SampleData");

	static final String GROUP = "fd/export";
	static final String PACKAGE_NAME = "DownloadedFormsPackage_525101667060900.zip";

	static final String LIST_FAILURE_RESPONSE = """
			<crx version="1.22.17" user="admin" workspace="crx.default">
				<request>
					<param name="cmd" value="ls"/>
			    </request>
				<response>
					<data>
						<packages>
						</packages>
					</data>
					<status code="500">Internal Server Error</status>
				</response>
			</crx>
			""";

	private PackageManagerClient underTest;

	@BeforeEach
	void setup(WireMockRuntimeInfo wmRuntimeInfo) {
		 underTest = PackageManagerClient.builder().port(wmRuntimeInfo.getHttpPort()).build();
	}

	@Test
	void testListPackages_Success() throws Exception {
		stubForListPackagesSuccess();
		assertEquals(322, underTest.listPackages().packages().size());
	}

	@Test
	void testListPackages_Failure() throws Exception {
		stubForListPackagesFailure();
		ListResponse result = underTest.listPackages();
		assertAll(
				()->assertEquals(500, result.status().code()),
				()->assertEquals("Internal Server Error", result.status().text()),
				()->assertEquals(0, result.packages().size()),
				()->assertEquals("cmd", result.request().name()),
				()->assertEquals("ls", result.request().value())
				);
	}

	@Test
	void testListPackages_Failure_404() throws Exception {
		stubForListPackagesFailure404();
		PackageManagerException ex = assertThrows(PackageManagerException.class, ()->underTest.listPackages());
		// Should contain that error was returned, operation name and text from the response.
		assertThat(ex, ExceptionMatchers.exceptionMsgContainsAll("Error while listing packages"));
	}

	@Test
	void testUninstallPackage_Success() throws Exception {
		stubForUninstallPackageSuccess();
		CommandResponse result = underTest.uninstallPackage(GROUP, PACKAGE_NAME);
		assertAll(
				()->assertTrue(result.success()),
				()->assertEquals("Package uninstalled", result.msg()),
				()->assertTrue(result.path().isEmpty())
				);
	}

	@Test
	void testUninstallPackage_Failure() throws Exception {
		stubForUninstallPackageFailure();
		CommandResponse result = underTest.uninstallPackage(GROUP, PACKAGE_NAME);
		assertAll(
				()->assertFalse(result.success()),
				()->assertEquals("no package", result.msg()),
				()->assertTrue(result.path().isEmpty())
				);
	}

	@Test
	void testInstallPackage_Success() throws Exception {
		stubForInstallPackageSuccess();
		CommandResponse result = underTest.installPackage(GROUP, PACKAGE_NAME);
		assertAll(
				()->assertTrue(result.success()),
				()->assertEquals("Package installed", result.msg()),
				()->assertTrue(result.path().isEmpty())
				);
	}

	@Test
	void testInstallPackage_Failure() throws Exception {
		stubForInstallPackageFailure();
		CommandResponse result = underTest.installPackage(GROUP, PACKAGE_NAME);
		assertAll(
				()->assertFalse(result.success()),
				()->assertEquals("failure", result.msg()),
				()->assertTrue(result.path().isEmpty())
				);
	}

	@Test
	void testDeletePackage_Success() {
		stubForDeletePackageSuccess();
		CommandResponse result = underTest.deletePackage(GROUP, PACKAGE_NAME);
		assertAll(
				()->assertTrue(result.success()),
				()->assertEquals("Package deleted", result.msg()),
				()->assertTrue(result.path().isEmpty())
				);
	}

	@Test
	void testDeletePackage_Failure() {
		stubForDeletePackageFailure();
		CommandResponse result = underTest.deletePackage(GROUP, PACKAGE_NAME);
		assertAll(
				()->assertFalse(result.success()),
				()->assertEquals("failure", result.msg()),
				()->assertTrue(result.path().isEmpty())
				);
	}

	@Test
	void testUpoadPackage_Failure() throws Exception {
		stubForUploadPackageFailure();
		CommandResponse result = underTest.uploadPackage(PACKAGE_NAME, SAMPLE_DATA_DIR.resolve(SAMPLE_PACKAGE_FILENAME));
		assertAll(
				()->assertFalse(result.success()),
				()->assertEquals("failure", result.msg()),
				()->assertTrue(result.path().isEmpty())
				);
	}

	@Test
	void testUpoadPackage_Success() throws Exception {
		stubForUploadPackageSuccess();
		CommandResponse result = underTest.uploadPackage(PACKAGE_NAME, SAMPLE_DATA_DIR.resolve(SAMPLE_PACKAGE_FILENAME));
		assertAll(
				()->assertTrue(result.success()),
				()->assertEquals("Package uploaded", result.msg()),
				()->assertEquals("/etc/packages/fd/export/DownloadedFormsPackage_525101667060900.zip", result.path().orElseThrow())
				);
	}

	static void stubForListPackagesSuccess() throws IOException {
		stubFor(get(urlPathEqualTo("/crx/packmgr/service.jsp"))
					.withQueryParam("cmd", equalTo("ls"))
				.willReturn(
						okForContentType(ContentType.TEXT_PLAIN.contentType(), Files.readString(SAMPLE_DATA_DIR.resolve("SampleListResponse.xml")))
						));
	}

	static void stubForListPackagesFailure() throws IOException {
		stubFor(get(urlPathEqualTo("/crx/packmgr/service.jsp"))
					.withQueryParam("cmd", equalTo("ls"))
				.willReturn(
						okForContentType(ContentType.TEXT_PLAIN.contentType(), LIST_FAILURE_RESPONSE)
						));
	}

	static void stubForListPackagesFailure404() throws IOException {
		stubFor(get(urlPathEqualTo("/crx/packmgr/service.jsp"))
					.withQueryParam("cmd", equalTo("ls"))
				.willReturn(
						notFound()
						));
	}

	static void stubForUninstallPackageSuccess() {
		stubFor(post(urlPathEqualTo("/crx/packmgr/service/.json/etc/packages/" + GROUP + "/" + PACKAGE_NAME))
				.withMultipartRequestBody(aMultipart("cmd").withBody(equalTo("uninstall")))
				.willReturn(okForContentType(ContentType.APPLICATION_JSON.contentType(), "{\"success\":true,\"msg\":\"Package uninstalled\"}")));
	}
	
	static void stubForUninstallPackageFailure() {
		stubFor(post(urlPathEqualTo("/crx/packmgr/service/.json/etc/packages/" + GROUP + "/" + PACKAGE_NAME))
				.withMultipartRequestBody(aMultipart("cmd").withBody(equalTo("uninstall")))
				.willReturn(okForContentType(ContentType.APPLICATION_JSON.contentType(), "{\"success\":false,\"msg\":\"no package\"}")));
	}
	
	static void stubForInstallPackageSuccess() {
		stubFor(post(urlPathEqualTo("/crx/packmgr/service/.json/etc/packages/" + GROUP + "/" + PACKAGE_NAME))
				.withMultipartRequestBody(aMultipart("cmd").withBody(equalTo("install")))
				.willReturn(okForContentType(ContentType.APPLICATION_JSON.contentType(), "{\"success\":true,\"msg\":\"Package installed\"}")));
	}

	static void stubForInstallPackageFailure() {
		stubFor(post(urlPathEqualTo("/crx/packmgr/service/.json/etc/packages/" + GROUP + "/" + PACKAGE_NAME))
				.withMultipartRequestBody(aMultipart("cmd").withBody(equalTo("install")))
				.willReturn(okForContentType(ContentType.APPLICATION_JSON.contentType(), "{\"success\":false,\"msg\":\"failure\"}")));
	}

	static void stubForDeletePackageSuccess() {
		stubFor(post(urlPathEqualTo("/crx/packmgr/service/.json/etc/packages/" + GROUP + "/" + PACKAGE_NAME))
				.withMultipartRequestBody(aMultipart("cmd").withBody(equalTo("delete")))
				.willReturn(okForContentType(ContentType.APPLICATION_JSON.contentType(), "{\"success\":true,\"msg\":\"Package deleted\"}")));
	}

	static void stubForDeletePackageFailure() {
		stubFor(post(urlPathEqualTo("/crx/packmgr/service/.json/etc/packages/" + GROUP + "/" + PACKAGE_NAME))
				.withMultipartRequestBody(aMultipart("cmd").withBody(equalTo("delete")))
				.willReturn(okForContentType(ContentType.APPLICATION_JSON.contentType(), "{\"success\":false,\"msg\":\"failure\"}")));
	}

	static void stubForUploadPackageSuccess() {
		stubFor(post(urlPathEqualTo("/crx/packmgr/service/.json"))
				.withMultipartRequestBody(aMultipart("cmd").withBody(equalTo("upload")))
				.withMultipartRequestBody(aMultipart("force").withBody(equalTo("true")))
				.withMultipartRequestBody(aMultipart("package").withName(SAMPLE_PACKAGE_FILENAME))
				.willReturn(okForContentType(ContentType.APPLICATION_JSON.contentType(), "{\"success\":true,\"msg\":\"Package uploaded\",\"path\":\"/etc/packages/fd/export/DownloadedFormsPackage_525101667060900.zip\"}")));
	}

	static void stubForUploadPackageFailure() {
		stubFor(post(urlPathEqualTo("/crx/packmgr/service/.json"))
				.withMultipartRequestBody(aMultipart("cmd").withBody(equalTo("upload")))
				.withMultipartRequestBody(aMultipart("force").withBody(equalTo("true")))
				.withMultipartRequestBody(aMultipart("package").withName(SAMPLE_PACKAGE_FILENAME))
				.willReturn(okForContentType(ContentType.APPLICATION_JSON.contentType(), "{\"success\":false,\"msg\":\"failure\"}")));
	}
	
}
