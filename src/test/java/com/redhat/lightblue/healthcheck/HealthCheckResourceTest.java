package com.redhat.lightblue.healthcheck;

import static com.redhat.lightblue.util.test.AbstractJsonNodeTest.loadJsonNode;
import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.URL;
import java.nio.charset.Charset;

import org.apache.commons.io.IOUtils;
import org.jboss.resteasy.plugins.server.sun.http.HttpContextBuilder;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.redhat.lightblue.client.http.HttpMethod;
import com.redhat.lightblue.client.integration.test.LightblueExternalResource;
import com.redhat.lightblue.client.integration.test.LightblueExternalResource.LightblueTestMethods;
import com.sun.net.httpserver.HttpServer;

public class HealthCheckResourceTest {

    private final static int DEFAULT_PORT = 7000;

    private static HttpServer httpServer;

    @ClassRule
    public static LightblueExternalResource lightblue = new LightblueExternalResource(new LightblueTestMethods() {

        @Override
        public JsonNode[] getMetadataJsonNodes() throws Exception {
            return new JsonNode[]{
                    loadJsonNode("metadata/test.json")
            };
        }
    });

    @BeforeClass
    public static void startHttpServer() throws Exception {
        httpServer = HttpServer.create(new InetSocketAddress(DEFAULT_PORT), 0);

        HttpContextBuilder dataContext = new HttpContextBuilder();
        dataContext.getDeployment().getActualResourceClasses().add(HealthCheckResource.class);
        dataContext.setPath("/rest/test");
        dataContext.bind(httpServer);

        httpServer.start();
    }

    @AfterClass
    public static void stopHttpServer() {
        if (httpServer != null) {
            httpServer.stop(0);
        }
        httpServer = null;
    }

    public HealthCheckResourceTest() {
        //called here so that the internal controller is initialized.
        lightblue.getLightblueClient();
    }

    private HttpURLConnection openConnection(String uri) throws IOException {
        URL url = new URL(uri);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();

        connection.setRequestMethod(HttpMethod.GET.name());
        connection.setRequestProperty("Accept", "application/json");
        connection.setRequestProperty("Accept-Charset", "utf-8");

        return connection;
    }

    private String response(HttpURLConnection connection) throws IOException {
        try (InputStream responseStream = connection.getInputStream()) {
            return readResponseStream(responseStream, connection);
        }
    }

    private String readResponseStream(InputStream responseStream, HttpURLConnection connection) throws IOException {
        int contentLength = connection.getContentLength();

        if (contentLength == 0) {
            return "";
        }

        if (contentLength > 0) {
            byte[] responseBytes = new byte[contentLength];
            IOUtils.readFully(responseStream, responseBytes);
            return new String(responseBytes, Charset.forName("UTF-8"));
        }

        return IOUtils.toString(responseStream, Charset.forName("UTF-8"));
    }

    @Test
    public void testCheck() throws IOException {
        HttpURLConnection connection = openConnection("http://localhost:" + DEFAULT_PORT + "/rest/test/healthcheck/check");
        String r = response(connection);
        connection.disconnect();

        assertEquals("hi", r);
    }

}
