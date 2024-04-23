package com._4point.aem.package_manager;

import java.util.Objects;

/**
 * AEM Configuration Parameters
 */
public interface AemConfig {
	String servername()	;
	Integer port();
	String user();
	String password();
	Boolean useSsl();

	default public String url() {
		return "http" + (useSsl() ? "s" : "") + "://" + servername() + (port() != 80 ? ":" + port() : "") + "/";
	}
	
	public static SimpleAemConfigBuilder builder() { return new SimpleAemConfigBuilder(); }
	
	/**
	 * Simple AEM Configuration Implementation (just uses a record as backing store)
	 * 
	 * @param servername server name String
	 * @param port port number (Integer)
	 * @param user user String
	 * @param password password String
	 * @param useSsl boolean true = use https, false = use http
	 * 
	 */
	record SimpleAemConfig(String servername, Integer port, String user, String password, Boolean useSsl) implements AemConfig {} ;

	/**
	 * Builder class for a Simple AEM Configuration.
	 * 
	 * The builder class supplies defaults settings for typical development environments (i.e. servername is localhost,
	 * port = 4502, default user/password, and no SSL). 
	 * 
	 */
	public class SimpleAemConfigBuilder {
		private String serverName = "localhost";
		private Integer port = 4502;
		private String ussr = "admin";
		private String password = "admin";
		private Boolean useSsl = Boolean.FALSE;

		/**
		 * Set the server name
		 * 
		 * @param serverName server name String
		 * @return builder object
		 */
		public SimpleAemConfigBuilder serverName(String serverName) {
			this.serverName = serverName;
			return this;
		}

		/**
		 * Set the port that will be used to call AEM.
		 * 
		 * @param port port number (Integer)
		 * @return builder object
		 */
		public SimpleAemConfigBuilder port(Integer port) {
			this.port = port;
			return this;
		}

		/**
		 * Set the user name used to access AEM.
		 * 
		 * @param user user String
		 * @return builder object
		 */
		public SimpleAemConfigBuilder ussr(String user) {
			this.ussr = user;
			return this;
		}

		/**
		 * Set the password for the user used to access AEM.
		 * 
		 * @param password password String
		 * @return builder object
		 */
		public SimpleAemConfigBuilder password(String password) {
			this.password = password;
			return this;
		}

		/**
		 * Set whether or not to use SSL when talking to AEM.
		 * 
		 * @param useSsl boolean true = use https, false = use http
		 * @return builder object
		 */
		public SimpleAemConfigBuilder useSsl(Boolean useSsl) {
			this.useSsl = useSsl;
			return this;
		}

		/**
		 * Build the AemConfig object.
		 * 
		 * @return
		 */
		public AemConfig build() {
			return new SimpleAemConfig(
					Objects.requireNonNull(serverName, "Servername cannot be null"),
					Objects.requireNonNull(port,"Port cannot be null"),
					Objects.requireNonNull(ussr, "User cannot be null"),
					Objects.requireNonNull(password, "Password cannot be null"),
					Objects.requireNonNull(useSsl,"UseSSL cannot be null")
					);
		}
	}
}
