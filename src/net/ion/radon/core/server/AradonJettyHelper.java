package net.ion.radon.core.server;

import java.util.Map.Entry;

import net.ion.radon.core.Aradon;
import net.ion.radon.core.config.ConnectorConfiguration;
import net.ion.radon.core.server.jetty.JettyServerHelper;

import org.restlet.Context;
import org.restlet.Server;
import org.restlet.data.Protocol;
import org.restlet.engine.Engine;
import org.restlet.engine.local.RiapServerHelper;

public class AradonJettyHelper implements AradonServerHelper {

	private JettyServerHelper helper;

	private AradonJettyHelper(JettyServerHelper server) {
		this.helper = server;
	}

	public final static AradonJettyHelper create(Context context, ConnectorConfiguration cfig, Aradon aradon) {
//		ConnectorHelper<Server> helper = new HttpServerHelper(null);
// 		ConnectorHelper<Server> helper = new HttpsServerHelper(null) ;
//		Engine.getInstance().getRegisteredServers().add(0, helper);
		Engine.getInstance().getRegisteredServers().clear() ;
		Engine.getInstance().getRegisteredServers().add(new RiapServerHelper(null)) ;
		
		Server server = new Server(context, cfig.protocol(), cfig.port(), aradon);
		
		for (Entry<String, String> entry : cfig.properties().entrySet()) {
			server.getContext().getParameters().add(entry.getKey(), entry.getValue());
		}
		if (cfig.protocol() == Protocol.HTTP){
			// It seems that I get the deadlocks with both NIO connectors, 
			// the selecting one and the blocking one. This is both 2.1-RC4 and the 3rd of May snaphost. The BIO one ("3") is OK.
			server.getContext().getParameters().set("type", "3") ;
			return new AradonJettyHelper(new  net.ion.radon.core.server.jetty.HttpServerHelper(server));
		} else {
//			server.getContext().getParameters().set("type", "1") ;
			return new AradonJettyHelper(new  net.ion.radon.core.server.jetty.HttpsServerHelper(server)); 
		}
	}

	public void start() throws Exception {
		helper.start() ;
	}

	public void stop() throws Exception {
		helper.stop() ;
	}

}
