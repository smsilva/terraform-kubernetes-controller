package com.github.smsilva.platform.model.v1alpha1;

import java.util.List;
import java.util.Map;

public class StackInstanceSpec {

    private StackInstanceSpecStack stack;
    private Map<String, String> vars;
    private List<String> outputs;

    public StackInstanceSpecStack getStack() {
        return stack;
    }

    public void setStack(StackInstanceSpecStack stack) {
        this.stack = stack;
    }

    public Map<String, String> getVars() {
        return vars;
    }

    public void setVars(Map<String, String> vars) {
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
