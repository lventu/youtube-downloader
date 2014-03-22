/**
 *  This file is part of ytd2
 *
 *  ytd2 is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  ytd2 is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *  You should have received a copy of the GNU General Public License
 *  along with ytd2.
 *  If not, see <http://www.gnu.org/licenses/>.
 */
package it.ventuland.ytd;

import it.ventuland.ytd.config.YtdConfigManager;
import it.ventuland.ytd.event.DownloadEvent;
import it.ventuland.ytd.event.DownloadEvent.DOWNLOAD_STATUS;
import it.ventuland.ytd.event.DownloaderListener;
import it.ventuland.ytd.httpclient.IYtdResponse;
import it.ventuland.ytd.httpclient.YTDHttpConfig;
import it.ventuland.ytd.ui.GUIClient;
import it.ventuland.ytd.ui.UrlList;
import it.ventuland.ytd.ui.VideoElement;
import it.ventuland.ytd.utils.UTF8Utils;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;

import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpResponse;

/**
 * http://www.youtube.com/watch?v=9QFK1cLhytY Javatar and .NOT http://www.youtube.com/watch?v=Mt7zsortIXs 1080p "Lady Java" http://www.youtube.com/watch?v=WowZLe95WDY Tom Petty And the Heartbreakers - Learning to Fly (with lyrics) http://www.youtube.com/watch?v=86OfBExGSE0 URZ 720p http://www.youtube.com/watch?v=cNOP2t9FObw Blade 360 - 480 http://www.youtube.com/watch?v=HvQBrM_i8bU MZ 1000 Street Fighter http://www.youtube.com/watch?v=yVpbFMhOAwE How Linx is build http://www.youtube.com/watch?v=4XpnKHJAok8 Tech Talk: Linus Torvalds on git
 * 
 * ODOT http://sourceforge.net/p/ytd2/bugs/7/ http://www.youtube.com/watch?v=fRVVzXnRsUQ uses RTMPE (http://en.wikipedia.org/wiki/Protected_Streaming), which ytd2 cannot download atm
 * 
 */
public class YTDownloadThread extends Thread {
	
	static volatile int iThreadcount = 0;

	int iThreadNo = YTDownloadThread.iThreadcount++; // every download thread get its own number

	boolean mForceNoDownload;
	final String ssourcecodeurl = "http://";
	final String ssourcecodeuri = "[a-zA-Z0-9%&=\\.]";

	String sTitle = null; // will be used as filename

	String sURL = null; // main URL (youtube start web page)
	String sVideoURL = null; // one video web resource
	Vector<YTUrl> vNextVideoURL = new Vector<YTUrl>(); // list of URLs from webpage source
	private UrlList mUrlList = null;
	String sFileName = null; // contains the absolute filename
	//boolean bisinterrupted = false; // basically the same as Thread.isInterrupted()
	int iRecursionCount = -1; // counted in downloadone() for the 3 webrequest to one video

	String sContentType = null;

	private boolean isDebug = false;
	
	private ArrayList<DownloaderListener> mDnlListenerList = new ArrayList<DownloaderListener>();
	
	public YTDownloadThread(YTDownloadThreadData pSetupData) {
		super();
		mUrlList = pSetupData.videoElementQueue;
		isDebug = pSetupData.isDebug;
		mForceNoDownload = pSetupData.forceNoDowload;
		String sv = "thread started: ".concat(this.getMyName());
		debugoutput(sv);
	}

	public synchronized void addDownloadListener(DownloaderListener listener) {
		if (!mDnlListenerList.contains(listener)) {
			mDnlListenerList.add(listener);
		}
	}
	
	/**
	 * Recursive function that perform video download
	 * @param pVideo the video url
	 * @return true if all ok, false otherwise
	 */
	private boolean downloadone(String pUrl, VideoElement pVideo) {
		BufferedReader textreader = null;
		BufferedInputStream binaryreader = null;
		YTDHttpConfig httpclient = null;
		boolean rc = false;
		// stop recursion
		String sURL = pUrl;
		try {
			if (sURL.equals("")) {
				return (false);
			}
		} catch (NullPointerException npe) {
			return (false);
		}
		if (isWorkerInterrupted()) {
			return (false); // try to get information about application shutdown
		}
		try{
			
			boolean rc204 = false;
			boolean rc302 = false;
	
			IYtdResponse response = null;
			
			iRecursionCount++;
	
			debugoutput("start.");
	
			httpclient = new YTDHttpConfig(sURL, GUIClient.getProxy());
			response = httpclient.connect();
			this.sVideoURL = null;
			this.sContentType = response.getFirstHeaderValue("Content-Type").toLowerCase();
	
			debugoutput("HTTP response status line:".concat(response.getStatusLine()));

			// abort if HTTP response code is != 200, != 302 and !=204 - wrong URL?
			if (!(rc = response.getStatusLine().matches("^(http)(.*)200(.*)")) & !(rc204 = response.getStatusLine().matches("^(http)(.*)204(.*)")) & !(rc302 = response.getStatusLine().matches("^(http)(.*)302(.*)"))) {
				debugoutput(response.getStatusLine().concat(" ").concat(sURL));
				output(response.getStatusLine().concat(" \"").concat(this.sTitle).concat("\""));
				return (rc & rc204 & rc302);
			}
			if (rc204) {
				debugoutput("last response code==204 - download: ".concat(this.vNextVideoURL.get(0).getsYTID()));
				rc = downloadone(this.vNextVideoURL.get(0).getsURL(), pVideo);
				return (rc);
			}
			if (rc302) {
				debugoutput("location from HTTP Header: ".concat(response.getFirstHeaderString("Location")));
			}
			
			// real file download
			InputStream lResponseStream = response.getEntityContent();
			if(lResponseStream != null){
				if (this.sContentType.matches("^text/html(.*)")) {
					textreader = new BufferedReader(new InputStreamReader(lResponseStream));
					rc = savetextdata(textreader, iRecursionCount, pVideo);
				} else if (this.sContentType.matches("video/(.)*")) {
					binaryreader = new BufferedInputStream(lResponseStream);
					if (mForceNoDownload) {
						reportheaderinfo(response.getNativeResponse());
					} else {
						Long lByteMax = Long.parseLong(response.getFirstHeaderValue("Content-Length"));
						savebinarydata(binaryreader, lByteMax);
					}
				}else { // content-type is not video/
					rc = false;
					this.sVideoURL = null;
				}
			}

			httpclient.close();
			
			debugoutput("done: ".concat(sURL));
			if (this.sVideoURL == null) {
				this.sVideoURL = ""; // to prevent NPE
			}
	
			if (!this.sVideoURL.matches(GUIClient.szURLREGEX)) {
				// no more recursion - html source hase been read
				// test !rc than video could not downloaded because of some error (like wrong protocol or restriction)
				if (!rc) {
					debugoutput("cannot download video - URL does not seem to be valid or could not be found: ".concat(this.sURL));
					output("there was a problem getting the video URL! perhaps not allowed in your country?!");
					output(("consider reporting the URL to author! - ").concat(this.sURL));
					this.sVideoURL = null;
				}
			} else {
				// enter recursion - download video resource
				debugoutput("try to download video from URL: ".concat(this.sVideoURL));
				rc = downloadone(this.sVideoURL, pVideo);
			}
			this.sVideoURL = null;
			
		} catch (UnknownHostException uhe) {
			output(("error connecting to: ").concat(uhe.getMessage()));
			debugoutput(uhe.getMessage());
			rc = false;
		} catch(Throwable t){
			debugoutput(t.getMessage());
			rc = false;
		}finally{
			try {
				if(textreader!=null) {
					textreader.close();
				}
			} catch (Exception e) {
			}
			try {
				if(binaryreader!=null) {
					binaryreader.close();
				}
			} catch (Exception e) {
			}
			try {
				if(httpclient!=null) {
					httpclient.close();
				}
			} catch (Exception e) {
			}
		}
		return (rc);
	}

	void reportheaderinfo(HttpResponse response) {
		if (isDebug) {
			debugoutput("");
			debugoutput("NO-DOWNLOAD mode active (ndl on)");
			debugoutput("all HTTP header fields:");
			for (int i = 0; i < response.getAllHeaders().length; i++) {
				debugoutput(response.getAllHeaders()[i].getName().concat("=").concat(response.getAllHeaders()[i].getValue()));
			}
			debugoutput("filename would be: ".concat(this.getTitle()).concat(".").concat(response.getFirstHeader("Content-Type").getValue().replaceFirst("video/", "").replaceAll("x-", ""))); // title contains just filename, no path
		} else {
			Long iFileSize = Long.parseLong(response.getFirstHeader("Content-Length").getValue());
			output("");
			output("NO-DOWNLOAD active (ndl on)");
			output("some HTTP header fields:");
			output("content-type: ".concat(response.getFirstHeader("Content-Type").getValue()));
			output("content-length: ".concat(iFileSize.toString()).concat(" Bytes").concat(" ~ ").concat(Long.toString((iFileSize / 1024)).concat(" KiB")).concat(" ~ ").concat(Long.toString((iFileSize / 1024 / 1024)).concat(" MiB")));
			if (mForceNoDownload) {
				output(("filename would be: ").concat(this.getTitle().concat(".").concat(response.getFirstHeader("Content-Type").getValue().replaceFirst("video/", "").replaceAll("x-", "")))); // title contains just filename, no path
			}
		}
	}

	private int addMPEG_HD_Urls(int iindex, HashMap<String, String> ssourcecodevideourls, VideoElement pVideo) {
		int inewiindex = iindex;
		// try 3D HD first if 3D is selected
		if (pVideo.is3D) {
			this.vNextVideoURL.add(inewiindex++, new YTUrl(ssourcecodevideourls.get("84"), this.sURL, "3D")); // mpeg 3D full HD
		}

		// if SDS is on reverse order! - 720p before 1080p for HD and so on
		if (!GUIClient.bSaveDiskSpace) {
			this.vNextVideoURL.add(inewiindex++, new YTUrl(ssourcecodevideourls.get("37"), this.sURL)); // mpeg full HD
			this.vNextVideoURL.add(inewiindex++, new YTUrl(ssourcecodevideourls.get("22"), this.sURL)); // mpeg half HD
		} else {
			this.vNextVideoURL.add(inewiindex++, new YTUrl(ssourcecodevideourls.get("22"), this.sURL)); // mpeg half HD
			this.vNextVideoURL.add(inewiindex++, new YTUrl(ssourcecodevideourls.get("37"), this.sURL)); // mpeg full HD
		}
		return inewiindex;
	}

	private int addWBEM_HD_Urls(int iindex, HashMap<String, String> ssourcecodevideourls, VideoElement pVideo) {
		int inewiindex = iindex;
		// try 3D HD first if 3D is selected
		if (pVideo.is3D) {
			this.vNextVideoURL.add(inewiindex++, new YTUrl(ssourcecodevideourls.get("100"), this.sURL, "3D")); // webm 3D HD
		}

		if (!GUIClient.bSaveDiskSpace) {
			this.vNextVideoURL.add(inewiindex++, new YTUrl(ssourcecodevideourls.get("46"), this.sURL)); // webm full HD
			this.vNextVideoURL.add(inewiindex++, new YTUrl(ssourcecodevideourls.get("45"), this.sURL)); // webm half HD
		} else {
			this.vNextVideoURL.add(inewiindex++, new YTUrl(ssourcecodevideourls.get("45"), this.sURL)); // webm half HD
			this.vNextVideoURL.add(inewiindex++, new YTUrl(ssourcecodevideourls.get("46"), this.sURL)); // webm full HD
		}
		return inewiindex;
	}

	private int addWBEM_SD_Urls(int iindex, HashMap<String, String> ssourcecodevideourls, VideoElement pVideo) {
		int inewiindex = iindex;
		// try 3D first if 3D is selected
		if (pVideo.is3D) {
			this.vNextVideoURL.add(inewiindex++, new YTUrl(ssourcecodevideourls.get("102"), this.sURL, "3D")); // webm 3D SD
		}

		if (!GUIClient.bSaveDiskSpace) {
			this.vNextVideoURL.add(inewiindex++, new YTUrl(ssourcecodevideourls.get("44"), this.sURL)); // webm SD
			this.vNextVideoURL.add(inewiindex++, new YTUrl(ssourcecodevideourls.get("43"), this.sURL)); // webm SD
		} else {
			this.vNextVideoURL.add(inewiindex++, new YTUrl(ssourcecodevideourls.get("43"), this.sURL)); // webm SD
			this.vNextVideoURL.add(inewiindex++, new YTUrl(ssourcecodevideourls.get("44"), this.sURL)); // webm SD
		}
		return inewiindex;
	}

	private int addFLV_SD_Urls(int iindex, HashMap<String, String> ssourcecodevideourls, VideoElement pVideo) {
		int inewiindex = iindex;

		if (!GUIClient.bSaveDiskSpace) {
			this.vNextVideoURL.add(inewiindex++, new YTUrl(ssourcecodevideourls.get("35"), this.sURL)); // flv SD
			this.vNextVideoURL.add(inewiindex++, new YTUrl(ssourcecodevideourls.get("34"), this.sURL)); // flv SD
		} else {
			this.vNextVideoURL.add(inewiindex++, new YTUrl(ssourcecodevideourls.get("34"), this.sURL)); // flv SD
			this.vNextVideoURL.add(inewiindex++, new YTUrl(ssourcecodevideourls.get("35"), this.sURL)); // flv SD
		}
		return inewiindex;
	}

	private int addMPEG_SD_Urls(int iindex, HashMap<String, String> ssourcecodevideourls , VideoElement pVideo) {
		int inewiindex = iindex;

		// try 3D first if 3D is selected
		if (pVideo.is3D) {
			this.vNextVideoURL.add(inewiindex++, new YTUrl(ssourcecodevideourls.get("82"), this.sURL, "3D")); // mpeg 3D SD
		}

		this.vNextVideoURL.add(inewiindex++, new YTUrl(ssourcecodevideourls.get("18"), this.sURL)); // mpeg SD
		return inewiindex;
	}

	private int addMPEG_LD_Urls(int iindex, HashMap<String, String> ssourcecodevideourls, VideoElement pVideo) {
		int inewiindex = iindex;
		this.vNextVideoURL.add(inewiindex++, new YTUrl(ssourcecodevideourls.get("36"), this.sURL)); // mpeg LD
		this.vNextVideoURL.add(inewiindex++, new YTUrl(ssourcecodevideourls.get("17"), this.sURL)); // mpeg LD
		return inewiindex;
	}

	private int addFLV_LD_Urls(int iindex, HashMap<String, String> ssourcecodevideourls, VideoElement pVideo) {
		int inewiindex = iindex;

		this.vNextVideoURL.add(inewiindex++, new YTUrl(ssourcecodevideourls.get("5"), this.sURL)); // flv LD
		return inewiindex;
	}

	boolean savetextdata(BufferedReader textreader, int iRecursionCount, VideoElement pVideo) throws IOException {
		boolean rc = false;
		// read html lines one by one and search for java script array of video URLs
		String sline = "";
		while (sline != null) {
			sline = textreader.readLine();
			try {
				if (iRecursionCount == 0 && sline.matches("(.*)\"url_encoded_fmt_stream_map\":(.*)")) {
					rc = true;
					HashMap<String, String> ssourcecodevideourls = new HashMap<String, String>();

					sline = sline.replaceFirst(".*\"url_encoded_fmt_stream_map\": \"", "");
					sline = sline.replaceFirst("\".*", "");
					sline = sline.replace("%25", "%");
					sline = sline.replace("\\u0026", "&");
					sline = sline.replace("\\", "");

					// by anonymous
					String[] ssourcecodeyturls = sline.split(",");
					debugoutput("ssourcecodeuturls.length: ".concat(Integer.toString(ssourcecodeyturls.length)));
					String sResolutions = "found video URL for resolution: ";

					for (String urlString : ssourcecodeyturls) {

						// assuming rtmpe is used for all resolutions, if found once - end download
						if (urlString.matches(".*conn=rtmpe.*")) {
							debugoutput("RTMPE found. cannot download this one!");
							output("Unable to download video due to unsupported protocol (RTMPE). sry!");
							break;
						}
						String[] fmtUrlPair = urlString.split("url=http", 2);
						fmtUrlPair[1] = "url=http" + fmtUrlPair[1] + "&" + fmtUrlPair[0];
						// grep itag=xz out and use xy as hash key
						// 2013-02 itag now has up to 3 digits
						fmtUrlPair[0] = fmtUrlPair[1].substring(fmtUrlPair[1].indexOf("itag=") + 5, fmtUrlPair[1].indexOf("itag=") + 5 + 1 + (fmtUrlPair[1].matches(".*itag=[0-9]{2}.*") ? 1 : 0) + (fmtUrlPair[1].matches(".*itag=[0-9]{3}.*") ? 1 : 0));
						fmtUrlPair[1] = fmtUrlPair[1].replaceFirst("url=http%3A%2F%2F", "http://");
						fmtUrlPair[1] = fmtUrlPair[1].replaceAll("%3F", "?").replaceAll("%2F", "/").replaceAll("%3B", ";").replaceAll("%2C", ",").replaceAll("%3D", "=").replaceAll("%26", "&").replaceAll("%252C", "%2C").replaceAll("sig=", "signature=").replaceAll("&s=", "&signature=").replaceAll("\\?s=", "?signature=");

						// remove duplicated &itag=xy
						if (StringUtils.countMatches(fmtUrlPair[1], "itag=") == 2) {
							fmtUrlPair[1] = fmtUrlPair[1].replaceFirst("itag=[0-9]{1,3}", "");
						}

						try {
							ssourcecodevideourls.put(fmtUrlPair[0], fmtUrlPair[1]); // save that URL
							// debugoutput(String.format( "video url saved with key %s: %s",fmtUrlPair[0],ssourcecodevideourls.get(fmtUrlPair[0]) ));
							sResolutions = sResolutions.concat(fmtUrlPair[0].equals("37") ? "1080p mpeg, " : // HD type=video/mp4;+codecs="avc1.64001F,+mp4a.40.2"
									fmtUrlPair[0].equals("22") ? "720p mpeg, " : // HD type=video/mp4;+codecs="avc1.64001F,+mp4a.40.2"
											fmtUrlPair[0].equals("84") ? "1080p 3d mpeg, " : // HD 3D type=video/mp4;+codecs="avc1.64001F,+mp4a.40.2"
													fmtUrlPair[0].equals("35") ? "480p flv, " : // SD type=video/x-flv
															fmtUrlPair[0].equals("18") ? "360p mpeg, " : // SD type=video/mp4;+codecs="avc1.42001E,+mp4a.40.2"
																	fmtUrlPair[0].equals("34") ? "360p flv, " : // SD type=video/x-flv
																			fmtUrlPair[0].equals("82") ? "360p 3d mpeg, " : // SD 3D type=video/mp4;+codecs="avc1.42001E,+mp4a.40.2"
																					fmtUrlPair[0].equals("36") ? "240p mpeg 3gpp, " : // LD type=video/3gpp;+codecs="mp4v.20.3,+mp4a.40.2"
																							fmtUrlPair[0].equals("17") ? "114p mpeg 3gpp, " : // LD type=video/3gpp;+codecs="mp4v.20.3,+mp4a.40.2"
																								
																									fmtUrlPair[0].equals("46") ? "1080p webm, " : // HD type=video/webm;+codecs="vp8.0,+vorbis"&
																											fmtUrlPair[0].equals("45") ? "720p webm, " : // HD type=video/webm;+codecs="vp8.0,+vorbis"
																													fmtUrlPair[0].equals("100") ? "1080p 3d webm, " : // HD 3D type=video/webm;+codecs="vp8.0,+vorbis"&
																															fmtUrlPair[0].equals("44") ? "480p webm, " : // SD type=video/webm;+codecs="vp8.0,+vorbis"
																																	fmtUrlPair[0].equals("43") ? "360p webm, " : // SD type=video/webm;+codecs="vp8.0,+vorbis"
																																			fmtUrlPair[0].equals("102") ? "360p 3d webm, " : // SD 3D type=video/webm;+codecs="vp8.0,+vorbis"&
																																					fmtUrlPair[0].equals("5") ? "240p flv, " : // LD type=video/x-flv
																																							"unknown resolution! (".concat(fmtUrlPair[0]).concat(")"));
						} catch (java.lang.ArrayIndexOutOfBoundsException aioobe) {
						}
					} // for

					output(sResolutions);
					debugoutput(sResolutions);

					int iindex;
					iindex = 0;
					this.vNextVideoURL.removeAllElements();

					debugoutput("ssourcecodevideourls.length: ".concat(Integer.toString(ssourcecodevideourls.size())));
					// figure out what resolution-button is pressed now and fill list with possible URLs
					
					
					switch (pVideo.videoQuality) {
					case "HD": // HD
						// try 1080p/720p in selected format first. if it's not available than the other format will be used
						if ( "MPEG".equals(pVideo.videoFormat)) {
							iindex = addMPEG_HD_Urls(iindex, ssourcecodevideourls, pVideo);
						}

						if ( "WEBM".equals(pVideo.videoFormat)) {
							iindex = addWBEM_HD_Urls(iindex, ssourcecodevideourls, pVideo);
						}

						// there are no FLV HD URLs for now, so at least try mpg,wbem HD then
						iindex = addMPEG_HD_Urls(iindex, ssourcecodevideourls, pVideo);
						iindex = addWBEM_HD_Urls(iindex, ssourcecodevideourls, pVideo);

					case "Std": // SD
						// try to download desired format first, if it's not available we take the other of same res
						if ("MPEG".equals(pVideo.videoFormat)) {
							iindex = addMPEG_SD_Urls(iindex, ssourcecodevideourls, pVideo);
						}

						if ("WEBM".equals(pVideo.videoFormat)) {
							iindex = addWBEM_SD_Urls(iindex, ssourcecodevideourls, pVideo);
						}

						if ("FLV".equals(pVideo.videoFormat)) {
							iindex = addFLV_SD_Urls(iindex, ssourcecodevideourls, pVideo);
						}

						iindex = addMPEG_SD_Urls(iindex, ssourcecodevideourls, pVideo);
						iindex = addWBEM_SD_Urls(iindex, ssourcecodevideourls, pVideo);
						iindex = addFLV_SD_Urls(iindex, ssourcecodevideourls, pVideo);

					case "LD": // LD

						// TODO this.sFilenameResPart = "(LD)"; // adding LD to filename because HD-Videos are almost already named HD (?)
						if ("MPEG".equals(pVideo.videoFormat)) {
							iindex = addMPEG_LD_Urls(iindex, ssourcecodevideourls, pVideo);
						}

						if ("WEBM".equals(pVideo.videoFormat)) {
							// there are no wbem LD URLs for now
						}

						if ("FLV".equals(pVideo.videoFormat)) {
							iindex = addFLV_LD_Urls(iindex, ssourcecodevideourls, pVideo);
						}

						// we must ensure all (16) possible URLs get added to the list so that the list of URLs is never empty
						iindex = addMPEG_LD_Urls(iindex, ssourcecodevideourls, pVideo);
						iindex = addFLV_LD_Urls(iindex, ssourcecodevideourls, pVideo);

						break;
					default:
						// this.vNextVideoURL = null;
						this.sVideoURL = null;
						break;
					}

					// if the first 2 entries are null than there are no URLs for the selected resolution
					// strictly speaking this is only true for HD as there are only two URLs in contrast to three of SD - in this case the output will not be shown but downloading should work anyway
					if (this.vNextVideoURL.get(0).getsURL() == null && this.vNextVideoURL.get(1).getsURL() == null) {
						String smsg = "could not find video url for selected resolution! trying lower res...";
						output(smsg);
						debugoutput(smsg);
					}

					// remove null entries in list - we later try to download the first (index 0) and if it fails the next one (at index 1) and so on
					for (int x = this.vNextVideoURL.size() - 1; x >= 0; x--) {
						if (this.vNextVideoURL.get(x).getsURL() == null) {
							this.vNextVideoURL.remove(x);
						}
					}

					try {
						this.sVideoURL = this.vNextVideoURL.get(0).getsURL();
						debugoutput(String.format("trying this one: %s %s %s", this.vNextVideoURL.get(0).getsITAG(), this.vNextVideoURL.get(0).getsQUALITY(), this.vNextVideoURL.get(0).getsTYPE()));
					} catch (ArrayIndexOutOfBoundsException aioobe) {
					}

					this.setTitle(this.getTitle().concat(!this.vNextVideoURL.get(0).getsRESPART().equals("") ? "." + this.vNextVideoURL.get(0).getsRESPART() : ""));

				}

				if (iRecursionCount == 0 && sline.matches("(.*)<meta name=\"title\" content=(.*)")) {
					String stmp = sline.replaceFirst("(.*)<meta name=\"title\" content=", "").trim();
					// change html characters to their UTF8 counterpart
					stmp = UTF8Utils.changeHTMLtoUTF8(stmp);
					stmp = stmp.replaceFirst("^\"", "").replaceFirst("\">$", "");

					// http://msdn.microsoft.com/en-us/library/windows/desktop/aa365247%28v=vs.85%29.aspx
					//
					stmp = stmp.replaceAll("<", "");
					stmp = stmp.replaceAll(">", "");
					stmp = stmp.replaceAll(":", "");
					stmp = stmp.replaceAll("/", " ");
					stmp = stmp.replaceAll("\\\\", " ");
					stmp = stmp.replaceAll("|", "");
					stmp = stmp.replaceAll("\\?", "");
					stmp = stmp.replaceAll("\\*", "");
					stmp = stmp.replaceAll("/", " ");
					stmp = stmp.replaceAll("\"", " ");
					stmp = stmp.replaceAll("%", "");

					this.setTitle(stmp); // complete file name without path
				}

			} catch (NullPointerException npe) {
			}
		} // while
		return rc;
	} // savetextdata()

	void savebinarydata(BufferedInputStream binaryreader, Long iBytesMax) throws IOException {
		FileOutputStream fos = null;
		try {
			File f;
			Integer idupcount = 0;
			String sdirectorychoosed = YtdConfigManager.getInstance().getSaveDirectoryPath();

			String sfilename = this.getTitle();
			debugoutput("title: ".concat(this.getTitle()).concat("sfilename: ").concat(sfilename));
			do {
				f = new File(sdirectorychoosed, sfilename.concat((idupcount > 0 ? "(".concat(idupcount.toString()).concat(")") : "")).concat(".").concat(this.sContentType.replaceFirst("video/", "").replaceAll("x-", "")));
				idupcount += 1;
			} while (f.exists());
			this.setFileName(f.getAbsolutePath());

			Long iBytesReadSum = (long) 0;
			Long iPercentage = (long) -1;
			fos = new FileOutputStream(f);

			debugoutput(String.format("writing %d bytes to: %s", iBytesMax, this.getFileName()));
			output(("file size of \"").concat(this.getTitle()).concat("\" = ").concat(iBytesMax.toString()).concat(" Bytes").concat(" ~ ").concat(Long.toString((iBytesMax / 1024)).concat(" KiB")).concat(" ~ ").concat(Long.toString((iBytesMax / 1024 / 1024)).concat(" MiB")));

			byte[] bytes = new byte[4096];
			Integer iBytesRead = 1;

			// adjust blocks of percentage to output - larger files are shown with smaller pieces
			Integer iblocks = 10;
			if (iBytesMax > 20 * 1024 * 1024) {
				iblocks = 4;
			}
			if (iBytesMax > 32 * 1024 * 1024) {
				iblocks = 2;
			}
			if (iBytesMax > 56 * 1024 * 1024) {
				iblocks = 1;
			}
			while (!isWorkerInterrupted() && iBytesRead > 0) {
				iBytesRead = binaryreader.read(bytes);
				iBytesReadSum += iBytesRead;
				// drop a line every x% of the download
				if ((((iBytesReadSum * 100 / iBytesMax) / iblocks) * iblocks) > iPercentage) {
					iPercentage = (((iBytesReadSum * 100 / iBytesMax) / iblocks) * iblocks);
					processDownloadEvent(new DownloadEvent(this, iThreadNo, DOWNLOAD_STATUS.DOWNLOADING, iPercentage.intValue(), sURL, null));
				}
				// TODO calculate and show ETA for bigger downloads (remaining time > 60s) - every 20%?

				try {
					fos.write(bytes, 0, iBytesRead);
				} catch (IndexOutOfBoundsException ioob) {
				}
			}

			// rename files if download was interrupted before completion of download
			if (isWorkerInterrupted() && iBytesReadSum < iBytesMax) {
				try {
					// this part is especially for our M$-Windows users because of the different behavior of File.renameTo() in contrast to non-windows
					// see http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6213298 and others
					// even with Java 1.6.0_22 the renameTo() does not work directly on M$-Windows!
					fos.close();
				} catch (Exception e) {
				}
				// System.gc(); // we don't have to do this but to be sure the file handle gets released we do a thread sleep
				try {
					Thread.sleep(50);
				} catch (InterruptedException e) {
				}
				// this part runs on *ix platforms without closing the FileOutputStream explicitly
				debugoutput(String.format("download canceled. (%d)", (iBytesRead)));
				changeFileNamewith("CANCELED.");
				String smsg = "renaming unfinished file to: ".concat(this.getFileName());
				output(smsg);
				debugoutput(smsg);

				// CANCELED filenames overwrite others as we do not test for CANCELED one, two...
				if (!f.renameTo(new File(this.getFileName()))) {
					smsg = "error renaming unfinished file to: ".concat(this.getFileName());
					output(smsg);
					debugoutput(smsg);
				}
			}
			debugoutput("done writing.");
		} catch (FileNotFoundException fnfe) {
			throw (fnfe);
		} catch (IOException ioe) {
			debugoutput("IOException");
			throw (ioe);
		} finally {
			this.sVideoURL = null;
			try {
				fos.close();
			} catch (Exception e) {
			}
		}
	} 

	void changeFileNamewith(String string) {
		File f = null;
		Integer idupcount = 0;
		String sfilesep = System.getProperty("file.separator");
		if (sfilesep.equals("\\")) {
			sfilesep += sfilesep; // on m$-windows we need to escape the \
		}

		String sdirectorychoosed = "";
		String[] srenfilename = this.getFileName().split(sfilesep);

		try {
			for (int i = 0; i < srenfilename.length - 1; i++) {
				sdirectorychoosed += srenfilename[i].concat((i < srenfilename.length - 1) ? sfilesep : ""); // constructing folder where file is saved now (could be changed in GUI already)
			}
		} catch (ArrayIndexOutOfBoundsException aioobe) {
		}

		String sfilename = srenfilename[srenfilename.length - 1];
		debugoutput("changeFileNamewith() sfilename: ".concat(sfilename));
		do {
			// filename will be prepended with a parameter string and possibly a duplicate counter
			f = new File(sdirectorychoosed, string.concat((idupcount > 0 ? "(".concat(idupcount.toString()).concat(")") : "")).concat(sfilename));
			idupcount += 1;
		} while (f.exists());

		debugoutput("changeFileNamewith() new filename: ".concat(f.getAbsolutePath()));
		this.setFileName(f.getAbsolutePath());

	}

	String getTitle() {
		if (this.sTitle != null) {
			return this.sTitle;
		} else {
			return ("");
		}
	}

	void setTitle(String sTitle) {
		this.sTitle = sTitle;
	}

	String getFileName() {
		if (this.sFileName != null) {
			return this.sFileName;
		} else {
			return ("");
		}
	}

	void setFileName(String sFileName) {
		this.sFileName = sFileName;
	}

	synchronized void debugoutput(String s) {
		if (!isDebug) {
			return;
		}
		// sometimes this happens: Exception in thread "Thread-2" java.lang.Error: Interrupted attempt to aquire write lock (on quit only)
		try {
			GUIClient.addTextToConsole("#DEBUG ".concat(this.getMyName()).concat(" ").concat(s));
		} catch (Exception e) {
			try {
				Thread.sleep(50);
			} catch (InterruptedException e1) {
			}
			try {
				GUIClient.addTextToConsole("#DEBUG ".concat(this.getMyName()).concat(" ").concat(s));
			} catch (Exception e2) {
			}
		}
	}

	void output(String s) {
		if (isDebug) {
			return;
		}
		GUIClient.addTextToConsole("#info - ".concat(s));
	}

	String getMyName() {
		return this.getClass().getName().concat(Integer.toString(this.iThreadNo));
	}

	@Override
	public void run() {
		boolean lbDownloadOk = false;
		boolean lbRun = true;
		while (lbRun && !isWorkerInterrupted()) {
			try {
				VideoElement lUrl = mUrlList.getElement();
				
				if(lUrl != null){
					sURL = lUrl.videoUrl;
					this.processDownloadEvent(new DownloadEvent(this, iThreadNo, DOWNLOAD_STATUS.STARTED, 0, sURL, null));
						
					// download one webresource and show result
					lbDownloadOk = downloadone(sURL, lUrl);
					this.iRecursionCount = -1;
					if (lbDownloadOk && !this.mForceNoDownload) {
						output(("download complete: ").concat("\"").concat(this.getTitle()).concat("\"").concat(" to ").concat(this.getFileName()));
						this.processDownloadEvent(new DownloadEvent(this, iThreadNo, DOWNLOAD_STATUS.COMPLETED, 0, sURL, null));
					} else {
						if(!mForceNoDownload){
							this.processDownloadEvent(new DownloadEvent(this, iThreadNo, DOWNLOAD_STATUS.FAILED, 0, sURL, null));
						}else{
							this.processDownloadEvent(new DownloadEvent(this, iThreadNo, DOWNLOAD_STATUS.COMPLETED_NOT_DOWNLOAD, 0, sURL, null));
						}
					}
				}else{
					this.processDownloadEvent(new DownloadEvent(this, iThreadNo, DOWNLOAD_STATUS.IDLE, 0, sURL, null));
				}

			} catch (InterruptedException e) {
				lbRun = false;
			}catch(Throwable t){
				t.printStackTrace();
				lbRun = false;
			}
		}
		debugoutput("thread ended: ".concat(this.getMyName()));
		this.processDownloadEvent(new DownloadEvent(this, iThreadNo, DOWNLOAD_STATUS.ABORTING, 0, null, null));
		YTDownloadThread.iThreadcount--;
	}

	private boolean isWorkerInterrupted(){
		boolean isInterrupted = isInterrupted();
		
		return isInterrupted;
	}
	
	@SuppressWarnings("unchecked")
	private void processDownloadEvent(DownloadEvent pDnlEvent) {
		List<DownloaderListener> lDnlListenerList;

		synchronized (this) {
			if (mDnlListenerList.size() == 0) {
				return;
			}
			lDnlListenerList = (ArrayList<DownloaderListener>) mDnlListenerList.clone();
		}

		for (DownloaderListener lListener : lDnlListenerList) {
			DOWNLOAD_STATUS lStatus = pDnlEvent.getStatus();
			switch(lStatus){
			case STARTED:
				lListener.downloadStarted(pDnlEvent);
				output(("trying to download: ").concat(this.sURL));
				break;
			case DOWNLOADING:
				lListener.downloadProgress(pDnlEvent);
				break;
			case FAILED:
				output(("download failed: ").concat("\"").concat(this.getTitle()).concat("\"")); // not downloaded does not mean it was erroneous
				lListener.downloadFailure(pDnlEvent);
				break;
			case COMPLETED:
				lListener.downloadCompleted(pDnlEvent);
				break;
			case COMPLETED_NOT_DOWNLOAD:
				output(("not downloaded: ").concat("\"").concat(this.getTitle()).concat("\"")); // not downloaded does not mean it was erroneous
				lListener.downloadCompletedNotDownloaded(pDnlEvent);
				break;
			case IDLE:
				lListener.threadIdle(pDnlEvent);
				break;
			case ABORTING:
				lListener.threadAborted(pDnlEvent);
			default:
				break;
			}
		}
	}

}