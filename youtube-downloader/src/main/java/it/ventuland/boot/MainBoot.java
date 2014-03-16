package it.ventuland.boot;
import it.ventuland.ytd.ui.GUIClient;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;


public class MainBoot {

	private static Configuration mConfig;
	
	static{
		try {
			mConfig = new PropertiesConfiguration("application.properties");
		} catch (ConfigurationException e) {
			e.printStackTrace();
		}
	}
	
	MainBoot(String[] args){
		new GUIClient(mConfig);
	}
	
	public static void main(String[] args) {
		
		new MainBoot(args);
		
	}

}
