package com._4point.aem.package_manager;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Path;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

@Disabled("This is an Integration test.  It assumes that AEM is running on localhost:4502 with default credentials.")
@TestMethodOrder(OrderAnnotation.class)
class PackageManagerClientIT {
	private static final Path RESOURCES_DIR = Path.of("src", "test", "resources");
	private static final Path SAMPLE_DATA_DIR = RESOURCES_DIR.resolve("SampleData");

	private final PackageManagerClient underTest = PackageManagerClient.builder().build();

	@Test
	@Order(1)
	void testListPackages() {
		assertTrue(underTest.listPackages().packages().size() > 0);
	}

	@Test
	@Order(2)
	void testUpoadPackage() {
		CommandResponse uploadPackage = underTest.uploadPackage("DownloadedFormsPackage_525101667060900.zip", SAMPLE_DATA_DIR.resolve("sample00002test.zip"));
		boolean success = uploadPackage.success();
		if (!success) {
			System.out.println(uploadPackage.msg());
		}
		assertTrue(success);
	}
	
	@Test
	@Order(3)
	void testUninstallPackage() {
		assertTrue(underTest.uninstallPackage("fd/export", "DownloadedFormsPackage_525101667060900.zip").success());
		assertTrue(underTest.installPackage("fd/export", "DownloadedFormsPackage_525101667060900.zip").success());
	}
	
	@Test
	@Order(4)
	void testDeletePackage() {
		assertTrue(underTest.uninstallPackage("fd/export", "DownloadedFormsPackage_525101667060900.zip").success());
		assertTrue(underTest.deletePackage("fd/export", "DownloadedFormsPackage_525101667060900.zip").success());
	}
}
