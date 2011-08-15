package net.ion.radon.impl.section;

import net.ion.framework.util.StringUtil;
import net.ion.radon.core.EnumClass.IMatchMode;
import net.ion.radon.core.let.AbstractLet;
import net.ion.radon.core.path.URLPattern;
import net.ion.radon.impl.let.HelloWorldLet;
import net.ion.radon.impl.let.ObjectLet;

import org.apache.ecs.xhtml.map;
import org.restlet.data.Reference;
import org.restlet.resource.ServerResource;

public class PathInfo extends BasePathInfo{

	private String name;
	private String[] urls;
	private IMatchMode mmode;
	private String description;
	private Class<? extends ServerResource> handlerClass;

	public final static PathInfo HELLO = PathInfo.create("test", "/test", "", "", HelloWorldLet.class) ;
	
	private PathInfo(String name, String urls, String matchMode, String description, Class<? extends ServerResource> handlerClass) {
		this.name = name;
		this.urls = StringUtil.split(urls, ", \t\n");
		this.mmode =  IMatchMode.fromString(matchMode) ;
		this.description = description;
		this.handlerClass = handlerClass;
	}

	public static PathInfo create(String name, String urls, String matchMode, String description, Class<? extends ServerResource> handlerClass) {
		return new PathInfo(name, urls, matchMode, description, handlerClass);
	}

	public static PathInfo create(String name, String urls, Class<? extends ServerResource> handlerClass) {
		return create(name, urls, "", "", handlerClass);
	}

	
	@Override
	public Class<? extends ServerResource> getHandlerClass() {
		return handlerClass;
	}

	@Override
	public String[] getUrls() {
		return urls;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public IMatchMode getMatchMode() {
		return mmode;
	}

	@Override
	public String getDescription() {
		return description;
	}

	public String toString() {
		return "name:" + name + ", desc:" + description;
	}

}
