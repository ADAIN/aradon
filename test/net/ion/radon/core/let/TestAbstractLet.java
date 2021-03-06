package net.ion.radon.core.let;

import static org.junit.Assert.assertEquals;
import net.ion.radon.core.Aradon;
import net.ion.radon.core.IService;
import net.ion.radon.core.SectionService;
import net.ion.radon.core.TestBaseAradon;
import net.ion.radon.core.config.PathConfiguration;
import net.ion.radon.core.config.SectionConfiguration;
import net.ion.radon.impl.let.HelloWorldLet;
import net.ion.radon.util.AradonTester;

import org.junit.Test;
import org.restlet.Request;
import org.restlet.Response;
import org.restlet.data.Method;

public class TestAbstractLet extends TestBaseAradon {

	@Test
	public void testAbstractLet() throws Exception {
		Aradon aradon = AradonTester.create().register("", "/hello", HelloWorldLet.class).getAradon() ;
		
		SectionService section = aradon.attach(SectionConfiguration.createBlank("test"));
		final PathConfiguration pathInfo = PathConfiguration.create("test", "/test", HelloWorldLet.class);
		section.attach(pathInfo);
		
		Request request = new Request(Method.GET, "riap://component/test/test");
		Response response = aradon.handle(request);

		InnerResponse res = (InnerResponse) Response.getCurrent() ;
		assertEquals(true, res.getPathConfiguration() == pathInfo);
	}
	
	@Test
	public void testParentService() throws Exception {
		Aradon aradon = AradonTester.create().register("", "/hello", HelloWorldLet.class).getAradon() ;
		
		SectionService section = aradon.attach(SectionConfiguration.createBlank("test"));
		section.attach(PathConfiguration.create("test", "/test", HelloWorldLet.class));
		
		Request request = new Request(Method.GET, "riap://component/test/test");
		Response response = aradon.handle(request);
		
		InnerResponse res = (InnerResponse) Response.getCurrent() ;
		final PathService pservice = res.getInnerRequest().getPathService();
		assertEquals(true, pservice.getConfig() == section.getChildService("test").getConfig()) ;
		assertEquals(true, section == pservice.getParent());
		assertEquals(true, aradon == pservice.getParent().getParent());
		assertEquals(true, IService.ROOT == pservice.getParent().getParent().getParent());
	}
	
	
	
	
	
}
