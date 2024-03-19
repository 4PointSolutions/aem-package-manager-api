package com._4point.aem.package_manager;

import static com._4point.aem.package_manager.PackageManagerClientTest.*;
import static org.hamcrest.MatcherAssert.assertThat; 
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com._4point.aem.package_manager.PackageManagerClient.PackageManagerException;
import com._4point.testing.matchers.javalang.ExceptionMatchers;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;

@WireMockTest
class PackageManagerClientExTest {

	private PackageManagerClientEx underTest;

	@BeforeEach
	void setup(WireMockRuntimeInfo wmRuntimeInfo) {
		 underTest = PackageManagerClient.builder().port(wmRuntimeInfo.getHttpPort()).buildEx();
	}

	@Test
	void testListPackages_Success() throws Exception {
		stubForListPackagesSuccess();
		assertEquals(322, underTest.listPackages().size());
	}

	@Test
	void testListPackages_Failure() throws Exception {
		stubForListPackagesFailure();
		PackageManagerException ex = assertThrows(PackageManagerException.class, ()->underTest.listPackages().size());
		// Should contain that error was returned, operation name and text from the response.
		assertThat(ex, ExceptionMatchers.exceptionMsgContainsAll("Error returned", "List Packages", "Internal Server Error"));
	}

	@Test
	void testListPackages_Failure404() throws Exception {
		stubForListPackagesFailure404();
		PackageManagerException ex = assertThrows(PackageManagerException.class, ()->underTest.listPackages().size());
		// Should contain that error was returned, operation name and text from the response.
		assertThat(ex, ExceptionMatchers.exceptionMsgContainsAll("Error while listing packages"));
	}

	@Test
	void testUploadPackage_Success() throws Exception {
		stubForUploadPackageSuccess();
		String result = underTest.uploadPackage(PACKAGE_NAME, SAMPLE_DATA_DIR.resolve(SAMPLE_PACKAGE_FILENAME));
		assertEquals("/etc/packages/fd/export/DownloadedFormsPackage_525101667060900.zip", result);
	}

	@Test
	void testUploadPackage_Failure() throws Exception {
		stubForUploadPackageFailure();
		PackageManagerException ex = assertThrows(PackageManagerException.class, ()->underTest.uploadPackage(PACKAGE_NAME, SAMPLE_DATA_DIR.resolve(SAMPLE_PACKAGE_FILENAME)));
		// Should contain that error was returned, operation name and text from the response.
		assertThat(ex, ExceptionMatchers.exceptionMsgContainsAll("Error returned", "Upload Package", "failure"));
	}

	@Test
	void testInstallPackage_Success() throws Exception {
		stubForInstallPackageSuccess();
		underTest.installPackage(GROUP, PACKAGE_NAME);
	}

	@Test
	void testInstallPackage_Failure() throws Exception {
		stubForInstallPackageFailure();
		PackageManagerException ex = assertThrows(PackageManagerException.class, ()->underTest.installPackage(GROUP, PACKAGE_NAME));
		// Should contain that error was returned, operation name and text from the response.
		assertThat(ex, ExceptionMatchers.exceptionMsgContainsAll("Error returned", "Install Package", "failure"));
	}

	@Test
	void testUninstallPackage_Success() throws Exception {
		stubForUninstallPackageSuccess();
		underTest.uninstallPackage(GROUP, PACKAGE_NAME);
	}

	@Test
	void testUninstallPackage_Failure() throws Exception {
		stubForUninstallPackageFailure();
		PackageManagerException ex = assertThrows(PackageManagerException.class, ()->underTest.uninstallPackage(GROUP, PACKAGE_NAME));
		// Should contain that error was returned, operation name and text from the response.
		assertThat(ex, ExceptionMatchers.exceptionMsgContainsAll("Error returned", "Uninstall Package", "no package"));
	}

	@Test
	void testDeletePackage_Success() throws Exception {
		stubForDeletePackageSuccess();
		underTest.deletePackage(GROUP, PACKAGE_NAME);
	}

	@Test
	void testDeletePackage_Failure() throws Exception {
		stubForDeletePackageFailure();
		PackageManagerException ex = assertThrows(PackageManagerException.class, ()->underTest.deletePackage(GROUP, PACKAGE_NAME));
		// Should contain that error was returned, operation name and text from the response.
		assertThat(ex, ExceptionMatchers.exceptionMsgContainsAll("Error returned", "Delete Package", "failure"));
	}

	@Test
	void testDeletePackages_Success() throws Exception {
		stubForListPackagesSuccess();
		stubForUninstallPackageSuccess();
		stubForDeletePackageSuccess();
		underTest.deletePackages(pkg->GROUP.equals(pkg.group()), true);
	}

	@Test
	void testDeletePackages_FailureInList() throws Exception {
		stubForListPackagesFailure();
		PackageManagerException ex = assertThrows(PackageManagerException.class, ()->underTest.deletePackages(pkg->GROUP.equals(pkg.group()), true));
		// Should contain that error was returned, operation name and text from the response.
		assertThat(ex, ExceptionMatchers.exceptionMsgContainsAll("Error returned", "List Packages", "Internal Server Error"));
	}

	@Test
	void testDeletePackages_FailureInUninstall() throws Exception {
		stubForListPackagesSuccess();
		stubForUninstallPackageFailure();
		PackageManagerException ex = assertThrows(PackageManagerException.class, ()->underTest.deletePackages(pkg->GROUP.equals(pkg.group()), true));
		// Should contain that error was returned, operation name and text from the response.
		assertThat(ex, ExceptionMatchers.exceptionMsgContainsAll("Error returned", "Uninstall Package", "no package"));
	}

	@Test
	void testDeletePackages_FailureInDelete() throws Exception {
		stubForListPackagesSuccess();
		stubForUninstallPackageSuccess();
		stubForDeletePackageFailure();
		PackageManagerException ex = assertThrows(PackageManagerException.class, ()->underTest.deletePackages(pkg->GROUP.equals(pkg.group()), true));
		// Should contain that error was returned, operation name and text from the response.
		assertThat(ex, ExceptionMatchers.exceptionMsgContainsAll("Error returned", "Delete Package", "failure"));
	}
}
