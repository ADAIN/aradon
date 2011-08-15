package net.ion.bulletin

import net.ion.framework.util.Debug;
import net.ion.radon.core.IService;
import net.ion.radon.core.TreeContext;
import net.ion.radon.core.let.InnerRequest;
import net.ion.radon.core.let.MultiValueMap;

import org.restlet.Request;
import org.restlet.Response;

import net.ion.radon.core.filter.IFilterResult;
import net.ion.radon.core.filter.IRadonFilter;

public class HelloFilter extends IRadonFilter {
	
	def name
	
	public IFilterResult afterHandle(IService iservice, Request request, Response response) {
		
		return IFilterResult.CONTINUE_RESULT  
	}
	
	public IFilterResult preHandle(IService iservice, Request request, Response response) {
		MultiValueMap param = innerRequest.formParameter;
		
		name =  param.name ?: "anony"
		
		if (param.name && param.name.length() > 10) {
			Debug.debug "LONG NAME"	
		}
		
		Debug.debug(name) 
	}
}
