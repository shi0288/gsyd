package com.esoft.core.jsf.message;

import javax.faces.application.FacesMessage;

public class OpenUrlFacesMessage extends FacesMessage {

	private String yesValue = "确定";
	private String yesUrl;
	private boolean isBlank = false;

	public OpenUrlFacesMessage(Severity severity, String summary,
			String detail, String yesValue, String yesUrl, boolean isBlank) {
		super();
		setSeverity(severity);
		setSummary(summary);
		setDetail(detail);
		this.yesValue = yesValue;
		this.yesUrl = yesUrl;
		this.isBlank = isBlank;
	}

	public OpenUrlFacesMessage(Severity severity, String summary,
			String detail, String yesUrl, boolean isBlank) {
		super();
		setSeverity(severity);
		setSummary(summary);
		setDetail(detail);
		this.yesUrl = yesUrl;
		this.isBlank = isBlank;
	}

	public String getYesValue() {
		return yesValue;
	}

	public void setYesValue(String yesValue) {
		this.yesValue = yesValue;
	}

	public String getYesUrl() {
		return yesUrl;
	}

	public void setYesUrl(String yesUrl) {
		this.yesUrl = yesUrl;
	}

	public boolean isBlank() {
		return isBlank;
	}

	public void setBlank(boolean isBlank) {
		this.isBlank = isBlank;
	}

}
