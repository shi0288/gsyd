package com.esoft.archer.managelog.controller;

import com.esoft.archer.common.controller.EntityHome;
import com.esoft.archer.system.controller.LoginUserInfo;
import com.esoft.archer.user.model.User;
import com.esoft.core.annotations.Logger;
import org.apache.commons.logging.Log;

import javax.annotation.Resource;

/**
 * Created by Administrator on 2015/12/15 0015.
 */
public class ManageLogHome extends EntityHome<ManageLogHome> implements
        java.io.Serializable{
    @Logger
    private static Log log;

    @Resource
    private LoginUserInfo loginUserInfo;



}
