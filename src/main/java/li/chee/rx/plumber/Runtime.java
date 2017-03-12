package li.chee.rx.plumber;

import groovy.lang.GroovyShell;
import groovy.lang.Script;
import guru.nidi.graphviz.attribute.Attribute;
import guru.nidi.graphviz.attribute.Color;
import static guru.nidi.graphviz.attribute.Color.*;

import static guru.nidi.graphviz.model.Factory.*;

import guru.nidi.graphviz.attribute.Shape;
import guru.nidi.graphviz.attribute.Style;
import guru.nidi.graphviz.engine.Graphviz;
import guru.nidi.graphviz.model.*;
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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;


/**
 * Evaluate groovy scripts.
 */
public class Runtime {

    public enum GraphTheme {
        LIGHT(WHITE, GRAY, GRAY3, WHITE),
        DARK(GRAY3, GRAY, LIGHTGRAY, BLACK);

        public Color background, arrow, box, text;
        GraphTheme(Color... colors) {
            background = colors[0];
            arrow = colors[1];
            box = colors[2];
            text = colors[3];
        }
    }

    private GraphTheme theme = GraphTheme.LIGHT;

    public void setGraphTheme(GraphTheme theme) {
        this.theme = theme;
    }

    public Object run(String scriptText) {
        CompilerConfiguration config = new CompilerConfiguration();
        config.addCompilationCustomizers(graphOutputCustomizer());
        GroovyShell shell = new GroovyShell(config);
        Script script = shell.parse(scriptText);
        return script.run();
    }

    private CompilationCustomizer graphOutputCustomizer() {
        return new CompilationCustomizer(CompilePhase.SEMANTIC_ANALYSIS) {
            DeclarationExpression currentDeclaration;
            Node previousNode;
            Map<Object, Node> nodes = new HashMap<>();
            MutableGraph graph = new MutableGraph()
                    .generalAttrs().
                            add(theme.background.background())
                    .nodeAttrs()
                        .add(theme.box.fill());

            MutableGraph currentSubGraph;
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
                            currentSubGraph = subGraph(id, name);
                            currentDeclaration = expression;
                            graph.add(currentSubGraph);
                            expression.getRightExpression().visit(this);
                            currentSources.forEach(source -> graph.addLink(edge(source, previousNode)));
                            if (currentSources.size() > 0) {
                                currentSubGraph.graphAttrs().add(Style.BOLD);
                            }
                            currentSubGraph = null;
                            previousNode = null;
                            currentSources.clear();
                        } else if (right instanceof StaticMethodCallExpression &&
                                ((StaticMethodCallExpression) right).getMethod().equals("split")) {
                            StringBuilder label = new StringBuilder("split");
                            ArgumentListExpression args = ((ArgumentListExpression) (((StaticMethodCallExpression) right).getArguments()));
                            Node node = node(objId(right));
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
                                            graph.addLink(edge(peer, node).with(Style.DASHED));
                                        }
                                    });
                            args.getExpression(1).visit(
                                    new CodeVisitorSupport() {
                                        @Override
                                        public void visitVariableExpression(VariableExpression expression) {
                                            Node peer = nodes.get(expression.getAccessedVariable());
                                            graph.addLink(edge(peer, node));
                                        }
                                    });
                            graph.add(node);
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
                            Node node = node(objId(call));
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
                            node.with(Label.of(html(label.toString().trim())));
                            if (isCommon) {
                                //node.attr(Attribute.FONTNAME, "arial");
                            }
                            if (isTo && peers.size() > 0) {
                                node.with(Style.ROUNDED, Style.SOLID).with("width", "0.3").with("fixedsize", "true");
                                Link edge = edge(node, peers.get(0)).with(Style.DASHED);
                                graph.addLink(edge);
                            } else {
                                peers.forEach(peer -> {
                                    Link edge = edge(peer, node).with(Style.DASHED);
                                    graph.addLink(edge);
                                });
                            }
                            if (!nodes.containsKey(currentDeclaration.getVariableExpression())) {
                                nodes.put(currentDeclaration.getVariableExpression(), node);
                            }
                            currentSubGraph.add(node);
                            if (previousNode != null) {
                                Link edge = edge(node, previousNode);
                                currentSubGraph.addLink(edge);
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
                                            node[0] = node(objId(call))
                                                    .with(Label.of(html(statics(call.getMethodAsString()))));
                                            if (sources.size() == 0) {
                                                if (!nodes.containsKey(call.getText())) {
                                                    nodes.put(call.getText(), node[0]);
                                                    graph.add(node[0]);
                                                }
                                            } else {
                                                currentSubGraph.add(node[0]);
                                                sources.forEach(source -> graph.addLink(edge(source, node[0])));
                                            }
                                            if (previousNode != null) {
                                                graph.addLink(edge(node[0], previousNode));
                                            } else {
                                                nodes.putIfAbsent(currentDeclaration.getVariableExpression(), node[0]);
                                            }
                                        }

                                        @Override
                                        public void visitVariableExpression(VariableExpression expression) {
                                            if (!expression.getAccessedVariable().getName().equals("it")) {
                                                graph.addLink(edge(nodes.get(expression.getAccessedVariable()), previousNode));
                                            } else {
                                                if (currentSources.isEmpty()) {
                                                    currentSubGraph.graphAttrs().add(Style.ROUNDED);
                                                    nodes.put(currentDeclaration.getVariableExpression(), previousNode);
                                                }
                                            }
                                        }
                                    });
                        } else if (call.getMethodAsString().equals("sink")) {
                            AtomicInteger count = new AtomicInteger();
                            Node target = node("sink")
                                    .with(Shape.CIRCLE)
                                    .with("width", "0.2")
                                    .with("fixedsize", "true")
                                    .with(Label.of(""));
                            graph.add(target);
                            Link[] edge = new Link[1];
                            call.getArguments().visit(
                                    new CodeVisitorSupport() {
                                        @Override
                                        public void visitVariableExpression(VariableExpression expression) {
                                            Node source = nodes.get(expression.getAccessedVariable());
                                            edge[0] = edge(source, target)
                                                    .with(Label.of(""+count.incrementAndGet()));
                                            graph.addLink(edge[0]);
                                        }
                                    });
                            if (count.get() == 1) {
                                edge[0] = edge[0].with(Label.of(""));
                            }
                        } else if (call.getMethodAsString().equals("parallel")) {
                            // currentSubGraph = currentSubGraph..attr(Attribute.LABEL, currentSubGraph.attr(Attribute.LABEL) + " //");
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

                    private MutableGraph subGraph(String id, String name) {
                        return mutGraph(id).graphAttrs().add(Label.of(name));
                    }
                });
                Graphviz.fromGraph(graph).renderToFile(new File("target/graph.png"));
            }

            private String html(String s) {
                return s;
            }

            private Link edge(Node from, Node to) {
                assert from != null;
                assert to != null;
                return Link.between(from, to);
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
