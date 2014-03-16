package it.ventuland.ytd;

import it.ventuland.ytd.event.DownloaderListener;
import it.ventuland.ytd.ui.UrlList;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DownloaderStub {

	private final static int THREAD_POOL_SIZE = 6;
	
	private ExecutorService dnlThreadExecutor = null; 
	private UrlList mDnlList = null;
	private DownloaderListener mEventListener = null;
	
	public DownloaderStub(UrlList pDnlList, DownloaderListener pEventListener) {
		initialize(pDnlList, pEventListener);
	}
	
	protected void initialize(UrlList pDnlList, DownloaderListener pEventListener) {
		dnlThreadExecutor = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
		
		mDnlList = pDnlList;
		mEventListener = pEventListener;
	}

	public void startAll(){
		for(int i = 0; i<THREAD_POOL_SIZE; i++){
			YTDownloadThread lThread = new YTDownloadThread(mDnlList);
			lThread.addDownloadListener(mEventListener);
			dnlThreadExecutor.execute(lThread);
		}
		dnlThreadExecutor.shutdown();
	}
	
	public void killAll(){
		dnlThreadExecutor.shutdownNow();
	}
	
}
