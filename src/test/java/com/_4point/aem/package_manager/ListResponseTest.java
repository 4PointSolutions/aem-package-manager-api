package com._4point.aem.package_manager;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.commons.lang3.exception.UncheckedException;
import org.junit.jupiter.api.Test;

import com._4point.aem.package_manager.ListResponse.Package;

class ListResponseTest {
	private static final Path RESOURCES_DIR = Path.of("src", "test", "resources");
	private static final Path SAMPLE_DATA_DIR = RESOURCES_DIR.resolve("SampleData");
	private static final Path SAMPLE_XML_FILE = SAMPLE_DATA_DIR.resolve("SampleListResponse.xml");
	private static final byte[] SAMPLE_XML_BYTES = readAllBytes(SAMPLE_XML_FILE);
	
	private static byte[] readAllBytes(Path sampleXmlFile) {
		try {
			return Files.readAllBytes(sampleXmlFile);
		} catch (IOException e) {
			throw new UncheckedException(e);
		}
	}

	private final ListResponse underTest = ListResponse.from(XmlDocument.initializeXmlDoc(SAMPLE_XML_BYTES));
	
	@Test
	void testFrom() {
		assertAll(
				()->assertEquals(200, underTest.status().code()),
				()->assertEquals("ok", underTest.status().text()),
				()->assertEquals("cmd", underTest.request().name()),
				()->assertEquals("ls", underTest.request().value()),
				()->assertEquals(322, underTest.packages().size())
				);
		
		Package aPackage = underTest.packages().get(0);
		assertAll(
				()->assertEquals("adobe/aem6", aPackage.group()),
				()->assertEquals("we.retail.config", aPackage.name()),
				()->assertEquals("4.0.0", aPackage.version()),
				()->assertEquals("we.retail.config-4.0.0.zip", aPackage.downloadName()),
				()->assertEquals("15614", aPackage.size()),
				()->assertEquals("Mon., 25 Feb. 2019 15:45:19 -0500", aPackage.created()),
				()->assertEquals("bailescu", aPackage.createdBy()),
				()->assertEquals("", aPackage.lastModified()),
				()->assertEquals("null", aPackage.lastModifiedBy()),
				()->assertEquals("Fri., 8 Dec. 2023 10:28:02 -0500", aPackage.lastUnpacked()),
				()->assertEquals("admin", aPackage.lastUnpackedBy())
				);
	}

}
