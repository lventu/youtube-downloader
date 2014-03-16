package it.ventuland.ytd.ui;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class UrlList {
	
	LinkedBlockingQueue<String> mQueue;
	
	public UrlList() {
		mQueue = new LinkedBlockingQueue<String>();
	}

	public String getElement() throws InterruptedException {
		return mQueue.poll(100, TimeUnit.MILLISECONDS);
	}
	
	public void setElement(String e) throws InterruptedException{
		mQueue.put(e);
	}
	
	public String[] toArray(){
		String[] lTmp = new String[mQueue.size()];
		return mQueue.toArray(lTmp);
	}
	
}
