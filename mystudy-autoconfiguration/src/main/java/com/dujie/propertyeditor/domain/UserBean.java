package com.dujie.propertyeditor.domain;

import lombok.Data;
import lombok.ToString;

import java.util.Date;

@Data
@ToString
public class UserBean {

	private Date birthday;

	public UserBean() {
	}

	public UserBean(Date birthday) {
		this.birthday = birthday;
	}

	public Date getBirthday() {
		return birthday;
	}

	public void setBirthday(Date birthday) {
		this.birthday = birthday;
	}
}