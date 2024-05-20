package com.github.igormich.simple_plugin;

import com.github.igormich.processor.PrintToConsole;
import com.github.igormich.processor.Service;

@Service
public class IntSumFunction implements BiIntFunction{

    @Override
    @PrintToConsole
    public int apply(int a, int b) {
        return a+b;
    }
}
