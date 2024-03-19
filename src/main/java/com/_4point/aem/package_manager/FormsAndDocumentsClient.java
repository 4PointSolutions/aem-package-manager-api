package com._4point.aem.package_manager;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.function.Consumer;

import com._4point.aem.package_manager.AemConfig.SimpleAemConfigBuilder;
import com._4point.aem.package_manager.FormsAndDocumentsClient.PreviewResponse.PreviewSuccess;
import com._4point.aem.package_manager.rest_client.RestClient;
import com._4point.aem.package_manager.rest_client.RestClient.ContentType;
import com._4point.aem.package_manager.rest_client.RestClient.Response;
import com._4point.aem.package_manager.rest_client.RestClient.RestClientException;
import com._4point.aem.package_manager.rest_client.jersey.JerseyRestClient;

/**
 * FormsAndDocumentsClient performs operations on the objects under Forms And Documents (which is a section of CRX dedicated
 * to the AEM Forms add-on).
 * 
 * It allow someone to upload files (including .zips), delete files and folders and create folders.  
 * 
 */
public class FormsAndDocumentsClient {
	private final RestClient contentManagerClient;
	private final JerseyRestClient formsAndDocumentsClient;
	private final Logger logger;
	
	private FormsAndDocumentsClient(AemConfig aemConfig, Logger logger) {
		this.contentManagerClient = new JerseyRestClient(aemConfig, "/libs/fd/fm/content/manage.json"); // ?func=deleteAssets
		this.formsAndDocumentsClient = new JerseyRestClient(aemConfig, "/content/dam/formsanddocuments");
		this.logger = logger; 
	}
	
	
	/**
	 * Represents an error returned from AEM
	 * 
	 */
	public record AemError(
			String code,
			String type,
			String title,
			String description,
			Optional<String> unresolvedMessage,
			List<String> messageArgs,
			String rootCause
			) implements DeleteResponse, PreviewResponse, UploadResponse {

		
		static Optional<AemError> from(JsonData json) {
			try {
				String code = json.at("/code").orElseThrow();
				String type = json.at("/type").orElseThrow();
				String title = json.at("/title").orElseThrow();
				String description = json.at("/description").orElseThrow();
				Optional<String> unresolvedMessage = json.at("/unresolvedMessage");
				Optional<String> messageArgs = json.at("/messageArgs/0");
				String rootCause = json.at("/rootCause").orElseThrow();
				
				return Optional.of(new AemError(code, type, title, description, unresolvedMessage, messageArgs.map(List::of).orElse(List.of()), rootCause));
			} catch (NoSuchElementException e) {
				// return empty so that we throw an unexpected response Exception.
				return Optional.empty();
			}
		}
	}

	/**
	 * Represents a response from asking AEM to delete something.  It can be a DeleteSuccess or AemError.
	 * 
	 */
	public static sealed interface DeleteResponse {

		public static final class DeleteSuccess implements DeleteResponse {
			
			private static Optional<DeleteResponse> from(JsonData json) {
				return json.at("/requestStatus").map(DeleteSuccess::from);
			}

			private static DeleteSuccess from(String requestStatusString) {
				return switch (requestStatusString) {
					case "success"->new DeleteSuccess();
					default->throw new IllegalArgumentException("Unexpected requestStatus returned from AEM (" + requestStatusString + ")." );
				};
			};
		}
		
		private static DeleteResponse from(String jsonString) {
			JsonData json = JsonData.from(jsonString);
			
			return DeleteSuccess.from(json)
					.map(ds->(DeleteResponse)ds)
					.or(()->AemError.from(json))
					.orElseThrow(()->new IllegalArgumentException("Unexpected response returned from AEM:\n" + jsonString))
					;
		}

	}
	
	/**
	 * Deletes the target file or folder from under the FormsAndDocuments directory.
	 * 
	 * @param target file or folder to delete.  The path is relative to the FormsAndDocuments directory.
	 * @return DeleteResponse indicating the success or failure of the delete operation.
	 */
	public DeleteResponse delete(String target) {
		try {
			Response response = contentManagerClient.multipartPayloadBuilder()
													.add("assetPaths", actualLocation(target))
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

	/**
	 * Represents a response from asking AEM to preview a .zip.  It can be a PreviewSuccess or AemError.
	 */
	public static sealed interface PreviewResponse {
		public record PreviewSuccess(String fileId) implements PreviewResponse {
	
			private static Optional<PreviewSuccess> from(JsonData json) {
				return json.at("/fileId").map(PreviewSuccess::new);
			}
		}

		private static PreviewResponse from(String jsonString) {
			JsonData json = JsonData.from(jsonString);

			return PreviewSuccess.from(json)
					.map(ps->(PreviewResponse)ps)
					.or(()->AemError.from(json))
					.orElseThrow(()->new IllegalArgumentException("Unexpected response returned from AEM:\n" + jsonString))
					;
		};
	}
	
	/**
	 * Previews the contents of a .zip that is being uploaded to a location the under FormsAndDocuments directory..
	 * 
	 * @param filename filename
	 * @param content the bytes of the uploaded file.
	 * @param targetLocation the location where the file will be unpacked
	 * @return PreviewResponse indicating the success or failure of the preview operation.
	 */
	public PreviewResponse preview(String filename, byte[] content, String targetLocation) {
		try {
			Response response = contentManagerClient.multipartPayloadBuilder()
													.queryParam("func", "uploadFormsPreview")
													.queryParam("folderPath", actualLocation(targetLocation))
													.queryParam("isIE", "false")
													.add("filename", filename)
													.add("file", content, ContentType.of("application/x-zip-compressed;filename=fidelity-of-0.0.1-SNAPSHOT.zip"))
													.add("_charset_", "UTF-8")
													.build()
													.postToServer(ContentType.APPLICATION_JSON)
													.orElseThrow(()->new FormsAndDocumentsException("Error while uploading file (" + filename + ")  to '" + actualLocation(targetLocation) + "'. No content was returned."));
			return PreviewResponse.from(new String(response.data().readAllBytes()));
		} catch (RestClientException | IOException | IllegalArgumentException e) {
			throw new FormsAndDocumentsException("Error while uploading file (" + filename + ") to '" + actualLocation(targetLocation) + "'.", e);
		}
	}

	/**
	 * Previews the contents of a .zip that is being uploaded to a location the under FormsAndDocuments directory..
	 * 
	 * @param file Path to a file that will be previewed
	 * @param targetLocation the location where the file will be unpacked
	 * @return PreviewResponse indicating the success or failure of the preview operation.
	 */
	public PreviewResponse preview(Path file, String targetLocation) {
		try {
			return preview(file.getFileName().toString(), Files.readAllBytes(file), targetLocation);
		} catch (IOException e) {
			throw new FormsAndDocumentsException("Error reading data from file '" + file.toString() + "'.", e );
		}
	}

	/**
	 * Represents a response from asking AEM to upload something.  It can be a UploadSuccess or AemError.
	 * 
	 */
	public static sealed interface UploadResponse {
		public static final class UploadSuccess implements UploadResponse {
			private static final UploadSuccess INSTANCE = new UploadSuccess();	// Since this is just a placeholder. we'll make it a singleton.

			private static Optional<UploadSuccess> from(JsonData json) {
				return json.at("/lastUploadedAssetPath").map(__->INSTANCE);
			}
		}

		private static UploadResponse from(String jsonString) {
			JsonData json = JsonData.from(jsonString);
		
			return UploadSuccess.from(json)
					.map(ur->(UploadResponse)ur)
					.or(()->AemError.from(json))
					.orElseThrow(()->new IllegalArgumentException("Unexpected response returned from AEM:\n" + jsonString))
					;
		}
	}

	/**
	 * Uploads the file that was previously previewed to a location the under FormsAndDocuments directory..
	 * 
	 * @param fileId a file id returned by preview operation
	 * @param targetLocation the location where the file will be uploaded
	 * @return UploadResponse indicating the success or failure of the upload operation.
	 */
	public UploadResponse upload(String fileId, String targetLocation) {
		try {
			Response response = contentManagerClient.multipartPayloadBuilder()
													.add("_charset_", "UTF-8")
													.queryParam("func", "uploadForms")
													.queryParam("folderPath", actualLocation(targetLocation))
													.queryParam("fileId", fileId)
													.queryParam("uploadType", "assets")
													.build()
													.postToServer(ContentType.APPLICATION_JSON)
													.orElseThrow(()->new FormsAndDocumentsException("Error while uploading fileId (" + fileId + ")  to '" + actualLocation(targetLocation) + "'. No content was returned."));
			return UploadResponse.from(new String(response.data().readAllBytes()));
		} catch (RestClientException | IOException | IllegalArgumentException e) {
			throw new FormsAndDocumentsException("Error while uploading fileId (" + fileId + ") to '" + actualLocation(targetLocation) + "'.", e);
		}
	}

	private String actualLocation(String targetLocation) {
		return "/content/dam/formsanddocuments" + (targetLocation.isBlank() || targetLocation.startsWith("/") ? "" : "/") + targetLocation;
	}
	
	/**
	 * Previews and uploads the file to a location the under FormsAndDocuments directory..
	 * 
	 * @param file file that will be uploaded
	 * @param targetLocation the location where the file will be uploaded
	 * @return UploadResponse indicating the success or failure of the upload operation.
	 */
	public UploadResponse upload(Path file, String targetLocation) {
		PreviewResponse previewResponse = preview(file, targetLocation);
		
		return switch(previewResponse) {
			case PreviewSuccess previewSuccess -> upload(previewSuccess.fileId(), targetLocation); 
			case AemError 		previewFailure -> previewFailure; 
		};
	}

	/**
	 * Previews and uploads the file directly under the FormsAndDocuments directory..
	 * 
	 * @param file file that will be uploaded
	 * @return UploadResponse indicating the success or failure of the upload operation.
	 */
	public UploadResponse upload(Path file) {
		return upload(file, "");
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
		private Logger logger = new Logger.NoOpLogger();
		
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
		 * Accepts a Consumer that will be used to publish logging messages.
		 * 
		 * If this is not supplied, then no logging will occur,
		 * 
		 * @param msgConsumer
		 * @return
		 */
		public FormsAndDocumentsBuilder logger(Consumer<? super String> msgConsumer) {
			this.logger = new Logger.PassThroughLogger(msgConsumer);
			return this;
		}

		/**
		 * Build a FormsAndDocumentsClient instance.
		 * 
		 * @return new FormsAndDocumentsClient instance
		 */
		public FormsAndDocumentsClient build() {
			return new FormsAndDocumentsClient(aemConfigBuilder.build(), logger);
		}

		/**
		 * Build a FormsAndDocumentsClientEx instance.
		 * 
		 * @return new FormsAndDocumentsClientEx instance
		 */
		public FormsAndDocumentsClientEx buildEx() {
			return FormsAndDocumentsClientEx.from(new FormsAndDocumentsClient(aemConfigBuilder.build(), logger));
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
