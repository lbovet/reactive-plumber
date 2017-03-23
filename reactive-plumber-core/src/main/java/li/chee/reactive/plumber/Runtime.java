package li.chee.reactive.plumber;

import groovy.lang.GroovyShell;
import groovy.lang.Script;
import guru.nidi.graphviz.engine.Graphviz;
import org.codehaus.groovy.ast.*;
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
import java.io.*;
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

    private GroovyShell shell;

    private GraphTheme theme = GraphTheme.DARK;

    private boolean hideToLinks = true;

    public void setGraphTheme(GraphTheme theme) {
        this.theme = theme;
    }

    public void setGraphShowToLinks(boolean showToLinks) {
        this.hideToLinks = !showToLinks;
    }

    public Runtime withGraphTheme(GraphTheme theme) {
        setGraphTheme(theme);
        return this;
    }

    public Runtime withGraphShowToLinks(boolean showToLinks) {
        setGraphShowToLinks(showToLinks);
        return this;
    }

    public Runtime() {
    }

    public Runtime(boolean generateGraph) {
        this.generateGraph = generateGraph;
    }

    public void generateGraph(String scriptText, File file) {
        try {
            generateGraph(scriptText, Arrays.stream(file.getName().split("\\.")).reduce((a, b) -> b).get(), new FileOutputStream(file));
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public void generateGraph(String scriptText, String type, OutputStream output) {
        CompilerConfiguration config = new CompilerConfiguration();
        config.addCompilationCustomizers(getGraphOutputCustomizer(type, output));
        GroovyShell shell = new GroovyShell(config);
        shell.parse(scriptText);
    }

    public Object run(String scriptText) {
        run(scriptText, null)
    }

    public Object run(String scriptText, String filename) {
        if(shell == null) {
            CompilerConfiguration config = new CompilerConfiguration();
            if (generateGraph) {
                config.addCompilationCustomizers(getGraphOutputCustomizer());
            }
            shell = new GroovyShell(config);
        }
        Script script;
        if(filename != null) {
            shell.parse(scriptText, filename);
        } else {
            shell.parse(scriptText);
        }
        return script.run();
    }

    public CompilationCustomizer getGraphOutputCustomizer() {
        return getGraphOutputCustomizer(null, null);
    }

    public CompilationCustomizer getGraphOutputCustomizer(String type, OutputStream output) {
        return new CompilationCustomizer(CompilePhase.SEMANTIC_ANALYSIS) {
            DeclarationExpression currentDeclaration;
            Node previousNode;
            Map<Object, Node> nodes = new HashMap<>();
            Map<Object, String> edgeLabels = new HashMap<>();
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
                MethodNode method = classNode.getDeclaredMethod("run", new Parameter[0]);
                if(method == null) {
                    return;
                }
                BlockStatement block = (BlockStatement) method.getCode();
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
                                expression.getLeftExpression() instanceof ArgumentListExpression) {
                            List<Expression> vars =
                                    ((ArgumentListExpression) expression.getLeftExpression()).getExpressions();
                            StringBuilder label = new StringBuilder();
                            String method = ((StaticMethodCallExpression) right).getMethod();
                            if(!method.startsWith("from")) {
                                label.append(method);
                            }
                            ArgumentListExpression args = ((ArgumentListExpression) (((StaticMethodCallExpression) right).getArguments()));
                            Node node = new Node();
                            args.visit(
                                    new CodeVisitorSupport() {
                                        @Override
                                        public void visitStaticMethodCallExpression(StaticMethodCallExpression call) {
                                            label.append(" ").append(statics(call.getMethodAsString()));
                                            call.getArguments().visit(this);
                                        }

                                        @Override
                                        public void visitConstantExpression(ConstantExpression expression) {
                                            label.append(" ")
                                                    .append(statics(expression.getValue().toString()));
                                        }

                                        @Override
                                        public void visitVariableExpression(VariableExpression expression) {
                                            Node peer = nodes.get(expression.getAccessedVariable());
                                            graph.edge(edge(peer, node));
                                        }
                                    });
                            node.attr(Attribute.LABEL, label.toString()).attr(Attribute.FONTNAME, "arial");
                            graph.node(node);
                            vars.forEach(var -> {
                                nodes.put(((VariableExpression) var).getAccessedVariable(), node);
                                edgeLabels.put(((VariableExpression) var).getAccessedVariable(), ((VariableExpression) var).getAccessedVariable().getName());
                            });
                        }
                    }

                    private List<String> commonMethods =
                            Arrays.asList("map", "flatMap", "flatMapIterable", "flatMapSequential", "concatMap",
                                    "doOnNext", "compose", "doOnSuccess", "to", "transform", "zipWith", "value");

                    @Override
                    public void visitMethodCallExpression(MethodCallExpression call) {
                        if (currentSubGraph != null) {
                            String method = call.getMethodAsString();
                            boolean isCommon = commonMethods.contains(method);
                            boolean isTo = (method.equals("to") || method.equals("transform") || method.equals("compose")) &&
                                    VariableExpression.class.isAssignableFrom(
                                            ((ArgumentListExpression)call.getArguments()).getExpression(0).getClass());
                            StringBuilder label = new StringBuilder(isCommon || isTo ? "" : call.getMethodAsString());
                            List<Node> peers = new ArrayList<>();
                            Node node = new Node();
                            call.getArguments().visit(
                                    new CodeVisitorSupport() {
                                        @Override
                                        public void visitStaticMethodCallExpression(StaticMethodCallExpression call) {
                                            if(!commonMethods.contains(call.getMethodAsString())) {
                                                label.append(" ").append(statics(call.getMethodAsString()));
                                            }
                                            call.getArguments().visit(this);
                                        }

                                        @Override
                                        public void visitConstantExpression(ConstantExpression expression) {
                                            label.append(" ")
                                                    .append(statics(expression.getValue().toString()));
                                        }

                                        @Override
                                        public void visitVariableExpression(VariableExpression expression) {
                                            Node peer = nodes.get(expression.getAccessedVariable());
                                            if (isTo && hideToLinks) {
                                                label.append(expression.getName());
                                            }
                                            if (peer != null) {
                                                peers.add(peer);
                                            }
                                        }
                                    });
                            node.attr(Attribute.LABEL, html(label.toString().trim()));
                            if (isTo && peers.size() > 0) {
                                node.attr(Attribute.STYLE, StyleAttr.ROUNDED)
                                        .attr(Attribute.FONTCOLOR, theme.box);
                                if (!hideToLinks) {
                                    node.attr(Attribute.WIDTH, 0.3F)
                                            .attr(Attribute.FIXEDSIZE, true);
                                    Edge edge = edge(node, peers.get(0)).attr(Attribute.STYLE, StyleAttr.DASHED);
                                    graph.edge(edge);
                                }
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
                        if (currentSubGraph != null && call.getMethodAsString().startsWith("from")) {
                            Node[] node = new Node[1];
                            call.getArguments().visit(
                                    new CodeVisitorSupport() {
                                        @Override
                                        public void visitStaticMethodCallExpression(StaticMethodCallExpression call) {
                                            List<Node> sources = new ArrayList<>();
                                            StringBuilder label = new StringBuilder();
                                            if(!commonMethods.contains(call.getMethod())) {
                                                label.append(html(statics(call.getMethodAsString())));
                                            }
                                            call.getArguments().visit(new CodeVisitorSupport() {
                                                @Override
                                                public void visitStaticMethodCallExpression(StaticMethodCallExpression call) {
                                                    label.append(" ").append(statics(call.getMethodAsString()));
                                                    call.getArguments().visit(this);
                                                }

                                                @Override
                                                public void visitVariableExpression(VariableExpression expression) {
                                                    sources.add(nodes.get(expression.getAccessedVariable()));
                                                }

                                                @Override
                                                public void visitConstantExpression(ConstantExpression expression) {
                                                    label.append(" ")
                                                            .append(statics(expression.getValue().toString()));
                                                }
                                            });
                                            node[0] = new Node()
                                                    .attr(Attribute.LABEL, label.toString())
                                                    .attr(Attribute.FONTNAME, "arial");
                                            if (sources.size() == 0) {
                                                if (!nodes.containsKey(call.getText())) {
                                                    nodes.put(call.getText(), node[0]);
                                                    graph.node(node[0]);
                                                }
                                            } else {
                                                currentSubGraph.node(node[0]);
                                                AtomicInteger i = new AtomicInteger();
                                                sources.forEach(source -> {
                                                    Edge edge = edge(source, node[0]);
                                                    if(call.getMethod().equals("concat")) {
                                                        edge.attr(Attribute.LABEL, ""+i.incrementAndGet());
                                                    }
                                                    graph.edge(edge);
                                                });
                                            }
                                            if (previousNode != null) {
                                                graph.edge(edge(node[0], previousNode));
                                            } else {
                                                nodes.putIfAbsent(currentDeclaration.getVariableExpression(), node[0]);
                                            }
                                        }

                                        @Override
                                        public void visitConstantExpression(ConstantExpression expression) {
                                            Node source = new Node()
                                                    .attr(Attribute.LABEL, expression.getText())
                                                    .attr(Attribute.FONTNAME, "arial");
                                            graph.node(source);
                                            if(previousNode != null) {
                                                Edge edge = edge(source, previousNode);
                                                graph.edge(edge);
                                            } else {
                                                nodes.putIfAbsent(currentDeclaration.getVariableExpression(), source);
                                            }
                                        }

                                        @Override
                                        public void visitVariableExpression(VariableExpression expression) {
                                            if (!expression.getAccessedVariable().getName().equals("it")) {
                                                if(previousNode != null) {
                                                    Edge edge = edge(nodes.get(expression.getAccessedVariable()), previousNode);
                                                    Optional.ofNullable(edgeLabels.get(expression.getAccessedVariable())).map(label -> edge.attr(Attribute.LABEL, label));
                                                    graph.edge(edge);
                                                } else{
                                                    Node source = nodes.get(expression.getAccessedVariable());
                                                    if(node != null) {
                                                        nodes.putIfAbsent(currentDeclaration.getVariableExpression(), source);
                                                    }
                                                }
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
                                            edge[0] = edge(source, target);
                                            edge[0].attr(Attribute.LABEL, "" + count.incrementAndGet());
                                            graph.edge(edge[0]);
                                        }
                                    });
                            if (count.get() == 1) {
                                edge[0].attr(Attribute.LABEL, "");
                            }
                        } else if (call.getMethodAsString().equals("concurrent")) {
                            currentSubGraph.attr(Attribute.LABEL, "// " + currentSubGraph.attr(Attribute.LABEL));
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
                Graphviz g = Graphviz.fromString(out.toString().replaceAll("\\r", ""));
                g = g.scale(1f);

                if (type == null) {
                    g.renderToFile(new File("target/graph.png"));
                    try {
                        Files.write(Paths.get("target/graph.svg"), g.createSvg().getBytes(Charset.forName("UTF8")));
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                } else {
                    if ("svg".equals(type)) {
                        try {
                            output.write(g.createSvg().getBytes(Charset.forName("UTF8")));
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    } else {
                        try {
                            File f = File.createTempFile("tmp", "." + type);
                            g.renderToFile(f);
                            Files.copy(f.toPath(), output);
                            f.delete();
                            f.deleteOnExit();
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }
            }

            private String html(String s) {
                return s;
            }

            private Edge edge(Node from, Node to) {
                if(to==null) {
                    to = new Node().id("unknown");
                }
                if(from==null) {
                    from = new Node().id("unknown");
                }
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
