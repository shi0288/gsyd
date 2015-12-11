package com.esoft.archer.user.model_backup;

import java.util.Date;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.Table;

@Entity
@Table(name = "db_operation_bak")
public class DBOperationBak {
	private String id;
	/** 操作表 */
	private String table;
	/** 操作 */
	private String operation;
	/** 操作数据 */
	private String data;
	/** 操作时间 */
	private Date time;

	@Lob
	@Basic(fetch = FetchType.LAZY)
	@Column(name = "description", columnDefinition = "longtext")
	public String getData() {
		return data;
	}

	@Id
	@Column(name = "id", unique = true, nullable = false, length = 32)
	public String getId() {
		return id;
	}

	@Column(name = "operation", length = 500)
	public String getOperation() {
		return operation;
	}

	@Column(name = "table", length = 500)
	public String getTable() {
		return table;
	}

	@Column(name = "time")
	public Date getTime() {
		return time;
	}

	public void setData(String data) {
		this.data = data;
	}

	public void setId(String id) {
		this.id = id;
	}

	public void setOperation(String operation) {
		this.operation = operation;
	}

	public void setTable(String table) {
		this.table = table;
	}

	public void setTime(Date time) {
		this.time = time;
	}

}
