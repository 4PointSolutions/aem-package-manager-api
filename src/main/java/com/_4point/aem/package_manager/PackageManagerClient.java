package com._4point.aem.package_manager;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;

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
	
	private PackageManagerClient(AemConfig aemConfig) {
		this.listAllPackagesClient = new JerseyRestClient(aemConfig, "/crx/packmgr/service.jsp");
		this.commandPackageClient = new JerseyUntargettedRestClient(aemConfig);
		this.uploadPackageClient = new JerseyRestClient(aemConfig, "/crx/packmgr/service/.json");;
	}
	
	// List all packages
	// curl -u <user>:<password> http://<host>:<port>/crx/packmgr/service.jsp?cmd=ls
	public ListResponse listPackages() {
		try {
			Optional<Response> fromServer = this.listAllPackagesClient.getRequestBuilder()
									  .queryParam("cmd", "ls")
									  .build()
									  .getFromServer(ContentType.of("text/plain"));	// Not sure why AEM returnes "text/plain" when it is clearly XML.
			
			return ListResponse.from(XmlDocument.initializeXmlDoc(fromServer.orElseThrow().data().readAllBytes()));
		} catch (RestClientException | IOException e) {
			throw new PackageManagerException("Error while listing packages.", e);
		}
	}
	
	// Upload a package
	// curl -u <user>:<password> -F cmd=upload -F force=true -F package=@test.zip http://localhost:4502/crx/packmgr/service/.json
	public CommandResponse uploadPackage(String packageFilename, Path file) {
		try {
			Optional<Response> fromServer = this.uploadPackageClient.multipartPayloadBuilder()
							.add("cmd", "upload")
							.add("force", "true")
							.add("package", file, ContentType.APPLICATION_OCTET_STREAM)
							.build()
							.postToServer(ContentType.APPLICATION_JSON);
			return CommandResponse.from(JsonData.from(new String(fromServer.orElseThrow().data().readAllBytes())));
		} catch (RestClientException | IOException e) {
			throw new PackageManagerException("Error while uploading pacakge(" + packageFilename + ").", e);
		}
	}

	// Install a package
	// curl -u <user>:<password> -F cmd=install http://localhost:4502/crx/packmgr/service/.json/etc/packages/my_packages/test.zip
	public CommandResponse installPackage(String group, String packageFilename) {
		return executePackageCommand("install", group, packageFilename);
	}

	// Uninstall a package
	// curl -u <user>:<password> -F cmd=uninstall http://localhost:4502/crx/packmgr/service/.json/etc/packages/my_packages/test.zip
	public CommandResponse uninstallPackage(String group, String packageFilename) {
		return executePackageCommand("uninstall", group, packageFilename);
	}

	// Delete a package
	// curl -u <user>:<password> -F cmd=delete http://localhost:4502/crx/packmgr/service/.json/etc/packages/my_packages/test.zip
	public CommandResponse deletePackage(String group, String packageFilename) {
		return executePackageCommand("delete", group, packageFilename);
	}

	public CommandResponse executePackageCommand(String command, String group, String packageFilename) {
		try {
			RestClient restClient = this.commandPackageClient.target("/crx/packmgr/service/.json/etc/packages/" + group + "/" + packageFilename);
			Optional<Response> fromServer = restClient.multipartPayloadBuilder()
													  .add("cmd", command)
													  .build()
													  .postToServer(ContentType.APPLICATION_JSON);
			return CommandResponse.from(JsonData.from(new String(fromServer.orElseThrow().data().readAllBytes())));
		} catch (RestClientException | IOException e) {
			throw new PackageManagerException("Error while uninstalling pacakge(" + packageFilename + ") from group '" + group + "'.", e);
		}
	}

	public static PackageManagerBuilder builder() {
		return new PackageManagerBuilder();
	}

	public static class PackageManagerBuilder {
		private final SimpleAemConfigBuilder aemConfigBuilder = new SimpleAemConfigBuilder();
		
		public PackageManagerBuilder serverName(String serverName) {
			aemConfigBuilder.serverName(serverName);
			return this;
		}

		public PackageManagerBuilder port(Integer port) {
			aemConfigBuilder.port(port);
			return this;
		}

		public PackageManagerBuilder user(String ussr) {
			aemConfigBuilder.ussr(ussr);
			return this;
		}

		public PackageManagerBuilder password(String password) {
			aemConfigBuilder.password(password);
			return this;
		}

		public PackageManagerBuilder useSsl(Boolean useSsl) {
			aemConfigBuilder.useSsl(useSsl);
			return this;
		}

		public PackageManagerClient build() {
			return new PackageManagerClient(aemConfigBuilder.build());
		}

		public PackageManagerClientEx buildEx() {
			return PackageManagerClientEx.from(new PackageManagerClient(aemConfigBuilder.build()));
		}
	}
	
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
