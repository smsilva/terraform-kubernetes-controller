package com.github.smsilva.platform.model.v1alpha1;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StackInstanceSpec {

    private StackInstanceSpecStack stack;
    private Map<String, Object> vars = new HashMap<>();
    private List<String> outputs = new ArrayList<>();

    public StackInstanceSpecStack getStack() {
        return stack;
    }

    public void setStack(StackInstanceSpecStack stack) {
        this.stack = stack;
    }

    public Map<String, Object> getVars() {
        return vars;
    }

    public void setVars(Map<String, Object> vars) {
        this.vars = vars;
    }

    public List<String> getOutputs() {
        return outputs;
    }

    public void setOutputs(List<String> outputs) {
        this.outputs = outputs;
    }

    @Override
    public String toString() {
        return "StackInstanceSpec{" +
                "stack=" + stack + "," +
                "vars=" + vars +
                '}';
    }
}
