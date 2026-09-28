package works.lysenko.base.output;

import guru.nidi.graphviz.engine.Format;
import guru.nidi.graphviz.engine.Graphviz;
import guru.nidi.graphviz.model.Graph;

import java.io.IOException;

import static guru.nidi.graphviz.model.Factory.graph;
import static guru.nidi.graphviz.model.Factory.node;
import static works.lysenko.util.chrs.__.TO;
import static works.lysenko.util.chrs.___.PNG;
import static works.lysenko.util.chrs.____.FILE;
import static works.lysenko.util.chrs.____.INTO;
import static works.lysenko.util.data.strs.Bind.b;
import static works.lysenko.util.data.strs.Case.c;
import static works.lysenko.util.lang.word.U.UNABLE;
import static works.lysenko.util.lang.word.W.WRITE;

@SuppressWarnings({"unused", "MissingJavadoc", "ProhibitedExceptionThrown",
        "ThrowInsideCatchBlockWhichIgnoresCaughtException", "ClassIndependentOfModule"})
public record Png() {

    @SuppressWarnings({"MagicNumber", "HardCodedStringLiteral", "ThrowInsideCatchBlockWhichIgnoresCaughtException",
            "ProhibitedExceptionThrown", "HardcodedFileSeparator"})
    public static void pngStats() {

        try {
            final Graph g = graph("example1").directed().with(node("a").link(node("b")));
            Graphviz.fromGraph(g).width(200).render(Format.PNG).toFile(new java.io.File("example/ex1.png"));
        } catch (final IOException e) {
            throw new RuntimeException(b(c(UNABLE), TO, WRITE, INTO, PNG, FILE));
        }
    }
}
