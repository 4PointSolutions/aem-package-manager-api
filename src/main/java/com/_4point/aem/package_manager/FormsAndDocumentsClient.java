package com._4point.aem.package_manager;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import com._4point.aem.package_manager.AemConfig.SimpleAemConfigBuilder;
import com._4point.aem.package_manager.rest_client.RestClient;
import com._4point.aem.package_manager.rest_client.RestClient.ContentType;
import com._4point.aem.package_manager.rest_client.RestClient.Response;
import com._4point.aem.package_manager.rest_client.RestClient.RestClientException;
import com._4point.aem.package_manager.rest_client.jersey.JerseyRestClient;

public class FormsAndDocumentsClient {
	private final RestClient contentManagerClient;
	private final JerseyRestClient formsAndDocumentsClient;
	
	private FormsAndDocumentsClient(AemConfig aemConfig) {
		this.contentManagerClient = new JerseyRestClient(aemConfig, "/libs/fd/fm/content/manage.json"); // ?func=deleteAssets
		this.formsAndDocumentsClient = new JerseyRestClient(aemConfig, "/content/dam/formsanddocuments"); 
//		this.commandPackageClient = new JerseyRestClient(aemConfig, "/libs/fd/fm/content/manage.json"); // func=uploadFormsPreview&folderPath=/content/dam/formsanddocuments&isIE=false
//		this.uploadPackageClient = new JerseyRestClient(aemConfig, "/crx/packmgr/service/.json");;
	}
	
	
	public static sealed interface DeleteResponse {

		public static final class DeleteSuccess implements DeleteResponse {
			
			private static DeleteSuccess from(String requestStatusString) {
				return switch (requestStatusString) {
					case "success"->new DeleteSuccess();
					default->throw new IllegalArgumentException("Unexpected requestStatus returned from AEM (" + requestStatusString + ")." );
				};
			};
		}
		
		public record DeleteError(
				String code,
				String type,
				String title,
				String description,
				String unresolvedMessage,
				List<String> messageArgs,
				String rootCause
				) implements DeleteResponse {
			
		}

		private static DeleteResponse from(String jsonString) {
			JsonData json = JsonData.from(jsonString);
			
			Optional<String> requestStatus = json.at("/requestStatus");
			if (requestStatus.isPresent()) {
				return DeleteSuccess.from(requestStatus.get());
			} else {
				// Must be error
				
			}
			// TODO Auto-generated method stub
			return null;
		}

	}
	
	public DeleteResponse delete(String target) {
		try {
			Response response = contentManagerClient.multipartPayloadBuilder()
													.add("assetPaths", target)
													.add("_charset_", "UTF-8")
													.queryParam("func", "deleteAssets")
													.build()
													.postToServer(ContentType.APPLICATION_JSON)
													.orElseThrow(()->new FormsAndDocumentsException("Error while deleting folder (" + target + "). No content was returned."));
			return DeleteResponse.from(new String(response.data().readAllBytes()));
		} catch (RestClientException | IOException | IllegalArgumentException e) {
			throw new FormsAndDocumentsException("Error while deleting folder (" + target + ").", e);
		}
	}

	public record PreviewResponse(String fileId) {};
	
	// filename, file contents, target location
	public PreviewResponse preview(String filename, byte[] content, String targetLocation) {
		// TODO:  Implement this.
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	public PreviewResponse preview(Path file, String targetLocation) {
		try {
			return preview(file.getFileName().toString(), Files.readAllBytes(file), targetLocation);
		} catch (IOException e) {
			throw new FormsAndDocumentsException("Error reading data from file '" + file.toString() + "'.", e );
		}
	}

	public boolean upload(String fileId) {
		// TODO:  Implement this.
		throw new UnsupportedOperationException("Not implemented yet.");
	}
	
	public boolean createFolder(String folderName) {
		// TODO:  Implement this.
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	public static FormsAndDocumentsBuilder builder() {
		return new FormsAndDocumentsBuilder();
	}

	/**
	 * Builder object for configuring/creating a FormsAndDocumentsClient instance.
	 * 
	 * 
	 */
	public static class FormsAndDocumentsBuilder {
		private final SimpleAemConfigBuilder aemConfigBuilder = new SimpleAemConfigBuilder();
		
		/**
		 * Set the machine name where the AEM instance resides.
		 * 
		 * @param serverName
		 * @return
		 */
		public FormsAndDocumentsBuilder serverName(String serverName) {
			aemConfigBuilder.serverName(serverName);
			return this;
		}

		/**
		 * Set the port that AEM is listening on
		 * 
		 * @param port
		 * @return
		 */
		public FormsAndDocumentsBuilder port(Integer port) {
			aemConfigBuilder.port(port);
			return this;
		}

		/**
		 * Set the user that will be used to authenticate with the AEM server.
		 * 
		 * @param ussr
		 * @return
		 */
		public FormsAndDocumentsBuilder user(String ussr) {
			aemConfigBuilder.ussr(ussr);
			return this;
		}

		/**
		 * Set the password that will be used to authenticate with the AEM server.
		 * 
		 * @param password
		 * @return
		 */
		public FormsAndDocumentsBuilder password(String password) {
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
		public FormsAndDocumentsBuilder useSsl(Boolean useSsl) {
			aemConfigBuilder.useSsl(useSsl);
			return this;
		}

		/**
		 * Build a FormsAndDocumentsClient instance.
		 * 
		 * @return new FormsAndDocumentsClient instance
		 */
		public FormsAndDocumentsClient build() {
			return new FormsAndDocumentsClient(aemConfigBuilder.build());
		}

		/**
		 * Build a FormsAndDocumentsClientEx instance.
		 * 
		 * @return new FormsAndDocumentsClientEx instance
		 */
		public FormsAndDocumentsClientEx buildEx() {
			return FormsAndDocumentsClientEx.from(new FormsAndDocumentsClient(aemConfigBuilder.build()));
		}
	}
	
	/**
	 * Package Manager Exceptions
	 * 
	 * All exceptions produced by objects in this library throw this kind of error.
	 * 
	 */
	@SuppressWarnings("serial")
	public static class FormsAndDocumentsException extends RuntimeException {

		FormsAndDocumentsException() {
		}

		FormsAndDocumentsException(String message, Throwable cause) {
			super(message, cause);
		}

		FormsAndDocumentsException(String message) {
			super(message);
		}

		FormsAndDocumentsException(Throwable cause) {
			super(cause);
		}
	}
}
