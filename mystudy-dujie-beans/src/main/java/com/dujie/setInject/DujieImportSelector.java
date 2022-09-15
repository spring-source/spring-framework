package com.dujie.setInject;

import org.springframework.context.annotation.ImportSelector;
import org.springframework.core.type.AnnotationMetadata;

/**
 * Created by dujie on 2019/8/26.
 */
public class DujieImportSelector implements ImportSelector {
	@Override
	public String[] selectImports(AnnotationMetadata importingClassMetadata) {
		return new String[]{"com.dujie.setInject.InstE"};
	}
}
