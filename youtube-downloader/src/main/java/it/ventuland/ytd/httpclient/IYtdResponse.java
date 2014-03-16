package it.ventuland.ytd.httpclient;

import java.io.IOException;
import java.io.InputStream;

import org.apache.http.HttpResponse;

public interface IYtdResponse {

	String getStatusLine();
	
	InputStream getEntityContent() throws IllegalStateException, IOException;

	String getFirstHeaderString(String pTag);

	String getFirstHeaderValue(String pTag);

	HttpResponse getNativeResponse();
	
}
