package com._4point.aem.package_manager;

import java.nio.file.Path;
import java.util.List;

import com._4point.aem.package_manager.PackageManagerClient.PackageManagerException;

public class PackageManagerClientEx {
	private final PackageManagerClient client;
	
	private PackageManagerClientEx(PackageManagerClient client) {
		this.client = client;
	}

	public List<ListResponse.Package> listPackages() {
		ListResponse result = client.listPackages();
		int statusCode = result.status().code();
		if (statusCode < 200 || statusCode > 299) {
			throw new PackageManagerException("Error returned from List Packages call (status code=" + statusCode + ", reason='" + result.status().text() + "'.");
		}
		return result.packages();
	}
	
	public String uploadPackage(String packageFilename, Path file) {
		CommandResponse result = client.uploadPackage(packageFilename, file);
		if (!result.success()) {
			throw new PackageManagerException("Error returned from Upload Package call (" + result.msg() + ").");
		}
		return result.path().orElseThrow(()->new PackageManagerException("No filename returned from Upload Package call (" + result.msg() + ")."));
	}

	public void installPackage(String group, String packageFilename) {
		CommandResponse result = client.installPackage(group, packageFilename);
		if (!result.success()) {
			throw new PackageManagerException("Error returned from Install Package call (" + result.msg() + ").");
		}
	}

	public void uninstallPackage(String group, String packageFilename) {
		CommandResponse result = client.uninstallPackage(group, packageFilename);
		if (!result.success()) {
			throw new PackageManagerException("Error returned from Uninstall Package call (" + result.msg() + ").");
		}
	}

	public void deletePackage(String group, String packageFilename) {
		CommandResponse result = client.deletePackage(group, packageFilename);
		if (!result.success()) {
			throw new PackageManagerException("Error returned from Delete Package call (" + result.msg() + ").");
		}
	}
	
	public static PackageManagerClientEx from(PackageManagerClient client) {
		return new PackageManagerClientEx(client);
	}
}
