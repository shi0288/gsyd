package com.esoft.archer.managelog.aop;

import com.esoft.archer.managelog.model.ManageLog;
import com.esoft.archer.system.controller.LoginUserInfo;
import com.esoft.archer.user.model.User;
import com.esoft.core.annotations.Logger;
import com.esoft.core.util.IdGenerator;
import org.apache.commons.logging.Log;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Date;

/**
 * Created by Administrator on 2015/12/15 0015.
 */

@Component
@Aspect
public class ManageLogAop {


    @Logger
    private static Log log;

    @Resource
    private LoginUserInfo loginUserInfo;

    @Resource
    HibernateTemplate ht;

    @AfterReturning(value = "execution(* com.esoft.archer.user.controller.UserHome.save(..))",returning="rtv")
    public void insertToLog(JoinPoint jp ,Object rtv) {
        System.out.println("loginUserInfo.getLoginUserId(): " + loginUserInfo.getLoginUserId());

        ManageLog manageLog=new ManageLog();
        manageLog.setId(IdGenerator.randomUUID());
        manageLog.setCreateTime(new Date());
        manageLog.setUser(new User(loginUserInfo.getLoginUserId()));
        manageLog.setDes("添加新用户");
        ht.save(manageLog);
//        Signature signature = jp.getSignature();
//        System.out.println("DeclaringType:" + signature.getDeclaringType());
//        System.out.println("DeclaringTypeName:" + signature.getDeclaringTypeName());
//        System.out.println("Modifiers:" + signature.getModifiers());
//        System.out.println("Name:" + signature.getName());
//        System.out.println("LongString:" + signature.toLongString());
//        System.out.println("ShortString:" + signature.toShortString());
//        System.out.println("后置通知");
//        for (int i = 0; i < jp.getArgs().length; i++) {
//            Object arg = jp.getArgs()[i];
//            if(null != arg) {
//                System.out.println("Args:" + arg.toString());
//            }
//        }
//        System.out.println("Return:" + rtv);
//        System.out.println("====================================");
    }


}
