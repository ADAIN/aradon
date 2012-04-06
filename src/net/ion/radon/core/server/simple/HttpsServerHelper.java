/**
 * Copyright 2005-2011 Noelios Technologies.
 * 
 * The contents of this file are subject to the terms of one of the following
 * open source licenses: LGPL 3.0 or LGPL 2.1 or CDDL 1.0 or EPL 1.0 (the
 * "Licenses"). You can select the license that you prefer but you may not use
 * this file except in compliance with one of these Licenses.
 * 
 * You can obtain a copy of the LGPL 3.0 license at
 * http://www.opensource.org/licenses/lgpl-3.0.html
 * 
 * You can obtain a copy of the LGPL 2.1 license at
 * http://www.opensource.org/licenses/lgpl-2.1.php
 * 
 * You can obtain a copy of the CDDL 1.0 license at
 * http://www.opensource.org/licenses/cddl1.php
 * 
 * You can obtain a copy of the EPL 1.0 license at
 * http://www.opensource.org/licenses/eclipse-1.0.php
 * 
 * See the Licenses for the specific language governing permissions and
 * limitations under the Licenses.
 * 
 * Alternatively, you can obtain a royalty free commercial license with less
 * limitations, transferable or non-transferable, directly at
 * http://www.noelios.com/products/restlet-engine
 * 
 * Restlet is a registered trademark of Noelios Technologies.
 */

package net.ion.radon.core.server.simple;

import java.net.InetAddress;
import java.net.InetSocketAddress;

import javax.net.ssl.SSLContext;

import org.restlet.Server;
import org.restlet.data.Protocol;
import net.ion.radon.core.server.simple.internal.SimpleContainer;
import net.ion.radon.core.server.simple.internal.SimpleServer;
import org.restlet.ext.ssl.DefaultSslContextFactory;
import org.restlet.ext.ssl.SslContextFactory;
import org.restlet.ext.ssl.internal.SslUtils;
import org.simpleframework.http.core.Container;
import org.simpleframework.http.core.ContainerServer;
import org.simpleframework.transport.connect.Connection;
import org.simpleframework.transport.connect.SocketConnection;

/**
 * Simple HTTP server connector. Here is the list of additional parameters that are supported. They should be set in the Server's context before it is started:
 * <table>
 * <tr>
 * <th>Parameter name</th>
 * <th>Value type</th>
 * <th>Default value</th>
 * <th>Description</th>
 * </tr>
 * <tr>
 * <td>sslContextFactory</td>
 * <td>String</td>
 * <td>org.restlet.ext.ssl.DefaultSslContextFactory</td>
 * <td>Let you specify a {@link SslContextFactory} qualified class name as a parameter, or an instance as an attribute for a more complete and flexible SSL context setting.</td>
 * </tr>
 * </table>
 * For the default SSL parameters see the Javadocs of the {@link DefaultSslContextFactory} class.
 * 
 * @author Lars Heuer
 * @author Jerome Louvel
 */
public class HttpsServerHelper extends SimpleServerHelper {

	/**
	 * Constructor.
	 * 
	 * @param server
	 *            The server to help.
	 */
	public HttpsServerHelper(Server server) {
		super(server);
		getProtocols().add(Protocol.HTTPS);
	}

	/** Starts the Restlet. */
	@Override
	public void start() throws Exception {
		// Initialize the SSL context
		SslContextFactory sslContextFactory = SslUtils.getSslContextFactory(this);
		SSLContext sslContext = sslContextFactory.createSslContext();
		String addr = getHelped().getAddress();

		if (addr != null) {
			// This call may throw UnknownHostException and otherwise always
			// returns an instance of INetAddress.
			// Note: textual representation of inet addresses are supported
			InetAddress iaddr = InetAddress.getByName(addr);

			// Note: the backlog of 50 is the default
			setAddress(new InetSocketAddress(iaddr, getHelped().getPort()));
		} else {
			int port = getHelped().getPort();

			// Use ephemeral port
			if (port > 0) {
				setAddress(new InetSocketAddress(getHelped().getPort()));
			}
		}

		// Complete initialization
		Container container = new SimpleContainer(this);
		ContainerServer server = new ContainerServer(container, getDefaultThreads());
		SimpleServer filter = new SimpleServer(server);
		Connection connection = new SocketConnection(filter);
		setConfidential(true);
		setContainerServer(server);
		setConnection(connection);

		// Effectively connect the server socket
		InetSocketAddress address = (InetSocketAddress) getConnection().connect(getAddress(), sslContext);
		setEphemeralPort(address.getPort());
		super.start();
	}
}
