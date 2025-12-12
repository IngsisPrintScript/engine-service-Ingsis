package com.ingsis.snippetManager.engine.runner;

import com.ingsis.utils.nodes.expressions.ExpressionNode;
import com.ingsis.utils.nodes.visitors.Checker;
import com.ingsis.utils.nodes.visitors.Interpreter;
import com.ingsis.utils.result.CorrectResult;
import com.ingsis.utils.result.Result;
import com.ingsis.utils.token.tokenstream.TokenStream;
import java.util.List;
import java.util.function.Supplier;

public class NativeExpressionNode implements ExpressionNode {

    private final Supplier<Object> supplier;

    public NativeExpressionNode(Supplier<Object> supplier) {
        this.supplier = supplier;
    }

    @Override
    public List<ExpressionNode> children() {
        return List.of();
    }

    @Override
    public Result<Object> solve() {
        return new CorrectResult<>(supplier.get());
    }

    @Override
    public String symbol() {
        return "";
    }

    @Override
    public Result<String> acceptInterpreter(Interpreter interpreter) {
        return new CorrectResult<>("native");
    }

    @Override
    public Result<String> acceptChecker(Checker checker) {
        return new CorrectResult<>("native");
    }

    @Override
    public TokenStream stream() {
        return null;
    }

    @Override
    public Integer line() {
        return 0;
    }

    @Override
    public Integer column() {
        return 0;
    }
}
