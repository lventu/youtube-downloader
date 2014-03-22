package it.ventuland.ytd.event;

import it.ventuland.ytd.YTUrl;

import java.util.EventObject;

public class DownloadEvent extends EventObject {
	
	public enum DOWNLOAD_STATUS{
		STARTED, COMPLETED, COMPLETED_NOT_DOWNLOAD, FAILED, DOWNLOADING, IDLE, ABORTING;
		
		@Override
		public String toString() {
			return this.name().replaceAll("_", " ");
		};
	}
	
	private static final long serialVersionUID = 1L;

	private int mThreadNum;
	private DOWNLOAD_STATUS mStatus;
	private int mPerc;
	private YTUrl mVideoSrc;
	private String mVideoUrl;
	
	public DownloadEvent(Object source, int pThreadNum, DOWNLOAD_STATUS pStatus, int pPerc, String pVideoUrl, YTUrl pVideoSrc) {
		super(source);
		
		mThreadNum = pThreadNum;
		mStatus = pStatus;
		mPerc = pPerc;
		mVideoSrc = pVideoSrc;
		mVideoUrl = pVideoUrl;
	}

	public int getThreadNum() {
		return mThreadNum;
	}

	public DOWNLOAD_STATUS getStatus() {
		return mStatus;
	}

	public int getCompletePerc() {
		return mPerc;
	}

	public YTUrl getVideoSrc() {
		return mVideoSrc;
	}
	
	public String getVideoUrl() {
		return mVideoUrl;
	}
	
}
