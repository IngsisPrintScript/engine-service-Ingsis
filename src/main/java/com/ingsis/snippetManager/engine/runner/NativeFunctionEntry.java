package com.ingsis.snippetManager.engine.runner;

import com.ingsis.utils.nodes.expressions.ExpressionNode;
import com.ingsis.utils.runtime.environment.Environment;
import com.ingsis.utils.runtime.environment.entries.FunctionEntry;
import com.ingsis.utils.type.types.Types;
import java.util.LinkedHashMap;
import java.util.List;

public class NativeFunctionEntry implements FunctionEntry {

    private final List<ExpressionNode> body;
    private final LinkedHashMap<String, Types> args;
    private final Environment closure;

    public NativeFunctionEntry(List<ExpressionNode> body, LinkedHashMap<String, Types> args, Environment closure) {
        this.body = body;
        this.args = args;
        this.closure = closure;
    }

    @Override
    public Types returnType() {
        return Types.STRING; // o ANY
    }

    @Override
    public LinkedHashMap<String, Types> arguments() {
        return args;
    }

    @Override
    public List<ExpressionNode> body() {
        return body;
    }

    @Override
    public Environment closure() {
        return closure;
    }
}
