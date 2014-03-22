package it.ventuland.ytd;

import it.ventuland.ytd.event.DownloaderListener;
import it.ventuland.ytd.ui.UrlList;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

public class DownloaderStub {

	private final static int THREAD_POOL_SIZE = 6;
	
	private ExecutorService dnlThreadExecutor = null; 
	private UrlList mDnlList = null;
	private DownloaderListener mEventListener = null;
	private boolean mIsDebug;
	private boolean mNoDowload;
	
	public DownloaderStub(UrlList pDnlList, DownloaderListener pEventListener, boolean pIsDebug, boolean pNoDowload) {
		initialize(pDnlList, pEventListener, pIsDebug, pNoDowload);
	}
	
	protected void initialize(UrlList pDnlList, DownloaderListener pEventListener, boolean pIsDebug, boolean pNoDowload) {
		dnlThreadExecutor = Executors.newFixedThreadPool(THREAD_POOL_SIZE, new ThreadFactory() {
			@Override
			public Thread newThread(Runnable r) {
				Thread lNewThread = Executors.defaultThreadFactory().newThread(r);
				lNewThread.setDaemon(false);
				return lNewThread;
			}
		});
		
		mDnlList = pDnlList;
		mEventListener = pEventListener;
		mIsDebug = pIsDebug;
		mNoDowload = pNoDowload;
	}

	public void startAll(){
		YTDownloadThreadData lData = new YTDownloadThreadData();
		lData.videoElementQueue = mDnlList;
		lData.isDebug = mIsDebug;
		lData.forceNoDowload = mNoDowload;
		for(int i = 0; i<THREAD_POOL_SIZE; i++){
			YTDownloadThread lThread = new YTDownloadThread(lData);
			lThread.addDownloadListener(mEventListener);
			dnlThreadExecutor.execute(lThread);
		}
		dnlThreadExecutor.shutdown();
	}
	
	public void killAll(){
		dnlThreadExecutor.shutdownNow();
	}
	
}
