import com.sun.source.util.JavacTask;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/** Parses (syntax only, Java 17) every .java file under the given roots and prints errors. */
public class ParseCheck {
    public static void main(String[] args) throws Exception {
        List<File> files = new ArrayList<>();
        for (String root : args) {
            try (Stream<Path> s = Files.walk(Paths.get(root))) {
                files.addAll(s.filter(p -> p.toString().endsWith(".java")).map(Path::toFile).collect(Collectors.toList()));
            }
        }
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        DiagnosticCollector<JavaFileObject> diags = new DiagnosticCollector<>();
        StandardJavaFileManager fm = compiler.getStandardFileManager(diags, null, null);
        Iterable<? extends JavaFileObject> units = fm.getJavaFileObjectsFromFiles(files);
        JavacTask task = (JavacTask) compiler.getTask(null, fm, diags, List.of("--release", "17", "-proc:none"), null, units);
        task.parse();
        int errors = 0;
        for (Diagnostic<? extends JavaFileObject> d : diags.getDiagnostics()) {
            if (d.getKind() == Diagnostic.Kind.ERROR) {
                errors++;
                System.out.println(d.getSource().getName() + ":" + d.getLineNumber() + ": " + d.getMessage(null));
            }
        }
        System.out.println("parsed " + files.size() + " files, " + errors + " syntax errors");
        System.exit(errors > 0 ? 1 : 0);
    }
}
