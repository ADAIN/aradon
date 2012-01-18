package net.ion.nradon.handler;

import static net.ion.nradon.WebServers.createWebServer;
import static net.ion.nradon.testutil.HttpClient.contents;
import static net.ion.nradon.testutil.HttpClient.httpGet;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.io.IOException;
import java.net.URLConnection;

import net.ion.nradon.WebServer;

import org.junit.Test;

public class TestServerHeaderHandler {

    @Test
    public void setsHttpServerHeader() throws IOException, InterruptedException {
        WebServer webServer = createWebServer(59504)
                .add(new ServerHeaderHandler("My Server"))
                .add(new StringHttpHandler("text/plain", "body"))
                .start();
        try {
            URLConnection urlConnection = httpGet(webServer, "/");
            assertEquals("My Server", urlConnection.getHeaderField("Server"));
            assertEquals("body", contents(urlConnection));
        } finally {
            webServer.stop().join();
        }
    }

    @Test
    public void canBeOverriddenByOtherHandlers() throws IOException, InterruptedException {
        WebServer webServer = createWebServer(59504)
                .add(new ServerHeaderHandler("My Server"))
                .add(new ServerHeaderHandler("No actually, this is My Server"))
                .add(new StringHttpHandler("text/plain", "body"))
                .start();
        try {
            URLConnection urlConnection = httpGet(webServer, "/");
            assertEquals("No actually, this is My Server", urlConnection.getHeaderField("Server"));
            assertEquals("body", contents(urlConnection));
        } finally {
            webServer.stop().join();
        }
    }

    @Test
    public void canBeClearedByOtherHandlers() throws IOException, InterruptedException {
        WebServer webServer = createWebServer(59504)
                .add(new ServerHeaderHandler("My Server"))
                .add(new ServerHeaderHandler(null))
                .add(new StringHttpHandler("text/plain", "body"))
                .start();
        try {
            URLConnection urlConnection = httpGet(webServer, "/");
            assertFalse(urlConnection.getHeaderFields().containsKey("Server"));
            assertEquals("body", contents(urlConnection));
        } finally {
            webServer.stop().join();
        }
    }
}