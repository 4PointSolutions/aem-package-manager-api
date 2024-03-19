package com._4point.aem.package_manager;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;
import java.util.function.Consumer;

import com._4point.aem.package_manager.AemConfig.SimpleAemConfigBuilder;
import com._4point.aem.package_manager.rest_client.RestClient;
import com._4point.aem.package_manager.rest_client.RestClient.ContentType;
import com._4point.aem.package_manager.rest_client.RestClient.Response;
import com._4point.aem.package_manager.rest_client.RestClient.RestClientException;
import com._4point.aem.package_manager.rest_client.RestClient.UntargettedRestClient;
import com._4point.aem.package_manager.rest_client.jersey.JerseyRestClient;
import com._4point.aem.package_manager.rest_client.jersey.JerseyRestClient.JerseyUntargettedRestClient;

/**
 * PackageManagerClient provides a client that lets someone perform operations on a remote AEM server
 * using REST calls to Package Manager.
 * 
 * Operations include:
 *    * Listing the packages installed on the system
 *    * Uploading and installing a new version of a package.
 * 
 * This is based in this Adobe documentation:
 * https://experienceleague.adobe.com/docs/experience-manager-65/content/sites/administering/operations/curl.html?lang=en
 * 
 */
public class PackageManagerClient {
	private final RestClient listAllPackagesClient;
	private final UntargettedRestClient commandPackageClient;
	private final RestClient uploadPackageClient;
	private final Logger logger;
	
	private PackageManagerClient(AemConfig aemConfig, Logger logger) {
		this.listAllPackagesClient = new JerseyRestClient(aemConfig, "/crx/packmgr/service.jsp");
		this.commandPackageClient = new JerseyUntargettedRestClient(aemConfig);
		this.uploadPackageClient = new JerseyRestClient(aemConfig, "/crx/packmgr/service/.json");
		this.logger = logger;;
	}
	
	// List all packages
	// curl -u <user>:<password> http://<host>:<port>/crx/packmgr/service.jsp?cmd=ls
	/**
	 * List all the packages on the AEM instance.
	 * 
	 * @return A ListResponse object containing all the data from the response.
	 * @throws PackageManagerException if a network/IO exception occurs. 
	 */
	public ListResponse listPackages() {
		try {
			logger.log("Listing packages");
			Optional<Response> fromServer = this.listAllPackagesClient.getRequestBuilder()
									  .queryParam("cmd", "ls")
									  .build()
									  .getFromServer(ContentType.of("text/plain"));	// Not sure why AEM returnes "text/plain" when it is clearly XML.
			
			ListResponse listResponse = ListResponse.from(XmlDocument.initializeXmlDoc(fromServer.orElseThrow().data().readAllBytes()));
			logger.log(()->"  Found " + listResponse.packages().size() + " packages");
			return listResponse;
		} catch (RestClientException | IOException e) {
			throw new PackageManagerException("Error while listing packages.", e);
		}
	}
	
	// Upload a package
	// curl -u <user>:<password> -F cmd=upload -F force=true -F package=@test.zip http://localhost:4502/crx/packmgr/service/.json
	/**
	 * Upload a package to the AEM instance.
	 * 
	 * @param packageFilename Name of the package (which can be different than the filename)
	 * @param file Path to the file that will be uploaded
	 * @return
	 */
	public CommandResponse uploadPackage(String packageFilename, Path file) {
		try {
			logger.log(()->"Uploading Package '" + packageFilename + "'");
			Optional<Response> fromServer = this.uploadPackageClient.multipartPayloadBuilder()
							.add("cmd", "upload")
							.add("force", "true")
							.add("package", file, ContentType.APPLICATION_OCTET_STREAM)
							.build()
							.postToServer(ContentType.APPLICATION_JSON);
			CommandResponse commandResponse = CommandResponse.from(JsonData.from(new String(fromServer.orElseThrow().data().readAllBytes())));
			logger.log(()->"  Package " + (commandResponse.success() ? "uploaded successfully" : "not uploaded"));
			return commandResponse;
		} catch (RestClientException | IOException e) {
			throw new PackageManagerException("Error while uploading pacakge(" + packageFilename + ").", e);
		}
	}

	/**
	 * Upload a package. Uses the filename as the package name.
	 * 
	 * @param file Path to the file that will be uploaded
	 * @return Response from the AEM instance
	 */
	public CommandResponse uploadPackage(Path file) {
		return uploadPackage(file.getFileName().toString(), file);
	}

	// Install a package
	// curl -u <user>:<password> -F cmd=install http://localhost:4502/crx/packmgr/service/.json/etc/packages/my_packages/test.zip
	/**
	 * Install a package.
	 * 
	 * @param group group name of the package (as determined in the pom.xml used to create the package)
	 * @param packageFilename packageFilename (must match ths package filename provided when the package was uploaded)
	 * @return Response from the AEM instance
	 */
	public CommandResponse installPackage(String group, String packageFilename) {
		return executePackageCommand("install", group, packageFilename);
	}

	// Uninstall a package
	// curl -u <user>:<password> -F cmd=uninstall http://localhost:4502/crx/packmgr/service/.json/etc/packages/my_packages/test.zip
	/**
	 * Uninstall a package.
	 * 
	 * @param group group name of the package (as determined in the pom.xml used to create the package)
	 * @param packageFilename packageFilename (must match ths package filename provided when the package was uploaded)
	 * @return Response from the AEM instance
	 */
	public CommandResponse uninstallPackage(String group, String packageFilename) {
		return executePackageCommand("uninstall", group, packageFilename);
	}

	// Delete a package
	// curl -u <user>:<password> -F cmd=delete http://localhost:4502/crx/packmgr/service/.json/etc/packages/my_packages/test.zip
	/**
	 * Delete a package.
	 * 
	 * @param group group name of the package (as determined in the pom.xml used to create the package)
	 * @param packageFilename packageFilename (must match ths package filename provided when the package was uploaded)
	 * @return Response from the AEM instance
	 */
	public CommandResponse deletePackage(String group, String packageFilename) {
		return executePackageCommand("delete", group, packageFilename);
	}

	private CommandResponse executePackageCommand(String command, String group, String packageFilename) {
		try {
			logger.log(()->"Executing " + command + " package on '" + packageFilename + "'");
			RestClient restClient = this.commandPackageClient.target("/crx/packmgr/service/.json/etc/packages/" + group + "/" + packageFilename);
			Optional<Response> fromServer = restClient.multipartPayloadBuilder()
													  .add("cmd", command)
													  .build()
													  .postToServer(ContentType.APPLICATION_JSON);
			CommandResponse commandResponse = CommandResponse.from(JsonData.from(new String(fromServer.orElseThrow().data().readAllBytes())));
			logger.log(()->"  " + command + " completed " + (commandResponse.success() ? "successfully" : "unsuccessfully") );
			return commandResponse;
		} catch (RestClientException | IOException e) {
			throw new PackageManagerException("Error while performing '" + command + "' on package '" + packageFilename + "' from group '" + group + "'.", e);
		}
	}

	/**
	 * Provides a builder object for creating a PackageManagerClient instance.
	 * 
	 * @return A PackageManagerBuilder object used for configuring/creating a PackageManagerClient instance.
	 */
	public static PackageManagerBuilder builder() {
		return new PackageManagerBuilder();
	}

	/**
	 * Builder object for configuring/creating a PackageManagerClient instance.
	 * 
	 * 
	 */
	public static class PackageManagerBuilder {
		private final SimpleAemConfigBuilder aemConfigBuilder = new SimpleAemConfigBuilder();
		private Logger logger = new Logger.NoOpLogger();
		
		/**
		 * Set the machine name where the AEM instance resides.
		 * 
		 * @param serverName
		 * @return
		 */
		public PackageManagerBuilder serverName(String serverName) {
			aemConfigBuilder.serverName(serverName);
			return this;
		}

		/**
		 * Set the port that AEM is listening on
		 * 
		 * @param port
		 * @return
		 */
		public PackageManagerBuilder port(Integer port) {
			aemConfigBuilder.port(port);
			return this;
		}

		/**
		 * Set the user that will be used to authenticate with the AEM server.
		 * 
		 * @param ussr
		 * @return
		 */
		public PackageManagerBuilder user(String ussr) {
			aemConfigBuilder.ussr(ussr);
			return this;
		}

		/**
		 * Set the password that will be used to authenticate with the AEM server.
		 * 
		 * @param password
		 * @return
		 */
		public PackageManagerBuilder password(String password) {
			aemConfigBuilder.password(password);
			return this;
		}

		/**
		 * Indicate whether the connection to the AEM server will utilize TLS.
		 * 
		 * Note: The certificate used by the AEM server must be trusted.
		 * 
		 * @param useSsl true - use https connection, or false - use a regular http connection. 
		 * @return
		 */
		public PackageManagerBuilder useSsl(Boolean useSsl) {
			aemConfigBuilder.useSsl(useSsl);
			return this;
		}

		/**
		 * Accepts a Consumer that will be used to publish logging messages.
		 * 
		 * If this is not supplied, then no logging will occur,
		 * 
		 * @param msgConsumer
		 * @return
		 */
		public PackageManagerBuilder logger(Consumer<? super String> msgConsumer) {
			this.logger = new Logger.PassThroughLogger(msgConsumer);
			return this;
		}
		
		/**
		 * Build a PackageManagerClient instance.
		 * 
		 * @return new PackageManagerClient instance
		 */
		public PackageManagerClient build() {
			return new PackageManagerClient(aemConfigBuilder.build(), logger);
		}

		/**
		 * Build a PackageManagerClientEx instance.
		 * 
		 * @return new PackageManagerClientEx instance
		 */
		public PackageManagerClientEx buildEx() {
			return PackageManagerClientEx.from(new PackageManagerClient(aemConfigBuilder.build(), logger));
		}
	}
	
	/**
	 * Package Manager Exceptions
	 * 
	 * All exceptions produced by objects in this library throw this kind of error.
	 * 
	 */
	@SuppressWarnings("serial")
	public static class PackageManagerException extends RuntimeException {

		PackageManagerException() {
		}

		PackageManagerException(String message, Throwable cause) {
			super(message, cause);
		}

		PackageManagerException(String message) {
			super(message);
		}

		PackageManagerException(Throwable cause) {
			super(cause);
		}
	}
}
