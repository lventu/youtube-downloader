package it.ventuland.ytd.utils;

import java.net.URL;

public class UrlConverter implements IOperation<String> {

	private static IOperation<String> mOp = null;
	public static synchronized IOperation<String> getInstance(){
		if (mOp == null){
			mOp = new UrlConverter();
		}
		return mOp;
	}
	
	protected UrlConverter(){
		
	}
	
	@Override
	public synchronized String execute(String pInput) throws Exception {
		String lOut = pInput;
		try{
			if (pInput.toLowerCase().startsWith("youtube")) {
				lOut = "http://www.".concat(pInput);
			}
			if (pInput.toLowerCase().startsWith("www")) {
				lOut = "http://".concat(pInput);
			}
			URL lUrl = new URL(lOut);
	        lUrl.openConnection();
		}catch(Throwable t){
			throw t;
		}
		return lOut;
	}

}
