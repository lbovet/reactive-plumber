package li.chee.rx.plumber;

import groovy.lang.GroovyShell;
import groovy.lang.Script;
import guru.nidi.graphviz.engine.Graphviz;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.CodeVisitorSupport;
import org.codehaus.groovy.ast.Parameter;
import org.codehaus.groovy.ast.Variable;
import org.codehaus.groovy.ast.expr.*;
import org.codehaus.groovy.ast.stmt.BlockStatement;
import org.codehaus.groovy.classgen.GeneratorContext;
import org.codehaus.groovy.control.CompilationFailedException;
import org.codehaus.groovy.control.CompilePhase;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.codehaus.groovy.control.SourceUnit;
import org.codehaus.groovy.control.customizers.CompilationCustomizer;
import org.kohsuke.graphviz.*;
import org.kohsuke.graphviz.Shape;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static java.awt.Color.*;

/**
 * Evaluate groovy scripts.
 */
public class Runtime {

    public enum GraphTheme {
        LIGHT(WHITE, GRAY, DARK_GRAY, WHITE),
        DARK(DARK_GRAY, GRAY, LIGHT_GRAY, BLACK);

        public Color background, arrow, box, text;
        GraphTheme(Color... colors) {
            background = colors[0];
            arrow = colors[1];
            box = colors[2];
            text = colors[3];
        }
    }

    private boolean generateGraph = false;

    private GraphTheme theme = GraphTheme.DARK;

    public void setGraphTheme(GraphTheme theme) {
        this.theme = theme;
    }

    public Runtime() {}

    public Runtime(boolean generateGraph) {
        this.generateGraph = generateGraph;
    }

    public Object run(String scriptText) {
        CompilerConfiguration config = new CompilerConfiguration();
        if(generateGraph) {
            config.addCompilationCustomizers(graphOutputCustomizer());
        }
        GroovyShell shell = new GroovyShell(config);
        Script script = shell.parse(scriptText);
        return script.run();
    }

    private CompilationCustomizer graphOutputCustomizer() {
        return new CompilationCustomizer(CompilePhase.SEMANTIC_ANALYSIS) {
            DeclarationExpression currentDeclaration;
            Node previousNode;
            Map<Object, Node> nodes = new HashMap<>();
            Style edgeStyle = new Style()
                    .attr(Attribute.COLOR, theme.arrow)
                    .attr(Attribute.FONTNAME, "arial")
                    .attr(Attribute.FONTSIZE, 7f)
                    .attr(Attribute.FONTCOLOR, theme.box)
                    .attr(Attribute.ARROWSIZE, 0.7f);
            Style nodeStyle = new Style()
                    .attr(Attribute.FONTNAME, "arial")
                    .attr(Attribute.FONTSIZE, 8f)
                    .attr(Attribute.COLOR, theme.box)
                    .attr(Attribute.FONTCOLOR, theme.text)
                    .attr(Attribute.SHAPE, Shape.RECTANGLE)
                    .attr(Attribute.HEIGHT, 0.2F)
                    .attr(Attribute.MARGIN, 0.11F)
                    .attr("style", "rounded,filled");
            Graph graph = new Graph()
                    .style(new Style()
                            .attr(Attribute.BGCOLOR, theme.background))
                    .nodeWith(nodeStyle)
                    .edgeWith(edgeStyle);
            Graph currentSubGraph;
            List<Node> currentSources = new ArrayList<>();

            @Override
            public void call(SourceUnit sourceUnit, GeneratorContext generatorContext, ClassNode classNode) throws CompilationFailedException {
                BlockStatement block = (BlockStatement) classNode.getDeclaredMethod("run", new Parameter[0]).getCode();
                block.visit(new CodeVisitorSupport() {

                    @Override
                    public void visitDeclarationExpression(DeclarationExpression expression) {
                        Expression right = expression.getRightExpression();
                        if (right instanceof StaticMethodCallExpression &&
                                ((StaticMethodCallExpression) right).getMethod().equals("pipe") ||
                                right instanceof ClosureExpression) {
                            Variable var = expression.getVariableExpression().getAccessedVariable();
                            String id = objId(var);
                            String name = var.getName();
                            currentSubGraph = subGraph(id, name).attr(Attribute.RANKSEP, 10F);
                            currentDeclaration = expression;
                            graph.subGraph(currentSubGraph);
                            expression.getRightExpression().visit(this);
                            currentSources.forEach(source -> graph.edge(edge(source, previousNode)));
                            if (currentSources.size() > 0) {
                                currentSubGraph.attr(Attribute.LABEL, currentSubGraph.attr(Attribute.LABEL) + " *");
                                currentSubGraph.attr(Attribute.STYLE, StyleAttr.BOLD);
                            }
                            currentSubGraph = null;
                            previousNode = null;
                            currentSources.clear();
                        } else if (right instanceof StaticMethodCallExpression &&
                                ((StaticMethodCallExpression) right).getMethod().equals("split")) {
                            StringBuilder label = new StringBuilder("split");
                            ArgumentListExpression args = ((ArgumentListExpression) (((StaticMethodCallExpression) right).getArguments()));
                            Node node = new Node();
                            args.getExpression(0).visit(
                                    new CodeVisitorSupport() {
                                        @Override
                                        public void visitStaticMethodCallExpression(StaticMethodCallExpression call) {
                                            label.append(" ").append(statics(call.getMethodAsString()));
                                            call.getArguments().visit(this);
                                        }

                                        @Override
                                        public void visitVariableExpression(VariableExpression expression) {
                                            Node peer = nodes.get(expression.getAccessedVariable());
                                            graph.edge(edge(peer, node).attr(Attribute.STYLE, StyleAttr.DASHED));
                                        }
                                    });
                            args.getExpression(1).visit(
                                    new CodeVisitorSupport() {
                                        @Override
                                        public void visitVariableExpression(VariableExpression expression) {
                                            Node peer = nodes.get(expression.getAccessedVariable());
                                            graph.edge(edge(peer, node));
                                        }
                                    });
                            node.attr(Attribute.LABEL, label.toString()).attr(Attribute.FONTNAME, "arial italic");
                            graph.node(node);
                            List<Expression> vars =
                                    ((ArgumentListExpression) expression.getLeftExpression()).getExpressions();
                            vars.forEach(var -> nodes.put(((VariableExpression) var).getAccessedVariable(), node));
                        }
                    }

                    @Override
                    public void visitMethodCallExpression(MethodCallExpression call) {
                        if (currentSubGraph != null) {
                            String method = call.getMethodAsString();
                            boolean isCommon = method.equals("map") || method.equals("doOnNext") || method.equals("compose");
                            boolean isTo = method.equals("to");
                            StringBuilder label = new StringBuilder(isCommon || isTo ? "" : call.getMethodAsString());
                            List<Node> peers = new ArrayList<>();
                            Node node = new Node();
                            call.getArguments().visit(
                                    new CodeVisitorSupport() {
                                        @Override
                                        public void visitStaticMethodCallExpression(StaticMethodCallExpression call) {
                                            label.append(" ").append(statics(call.getMethodAsString()));
                                            call.getArguments().visit(this);
                                        }

                                        @Override
                                        public void visitVariableExpression(VariableExpression expression) {
                                            Node peer = nodes.get(expression.getAccessedVariable());
                                            if (peer != null) {
                                                peers.add(peer);
                                            }
                                        }
                                    });
                            node.attr(Attribute.LABEL, html(label.toString().trim()));
                            if (isCommon) {
                                node.attr(Attribute.FONTNAME, "arial");
                            }
                            if (isTo && peers.size() > 0) {
                                node.attr("shape", "cds")
                                        .attr(Attribute.WIDTH, 0.3F)
                                        .attr(Attribute.FIXEDSIZE, true);
                                Edge edge = edge(node, peers.get(0)).attr(Attribute.STYLE, StyleAttr.DASHED);
                                graph.edge(edge);
                            } else {
                                peers.forEach(peer -> {
                                    Edge edge = edge(peer, node).attr(Attribute.STYLE, StyleAttr.DASHED);
                                    graph.edge(edge);
                                });
                            }
                            if (!nodes.containsKey(currentDeclaration.getVariableExpression())) {
                                nodes.put(currentDeclaration.getVariableExpression(), node);
                            }
                            currentSubGraph.node(node);
                            if (previousNode != null) {
                                Edge edge = edge(node, previousNode);
                                currentSubGraph.edge(edge);
                            }
                            previousNode = node;
                            call.getObjectExpression().visit(this);
                        }
                    }

                    @Override
                    public void visitStaticMethodCallExpression(StaticMethodCallExpression call) {
                        if (currentSubGraph != null && call.getMethodAsString().equals("from")) {
                            Node[] node = new Node[1];
                            call.getArguments().visit(
                                    new CodeVisitorSupport() {
                                        @Override
                                        public void visitStaticMethodCallExpression(StaticMethodCallExpression call) {
                                            List<Node> sources = new ArrayList<>();
                                            call.getArguments().visit(new CodeVisitorSupport() {
                                                @Override
                                                public void visitVariableExpression(VariableExpression expression) {
                                                    sources.add(nodes.get(expression.getAccessedVariable()));
                                                }
                                            });
                                            node[0] = new Node()
                                                    .attr(Attribute.LABEL, html(statics(call.getMethodAsString())))
                                                    .attr(Attribute.FONTNAME, "arial italic");
                                            if (sources.size() == 0) {
                                                if (!nodes.containsKey(call.getText())) {
                                                    nodes.put(call.getText(), node[0]);
                                                    graph.node(node[0]);
                                                }
                                            } else {
                                                currentSubGraph.node(node[0]);
                                                sources.forEach(source -> graph.edge(edge(source, node[0])));
                                            }
                                            if (previousNode != null) {
                                                graph.edge(edge(node[0], previousNode));
                                            } else {
                                                nodes.putIfAbsent(currentDeclaration.getVariableExpression(), node[0]);
                                            }
                                        }

                                        @Override
                                        public void visitVariableExpression(VariableExpression expression) {
                                            if (!expression.getAccessedVariable().getName().equals("it")) {
                                                graph.edge(edge(nodes.get(expression.getAccessedVariable()), previousNode));
                                            } else {
                                                if (currentSources.isEmpty()) {
                                                    currentSubGraph.attr(Attribute.STYLE, StyleAttr.ROUNDED);
                                                    nodes.put(currentDeclaration.getVariableExpression(), previousNode);
                                                }
                                            }
                                        }
                                    });
                        } else if (call.getMethodAsString().equals("drain")) {
                            AtomicInteger count = new AtomicInteger();
                            Node target = new Node()
                                    .attr(Attribute.SHAPE, Shape.CIRCLE)
                                    .attr(Attribute.WIDTH, 0.2F)
                                    .attr(Attribute.FIXEDSIZE, true)
                                    .attr(Attribute.LABEL, "");
                            graph.node(target);
                            Edge[] edge = new Edge[1];
                            call.getArguments().visit(
                                    new CodeVisitorSupport() {
                                        @Override
                                        public void visitVariableExpression(VariableExpression expression) {
                                            Node source = nodes.get(expression.getAccessedVariable());
                                            edge[0] = edge(source, target)
                                                    .attr(Attribute.LABEL, "" + count.incrementAndGet());
                                            graph.edge(edge[0]);
                                        }
                                    });
                            if (count.get() == 1) {
                                edge[0].attr(Attribute.LABEL, "");
                            }
                        } else if (call.getMethodAsString().equals("parallel")) {
                            currentSubGraph.attr(Attribute.LABEL, currentSubGraph.attr(Attribute.LABEL) + " //");
                        } else if (call.getMethodAsString().equals("each")) {
                            List<Expression> args = ((ArgumentListExpression) call.getArguments()).getExpressions();
                            args.forEach(arg -> {
                                if (!(arg instanceof ClosureExpression)) {
                                    arg.visit(new CodeVisitorSupport() {
                                        @Override
                                        public void visitVariableExpression(VariableExpression expression) {
                                            currentSources.add(nodes.get(expression.getAccessedVariable()));
                                        }
                                    });
                                }
                            });
                        }
                        call.getArguments().visit(this);
                    }

                    private String objId(Object obj) {
                        return "obj" + obj.toString().split("@")[1].split("\\[")[0];
                    }

                    private Graph subGraph(String id, String name) {
                        Graph subGraph = new Graph().id("cluster_" + id).nodeWith(nodeStyle).edgeWith(edgeStyle);
                        subGraph.attr(Attribute.LABEL, name);
                        subGraph.attr(Attribute.FONTNAME, "arial");
                        subGraph.attr(Attribute.FONTSIZE, 8f);
                        subGraph.attr(Attribute.COLOR, theme.box);
                        subGraph.attr(Attribute.FONTCOLOR, theme.box);
                        return subGraph;
                    }
                });
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                graph.writeTo(out);
                Graphviz g = Graphviz.fromString(out.toString());
                g.renderToFile(new File("target/graph.png"));
                try {
                    Files.write(Paths.get("target/graph.svg"), g.createSvg().getBytes(Charset.forName("UTF8")));
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }

            private String html(String s) {
                return s;
            }

            private Edge edge(Node from, Node to) {
                assert from != null;
                assert to != null;
                return new Edge(from, to);
            }

            private String statics(String name) {
                name = Arrays.stream(name.split("\\.")).reduce((a, b) -> b).orElse(null);
                if ((name.startsWith("set") || name.startsWith("get"))
                        && name.length() > 3) {
                    String ret = name.substring(3, 4);
                    if (!name.substring(4).equals(name.substring(4).toUpperCase())) {
                        ret = ret.toLowerCase();
                    }
                    if (name.length() > 4)
                        ret += name.substring(4);
                    return i(ret);
                } else {
                    return i(name);
                }
            }

            private String i(String s) {
                return s;
            }
        };
    }
}
