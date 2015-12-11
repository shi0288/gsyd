package com.esoft.archer.common.controller;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.List;

import javax.faces.component.UIComponent;
import javax.faces.component.UIInput;
import javax.faces.component.html.HtmlPanelGroup;

import org.apache.commons.logging.Log;
import org.primefaces.component.graphicimage.GraphicImage;
import org.primefaces.component.tooltip.Tooltip;
import org.primefaces.event.FileUploadEvent;
import org.primefaces.model.UploadedFile;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.esoft.core.annotations.Logger;
import com.esoft.core.annotations.ScopeType;
import com.esoft.core.jsf.util.FacesUtil;
import com.esoft.core.util.ImageUploadUtil;

@Component
@Scope(ScopeType.REQUEST)
public class UploadFileHome implements Serializable{
	
	//FIXME:目前仅实现了单个图片（文件）的上传组件，需实现多个图片（文件）上传。

	private static final long serialVersionUID = -1531462606650854352L;

	@Logger
	static Log log;

	private List<MyUploadedFile> files;
	private MyUploadedFile oneFile;
	
	/**
	 * 处理多个上传
	 * 
	 * @param event
	 */
	public void handleFileUpload(FileUploadEvent event) {
		MyUploadedFile file = handleUpload(event);
		if (file != null) {
			files.add(file);
			FacesUtil.addInfoMessage("上传成功！");
		} else {
			FacesUtil.addErrorMessage("上传失败！");
		}
	}

	private HtmlPanelGroup panelGroup;

	public HtmlPanelGroup getPanelGroup() {
		return panelGroup;
	}

	public void setPanelGroup(HtmlPanelGroup panelGroup) {
		this.panelGroup = panelGroup;
	}

	/**
	 * 处理单个文件上传
	 * 
	 * @param event
	 */
	public void handleOneFileUpload(FileUploadEvent event) {
		MyUploadedFile file = handleUpload(event);
		if (file != null) {
			this.oneFile = file;
			List<UIComponent> components = this.panelGroup.getChildren();
			for (UIComponent uiComp : components) {
				if (uiComp.getId().equals("thumbText")) {
					UIInput url = (UIInput) uiComp;
					url.setValue(file.getUrl());
				} else if (uiComp.getId().equals("thumbTooltip")) {
					Tooltip tooltip = (Tooltip) uiComp;
					for (UIComponent uiCompT : tooltip.getChildren()) {
						if (uiCompT.getId().equals("thumbImage")) {
							GraphicImage image = (GraphicImage) uiCompT;
							image.setValue(file.getUrl());
							image.setRendered(true);
						}
					}
				}
			}
			FacesUtil.getCurrentInstance().getPartialViewContext()
					.getRenderIds().add(this.panelGroup.getClientId());
			FacesUtil.addInfoMessage("上传成功！");
		} else {
			FacesUtil.addErrorMessage("上传失败！");
		}
	}

	private MyUploadedFile handleUpload(FileUploadEvent event) {
		UploadedFile uploadFile = event.getFile();
		InputStream is = null;
		try {
			is = uploadFile.getInputstream();
			String url = ImageUploadUtil.upload(is, uploadFile.getFileName());
			return new MyUploadedFile(url, uploadFile);
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}

	public List<MyUploadedFile> getFiles() {
		return files;
	}

	public void setFiles(List<MyUploadedFile> files) {
		this.files = files;
	}

	public MyUploadedFile getOneFile() {
		return oneFile;
	}

	public void setOneFile(MyUploadedFile oneFile) {
		this.oneFile = oneFile;
	}

}
