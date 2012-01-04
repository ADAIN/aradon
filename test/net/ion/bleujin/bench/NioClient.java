package net.ion.bleujin.bench;

import org.restlet.Client;
import org.restlet.Context;
import org.restlet.data.Protocol;
import org.restlet.engine.ConnectorHelper;
import org.restlet.engine.Engine;
import org.restlet.engine.connector.HttpClientHelper;
import org.restlet.representation.Representation;
import org.restlet.resource.ClientResource;

public class NioClient {

	public static void main(String[] args) throws Exception {
		// TraceHandler.register();

		Client client = new Client(new Context(), Protocol.HTTP);
		ConnectorHelper<Client> helper;
		helper = new HttpClientHelper(client);
		// helper = new org.restlet.ext.httpclient.HttpClientHelper(null);
		// helper = new org.restlet.ext.net.HttpClientHelper(null);
		Engine.getInstance().getRegisteredClients().add(0, helper);

		client.getContext().getParameters().add("tracing", "false");
		client.getContext().getParameters().add("minThreads", "1");
		client.getContext().getParameters().add("lowThreads", "30");
		client.getContext().getParameters().add("maxThreads", "40");
		client.getContext().getParameters().add("maxQueued", "20");
		client.getContext().getParameters().add("directBuffers", "false");
		client.start();

		// String uri =
		// "http://www.restlet.org/downloads/2.1/restlet-jse-2.1snapshot.zip";
		String uri = "http://127.0.0.1:9999/";
		int iterations = 1000;
		ClientResource cr = new ClientResource(uri);
		cr.setRetryOnError(false);
		cr.setNext(client);
		Representation r = null;
		// ClientResource fr = new ClientResource("file://C/temp/report.txt");

		System.out.println("Calling resource: " + uri + " " + iterations + " times");
		long start = System.currentTimeMillis();

		for (int i = 0; i < iterations; i++) {
			// r = cr.post("Sample content posted");

			r = cr.get();
			r.exhaust();
			// System.out.println(cr.get().getText());
			//
			// System.out.println("Copying to the local file");
			// fr.put(r);
			// System.out.println("Copy done!");
		}

		long total = (System.currentTimeMillis() - start);
		long avg = total / iterations;
		System.out.println("Bench completed in " + total + " ms. Average time per call: " + avg + " ms");
	}
}
