package com.github.jochenw.icm.core.api.plugins;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Documented
@Target(ElementType.TYPE)
public @interface IcmChange {
	String name() default "";
	String description() default "";
	String version() default "";
	Attribute[] attributes() default {};
}
