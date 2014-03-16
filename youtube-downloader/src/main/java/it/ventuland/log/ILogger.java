package it.ventuland.log;

public interface ILogger {

	public enum LogLevel{
		DEBUG, WARNING, ERROR;
	}
	
	public void debug(String pMessage, Object... args);
	
	public void warning(String pMessage, Object... args);
	
	public void error(String pMessage, Object... args);
	
}
