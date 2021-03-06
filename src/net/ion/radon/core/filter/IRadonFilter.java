package net.ion.radon.core.filter;


import java.util.Map;

import net.ion.framework.util.CaseInsensitiveHashMap;
import net.ion.radon.core.IService;
import net.ion.radon.core.config.Attribute;
import net.ion.radon.core.let.InnerRequest;

import org.restlet.Request;
import org.restlet.Response;
import org.restlet.representation.Representation;
public abstract class IRadonFilter {

	private CaseInsensitiveHashMap<Attribute> attrs = new CaseInsensitiveHashMap<Attribute>() ;

	public void init(IService service){
		; // noaction 
	}
	
//	public IFilterResult preHandle(Request request, Response response) {
//		return preHandle(getContext(request), request, response);
//	}
//	public void afterHandle(Request request, Response response) {
//		this.afterHandle(getContext(request), request, response) ;
//	}


	public abstract IFilterResult afterHandle(IService iservice, Request request, Response response) ;

	public abstract IFilterResult preHandle(IService iservice, Request request, Response response) ;

	
	public InnerRequest getInnerRequest(Request req){
		return (InnerRequest) req ;
	}
	
	public IFilterResult afterHandle(Representation result, IService iservice, Request request, Response response) {
		return afterHandle(iservice, request, response) ;
	}
	
	public void addAttribute(String key, Attribute attribute) {
		attrs.put(key, attribute) ;
	}
	
	protected Map<String, Attribute> getAttributes(){
		return attrs ;
	} 

	protected Attribute getAttribute(String id){
		return attrs.get(id) ;
	} 
	
	public String getElementValue(String key){
		return attrs.get(key) == null ? null : attrs.get(key).getElementValue() ;
	}
	
	public String getAttributeValue(String key, String attrKey, String defaultValue){
		Attribute value = attrs.get(key);
		return (value == null || value.getValue(attrKey) == null) ? defaultValue : value.getValue(attrKey);
	}
	
	@Override
	public String toString() {
		return this.getClass().getCanonicalName();
	}
}
