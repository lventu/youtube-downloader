package it.ventuland.ytd.utils;


public class URLUtils {

	// something like [http://][www.]youtube.[cc|to|pl|ev|do|ma|in]/watch?v=0123456789A 
	public static final String szYTREGEX = "^((H|h)(T|t)(T|t)(P|p)(S|s)?://)?((W|w)(W|w)(W|w)\\.)?(Y|y)(O|o)(U|u)(T|t)(U|u)(B|b)(E|e)\\..{2,5}/(W|w)(A|a)(T|t)(C|c)(H|h)\\?(v|V)=[^&]{11}"; // http://de.wikipedia.org/wiki/CcTLD
	// something like [http://][*].youtube.[cc|to|pl|ev|do|ma|in]/   the last / is for marking the end of host, it does not belong to the hostpart
	public static final String szYTHOSTREGEX = "^((H|h)(T|t)(T|t)(P|p)(S|s)?://)?(.*)\\.((Y|y)(O|o)(U|u)(T|t)(U|u)(B|b)(E|e)\\..{2,5}|(G|g)(O|o)(O|o)(G|g)(L|l)(E|e)(V|v)(I|i)(D|d)(E|e)(O|o)\\..{2,5})/";


	private URLUtils() {

	}

	public static boolean isHttps(String pUrl){
		boolean isHttps = false;
		if(pUrl.toLowerCase().startsWith("https")){
			isHttps = true;
		}
		return isHttps;
	}

	public static String getURI(String sURL) {
		String suri = "/".concat(sURL.replaceFirst(URLUtils.szYTHOSTREGEX, ""));
		return suri;
	}

	public static String getHost(String sURL) {
		String shost = sURL.replaceFirst(URLUtils.szYTHOSTREGEX, "");
		shost = sURL.substring(0, sURL.length()-shost.length());
		shost = shost.toLowerCase().replaceFirst("http[s]?://", "").replaceAll("/", "");
		return shost;
	} 

}
