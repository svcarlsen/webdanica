package dk.kb.webdanica.criteria;

import static org.junit.Assert.*;
import static dk.kb.webdanica.utils.UnitTestUtils.getTestResourceFileAsString;

import java.io.IOException;
import java.util.List;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class C4Tester {

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void test() throws IOException {
		//String sampleLangTextFilePath = getTestResourceFileAsString("language-samples/sample-text.txt");
		String sampleLangTextFilePath = getTestResourceFileAsString("language-samples/sample-text.da");
		List<String> results = C4.computeNewC4(sampleLangTextFilePath);
		String c4a = results.get(0);
		String c4b = results.get(1);
		System.out.println(c4a);
		System.out.println(c4b);
	}

}