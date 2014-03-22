package it.ventuland.ytd.config;

public interface IConfiguration {

	String getSaveDirectoryPath();
	void saveConfiguration(String pSaveDirectoryPath, String pProxy, String pVideoResolution, String pVideoQuality);
}
