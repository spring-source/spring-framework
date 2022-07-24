package com.dujie.propertyeditor.property;

import java.beans.PropertyEditorSupport;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * @Description: 自定义属性编辑器 通过 xml 将属性编辑注册到Spring
 * @Param
 * @Date: 2022/2/24 2:10 PM
 * @return
 */
public class DatePropertyEditor extends PropertyEditorSupport {
	private String format = "yyyy-MM-dd";

	public void setFormat(String format) {
		this.format = format;
	}

	@Override
	public void setAsText(String text) throws IllegalArgumentException {
		System.out.println("传入的参数为: " + text);
		SimpleDateFormat sdf = new SimpleDateFormat(format);
		try{
			Date date = sdf.parse(text);
			this.setValue(date);
		}catch (Exception e){
			e.printStackTrace();
		}
	}
}
