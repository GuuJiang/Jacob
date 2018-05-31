package com.guujiang.jacob.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.guujiang.jacob.Stub;

/**
 * Indicate that a generator method.
 * A method can be generator method if and only if <b>ALL</b> the conditions below holds.
 * <ul>
 * 	<li>return type is {@link java.lang.Iterable}</li>
 * 	<li>invoked {@link Stub#yield(Object) yield} at least once</li>
 * 	<li>has at most one return statement at the end</li>
 * </ul>
 * @author GuuJiang
 *
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface GeneratorMethod {
	public OverRangePolicy overIterate() default OverRangePolicy.ReturnNull;
}
