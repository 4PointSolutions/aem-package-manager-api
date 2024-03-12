package com._4point.aem.package_manager;

public record ListResponse(java.util.List<Package> packages, Request request, Status status) {

	public record Request(String name, String value) {
		private static Request from(XmlDocument xmlDoc) {
			return new Request(xmlDoc.getString("/request/param/@name").orElseThrow(), 
							   xmlDoc.getString("/request/param/@value").orElseThrow());
		}
	};
	
	public record Package(String group, 
						  String name,
						  String version,
						  String downloadName,
						  String size, 			// int
						  String created,		// Date
						  String createdBy,
						  String lastModified,	// Date
						  String lastModifiedBy,
						  String lastUnpacked,	// Data
						  String lastUnpackedBy
						  ) {
		private static Package from(XmlDocument xmlDoc) {
			return new Package(xmlDoc.getString("/package/group").orElseThrow(),
							   xmlDoc.getString("/package/name").orElseThrow(),
							   xmlDoc.getString("/package/version").orElseThrow(),
							   xmlDoc.getString("/package/downloadName").orElseThrow(),
							   xmlDoc.getString("/package/size").orElseThrow(),
							   xmlDoc.getString("/package/created").orElseThrow(),
							   xmlDoc.getString("/package/createdBy").orElseThrow(),
							   xmlDoc.getString("/package/lastModified").orElseThrow(),
							   xmlDoc.getString("/package/lastModifiedBy").orElseThrow(),
							   xmlDoc.getString("/package/lastUnpacked").orElseThrow(),
							   xmlDoc.getString("/package/lastUnpackedBy").orElseThrow());
		}
	};

	public record Status(int code, String text) {
		private static Status from(XmlDocument xmlDoc) {
			return new Status(Integer.parseInt(xmlDoc.getString("/status/@code").orElseThrow()),
							  xmlDoc.getString("/status").orElseThrow());
		}
	};
	
	public static ListResponse from(XmlDocument xmlDoc) {
		return new ListResponse(
				xmlDoc.getDocs("/crx/response/data/packages/package")
					  .stream()
					  .map(Package::from)
					  .toList(),
				Request.from(xmlDoc.getDoc("/crx/request").orElseThrow()),
				Status.from(xmlDoc.getDoc("/crx/response/status").orElseThrow())
				);
	}
};
