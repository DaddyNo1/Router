package com.daddyno1.router_compiler.utils;

import javax.annotation.processing.Messager;
import javax.tools.Diagnostic;

public class Utils {
    public static Messager messager;
    public static void logger(CharSequence s){
        messager.printMessage(Diagnostic.Kind.NOTE, s);
    }
}
