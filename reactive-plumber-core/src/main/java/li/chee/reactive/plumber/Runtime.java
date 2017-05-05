package li.chee.reactive.plumber;

import com.floreysoft.jmte.Engine;
import groovy.lang.GroovyShell;
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
import syntaxhighlight.PrettifyToHtml;

import java.awt.Color;
import java.io.*;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    private Graph overview = null;
    private Map<String, Node> scriptNodes = new HashMap<>();
    private Map<String, Edge> scriptEdges = new HashMap<>();
    private Map<String, String> junctions = new HashMap<>();

    private GroovyShell shell;
    private GraphTheme theme = GraphTheme.DARK;
    private String graphOutputDir = "target/graphs";

    private boolean hideToLinks = true;

    public void setGraphTheme(GraphTheme theme) {
        this.theme = theme;
    }

    public void setGraphOutputDir(String graphOutputDir) {
        this.graphOutputDir = graphOutputDir;
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

    public Runtime withGraphOutputDir(String dir) {
        setGraphOutputDir(dir);
        return this;
    }

    public Runtime() {
    }

    public Runtime(boolean generateGraph) {
        this.generateGraph = generateGraph;
    }

    public Runtime generateGraph(String scriptText, File file) {
        try {
            generateGraph(scriptText, Arrays.stream(file.getName().split("\\.")).reduce((a, b) -> b).get(), new FileOutputStream(file));
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
        return this;
    }

    public Runtime generateGraph(String scriptText, String type, OutputStream output) {
        CompilerConfiguration config = new CompilerConfiguration();
        config.addCompilationCustomizers(getGraphOutputCustomizer(type, output));
        GroovyShell shell = new GroovyShell(config);
        shell.parse(scriptText);
        return this;
    }

    public Runtime generateGraphs(URI... sources) {
        CompilerConfiguration config = new CompilerConfiguration();
        config.addCompilationCustomizers(getGraphOutputCustomizer());
        GroovyShell shell = new GroovyShell(config);
        try {
            for (URI source : sources) {
                shell.parse(source);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return this;
    }

    public Runtime generateOverviewGraph() {
        return generateOverviewGraph(null, null);
    }

    public Runtime generateOverviewGraph(String type, OutputStream output) {
        if (overview != null) {
            scriptNodes.values().forEach(node -> overview.node(node));
            scriptEdges.values().forEach(edge -> overview.edge(edge));
            createGraph(overview, "overview", type, output);
        }
        return this;
    }

    public Runtime run(URI... sources) {
        init();
        try {
            for (URI source : sources) {
                shell.evaluate(source);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return this;
    }

    public Runtime run(String scriptText) {
        init();
        shell.evaluate(scriptText);
        return this;
    }

    public Runtime run(String scriptText, String filename) {
        init();
        shell.evaluate(scriptText, filename);
        return this;
    }

    private void init() {
        if (shell == null) {
            CompilerConfiguration config = new CompilerConfiguration();
            if (generateGraph) {
                config.addCompilationCustomizers(getGraphOutputCustomizer());
            }
            shell = new GroovyShell(config);
        }
    }

    public CompilationCustomizer getGraphOutputCustomizer() {
        return getGraphOutputCustomizer(null, null);
    }

    public CompilationCustomizer getGraphOutputCustomizer(String type, OutputStream output) {
        return new CompilationCustomizer(CompilePhase.SEMANTIC_ANALYSIS) {
            DeclarationExpression currentDeclaration;
            Node previousNode;
            Map<Object, Node> nodes;
            Map<Object, String> edgeLabels;
            Style edgeStyle = new Style()
                    .attr(Attribute.COLOR, theme.arrow)
                    .attr(Attribute.FONTNAME, "arial")
                    .attr(Attribute.FONTSIZE, 7f)
                    .attr(Attribute.FONTCOLOR, theme.box)
                    .attr(Attribute.ARROWSIZE, 0.7f)
                    .attr("tooltip", " ");
            Style nodeStyle = new Style()
                    .attr(Attribute.FONTNAME, "arial")
                    .attr(Attribute.FONTSIZE, 8f)
                    .attr(Attribute.COLOR, theme.box)
                    .attr(Attribute.FONTCOLOR, theme.text)
                    .attr(Attribute.SHAPE, Shape.RECTANGLE)
                    .attr(Attribute.HEIGHT, 0.2F)
                    .attr(Attribute.MARGIN, 0.11F)
                    .attr("style", "rounded,filled")
                    .attr("tooltip", " ");
            Style scriptStyle = new Style()
                    .attr(Attribute.FONTNAME, "arial")
                    .attr(Attribute.FONTSIZE, 8f)
                    .attr(Attribute.COLOR, theme.box)
                    .attr(Attribute.FONTCOLOR, theme.text)
                    .attr(Attribute.SHAPE, Shape.RECTANGLE)
                    .attr(Attribute.HEIGHT, 0.2F)
                    .attr(Attribute.MARGIN, 0.11F)
                    .attr("style", "filled")
                    .attr("tooltip", " ");
            Graph graph;
            Graph currentSubGraph;
            List<Node> currentSources;

            @Override
            public void call(SourceUnit sourceUnit, GeneratorContext generatorContext, ClassNode classNode) throws CompilationFailedException {
                final String scriptName = new ArrayDeque<>(Arrays.asList(sourceUnit.getName().split("/"))).removeLast().split("\\.")[0];
                try {
                    String html = PrettifyToHtml.parseAndConvert(readStream(sourceUnit.getSource().getURI().toURL().openStream()));
                    HashMap<String,Object> vars = new HashMap<>();
                    vars.put("title", scriptName);
                    vars.put("content", html);
                    vars.put("upDestination", scriptName);
                    html = applyTemplate(vars);
                    new File("target/graphs").mkdirs();
                    Files.write(Paths.get("target/graphs/"+scriptName+"-source.html"), html.getBytes());
                } catch(IOException e) {
                    throw new RuntimeException();
                }
                MethodNode method = classNode.getDeclaredMethod("run", new Parameter[0]);
                if (method == null) {
                    return;
                }
                currentDeclaration = null;
                previousNode = null;
                nodes = new HashMap<>();
                edgeLabels = new HashMap<>();
                currentSubGraph = null;
                currentSources = new ArrayList<>();
                graph = new Graph()
                        .style(new Style().attr(Attribute.BGCOLOR, theme.background))
                        .nodeWith(nodeStyle)
                        .edgeWith(edgeStyle)
                        .attr("tooltip", " ");
                if (overview == null) {
                    overview = new Graph()
                            .style(new Style().attr(Attribute.BGCOLOR, theme.background))
                            .nodeWith(scriptStyle)
                            .edgeWith(edgeStyle)
                            .attr("tooltip", " ");
                }
                Map<String, Graph> otherGraphs = new HashMap<>();
                BlockStatement block = (BlockStatement) method.getCode();
                block.visit(new CodeVisitorSupport() {
                    @Override
                    public void visitDeclarationExpression(DeclarationExpression expression) {
                        Expression right = expression.getRightExpression();
                        if (right instanceof StaticMethodCallExpression &&
                                (((StaticMethodCallExpression) right).getMethod().equals("pipe") ||
                                        ((StaticMethodCallExpression) right).getMethod().equals("tube")) ||
                                right instanceof ClosureExpression) {
                            Variable var = expression.getVariableExpression().getAccessedVariable();
                            String id = objId(var);
                            String name = var.getName();
                            currentSubGraph = subGraph(id, name).attr(Attribute.RANKSEP, 10F);
                            currentSubGraph.attr(Attribute.URL, sourceLink(expression, true));
                            currentDeclaration = expression;
                            graph.subGraph(currentSubGraph);
                            expression.getRightExpression().visit(this);
                            currentSources.forEach(source -> graph.edge(edge(source, previousNode)));
                            if (currentSources.size() > 0) {
                                currentSubGraph.attr(Attribute.LABEL, currentSubGraph.attr(Attribute.LABEL) + " *");
                                currentSubGraph.attr(Attribute.STYLE, StyleAttr.BOLD);
                            }
                            if (right instanceof StaticMethodCallExpression && ((StaticMethodCallExpression) right).getMethod().equals("tube")) {
                                currentSubGraph.attr(Attribute.STYLE, StyleAttr.ROUNDED);
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
                            if (!method.startsWith("from")) {
                                label.append(method);
                            }
                            ArgumentListExpression args = ((ArgumentListExpression) (((StaticMethodCallExpression) right).getArguments()));
                            Node node = new Node();
                            args.visit(
                                    new CodeVisitorSupport() {
                                        @Override
                                        public void visitStaticMethodCallExpression(StaticMethodCallExpression call) {
                                            if (!commonMethods.contains(call.getMethodAsString())) {
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
                                            graph.edge(edge(peer, node));
                                        }
                                    });
                            node.attr(Attribute.LABEL, label.toString()).attr(Attribute.FONTNAME, "arial");
                            node.attr(Attribute.URL, sourceLink(expression, false));
                            graph.node(node);
                            vars.forEach(var -> {
                                nodes.put(((VariableExpression) var).getAccessedVariable(), node);
                                edgeLabels.put(((VariableExpression) var).getAccessedVariable(), ((VariableExpression) var).getAccessedVariable().getName());
                            });
                        }
                    }

                    private String sourceLink(Expression expression, boolean multiline) {
                        return scriptName+"-source.html#"+expression.getLineNumber()+(multiline ? "-"+expression.getLastLineNumber(): "");
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
                                            ((ArgumentListExpression) call.getArguments()).getExpression(0).getClass());
                            StringBuilder label = new StringBuilder(isCommon || isTo ? "" : call.getMethodAsString());
                            List<Node> peers = new ArrayList<>();
                            Node node = new Node();
                            call.getArguments().visit(
                                    new CodeVisitorSupport() {
                                        @Override
                                        public void visitStaticMethodCallExpression(StaticMethodCallExpression call) {
                                            if (!commonMethods.contains(call.getMethodAsString())) {
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
                                        public void visitPropertyExpression(PropertyExpression expression) {
                                            visitPropertyExpressionInternal(expression, peers, this);
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
                            node.attr(Attribute.URL, sourceLink(call.getMethod(), false));
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
                        } else if (call.getMethodAsString().equals("to")) {
                            Node[] src = new Node[1];
                            call.getObjectExpression().visit(new CodeVisitorSupport() {
                                @Override
                                public void visitVariableExpression(VariableExpression expression) {
                                    src[0] = nodes.get(expression.getAccessedVariable());
                                }
                            });
                            call.getArguments().visit(new CodeVisitorSupport() {
                                @Override
                                public void visitPropertyExpression(PropertyExpression expression) {
                                    if (expression.getObjectExpression() instanceof PropertyExpression) {
                                        String name = expression.getProperty().getText();
                                        PropertyExpression exp = (PropertyExpression) expression.getObjectExpression();
                                        Expression obj = exp.getObjectExpression();
                                        if (obj instanceof ClassExpression) {
                                            Expression prop = exp.getProperty();
                                            Graph otherGraph = otherGraphs.get(prop.getText());
                                            if (otherGraph == null) {
                                                otherGraph = subGraph(prop.getText(), prop.getText());
                                                otherGraph.attr(Attribute.STYLE, StyleAttr.DASHED);
                                                otherGraph.attr(Attribute.URL, makeUrl(prop.getText()));
                                                graph.subGraph(otherGraph);
                                                otherGraphs.put(prop.getText(), otherGraph);
                                            }
                                            String id = (exp.getText() + "." + name).replace(".", "_");
                                            Node target = new Node().id(id)
                                                    .attr(Attribute.LABEL, name);
                                            target.attr(Attribute.URL, sourceLink(expression, false));
                                            otherGraph.node(target);
                                            if (src[0] != null) {
                                                graph.edge(src[0], target);
                                            }
                                            if (exp.getProperty() instanceof ConstantExpression) {
                                                String otherScript = exp.getProperty().getText();
                                                String tgtName = otherScript + "_" + name;
                                                junctions.put(tgtName, scriptName);
                                            }
                                        }
                                    } else {
                                        expression.getObjectExpression().visit(this);
                                        expression.getProperty().visit(this);
                                    }
                                }
                            });
                        }
                    }

                    private void linkToPrevious(Node source) {
                        if (previousNode != null) {
                            Edge edge = edge(source, previousNode);
                            graph.edge(edge);
                        } else {
                            nodes.putIfAbsent(currentDeclaration.getVariableExpression(), source);
                        }
                    }

                    private void visitPropertyExpressionInternal(PropertyExpression expression, List<Node> sources, GroovyCodeVisitor visitor) {
                        if (expression.getObjectExpression() instanceof PropertyExpression) {
                            String name = expression.getProperty().getText();
                            PropertyExpression exp = (PropertyExpression) expression.getObjectExpression();
                            Expression obj = exp.getObjectExpression();
                            if (obj instanceof ClassExpression) {
                                Expression prop = exp.getProperty();
                                Graph sourceGraph = otherGraphs.get(prop.getText());
                                if (sourceGraph == null) {
                                    sourceGraph = subGraph(prop.getText(), prop.getText());
                                    sourceGraph.attr(Attribute.STYLE, StyleAttr.DASHED);
                                    if(!prop.getText().equals("exports")) {
                                        sourceGraph.attr(Attribute.URL, makeUrl(prop.getText()));
                                    } else {
                                        sourceGraph.attr(Attribute.LABEL, "["+sourceGraph.attr(Attribute.LABEL)+"]");
                                    }
                                    graph.subGraph(sourceGraph);
                                    otherGraphs.put(prop.getText(), sourceGraph);
                                }
                                String id = (exp.getText() + "." + name).replace(".", "_");
                                Node source = new Node().id(id)
                                        .attr(Attribute.LABEL, name);
                                source.attr(Attribute.URL, sourceLink(expression, false));
                                if (sources != null) {
                                    sources.add(source);
                                } else {
                                    linkToPrevious(source);
                                }
                                sourceGraph.node(source);
                                if (exp.getProperty() instanceof ConstantExpression) {
                                    String src;
                                    if (exp.getProperty().getText().equals("exports")) {
                                        src = junctions.get(scriptName + "_" + name);
                                    } else {
                                        src = exp.getProperty().getText();
                                    }
                                    scriptEdge(src, scriptName, name);
                                }
                            }
                        } else {
                            expression.getObjectExpression().visit(visitor);
                            expression.getProperty().visit(visitor);
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
                                            if (!commonMethods.contains(call.getMethodAsString())) {
                                                label.append(html(statics(call.getMethodAsString())));
                                            }
                                            call.getArguments().visit(new CodeVisitorSupport() {
                                                @Override
                                                public void visitStaticMethodCallExpression(StaticMethodCallExpression call) {
                                                    if (!commonMethods.contains(call.getMethodAsString())) {
                                                        label.append(" ").append(statics(call.getMethodAsString()));
                                                    }
                                                    call.getArguments().visit(this);
                                                }

                                                @Override
                                                public void visitVariableExpression(VariableExpression expression) {
                                                    sources.add(nodes.get(expression.getAccessedVariable()));
                                                }

                                                @Override
                                                public void visitPropertyExpression(PropertyExpression expression) {
                                                    visitPropertyExpressionInternal(expression, sources, this);
                                                }

                                                @Override
                                                public void visitConstantExpression(ConstantExpression expression) {
                                                    if (!expression.getValue().toString().equals("exports")) {
                                                        label.append(" ")
                                                                .append(statics(expression.getValue().toString()));
                                                    }
                                                }
                                            });
                                            node[0] = new Node()
                                                    .attr(Attribute.LABEL, label.toString())
                                                    .attr(Attribute.FONTNAME, "arial")
                                                    .attr(Attribute.URL, sourceLink(call, false));

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
                                                    if (call.getMethod().equals("concat")) {
                                                        edge.attr(Attribute.LABEL, "" + i.incrementAndGet());
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
                                        public void visitPropertyExpression(PropertyExpression expression) {
                                            visitPropertyExpressionInternal(expression, null, this);
                                        }

                                        @Override
                                        public void visitConstantExpression(ConstantExpression expression) {
                                            Node source = new Node()
                                                    .attr(Attribute.LABEL, expression.getText())
                                                    .attr(Attribute.URL, sourceLink(expression, false));
                                            graph.node(source);
                                            linkToPrevious(source);
                                        }

                                        @Override
                                        public void visitVariableExpression(VariableExpression expression) {
                                            if (!expression.getAccessedVariable().getName().equals("it")) {
                                                if (previousNode != null) {
                                                    Edge edge = edge(nodes.get(expression.getAccessedVariable()), previousNode);
                                                    Optional.ofNullable(edgeLabels.get(expression.getAccessedVariable())).map(label -> edge.attr(Attribute.LABEL, label));
                                                    graph.edge(edge);
                                                } else {
                                                    Node source = nodes.get(expression.getAccessedVariable());
                                                    if (node != null) {
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
                                    .attr(Attribute.LABEL, "")
                                    .attr(Attribute.URL, sourceLink(call, true));
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
                                            scriptEdge(scriptName, null, expression.getAccessedVariable().getName());
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


                    @Override
                    public void visitBinaryExpression(BinaryExpression expression) {
                        if (expression.getOperation().getText().equals("=")) {
                            expression.getRightExpression().visit(new CodeVisitorSupport() {
                                @Override
                                public void visitVariableExpression(VariableExpression expression) {
                                    Node target = new Node()
                                            .attr(Attribute.SHAPE, Shape.CIRCLE)
                                            .attr(Attribute.STYLE, StyleAttr.SOLID)
                                            .attr(Attribute.WIDTH, 0.2F)
                                            .attr(Attribute.FIXEDSIZE, true)
                                            .attr(Attribute.LABEL, "-")
                                            .attr(Attribute.URL, sourceLink(expression, false));
                                    graph.node(target);
                                    graph.edge(edge(nodes.get(expression.getAccessedVariable()), target));
                                }
                            });
                        }

                    }

                    private String objId(Object obj) {
                        String s = "obj" + obj.toString().split("@")[1].split("\\[")[0];
                        return s;
                    }

                });
                createGraph(graph, scriptName, type, output);
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

            private String html(String s) {
                return s;
            }

            private Edge edge(Node from, Node to) {
                if (to == null) {
                    to = new Node().id("unknown");
                }
                if (from == null) {
                    from = new Node().id("unknown");
                }
                return new Edge(from, to);
            }

            private Node end = new Node()
                    .attr(Attribute.SHAPE, Shape.CIRCLE)
                    .attr(Attribute.WIDTH, 0.2F)
                    .attr(Attribute.FIXEDSIZE, true)
                    .attr(Attribute.LABEL, "")
                    .attr(Attribute.STYLE, StyleAttr.FILLED);

            private void scriptEdge(String from, String to, String label) {
                scriptNodes.putIfAbsent(from, new Node().id(from).attr(Attribute.URL, makeUrl(from)));
                Node target;
                if (to == null) {
                    to = "<<<END>>>";
                    target = end;
                } else {
                    target = new Node().id(to).attr(Attribute.URL, makeUrl(to));
                }
                scriptNodes.putIfAbsent(to, target);
                scriptEdges.putIfAbsent(from + "_" + to, new Edge(scriptNodes.get(from), scriptNodes.get(to)));
                Edge edge = scriptEdges.get(from + "_" + to);
                if (edge.attr(Attribute.LABEL) == null || !edge.attr(Attribute.LABEL).matches(".*(\\\\l)?" + label + "\\\\?.*")) {
                    edge.attr(Attribute.LABEL, (edge.attr(Attribute.LABEL) != null ? edge.attr(Attribute.LABEL) + "\\l" : "") + label.trim());
                }
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

    private String makeUrl(String script) {
        return script + ".html";
    }

    private Pattern svgSizePattern = Pattern.compile("<svg width=\"([0-9]+)pt\" height=\"([0-9]+)pt\"");

    private void createGraph(Graph graph, String name, String type, OutputStream output) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        graph.writeTo(out);
        Graphviz g = Graphviz.fromString(out.toString().replaceAll("\\r", ""));
        File dir = new File(graphOutputDir);
        dir.mkdirs();
        g = g.scale(1f);
        if (type == null) {
            g.renderToFile(new File(dir, name + ".png"));
            try {
                String svg = g.createSvg();
                Matcher matcher = svgSizePattern.matcher(svg);
                int ratio = 2;
                matcher.find();
                int width = ratio*Integer.parseInt(matcher.group(1));
                int height = ratio*Integer.parseInt(matcher.group(2));
                svg = matcher.replaceAll("<svg width=\""+width+"pt\" height=\""+height+"\"pt\"");
                Files.write(Paths.get(dir + "/" + name + ".svg"), svg.getBytes(Charset.forName("UTF8")));
                String title = name;
                String upDestination = null;
                if(title.equals("overview")) {
                    title = "Overview";
                } else {
                    upDestination = "overview";
                }
                Map<String,Object> vars = new HashMap<>();
                vars.put("title", title);
                vars.put("upDestination", upDestination);
                vars.put("content", svg);
                String html = applyTemplate(vars);
                Files.write(Paths.get(dir + "/" + name + ".html"), html.getBytes("UTF-8"));
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

    private String applyTemplate(Map<String, Object> vars) throws IOException {
        return new Engine().transform(readStream(getClass().getResourceAsStream("/plumber-template.html")), vars);
    }

    public static String readStream(InputStream in) throws IOException {
        byte[] buf = new byte[in.available()];
        int read;
        for(int total = 0; (read = in.read(buf, total, Math.min(100000, buf.length - total))) > 0; total += read);
        return new String(buf, "utf-8");
    }
}
