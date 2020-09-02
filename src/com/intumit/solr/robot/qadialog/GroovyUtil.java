package com.intumit.solr.robot.qadialog;
import groovy.lang.Binding;
import groovy.lang.GroovyShell;


public class GroovyUtil {
	public static Object runScript(Binding binding, String scriptStr) {
		GroovyShell shell = new GroovyShell(binding);

		return shell.evaluate(scriptStr);
	}
}
