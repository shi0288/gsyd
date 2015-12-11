package com.esoft.archer.banner.controller;

import java.io.Serializable;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.esoft.core.annotations.ScopeType;

@Component(value="bannerPictureHome2")
@Scope(ScopeType.VIEW)
public class BannerPictureHome2 extends BannerPictureHome implements Serializable{
	
}
