package com._4point.aem.package_manager.rest_client;

import java.io.Closeable;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Optional;
import java.util.function.Function;

public interface RestClient {
	
	public static interface UntargettedRestClient {
		RestClient target(String target);
	}
	
	/**
	 * Returns String representing the final endpoint target location.
	 * 
	 * @return final endpoint target
	 */
	public String target();
	
	/**
	 * Represents a content type / mime type used to identify the content of a response.
	 * 
	 * @param contentType A String containing a Mime type (e.g. "application/pdf")
	 */
	public record ContentType(String contentType) {
		public static final ContentType APPLICATION_PDF = ContentType.of("application/pdf");
		public static final ContentType APPLICATION_XDP = ContentType.of("application/vnd.adobe.xdp+xml");
		public static final ContentType APPLICATION_XML = ContentType.of("application/xml");
		public static final ContentType APPLICATION_JSON = ContentType.of("application/json");
		public static final ContentType APPLICATION_ZIP = ContentType.of("application/zip"); // application/octet-stream
		public static final ContentType TEXT_HTML = ContentType.of("text/html");
		public static final ContentType TEXT_PLAIN = ContentType.of("text/plain");
		public static final ContentType APPLICATION_OCTET_STREAM = ContentType.of("application/octet-stream");
		public static final ContentType APPLICATION_DPL = ContentType.of("application/vnd.datamax-dpl");
		public static final ContentType APPLICATION_IPL = ContentType.of("application/vnd.intermec-ipl");
		public static final ContentType APPLICATION_PCL = ContentType.of("application/vnd.hp-pcl");
		public static final ContentType APPLICATION_PS = ContentType.of("application/postscript");
		public static final ContentType APPLICATION_TPCL = ContentType.of("application/vnd.toshiba-tpcl");
		public static final ContentType APPLICATION_ZPL = ContentType.of("x-application/zpl");
		
		public static ContentType of(String contentType) { return new ContentType(contentType); }
	};
	
	/**
	 * Payload interface is the payload to be sent to AEM.  
	 */
	public interface Payload {
		
		/**
		 * This method returns an Response object unless the response from the called service was NO_CONTENT, in which case
		 * no Response object is returned, 
		 * 
		 * If the called service returns an error code, then this method throws a RestClientException.
		 * 
		 * @param acceptContentType
		 * @return
		 * @throws RestClientException
		 */
		public Optional<Response> postToServer(ContentType acceptContentType) throws RestClientException;
		
	}
	
	/**
	 * Represents a payload for a multipart/form-data POST
	 */
	public interface MultipartPayload extends Payload, Closeable {
		
		public interface Builder {
			Builder add(String fieldName, String fieldData);
			Builder add(String fieldName, byte[] fieldData, ContentType contentType);
			Builder add(String fieldName, InputStream fieldData, ContentType contentType);
			Builder add(String fieldName, Path fieldData, ContentType contentType);
			default Builder addIfNotNull(String fieldName, String fieldData) {
				return fieldData != null ? add(fieldName, fieldData) : this;
			}
			default Builder addIfNotNull(String fieldName, byte[] fieldData, ContentType contentType) {
				return fieldData != null ? add(fieldName, fieldData, contentType) : this;
			}
			default Builder addIfNotNull(String fieldName, InputStream fieldData, ContentType contentType) {
				return fieldData != null ? add(fieldName, fieldData, contentType) : this;
			}
			default <T> Builder transformAndAdd(String fieldName, T fieldData, Function<T, String> fn) {
				return fieldData != null ? addIfNotNull(fieldName, fn.apply(fieldData)) : this;
			}
			default <T> Builder transformAndAddBytes(String fieldName, T fieldData, ContentType contentType, Function<T, byte[]> fn) {
				return fieldData != null ? addIfNotNull(fieldName, fn.apply(fieldData), contentType) : this;
			}
			default <T> Builder transformAndAddInputStream(String fieldName, T fieldData, ContentType contentType, Function<T, InputStream> fn) {
				return fieldData != null ? addIfNotNull(fieldName, fn.apply(fieldData), contentType) : this;
			}
			default <T> Builder addStringVersion(String fieldName, T fieldData) {
				return fieldData != null ? addIfNotNull(fieldName, fieldData.toString()) : this;
			}
			Builder queryParam(String name, String value);
			MultipartPayload build();
		}
	}

	/**
	 * Returns a builder that is used to construct the payload for a multipart POST.
	 * 
	 * @return
	 */
	public MultipartPayload.Builder multipartPayloadBuilder();
	
	/**
	 * Represents an HTTP GET Request
	 */
	public interface GetRequest {
		
		/**
		 * Performs an HTTP GET Request from a server
		 * 
		 * @param acceptContentType - expected content type
		 * @return Response from the server (Optional.isEMpty() if server returns "No Content"
		 * @throws RestClientException if an error occurs
		 */
		public Optional<Response> getFromServer(ContentType acceptContentType) throws RestClientException;
		
		/**
		 * Used for building a GET request
		 */
		public interface Builder {
			Builder queryParam(String name, String value);
			GetRequest build();
		}
	}
	
	/**
	 * Returns a builder that is used to construct a GET request.
	 * 
	 * @return
	 */
	public GetRequest.Builder getRequestBuilder();
	
	
	/**
	 * A response from the AEM Rest Service
	 */
	public interface Response {
		/**
		 * Retrieves the content type of the response (e.g. "application/pdf").
		 * 
		 * @return String representation of the content type.
		 */
		public ContentType contentType();
		/**
		 * Retrieves the InputStream for the response.  This inputStream can only be read once, so it should
		 * only be retrieved once.
		 * 
		 * Retriving more than once will generate an IllegalStateException.
		 * 
		 * @return
		 */
		public InputStream data();
		/**
		 * Retrieves the first HTTP header value for this header. 
		 * 
		 * @param header
		 * @return
		 */
		public Optional<String> retrieveHeader(String header);
		
	}

    @SuppressWarnings("serial")
	public static class RestClientException extends Exception {

		public RestClientException() {
		}

		public RestClientException(String message, Throwable cause) {
			super(message, cause);
		}

		public RestClientException(String message) {
			super(message);
		}

		public RestClientException(Throwable cause) {
			super(cause);
		}
	}
}
