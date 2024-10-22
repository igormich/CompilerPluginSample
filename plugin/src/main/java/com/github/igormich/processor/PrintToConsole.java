package com.github.igormich.processor;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.SOURCE;

@Target(METHOD)
@Retention(SOURCE)
public @interface PrintToConsole {
}