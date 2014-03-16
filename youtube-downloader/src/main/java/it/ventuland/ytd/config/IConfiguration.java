package it.ventuland.ytd.config;

public interface IConfiguration {

	String getSaveDirectoryPath();
	void setSaveDirectory(String pSaveDirectoryPath);
	void saveConfiguration(String pProxy, int pDownloadSelectedState, Boolean pMpegState);
}
