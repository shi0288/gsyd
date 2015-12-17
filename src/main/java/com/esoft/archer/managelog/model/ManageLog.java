package com.esoft.archer.managelog.model;

import com.esoft.archer.user.model.User;

import javax.persistence.*;
import javax.persistence.Entity;
import javax.persistence.Table;
import java.util.Date;

/**
 * Created by w44 on 2015/12/15 0015.
 */
@Entity
@Table(name = "managelog")
public class ManageLog {


    private String id;
    //操作人
    private User user;
    //操作时间
    private Date createTime;
    //操作内容
    private String des;

    @Id
    @Column(name = "id", unique = true, nullable = false, length = 32)
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    @Column(name = "create_time", nullable = false, length = 19)
    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    @Column(name = "des", length = 50)
    public String getDes() {
        return des;
    }

    public void setDes(String des) {
        this.des = des;
    }
}
