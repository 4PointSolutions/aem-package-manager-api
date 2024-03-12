package com._4point.aem.package_manager;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class CommandResponseTest {

	@Test
	void testFromSuccess() {
		var underTest = CommandResponse.from(JsonData.from("{\"success\":true,\"msg\":\"Package uninstalled\"}"));
		assertAll(
				()->assertTrue(underTest.success()),
				()->assertEquals("Package uninstalled", underTest.msg()),
				()->assertTrue(underTest.path().isEmpty())
				);
	}

	@Test
	void testFromFailure() {
		var underTest = CommandResponse.from(JsonData.from("{\"success\":false,\"msg\":\"no package\"}"));
		assertAll(
				()->assertFalse(underTest.success()),
				()->assertEquals("no package", underTest.msg()),
				()->assertTrue(underTest.path().isEmpty())
				);
		
	}

	@Test
	void testFromUploadSuccess() {
		var underTest = CommandResponse.from(JsonData.from("{\"success\":true,\"msg\":\"Package uploaded\",\"path\":\"/etc/packages/fd/export/DownloadedFormsPackage_525101667060900.zip\"}"));
		assertAll(
				()->assertTrue(underTest.success()),
				()->assertEquals("Package uploaded", underTest.msg()),
				()->assertEquals("/etc/packages/fd/export/DownloadedFormsPackage_525101667060900.zip", underTest.path().orElseThrow())
				);
	}

}
