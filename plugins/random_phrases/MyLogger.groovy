class MyLogger {
	
	private static MyLoggerImpl impl = { text -> System.err.println(text) }
	
	static void setImplementation(MyLoggerImpl impl) {
		MyLogger.impl = impl
	}
	
	static void log(String text) {
		impl.write(text)
	}
	
	static interface MyLoggerImpl {
		
		void write(String text)
		
	}
	
}
