package it.ventuland.ytd.exception;

public class BaseRuntimeException extends RuntimeException {
	private static final long serialVersionUID = -5592266248544140844L;

	public BaseRuntimeException(String message, Object...args) {
		initialize(message, null, args);
	}
	
	public BaseRuntimeException(String message, Throwable e, Object...args) {
		initialize(message, e, args);
	}

	protected void initialize(String message, Throwable e, Object[] args) {
		final String msg = String.format(message, args);
		fpLog(msg, e);
		
	}
	
	private void fpLog(String msg, Throwable e){
		System.out.print(msg);
		e.printStackTrace();
	}
	
}
