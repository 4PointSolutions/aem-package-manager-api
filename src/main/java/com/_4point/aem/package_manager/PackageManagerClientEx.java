package com._4point.aem.package_manager;

import java.nio.file.Path;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

import com._4point.aem.package_manager.PackageManagerClient.PackageManagerException;

/**
 * Package Manager Client that throws Exceptions on errors.
 * 
 * This is intended to make it easier to use for simple cases with minimal error handling (where you want to throw exceptions on errors anyway).
 * 
 * Client that use this interface don't have to do error checking, only exception handling (if they want).
 * 
 */
public class PackageManagerClientEx {
	private final PackageManagerClient client;
	
	private PackageManagerClientEx(PackageManagerClient client) {
		this.client = client;
	}

	/**
	 * List the packages
	 * 
	 * @return List of package records.
	 */
	public List<ListResponse.Package> listPackages() {
		ListResponse result = client.listPackages();
		int statusCode = result.status().code();
		if (statusCode < 200 || statusCode > 299) {
			throw new PackageManagerException("Error returned from List Packages call (status code=" + statusCode + ", reason='" + result.status().text() + "'.");
		}
		return result.packages();
	}
	
	/**
	 * Upload a package
	 * 
	 * @param packageFilename Name of the package (which can be different than the filename)
	 * @param file Path to the file that will be uploaded
	 * @return
	 */
	public String uploadPackage(String packageFilename, Path file) {
		CommandResponse result = client.uploadPackage(packageFilename, file);
		if (!result.success()) {
			throw new PackageManagerException("Error returned from Upload Package call (" + result.msg() + ").");
		}
		return result.path().orElseThrow(()->new PackageManagerException("No filename returned from Upload Package call (" + result.msg() + ")."));
	}

	/**
	 * Upload a package. Uses the filename as the package name.
	 * 
	 * @param file Path to the file that will be uploaded
	 * @return
	 */
	public String uploadPackage(Path file) {
		return uploadPackage(file.getFileName().toString(), file);
	}

	/**
	 * Install a package.
	 * 
	 * @param group group name of the package (as determined in the pom.xml used to create the package)
	 * @param packageFilename packageFilename (must match ths package filename provided when the package was uploaded)
	 */
	public void installPackage(String group, String packageFilename) {
		CommandResponse result = client.installPackage(group, packageFilename);
		if (!result.success()) {
			throw new PackageManagerException("Error returned from Install Package call (" + result.msg() + ").");
		}
	}

	/**
	 * Uninstall a package.
	 * 
	 * @param group group name of the package (as determined in the pom.xml used to create the package)
	 * @param packageFilename packageFilename (must match ths package filename provided when the package was uploaded)
	 */
	public void uninstallPackage(String group, String packageFilename) {
		CommandResponse result = client.uninstallPackage(group, packageFilename);
		if (!result.success()) {
			throw new PackageManagerException("Error returned from Uninstall Package call (" + result.msg() + ").");
		}
	}

	/**
	 * Uninstall a package.
	 * 
	 * @param pkg Package to be uninstalled
	 */
	public void uninstallPackage(ListResponse.Package pkg) {
		uninstallPackage(pkg.group(), pkg.downloadName());
	}

	/**
	 * Delete a package.
	 * 
	 * @param group group name of the package (as determined in the pom.xml used to create the package)
	 * @param packageFilename packageFilename (must match ths package filename provided when the package was uploaded)
	 */
	public void deletePackage(String group, String packageFilename) {
		CommandResponse result = client.deletePackage(group, packageFilename);
		if (!result.success()) {
			throw new PackageManagerException("Error returned from Delete Package call (" + result.msg() + ").");
		}
	}
	
	/**
	 * Delete a package.
	 * 
	 * @param pkg Package to be deleted
	 */
	public void deletePackage(ListResponse.Package pkg) {
		deletePackage(pkg.group(), pkg.downloadName());
	}
	
	/**
	 * Uninstall and delete a package
	 * 
	 * @param pkg Package to be uninstalled and deleted
	 */
	public void uninstallAndDeletePackage(ListResponse.Package pkg) {
		uninstallPackage(pkg);
		deletePackage(pkg);
	}

	/**
	 * Create a PackageManagerClientEs object from a PackageManagerClient object.
	 * 
	 * @param client
	 * @return
	 */
	static PackageManagerClientEx from(PackageManagerClient client) {
		return new PackageManagerClientEx(client);
	}

	/**
	 * Delete packages that match a certain condition (possibly uninstalling them first).
	 * 
	 * @param condition	Predicate that indicates which packages to be (maybe uninstalled and) deleted.
	 * @param uninstallFirst Boolean indicating whether to uninstall before deleting.
	 */
	public void deletePackages(Predicate<? super ListResponse.Package> condition, boolean uninstallFirst) {
		Consumer<? super ListResponse.Package> terminatingFn = uninstallFirst ?  this::uninstallAndDeletePackage
																			  :  this::deletePackage;
		this.listPackages()
			.stream()
			.filter(condition)
			.forEach(terminatingFn)
			;
	}
	/**
	 * Uninstall and delete 
	 * 
	 * @param condition	Predicate that indicates which packages to be uninstalled and deleted.
	 */
	public void uninstallAndDeletePackages(Predicate<ListResponse.Package> condition) {
		deletePackages(condition, true);
	}
}
