package com.esoft.archer.banner.controller;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.annotation.Resource;
import org.apache.commons.logging.Log;
import org.primefaces.event.FileUploadEvent;
import org.primefaces.model.UploadedFile;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.esoft.archer.banner.model.BannerPicture;
import com.esoft.archer.banner.service.BannerService;
import com.esoft.core.annotations.Logger;
import com.esoft.core.annotations.ScopeType;
import com.esoft.core.jsf.util.FacesUtil;
import com.esoft.core.util.IdGenerator;
import com.esoft.core.util.ImageUploadUtil;

@Component(value="bannerPictureHome")
@Scope(ScopeType.VIEW)
public class BannerPictureHome implements Serializable {

	@Logger
	static Log log;

	@Resource
	private BannerService bannerService;
	
	

	private List<BannerPicture> bannerPictures;

	/**
	 * 需要被修改图片的bannerPic
	 */
	private BannerPicture needChangedPic;

	public void deletePicture(BannerPicture bp) {
		if (bp != null) {
			try{				
				bannerService.deleteBannerPicture(bp);
				this.bannerPictures.remove(bp);
			} catch(Exception e){
				e.printStackTrace();
				FacesUtil.addInfoMessage("此图片已被使用，无法删除。");
			}
		}
	}

	public List<BannerPicture> getBannerPictures() {
		return bannerPictures;
	}

	/**
	 * 处理图片上传
	 * 
	 * @param event
	 */
	public void handleBannerPicturesUpload(FileUploadEvent event) {
		UploadedFile uploadFile = event.getFile();
		InputStream is = null;
		try {
			is = uploadFile.getInputstream();
			BannerPicture pp = new BannerPicture();
			pp.setId(IdGenerator.randomUUID());
			pp.setPicture(ImageUploadUtil.upload(is, uploadFile.getFileName()));
			pp.setSeqNum(this.getBannerPictures().size() + 1);
			this.getBannerPictures().add(pp);
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}
	}
	
	public void changeBannerPic(FileUploadEvent event) {
		UploadedFile uploadFile = event.getFile();
		InputStream is = null;
		try {
			is = uploadFile.getInputstream();
			if (this.getNeedChangedPic() != null) {
				this.getNeedChangedPic().setPicture(ImageUploadUtil.upload(is, uploadFile.getFileName()));
			} else {
				FacesUtil.addErrorMessage("被更改的banner为空。");
			}
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}
	}

	public void initBannerPictures(List<BannerPicture> value) {
//		if (this.bannerPictures == null) {
			this.bannerPictures = new ArrayList<BannerPicture>();			
//		}
		//如果value为null，则addAll方法抛异常
		if(value != null){
			this.bannerPictures.addAll(value);
		}
		this.bannerPictures = sortBySeqNum(bannerPictures);
	}

	private List<BannerPicture> sortBySeqNum(List<BannerPicture> pics) {
		Collections.sort(pics, new Comparator<BannerPicture>() {
			public int compare(BannerPicture o1, BannerPicture o2) {
				return o1.getSeqNum() - o2.getSeqNum();
			}
		});
		return pics;
	}

	public void setBannerPictures(List<BannerPicture> productPictures) {
		this.bannerPictures = productPictures;
	}

	public void moveUp(BannerPicture bp) {
		int currentIndex = this.bannerPictures.indexOf(bp);
		if (currentIndex == 0) {
			return;
		} else {
			this.bannerPictures.remove(bp);
			this.bannerPictures.add(currentIndex - 1, bp);
		}
	}

	public void moveDown(BannerPicture bp) {
		int currentIndex = this.bannerPictures.indexOf(bp);
		if (currentIndex == this.bannerPictures.size() - 1) {
			return;
		} else {
			this.bannerPictures.remove(bp);
			this.bannerPictures.add(currentIndex + 1, bp);
		}
	}

	public BannerPicture getNeedChangedPic() {
		return needChangedPic;
	}

	public void setNeedChangedPic(BannerPicture needChangedPic) {
		this.needChangedPic = needChangedPic;
	}
	
}
