package net.ion.bleujin.asyncrestlet;

import org.restlet.resource.Get;
import org.restlet.resource.ServerResource;

public class RestletResource extends ServerResource {

    @Get
    public String represent() {
        return "hello from restlet\n";
    }
}
