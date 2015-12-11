package com.esoft.archer.system.event;

import org.springframework.context.ApplicationEvent;

import com.esoft.archer.system.model.Audit;

/**
 * 审计事件
 * @author Administrator
 *
 */
public class AuditEvent extends ApplicationEvent {

	public AuditEvent(Audit audit) {
		super(audit);
	}

}
