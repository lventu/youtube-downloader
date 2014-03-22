package it.ventuland.ytd.utils;

public interface IOperation<T> {

	public T execute( T pInput ) throws Exception;
	
}
