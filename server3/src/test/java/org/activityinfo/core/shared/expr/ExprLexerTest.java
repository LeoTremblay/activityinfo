package org.activityinfo.core.shared.expr;

import org.activityinfo.core.shared.expr.functions.BooleanFunctions;
import org.activityinfo.core.shared.expr.functions.EqualFunction;
import org.activityinfo.core.shared.expr.functions.PlusFunction;
import org.junit.Test;

import static org.junit.Assert.*;

public class ExprLexerTest {

    @Test
    public void simpleTokenizing() {
        expect("1+2",
                new Token(TokenType.NUMBER, 0, "1"),
                new Token(TokenType.OPERATOR, 1, "+"),
                new Token(TokenType.NUMBER, 2, "2"));

        expect("1 \n+ 2",
                new Token(TokenType.NUMBER, 0, "1"),
                new Token(TokenType.WHITESPACE, 1, " \n"),
                new Token(TokenType.OPERATOR, 3, "+"),
                new Token(TokenType.WHITESPACE, 4, " "),
                new Token(TokenType.NUMBER, 5, "2"));


        expect("((1+3)*(44323+455))/66   ",
                new Token(TokenType.PAREN_START, 0, "("),
                new Token(TokenType.PAREN_START, 1, "("),
                new Token(TokenType.NUMBER, 2, "1"),
                new Token(TokenType.OPERATOR, 3, "+"),
                new Token(TokenType.NUMBER, 4, "3"),
                new Token(TokenType.PAREN_END, 5, ")"),
                new Token(TokenType.OPERATOR, 6, "*"),
                new Token(TokenType.PAREN_START, 7, "("),
                new Token(TokenType.NUMBER, 8, "44323"),
                new Token(TokenType.OPERATOR, 13, "+"),
                new Token(TokenType.NUMBER, 14, "455"),
                new Token(TokenType.PAREN_END, 17, ")"),
                new Token(TokenType.PAREN_END, 18, ")"),
                new Token(TokenType.OPERATOR, 19, "/"),
                new Token(TokenType.NUMBER, 20, "66"),
                new Token(TokenType.WHITESPACE, 22, "   "));
    }

    @Test
    public void booleanTokenizing() {
        expect("true", new Token(TokenType.BOOLEAN_LITERAL, 0, "true"));
        expect("false", new Token(TokenType.BOOLEAN_LITERAL, 0, "false"));
        expect("true&&false",
                new Token(TokenType.BOOLEAN_LITERAL, 0, "true"),
                new Token(TokenType.OPERATOR, 4, "&&"),
                new Token(TokenType.BOOLEAN_LITERAL, 6, "false")
        );
        expect("(true||false)&&(false&&true)",
                new Token(TokenType.PAREN_START, 0, "("),
                new Token(TokenType.BOOLEAN_LITERAL, 1, "true"),
                new Token(TokenType.OPERATOR, 5, "||"),
                new Token(TokenType.BOOLEAN_LITERAL, 7, "false"),
                new Token(TokenType.PAREN_END, 8, ")"),
                new Token(TokenType.OPERATOR, 9, "&&"),
                new Token(TokenType.PAREN_START, 11, "("),
                new Token(TokenType.BOOLEAN_LITERAL, 12, "false"),
                new Token(TokenType.OPERATOR, 17, "&&"),
                new Token(TokenType.BOOLEAN_LITERAL, 22, "true"),
                new Token(TokenType.PAREN_END, 23, ")")
        );
    }

    @Test
    public void stringTokenizing() {
        expect("\"true\"",
                new Token(TokenType.STRING_START, 0, "\""),
                new Token(TokenType.STRING_LITERAL, 1, "true"),
                new Token(TokenType.STRING_END, 5, "\"")
        );
        expect("\"1\"",
                new Token(TokenType.STRING_START, 0, "\""),
                new Token(TokenType.STRING_LITERAL, 1, "1"),
                new Token(TokenType.STRING_END, 2, "\"")
        );
        expect("\"1a1\"",
                new Token(TokenType.STRING_START, 0, "\""),
                new Token(TokenType.STRING_LITERAL, 1, "1a1"),
                new Token(TokenType.STRING_END, 4, "\"")
        );
    }

    @Test
    public void unescapedSymbolTokenizing() {
        expect("i1+i2",
                new Token(TokenType.SYMBOL, 1, "i1"),
                new Token(TokenType.OPERATOR, 4, "+"),
                new Token(TokenType.SYMBOL, 6, "i2")
        );
        expect("class1_i1+class2_i2",
                new Token(TokenType.SYMBOL, 1, "class1_i1"),
                new Token(TokenType.OPERATOR, 11, "+"),
                new Token(TokenType.SYMBOL, 13, "class2_i2")
        );
        expect("(class1_i1+class2_i2)/class3_i3",
                new Token(TokenType.PAREN_START, 0, "("),
                new Token(TokenType.SYMBOL, 2, "class1_i1"),
                new Token(TokenType.OPERATOR, 11, "+"),
                new Token(TokenType.SYMBOL, 14, "class2_i2"),
                new Token(TokenType.PAREN_END, 24, ")"),
                new Token(TokenType.OPERATOR, 25, "/"),
                new Token(TokenType.SYMBOL, 26, "class3_i3")
        );

        expect("s000002_i0009ls+s000002_i0009lt",
                new Token(TokenType.SYMBOL, 1, "s000002_i0009ls"),
                new Token(TokenType.OPERATOR, 17, "+"),
                new Token(TokenType.SYMBOL, 19, "s000002_i0009lt")
        );
    }

    @Test
    public void symbolTokenizing() {
        expect("{i1}+{i2}",
                new Token(TokenType.BRACE_START, 0, "{"),
                new Token(TokenType.SYMBOL, 1, "i1"),
                new Token(TokenType.BRACE_END, 3, "}"),
                new Token(TokenType.OPERATOR, 4, "+"),
                new Token(TokenType.BRACE_START, 5, "{"),
                new Token(TokenType.SYMBOL, 6, "i2"),
                new Token(TokenType.BRACE_END, 8, "}")
        );
        expect("{class1_i1}+{class2_i2}",
                new Token(TokenType.BRACE_START, 0, "{"),
                new Token(TokenType.SYMBOL, 1, "class1_i1"),
                new Token(TokenType.BRACE_END, 10, "}"),
                new Token(TokenType.OPERATOR, 11, "+"),
                new Token(TokenType.BRACE_START, 12, "{"),
                new Token(TokenType.SYMBOL, 13, "class2_i2"),
                new Token(TokenType.BRACE_END, 21, "}")
        );
        expect("({class1_i1}+{class2_i2})/{class3_i3}",
                new Token(TokenType.PAREN_START, 0, "("),
                new Token(TokenType.BRACE_START, 1, "{"),
                new Token(TokenType.SYMBOL, 2, "class1_i1"),
                new Token(TokenType.BRACE_END, 11, "}"),
                new Token(TokenType.OPERATOR, 11, "+"),
                new Token(TokenType.BRACE_START, 13, "{"),
                new Token(TokenType.SYMBOL, 14, "class2_i2"),
                new Token(TokenType.BRACE_END, 23, "}"),
                new Token(TokenType.PAREN_END, 24, ")"),
                new Token(TokenType.OPERATOR, 25, "/"),
                new Token(TokenType.BRACE_START, 25, "{"),
                new Token(TokenType.SYMBOL, 26, "class3_i3"),
                new Token(TokenType.BRACE_END, 35, "}")
        );

        expect("{s000002_i0009ls}+{s000002_i0009lt}",
                new Token(TokenType.BRACE_START, 0, "{"),
                new Token(TokenType.SYMBOL, 1, "s000002_i0009ls"),
                new Token(TokenType.BRACE_END, 16, "}"),
                new Token(TokenType.OPERATOR, 17, "+"),
                new Token(TokenType.BRACE_START, 18, "{"),
                new Token(TokenType.SYMBOL, 19, "s000002_i0009lt"),
                new Token(TokenType.BRACE_END, 44, "}")
        );
    }


    private void expect(String string, Token... tokens) {
        System.out.println("Tokenizing [" + string + "]");
        ExprLexer tokenizer = new ExprLexer(string);
        int expectedIndex = 0;
        while (!tokenizer.isEndOfInput()) {
            Token expected = tokens[expectedIndex++];
            Token actual = tokenizer.next();
            System.out.println(String.format("Expected: %15s, got %s", expected.toString(), actual.toString()));
            assertEquals("tokenStart", expected.getTokenStart(), actual.getTokenStart());
            assertEquals("text", expected.getString(), actual.getString());
            assertEquals("type", expected.getType(), actual.getType());

            if (!expected.equals(actual)) {
                System.err.println("Unexpected result!");
                throw new AssertionError();
            }
        }
    }


}