package com._4point.aem.package_manager;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com._4point.aem.package_manager.rest_client.RestClient.ContentType;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;

@WireMockTest
class PackageManagerClientTest {
	private static final String SAMPLE_PACKAGE_FILENAME = "sample00002test.zip";
	private static final Path RESOURCES_DIR = Path.of("src", "test", "resources");
	private static final Path SAMPLE_DATA_DIR = RESOURCES_DIR.resolve("SampleData");

	private static final String GROUP = "fd/export";
	private static final String PACKAGE_NAME = "DownloadedFormsPackage_525101667060900.zip";

	private PackageManagerClient underTest;

	@BeforeEach
	void setup(WireMockRuntimeInfo wmRuntimeInfo) {
		 underTest = PackageManagerClient.builder().port(wmRuntimeInfo.getHttpPort()).build();
	}

	@Test
	void testListPackages() throws Exception {
		stubForListPackagesSuccess();
		assertEquals(322, underTest.listPackages().packages().size());
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
	void testUpoadPackage_Success() throws Exception {
		stubForUploadPackageSuccess();
		CommandResponse result = underTest.uploadPackage(PACKAGE_NAME, SAMPLE_DATA_DIR.resolve(SAMPLE_PACKAGE_FILENAME));
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
				()->assertTrue(result.success()),
				()->assertEquals("Package uploaded", result.msg()),
				()->assertEquals("/etc/packages/fd/export/DownloadedFormsPackage_525101667060900.zip", result.path().orElseThrow())
				);
	}

	public static void stubForListPackagesSuccess() throws IOException {
		stubFor(get(urlPathEqualTo("/crx/packmgr/service.jsp"))
					.withQueryParam("cmd", equalTo("ls"))
				.willReturn(
						okForContentType(ContentType.TEXT_PLAIN.contentType(), Files.readString(SAMPLE_DATA_DIR.resolve("SampleListResponse.xml")))
						));
	}

	public static void stubForUninstallPackageSuccess() {
		stubFor(post(urlPathEqualTo("/crx/packmgr/service/.json/etc/packages/" + GROUP + "/" + PACKAGE_NAME))
				.withMultipartRequestBody(aMultipart("cmd").withBody(equalTo("uninstall")))
				.willReturn(okForContentType(ContentType.APPLICATION_JSON.contentType(), "{\"success\":true,\"msg\":\"Package uninstalled\"}")));
	}
	
	public static void stubForUninstallPackageFailure() {
		stubFor(post(urlPathEqualTo("/crx/packmgr/service/.json/etc/packages/" + GROUP + "/" + PACKAGE_NAME))
				.withMultipartRequestBody(aMultipart("cmd").withBody(equalTo("uninstall")))
				.willReturn(okForContentType(ContentType.APPLICATION_JSON.contentType(), "{\"success\":false,\"msg\":\"no package\"}")));
	}
	
	public static void stubForInstallPackageSuccess() {
		stubFor(post(urlPathEqualTo("/crx/packmgr/service/.json/etc/packages/" + GROUP + "/" + PACKAGE_NAME))
				.withMultipartRequestBody(aMultipart("cmd").withBody(equalTo("install")))
				.willReturn(okForContentType(ContentType.APPLICATION_JSON.contentType(), "{\"success\":true,\"msg\":\"Package installed\"}")));
	}

	public static void stubForInstallPackageFailure() {
		stubFor(post(urlPathEqualTo("/crx/packmgr/service/.json/etc/packages/" + GROUP + "/" + PACKAGE_NAME))
				.withMultipartRequestBody(aMultipart("cmd").withBody(equalTo("install")))
				.willReturn(okForContentType(ContentType.APPLICATION_JSON.contentType(), "{\"success\":false,\"msg\":\"failure\"}")));
	}

	public static void stubForDeletePackageSuccess() {
		stubFor(post(urlPathEqualTo("/crx/packmgr/service/.json/etc/packages/" + GROUP + "/" + PACKAGE_NAME))
				.withMultipartRequestBody(aMultipart("cmd").withBody(equalTo("delete")))
				.willReturn(okForContentType(ContentType.APPLICATION_JSON.contentType(), "{\"success\":true,\"msg\":\"Package deleted\"}")));
	}

	public static void stubForDeletePackageFailure() {
		stubFor(post(urlPathEqualTo("/crx/packmgr/service/.json/etc/packages/" + GROUP + "/" + PACKAGE_NAME))
				.withMultipartRequestBody(aMultipart("cmd").withBody(equalTo("delete")))
				.willReturn(okForContentType(ContentType.APPLICATION_JSON.contentType(), "{\"success\":false,\"msg\":\"failure\"}")));
	}

	public static void stubForUploadPackageSuccess() {
		stubFor(post(urlPathEqualTo("/crx/packmgr/service/.json"))
				.withMultipartRequestBody(aMultipart("cmd").withBody(equalTo("upload")))
				.withMultipartRequestBody(aMultipart("force").withBody(equalTo("true")))
				.withMultipartRequestBody(aMultipart("package").withName(SAMPLE_PACKAGE_FILENAME))
				.willReturn(okForContentType(ContentType.APPLICATION_JSON.contentType(), "{\"success\":false,\"msg\":\"failure\"}")));
	}

	public static void stubForUploadPackageFailure() {
		stubFor(post(urlPathEqualTo("/crx/packmgr/service/.json"))
				.withMultipartRequestBody(aMultipart("cmd").withBody(equalTo("upload")))
				.withMultipartRequestBody(aMultipart("force").withBody(equalTo("true")))
				.withMultipartRequestBody(aMultipart("package").withName(SAMPLE_PACKAGE_FILENAME))
				.willReturn(okForContentType(ContentType.APPLICATION_JSON.contentType(), "{\"success\":true,\"msg\":\"Package uploaded\",\"path\":\"/etc/packages/fd/export/DownloadedFormsPackage_525101667060900.zip\"}")));
	}
}
