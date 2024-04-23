package com._4point.aem.package_manager;

import java.util.Optional;

/**
 * Command Response from AEM.
 * 
 * Object contains the data returned by AEM in response to issuing a command.
 * 
 * @param success boolean indicating success (true) or failure (false)
 * @param msg message from AEM
 * @param path path provided by AEM
 * 
 */
public record CommandResponse(boolean success, String msg, Optional<String> path) {
	public static CommandResponse from(JsonData jsonData) {
		return new CommandResponse(Boolean.parseBoolean(jsonData.at("/success").orElseThrow()),
								   jsonData.at("/msg").orElseThrow(),
								   jsonData.at("/path")
								   );
	}
}
