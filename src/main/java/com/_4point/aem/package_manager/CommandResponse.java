package com._4point.aem.package_manager;

import java.util.Optional;

public record CommandResponse(boolean success, String msg, Optional<String> path) {
	public static CommandResponse from(JsonData jsonData) {
		return new CommandResponse(Boolean.parseBoolean(jsonData.at("/success").orElseThrow()),
								   jsonData.at("/msg").orElseThrow(),
								   jsonData.at("/path")
								   );
	}
}
