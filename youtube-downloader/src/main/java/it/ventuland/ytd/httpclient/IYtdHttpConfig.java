package it.ventuland.ytd.httpclient;

import java.io.IOException;

import org.apache.http.client.ClientProtocolException;

public interface IYtdHttpConfig {

	IYtdResponse connect() throws ClientProtocolException, IOException;

	void close() throws IOException;

	void clearProxy();

}
