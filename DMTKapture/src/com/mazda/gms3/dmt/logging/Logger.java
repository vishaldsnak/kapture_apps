package com.mazda.gms3.dmt.logging;

//import org.apache.log4j.Logger;

/**
 * This class acts as a wrapper to the log Interface/ Object to have some basic
 * checks and common managing point for the logging system
 */
public class Logger extends org.apache.log4j.Logger {

	/**
	 * 
	 */
	private org.apache.log4j.Logger log;
	private String className;

	public Logger(String name) {
		super(name);
		log = org.apache.log4j.LogManager.getLogger(name);
	}

	/**
	 * 
	 * @param obj
	 */
	public void debug(Object obj) {
		if (!this.isLoggingAvailable()) {
			System.out.println(this.className + " : " + obj);
		} else {
			// debug enabled check in actual code
			this.log.debug(obj);
		}

	}

	/**
	 * 
	 * @param obj
	 */
	public void warn(Object obj) {
		if (!this.isLoggingAvailable()) {
			System.out.println(this.className + " : " + obj);
		} else {
			if (this.log.isInfoEnabled()) {
				this.log.warn(obj);
			}
		}
	}

	/**
	 * 
	 * @param obj
	 */
	public void error(Object obj) {
		if (!this.isLoggingAvailable()) {
			System.out.println(this.className + " : " + obj);
		} else {
			if (this.log.isTraceEnabled()) {
				this.log.error(obj);
			}
		}
	}

	/**
	 * 
	 * @param obj
	 */
	public void fatal(Object obj) {
		if (!this.isLoggingAvailable()) {
			System.out.println(this.className + " : " + obj);
		} else {
			if (this.log.isTraceEnabled()) {
				this.log.fatal(obj);
			}
		}
	}

	/**
	 * 
	 * @param obj
	 */
	public void info(Object obj) {
		if (!this.isLoggingAvailable()) {
			System.out.println(this.className + " : " + obj);
		} else {
			if (this.log.isInfoEnabled()) {
				this.log.info(obj);
			}
		}
	}

	/**
	 * 
	 * @param obj
	 * @param thrbl
	 */
	public void debug(Object obj, Throwable thrbl) {
		if (!this.isLoggingAvailable()) {
			System.out.println(this.className + " : " + obj);
		} else {
			if (this.log.isDebugEnabled()) {
				this.log.debug(obj, thrbl);
			}
		}
	}

	/**
	 * 
	 * @param obj
	 * @param thrbl
	 */
	public void warn(Object obj, Throwable thrbl) {
		if (!this.isLoggingAvailable()) {
			System.out.println(this.className + " : " + obj);
		} else {
			if (this.log.isInfoEnabled()) {
				this.log.warn(obj, thrbl);
			}
		}
	}

	/**
	 * 
	 * @param obj
	 * @param thrbl
	 */
	public void error(Object obj, Throwable thrbl) {
		if (!this.isLoggingAvailable()) {
			System.out.println(this.className + " : " + obj);
		} else {
			if (this.log.isTraceEnabled()) {
				this.log.error(obj, thrbl);
			}
		}
	}

	/**
	 * 
	 * @param obj
	 * @param thrbl
	 */
	public void fatal(Object obj, Throwable thrbl) {
		if (!this.isLoggingAvailable()) {
			System.out.println(this.className + " : " + obj);
		} else {
			if (this.log.isTraceEnabled()) {
				this.log.fatal(obj, thrbl);
			}
		}
	}

	public void info(Object obj, Throwable thrbl) {
		if (!this.isLoggingAvailable()) {
			System.out.println(this.className + " : " + obj);
		} else {
			if (this.log.isInfoEnabled()) {
				this.log.info(obj, thrbl);
			}
		}
	}

	private boolean isLoggingAvailable() {
		if (this.log == null) {
			return false;
		}
		return true;
	}

	public boolean isDebugEnabled() {
		if (this.log != null) {
			return this.log.isDebugEnabled();
		}
		return false;
	}

	public boolean isInfoEnabled() {
		if (this.log != null) {
			return this.log.isInfoEnabled();
		}
		return false;
	}
}
