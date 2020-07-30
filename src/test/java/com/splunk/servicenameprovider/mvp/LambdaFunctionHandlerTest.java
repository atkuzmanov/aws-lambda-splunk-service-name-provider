package com.splunk.servicenameprovider.mvp;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.jupiter.api.BeforeEach;

public class LambdaFunctionHandlerTest {

	private static final File UPDE_HEALTH_GOOD = new File("src/test/resources/example.json");
	private static TestMockServer ts;

	@BeforeClass
	public static void createInput() throws IOException {
		startMockServer();
	}

	@AfterClass
	public static void cleanUp() {
		stopMockServer();
	}

	@BeforeEach
	public void init() {
	}

	public static void startMockServer() {
		ts = new TestMockServer();
		ts.startServer();
	}

	public static void stopMockServer() {
		ts.stopServer();
	}

	@Test
	public void testLambdaFunctionHandler() throws IOException {
		FileReader fr = new FileReader(UPDE_HEALTH_GOOD);
		BufferedReader reader = new BufferedReader(fr);
		String fileContents = reader.lines().collect(Collectors.joining(System.lineSeparator()));

		ts.createMockServerExpectation1(fileContents);
		fr.close();

		List<String> expected = new ArrayList<String>(Arrays.asList("annotations-rw-neo4j", "api-policy-component", "body-validation-service", "cms-notifier"));
		List<String> result = LambdaFunctionHandler.provideServiceNameList("http://127.0.0.1:1080/test1");

		Assert.assertEquals(expected, result);
	}
}
