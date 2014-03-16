package it.ventuland.ytd.httpclient;

import java.io.IOException;
import java.io.InputStream;

import org.apache.http.HttpResponse;

public class YtdResponse implements IYtdResponse {

	HttpResponse mResponse = null;
	
	public YtdResponse(HttpResponse pHttpResponse ) {
		this.mResponse = pHttpResponse;
	}
	
	@Override
	public HttpResponse getNativeResponse(){
		return mResponse;
	}
	
	@Override
	public String getStatusLine() {
		return mResponse.getStatusLine().toString().toLowerCase();
	}

	@Override
	public InputStream getEntityContent() throws IllegalStateException, IOException {
		InputStream iRet = null;
		if(mResponse.getEntity()!=null){
			iRet = mResponse.getEntity().getContent();
		}
		return iRet;
	}

	@Override
	public String getFirstHeaderString(String pTag) {
		return mResponse.getFirstHeader(pTag).toString();
	}

	@Override
	public String getFirstHeaderValue(String pTag) {
		return mResponse.getFirstHeader(pTag).getValue();
	}
	
}
