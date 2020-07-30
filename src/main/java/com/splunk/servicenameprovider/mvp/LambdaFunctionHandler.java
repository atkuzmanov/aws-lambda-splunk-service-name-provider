package com.splunk.servicenameprovider.mvp;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONObject;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;

public class LambdaFunctionHandler implements RequestHandler<Object, String> {

	private static final String EXAMPLE_EU_AZ_URL = "https://example-eu.com/__health";
	private static final String EXAMPLE_US_AZ_URL = "https://example-us.com/__health";

	@Override
	public String handleRequest(Object input, Context context) {
		List<String> urls = new ArrayList<String>(Arrays.asList(EXAMPLE_EU_AZ_URL, EXAMPLE_US_AZ_URL));
		Set<String> uniqueServiceNames = new HashSet<String>();
		JSONArray serviceNamesJSON = new JSONArray();

		try {
			for (String u : urls) {
				provideServiceNameList(u).stream().forEach(s -> uniqueServiceNames.add(s));
			}
			serviceNamesJSON = new JSONArray(uniqueServiceNames);
		} catch (IOException e) {
			e.printStackTrace();
			context.getLogger().log("Error: " + e.getStackTrace() + "\n");
		}
		return serviceNamesJSON.toString();
	}

	protected static List<String> provideServiceNameList(String urlString) throws IOException {
		URL url;
		List<String> serviceNamesList = new ArrayList<String>();
		try {
			url = new URL(urlString);

			HttpURLConnection con;
			con = (HttpURLConnection) url.openConnection();
			con.setRequestMethod("GET");
			con.setRequestProperty("Accept", "application/json");
			con.setConnectTimeout(5000);
			con.setReadTimeout(5000);
			con.setInstanceFollowRedirects(true);

			int status = con.getResponseCode();
			if (status == HttpURLConnection.HTTP_MOVED_TEMP || status == HttpURLConnection.HTTP_MOVED_PERM) {
				String location = con.getHeaderField("Location");
				URL newUrl = new URL(location);
				con = (HttpURLConnection) newUrl.openConnection();
				status = con.getResponseCode();
			}

			Reader streamReader = null;
			if (status > 299) {
				streamReader = new InputStreamReader(con.getErrorStream());
			} else {
				streamReader = new InputStreamReader(con.getInputStream());
			}

			BufferedReader in = new BufferedReader(streamReader);

			String inputLine = "";
			StringBuffer content = new StringBuffer();
			while ((inputLine = in.readLine()) != null) {
				content.append(inputLine);
			}

			in.close();
			con.disconnect();

			serviceNamesList = extractServiceNames(content.toString());
		} catch (IOException e) {
			e.printStackTrace();
			throw e;
		}

		return serviceNamesList;
	}

	private static List<String> extractServiceNames(String responseBody) {
		List<String> serviceNames = new ArrayList<String>();
		JSONObject obj = new JSONObject(responseBody);
		JSONArray arr = obj.getJSONArray("checks");

		for (int i = 0; i < arr.length(); i++) {
			String serviceName = arr.getJSONObject(i).getString("name");
			serviceNames.add(serviceName);
		}

		return serviceNames;
	}
}
