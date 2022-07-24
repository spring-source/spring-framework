package com.mybatis.v4.entity;

import lombok.Data;

/**
 * Created by dujie on 2019/3/22.
 */
@Data
public class Employee {

	private Integer id;

	private String lastName;

	private String email;

	private Integer gender;

	private Integer deptId;

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder("{");
		sb.append("\"id\":")
				.append(id);
		sb.append(",\"lastName\":\"")
				.append(lastName).append('\"');
		sb.append(",\"email\":\"")
				.append(email).append('\"');
		sb.append(",\"gender\":")
				.append(gender);
		sb.append(",\"deptId\":")
				.append(deptId);
		sb.append('}');
		return sb.toString();
	}
}
