package it.ventuland.log;

public class TestLogger implements ILogger {

	@Override
	public void debug(String pMessage, Object... args) {
		log(LogLevel.DEBUG, pMessage, args);
	}

	@Override
	public void warning(String pMessage, Object... args) {
		log(LogLevel.WARNING, pMessage, args);
	}

	@Override
	public void error(String pMessage, Object... args) {
		log(LogLevel.ERROR, pMessage, args);
	}

	private void log(LogLevel pLevel, String pMessage, Object... args){
		System.out.println(pLevel.toString()+": "+String.format(pMessage, args));
	}
	
}
