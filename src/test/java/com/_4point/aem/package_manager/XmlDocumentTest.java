package com._4point.aem.package_manager;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.commons.lang3.exception.UncheckedException;
import org.junit.jupiter.api.Test;


class XmlDocumentTest {
	private static final int EXPECTED_NUM_PACKAGES = 322;
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

	private XmlDocument underTest = XmlDocument.initializeXmlDoc(SAMPLE_XML_BYTES);
	
	@Test
	void testGetString_Attribute() {
		assertEquals("cmd", underTest.getString("/crx/request/param/@name").orElseThrow());
	}

	@Test
	void testGetString_Element() {
		assertEquals("ok", underTest.getString("/crx/response/status").orElseThrow());
	}

	@Test
	void testGetString_NoResult() {
		assertTrue(underTest.getString("/crx/response/foobar").isEmpty());
	}

	@Test
	void testGetStrings_Attribute() {
		assertEquals("cmd", underTest.getStrings("/crx/request/param/@name").get(0));
	}

	@Test
	void testGetStrings_Element() {
		assertEquals(EXPECTED_NUM_PACKAGES, underTest.getStrings("/crx/response/data/packages/package/group").size());
	}

	@Test
	void testGetStrings_NoResult() {
		assertTrue(underTest.getStrings("/crx/response/foobar").isEmpty());
	}

	@Test
	void testGetDoc_OneResult() {
		XmlDocument xmlDocument = underTest.getDoc("/crx/response/data/packages/package[1]").orElseThrow();
		assertEquals("adobe/aem6", xmlDocument.getString("/package/group").orElseThrow());
	}

	@Test
	void testGetDocs_OneResult() {
		XmlDocument xmlDocument = underTest.getDocs("/crx/response/data/packages/package[1]").get(0);
		assertEquals("adobe/aem6", xmlDocument.getString("/package/group").orElseThrow());
	}

	@Test
	void testGetDocs_NoResult() {
		assertTrue(underTest.getDocs("/crx/response/data/packages/package[500]").isEmpty());
	}

	@Test
	void testGetDocs() {
		assertEquals(EXPECTED_NUM_PACKAGES, underTest.getDocs("/crx/response/data/packages/package").size());
	}


}
