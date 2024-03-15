package com._4point.aem.package_manager;

public class FormsAndDocumentsClientEx {
	private final FormsAndDocumentsClient formsAndDocumentsClient;
	
	private FormsAndDocumentsClientEx(FormsAndDocumentsClient formsAndDocumentsClient) {
		this.formsAndDocumentsClient = formsAndDocumentsClient;
	}
	
	public static FormsAndDocumentsClientEx from(FormsAndDocumentsClient formsAndDocumentsClient) {
		return new FormsAndDocumentsClientEx(formsAndDocumentsClient);
	}

}
