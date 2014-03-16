package it.ventuland.ytd.httpclient;

import it.ventuland.ytd.utils.URLUtils;

import java.io.IOException;
import java.nio.charset.Charset;

import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.config.ConnectionConfig;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.protocol.HttpContext;

public class YTDHttpConfig implements IYtdHttpConfig{

	private String mUrl;
	private boolean hasProxy;
	private String mProxyUrl;
	private int mProxyPort;
	
	CloseableHttpClient	httpclient = null;
	HttpContext	localContext = null;
	
	public YTDHttpConfig(String pUrl, String pProxy) {
		initialize(pUrl, pProxy);
	}
	
	protected void initialize(String pUrl, String pProxy){
		this.mUrl = pUrl;
		this.setProxy(pProxy);
	}
	
	private void setProxy(String pProxy){
		if( pProxy!=null && !"".equals(pProxy) ){
			this.mProxyUrl = pProxy.toLowerCase().replaceFirst("http://", "").replaceFirst(":(.*)", "");
			this.mProxyPort = Integer.valueOf( pProxy.replaceFirst("(.*):", "") );
			this.hasProxy = true;
		}else{
			this.mProxyUrl = null;
			this.hasProxy = false;
		}
	}
	
	@Override
	public void clearProxy(){
		this.mProxyUrl = null;
		this.mProxyPort = 0;
		this.hasProxy = false;
	}
	
	@Override
	public IYtdResponse connect() throws ClientProtocolException, IOException{
		ConnectionConfig lConnCnf = ConnectionConfig.custom().setCharset(Charset.forName("UTF-8")).build();
		HttpClientConnectionManager ccm = new PoolingHttpClientConnectionManager();
		RequestConfig lReqCnf = null;
		if ( hasProxy ) {
			HttpHost proxy = new HttpHost( this.mProxyUrl, this.mProxyPort, "http");
			lReqCnf = RequestConfig.custom().setProxy(proxy).setCookieSpec("best-match").build();
		} else {
			lReqCnf = RequestConfig.custom().setCookieSpec("best-match").build();
		}
		this.httpclient = HttpClientBuilder.create().setDefaultRequestConfig(lReqCnf).setConnectionManager(ccm).setDefaultConnectionConfig(lConnCnf).build();
		HttpGet httpget = new HttpGet( mUrl );	
		HttpHost target = null;
		if (URLUtils.isHttps(mUrl)){
			target = new HttpHost( URLUtils.getHost(mUrl), 443, "https" );
		} else {
			target = new HttpHost( URLUtils.getHost(mUrl), 80, "http" );
		}
		HttpResponse response = this.httpclient.execute(target, httpget, this.localContext);
		return new YtdResponse(response);
	}
	
	@Override
	public void close() throws IOException{
		this.httpclient.close();
	}
	
	/*
	  debugoutput("executing request: ".concat( this.httpget.getRequestLine().toString()) );
        debugoutput("uri: ".concat( this.httpget.getURI().toString()) );
        debugoutput("host: ".concat( this.target.getHostName() ));
        debugoutput("using proxy: ".concat( this.getProxy() ));
        
	 */
}
