package it.ventuland.ytd.event;

import java.util.EventListener;

public interface DownloaderListener extends EventListener {
	
	public void downloadStarted(DownloadEvent e);
	
	public void downloadProgress(DownloadEvent e);
	
	public void downloadCompleted(DownloadEvent e);
	
	public void downloadFailure(DownloadEvent e);

	public void downloadCompletedNotDownloaded(DownloadEvent e);

	public void threadIdle(DownloadEvent e);
	
	public void threadAborted(DownloadEvent e);
	
}
