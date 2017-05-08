package li.chee.reactive.plumber;

import org.apache.commons.cli.*;

import java.io.IOException;
import java.net.URI;
import java.nio.file.*;

public class GraphGen {

    public static void main(String[] args) throws ParseException {
        Options opts = new Options();
        opts.addOption(Option.builder("i").longOpt("input").hasArg().argName("PATH")
                .desc("source file or directory to scan (defaults to ./src/main/resources)")
                .build());
        opts.addOption("k", "dark-theme", false, "use dark background for graphs");
        opts.addOption("h", "help", false, "print this message");
        opts.addOption(Option.builder("d").longOpt("dest-dir").hasArg().argName("DIR")
                .desc("where to put the generated graphs (defaults to ./target/graphs)")
                .build());
        opts.addOption(Option.builder("o").longOpt("overview-title").hasArg().argName("STRING")
                .desc("title of the overview graph")
                .build());
        opts.addOption(Option.builder("t").longOpt("type").hasArg().argName("TYPE")
                .desc("the output type of graphs: 'png', 'svg' or 'html' (default)")
                .build()
        );

        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = parser.parse(opts, args);

        if(cmd.hasOption("help")) {
            usage(opts);
        } else {
            try {
                generateGraphs(
                        cmd.getOptionValue("input", "./src/main/resources"),
                        cmd.getOptionValue("title", "Overview"),
                        cmd.getOptionValue("type", "html"),
                        cmd.hasOption("dark-theme"),
                        cmd.getOptionValue("dest-dir", "./target/graphs"));
            } catch (NoSuchFileException e) {
                System.err.println("File not found: " + e.getFile());
                usage(opts);
            } catch (IOException e) {
                System.err.println("\n");
                e.printStackTrace(System.err);
                usage(opts);
            }
        }
    }

    public static void generateGraphs(String input, String title, String output) throws IOException {
        generateGraphs(input, title, "html", false, output);
    }

    public static void generateGraphs(String input,
                                      String type,
                                      boolean dark) throws IOException {
        generateGraphs(input, "Overview", type, dark, "./target/graphs/");
    }

    public static void generateGraphs(String input,
                                      String title,
                                      String type,
                                      boolean dark,
                                      String output) throws IOException {

        Runtime runtime = new Runtime(true)
                .withGraphOutputDir(output)
                .withGraphTheme(dark ? Runtime.GraphTheme.DARK : Runtime.GraphTheme.LIGHT)
                .withGraphType(type)
                .withGraphOverviewTitle(title);
        URI[] files = files(input);
        runtime.generateGraphs(files);
        if (files.length > 0) {
            runtime.generateOverviewGraph();
        }
    }

    private static URI[] files(String input) throws IOException {
            return Files.walk(Paths.get(input)).filter(x -> x.toString().endsWith(".groovy")).map(Path::toUri).toArray(URI[]::new);
    }

    private static void usage(Options opts) {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("java " + GraphGen.class.getCanonicalName(),
                "Generate documentation graphs for reactive plumber pipelines.", opts, "", true);
    }
}
