package com._4point.aem.package_manager.rest_client.jersey;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.glassfish.jersey.client.authentication.HttpAuthenticationFeature;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.glassfish.jersey.media.multipart.MultiPartFeature;

import com._4point.aem.package_manager.AemConfig;
import com._4point.aem.package_manager.rest_client.RestClient;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response.Status;
import jakarta.ws.rs.core.Response.Status.Family;
import jakarta.ws.rs.core.Response.StatusType;

public class JerseyRestClient implements RestClient {
	private final WebTarget target;

	/**
	 * Constructor for JerseyRestClient if customization of the Jersey Client object is required.
	 * 
	 * If the Jersey Client object does not need to be customized, 
	 * use JerseyRestClient(AemConfig aemConfig, String target) instead.
	 * 
	 * @param aemConfig AEM configuration parameters
	 * @param target REST endpoint to be called.
	 * @param client Jersey Client object
	 */
	public JerseyRestClient(AemConfig aemConfig, String target, Client client) {
		this.target = configureClient(client,aemConfig.user(), aemConfig.password())
						.target(aemConfig.url())
						.path(target)
						;
	}

	/**
	 * Constructor for JerseyRestClient
	 * 
	 * @param aemConfig AEM configuration parameters
	 * @param target REST endpoint to be called.
	 */
	public JerseyRestClient(AemConfig aemConfig, String target) {
		this(aemConfig, target, getClient());
	}

	private static Client configureClient(Client client, String username, String password) {
		return client.register(MultiPartFeature.class)
					 .register(HttpAuthenticationFeature.basic(username, password));
	}
	
	public static RestClient restClient(AemConfig aemConfig, String target) {
		return new JerseyRestClient(aemConfig, target);
	}
	
	public static RestClient restClient(AemConfig aemConfig, String target, Client client) {
		return new JerseyRestClient(aemConfig, target, client);
	}
	
	@Override
	public MultipartPayload.Builder multipartPayloadBuilder() {
		return new JerseyMultipartPayloadBuilder();
	}
	
	public static class JerseyUntargettedRestClient implements UntargettedRestClient {
		private final AemConfig aemConfig;
		
		public JerseyUntargettedRestClient(AemConfig aemConfig) {
			this.aemConfig = aemConfig;
		}

		@Override
		public RestClient target(String target) {
			return new JerseyRestClient(aemConfig, target);
		}
	}
	
	private final static class JerseyResponse implements Response {
		private final jakarta.ws.rs.core.Response response;
		
		private JerseyResponse(jakarta.ws.rs.core.Response response) {
			this.response = response;
		}

		@Override
		public ContentType contentType() {
			return ContentType.of(response.getMediaType().toString());
		}

		@Override
		public InputStream data() {
			return (InputStream) response.getEntity();
		}

		@Override
		public Optional<String> retrieveHeader(String header) {
			return Optional.ofNullable(response.getHeaderString(header));
		}
		
		private static Optional<Response> processResponse(jakarta.ws.rs.core.Response response, MediaType expectedMediaType) throws RestClientException {
			try {
				StatusType resultStatus = response.getStatusInfo();
				if (resultStatus.getStatusCode() == Status.NO_CONTENT.getStatusCode()) {
					return Optional.empty();
				}
				if (!Family.SUCCESSFUL.equals(resultStatus.getFamily())) {
					String message = "Call to server failed, statusCode='" + resultStatus.getStatusCode() + "', reason='" + resultStatus.getReasonPhrase() + "'.";
					if (response.hasEntity()) {
						InputStream entityStream = (InputStream) response.getEntity();
						message += "\n" + inputStreamtoString(entityStream);
					}
					throw new RestClientException(message);
				}
				if (!response.hasEntity()) {
					throw new RestClientException("Call to server succeeded but server failed to return content.  This should never happen.");
				}

				String responseContentType = response.getHeaderString(HttpHeaders.CONTENT_TYPE);
				if ( responseContentType == null || !expectedMediaType.isCompatible(MediaType.valueOf(responseContentType))) {
					String msg = "Response from AEM server was not of expected type(" + expectedMediaType.toString() + ").  " + (responseContentType != null ? "content-type='" + responseContentType + "'" : "content-type was null") + ".";
					InputStream entityStream = (InputStream) response.getEntity();
					msg += "\n" + inputStreamtoString(entityStream);
					throw new RestClientException(msg);
				}
				return Optional.of(new JerseyResponse(response));
			} catch (IOException e) {
				throw new RestClientException("IO Error while reading AEM response.", e);
			}
		}
	}

	private static String inputStreamtoString(InputStream inputStream) throws IOException {
		return new String(inputStream.readAllBytes(),StandardCharsets.UTF_8);
	}


	/*
	 * Multipart Implementation Code
	 * 
	 */
	private final class JerseyMultipartPayload implements MultipartPayload {
		private final FormDataMultiPart multipart;
		private final List<JerseyMultipartPayloadBuilder.QueryParam> queryParams;
		
		private JerseyMultipartPayload(FormDataMultiPart multipart, List<JerseyMultipartPayloadBuilder.QueryParam> queryParams) {
			this.multipart = multipart;
			this.queryParams = queryParams;
		}

		@Override
		public Optional<Response> postToServer(ContentType acceptContentType) throws RestClientException {
			MediaType acceptMediaType = MediaType.valueOf(acceptContentType.contentType());
			WebTarget localTarget = target;
			for(var queryParam : queryParams) {
				localTarget = localTarget.queryParam(queryParam.name, queryParam.value);
			}
			jakarta.ws.rs.client.Invocation.Builder invokeBuilder = localTarget.request().accept(acceptMediaType);

//			if (this.correlationIdFn != null) {
//				invokeBuilder.header(CORRELATION_ID_HTTP_HDR, this.correlationIdFn.get());
//			}
//			Response result;
			try {
				return JerseyResponse.processResponse(invokeBuilder.post(Entity.entity(multipart, multipart.getMediaType())), acceptMediaType);
			} catch (jakarta.ws.rs.ProcessingException e) {
				String msg = e.getMessage();
				throw new RestClientException("Error when posting to '" + target.getUri().toString() + "'" + (msg != null ? " (" + msg + ")" : "") + ".", e); 
			}
		}

		@Override
		public void close() throws IOException {
			multipart.close();
		}
		
	}
	
	/**
	 * MultipartPayload.Builder implementation code.
	 */
	private final class JerseyMultipartPayloadBuilder implements MultipartPayload.Builder {
		private record QueryParam(String name, String value) {};
		private List<QueryParam> queryParams = new ArrayList<>();
		private final FormDataMultiPart multipart = new FormDataMultiPart();

		@Override
		public MultipartPayload.Builder add(String fieldName, String fieldData) {
			multipart.field(fieldName, fieldData);
			return this;
		}

		@Override
		public MultipartPayload.Builder add(String fieldName, byte[] fieldData, ContentType contentType) {
			multipart.field(fieldName, fieldData, MediaType.valueOf(contentType.contentType()));
			return this;
		}

		@Override
		public MultipartPayload.Builder add(String fieldName, InputStream fieldData, ContentType contentType) {
			multipart.field(fieldName, fieldData, MediaType.valueOf(contentType.contentType()));
			return this;
		}
		
		@Override
		public MultipartPayload.Builder add(String fieldName, Path file, ContentType contentType) {
			multipart.field(fieldName, file.toFile(), MediaType.valueOf(contentType.contentType()));
			multipart.getField(fieldName).setContentDisposition(FormDataContentDisposition
			            .name(fieldName)
			            .fileName(file.getFileName().toString())
			            .build());
			return this;
		}
		
		@Override
		public MultipartPayload.Builder queryParam(String name, String value) {
			queryParams.add(new QueryParam(name, value));
			return this;
		}

		@Override
		public MultipartPayload build() {
			return new JerseyMultipartPayload(multipart, queryParams);
		}
	}

	/*
	 * Singleton Client-related code
	 * 
	 */
	// Safe way to lazily initialize singeleton.  See https://en.wikipedia.org/wiki/Initialization-on-demand_holder_idiom 
	private static class ClientHolder {
		static final Client INSTANCE = ClientBuilder.newClient();
	}
	
	/**
	 * Gets a singleton client instance that can be custom configured before being used to construct a JersetRestClient
	 * object.
	 * 
	 * @return a singleton Jersey Jarkate REST Services Client object
	 */
	public static Client getClient() {
		return ClientHolder.INSTANCE;
	}

	@Override
	public String target() {
		return target.toString();
	}

	@Override
	public GetRequest.Builder getRequestBuilder() {
		return new JerseyGetRequestBuilder();
	}
	
	private final class JerseyGetRequestBuilder implements GetRequest.Builder {
		private record QueryParam(String name, String value) {};
		private List<QueryParam> queryParams = new ArrayList<>();

		@Override
		public JerseyGetRequestBuilder queryParam(String name, String value) {
			queryParams.add(new QueryParam(name, value));
			return this;
		}

		@Override
		public GetRequest build() {
			return new JerseyGetRequest(Collections.unmodifiableList(queryParams));
		}
	}
	
	private final class JerseyGetRequest implements GetRequest {
		private final List<JerseyGetRequestBuilder.QueryParam> queryParams;
		
		JerseyGetRequest(List<JerseyGetRequestBuilder.QueryParam> queryParams) {
			this.queryParams = queryParams;
		}

		@Override
		public Optional<Response> getFromServer(ContentType acceptContentType) throws RestClientException {
			MediaType acceptMediaType = MediaType.valueOf(acceptContentType.contentType());
			WebTarget localTarget = target;
			for(var queryParam : queryParams) {
				localTarget = localTarget.queryParam(queryParam.name, queryParam.value);
			}
			jakarta.ws.rs.client.Invocation.Builder invokeBuilder = localTarget.request().accept(acceptMediaType);
			try {
				return JerseyResponse.processResponse(invokeBuilder.get(), acceptMediaType);
			} catch (jakarta.ws.rs.ProcessingException e) {
				String msg = e.getMessage();
				throw new RestClientException("Error when posting to '" + target.getUri().toString() + "'" + (msg != null ? " (" + msg + ")" : "") + ".", e); 
			}
		}
	}
}
