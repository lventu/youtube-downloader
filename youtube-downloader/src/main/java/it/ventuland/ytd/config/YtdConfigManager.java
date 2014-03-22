package it.ventuland.ytd.config;

import java.io.File;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.XMLConfiguration;

public class YtdConfigManager implements IConfiguration {

	private static final String VIDEOQUALITY_PROPERTY = "videoquality";
	private static final String VIDEORESOLUTION_PROPERTY = "videoresolution";
	private static final String TARGET_FOLDER_PROPERTY = "targetfolder";
	private static final String HTTP_PROXY_PROPERTY = "http_proxy";
	private static final String SAVEFOLDER_PROPERTY = "savefolder";
	final static String CONFIG_LOCATION = System.getProperty("user.home") + "ytd2.config.xml";
	private XMLConfiguration mXmlConfig;
	
	private static volatile YtdConfigManager iConfigurationManger = null;
	public static synchronized IConfiguration getInstance(){
		if(iConfigurationManger==null){
			iConfigurationManger = new YtdConfigManager();
		}
		return iConfigurationManger;
	}
	
	private YtdConfigManager() {
		initialize();
	}

	protected void initialize() {
		try {
			mXmlConfig = new XMLConfiguration(CONFIG_LOCATION);
		} catch (ConfigurationException e) {
			mXmlConfig = new XMLConfiguration();
			File lConfFile = new File(CONFIG_LOCATION);
			if(mXmlConfig.getFile()==null){
				mXmlConfig.setFile(lConfFile);
			}
		}
	}

	@Override
	public String getSaveDirectoryPath() {
		return mXmlConfig.getString(SAVEFOLDER_PROPERTY);
	}
	
	@Override
	public void saveConfiguration(String pSaveDirectoryPath, String pProxy, String pVideoResolution, String pVideoQuality){
		mXmlConfig.setProperty(HTTP_PROXY_PROPERTY, pProxy);
		mXmlConfig.setProperty(TARGET_FOLDER_PROPERTY, getSaveDirectoryPath() );
		mXmlConfig.setProperty(VIDEORESOLUTION_PROPERTY, pVideoResolution);
		mXmlConfig.setProperty(VIDEOQUALITY_PROPERTY, pVideoQuality);
		mXmlConfig.setProperty(SAVEFOLDER_PROPERTY, pSaveDirectoryPath);
		try {
			mXmlConfig.save();
		} catch (ConfigurationException e) {
		}
	}
	
}
