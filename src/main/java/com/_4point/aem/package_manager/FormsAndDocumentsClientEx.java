package com._4point.aem.package_manager;

import java.nio.file.Path;

import com._4point.aem.package_manager.FormsAndDocumentsClient.AemError;
import com._4point.aem.package_manager.FormsAndDocumentsClient.FormsAndDocumentsException;

public class FormsAndDocumentsClientEx {
	private final FormsAndDocumentsClient formsAndDocumentsClient;
	
	private FormsAndDocumentsClientEx(FormsAndDocumentsClient formsAndDocumentsClient) {
		this.formsAndDocumentsClient = formsAndDocumentsClient;
	}
	
	static FormsAndDocumentsClientEx from(FormsAndDocumentsClient formsAndDocumentsClient) {
		return new FormsAndDocumentsClientEx(formsAndDocumentsClient);
	}

	/**
	 * Deletes the target file or folder from under the FormsAndDocuments directory.
	 * 
	 * @param target file or folder to delete.  The path is relative to the FormsAndDocuments directory.
	 */
	public void delete(String target) {
		if (formsAndDocumentsClient.delete(target) instanceof AemError aemError) {
			throwException(aemError, "delete");
		}
	}
	

	/**
	 * Previews and uploads the file directly under the FormsAndDocuments directory..
	 * 
	 * @param file file that will be uploaded
	 */
	public void upload(Path file) {
		if (formsAndDocumentsClient.upload(file) instanceof AemError aemError) {
			throwException(aemError, "upload");
		}
	}
	
	/**
	 * Previews and uploads the file to a location the under FormsAndDocuments directory..
	 * 
	 * @param file file that will be uploaded
	 * @param targetLocation the location where the file will be uploaded
	 */
	public void upload(Path file, String targetLocation) {
		if (formsAndDocumentsClient.upload(file, targetLocation) instanceof AemError aemError) {
			throwException(aemError, "upload");
		}
	}
	
	private static void throwException(AemError aemError, String operation) {
		throw new FormsAndDocumentsException("Error while performing %s (%s)".formatted(operation, aemError.description()));
	}
}
