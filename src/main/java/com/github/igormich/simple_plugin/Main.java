package com.github.igormich.simple_plugin;

import com.github.igormich.processor.AutoWired;
import com.github.igormich.processor.PrintToConsole;

public class Main {

    @AutoWired
    private BiIntFunction fun;

    public void start() {
        System.out.println(fun.apply(40, 2));
    };
    public static void main(String[] args) {
        new Main().start();
    }
}