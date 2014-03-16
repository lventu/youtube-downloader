package it.ventuland.log;

public class LogFactory {

	private static ILogger mLogger;
	
	public static ILogger getLogger(){
		if(mLogger==null){
			mLogger = new TestLogger();
		}
		return mLogger;
	}
	
}
