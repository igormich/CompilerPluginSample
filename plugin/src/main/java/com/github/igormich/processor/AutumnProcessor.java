package com.github.igormich.processor;

import com.google.auto.service.AutoService;
import com.google.auto.service.AutoService;
import com.sun.source.util.Trees;
import com.sun.tools.javac.code.*;
import com.sun.tools.javac.comp.Modules;
import com.sun.tools.javac.comp.Resolve;
import com.sun.tools.javac.model.JavacElements;
import com.sun.tools.javac.parser.Parser;
import com.sun.tools.javac.processing.JavacProcessingEnvironment;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.Name;
import com.sun.tools.javac.util.Names;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import java.util.Random;
import java.util.Set;

import static com.sun.tools.javac.code.Kinds.Kind.MTH;

@SupportedAnnotationTypes({"com.github.igormich.processor.PrintToConsole",
        "com.github.igormich.processor.AutoWired",
        "com.github.igormich.processor.Service"})
@AutoService(Processor.class)
public class AutumnProcessor extends AbstractProcessor {
    private Trees trees;
    private TreeMaker treeMaker;
    private Name.Table names;
    private Symtab syms;
    private Types types;
    private Resolve rs;

    private Parser parser;
    private Modules modules;
    private JavacElements elements;

    @Override
    public synchronized void init(ProcessingEnvironment env) {
        super.init(env);
        trees = Trees.instance(env);
        Context context = ((JavacProcessingEnvironment) env).getContext();
        treeMaker = TreeMaker.instance(context);
        names = Names.instance(context).table;
        syms = Symtab.instance(context);
        types = Types.instance(context);
        rs = Resolve.instance(context);
        modules = Modules.instance(context);
        elements = JavacElements.instance(context);
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        if (annotations.isEmpty()) {
            return false;
        }
        Set<? extends Element> elements = roundEnv.getElementsAnnotatedWith(PrintToConsole.class);
        for (final Element element : elements) {
            doWrapMethod(element);
        }
        elements = roundEnv.getElementsAnnotatedWith(AutoWired.class);
        for (final Element element : elements) {
            JCTree.JCVariableDecl variable = (JCTree.JCVariableDecl) trees.getTree(element);
            System.out.println(variable.vartype.type);
            //var tree = (com.sun.tools.javac.tree.JCTree.JCClassDecl)trees.getTree(variable.vartype.type.asElement());
            var services = roundEnv.getElementsAnnotatedWith(Service.class);
            List<JCTree.JCExpression> childs = List.nil();

            for (final Element serviceEl : services) {
                var service = (com.sun.tools.javac.tree.JCTree.JCClassDecl)trees.getTree(serviceEl);
                System.out.println("service");
                System.out.println("service.getImplementsClause()" + service.getImplementsClause());
                if(service.getImplementsClause().stream().anyMatch(i->i.type.equals(variable.vartype.type)))
                    childs = childs.append(treeMaker.Ident(service.name));
            }
            System.out.println(childs);
            if(childs.size() == 1) {
                System.out.println("treeMaker.NewClass");
                variable.init = treeMaker.NewClass(null, List.nil(), childs.getFirst(), List.nil(), null);
            }
        }
        return true;
    }

    private void doWrapMethod(Element element) {
        JCTree.JCMethodDecl decl = (JCTree.JCMethodDecl) trees.getTree(element);
        JCTree.JCClassDecl classDecl = (JCTree.JCClassDecl) trees.getTree(decl.sym.owner);

        List<JCTree.JCAnnotation> wrapMethod = List.nil();
        for (JCTree.JCAnnotation anno : decl.mods.annotations) {
            if (anno.annotationType.toString().contains("PrintToConsole"))
                wrapMethod = wrapMethod.append(anno);
        }
        System.out.println(decl.body);
        decl.body = rewriteFunctionForTrace(decl, wrapMethod.getFirst());
        System.out.println(decl.body);
    }

    private JCTree.JCBlock rewriteFunctionForTrace(JCTree.JCMethodDecl decl, JCTree.JCAnnotation anno) {
        JCTree.JCFieldAccess systemAccess = treeMaker.Select(treeMaker.Ident(names.fromString("System")), names.fromString("out"));
        JCTree.JCFieldAccess printlnAccess = treeMaker.Select(systemAccess, names.fromString("println"));
        JCTree.JCFieldAccess printAccess = treeMaker.Select(systemAccess, names.fromString("print"));
        List<JCTree.JCStatement> statements = List.nil();
        statements = statements.append(treeMaker.Exec(treeMaker.Apply(List.nil(), printAccess,
                List.of(treeMaker.Literal("!!!call " +decl.name.toString()+ " with args: ")))));
        var params = decl.getParameters();
        for(var param: params) {
            JCTree.JCFieldAccess printAccessLocal = treeMaker.Select(systemAccess, names.fromString("print"));
            statements = statements.append(treeMaker.Exec(treeMaker.Apply(List.nil(), printAccessLocal,
                    List.of(treeMaker.Ident(names.fromString(param.name.toString())).setType(param.type)))));
            if(params.last() !=param)
                statements = statements.append(treeMaker.Exec(treeMaker.Apply(List.nil(), printAccess,
                    List.of(treeMaker.Literal(", ")))));
        }
        statements = statements.append(treeMaker.Exec(treeMaker.Apply(List.nil(), printlnAccess,
                List.of(treeMaker.Literal("")))));
        JCTree.JCBlock oldBody = decl.body;
        for (JCTree.JCStatement stmt : oldBody.getStatements()) {

            if (!(stmt instanceof JCTree.JCReturn)) {
                statements = statements.append(stmt);
            } else {
                String actualReturnVar = "$result";
                JCTree.JCReturn ret = (JCTree.JCReturn) stmt;
                JCTree.JCExpression exp = ret.getExpression();

                JCTree.JCVariableDecl origStmtToVar = treeMaker.VarDef(treeMaker.Modifiers(0L), names.fromString(actualReturnVar), treeMaker.Type(decl.getReturnType().type), exp);
                statements = statements.append(origStmtToVar);

                statements = statements.append(treeMaker.Exec(treeMaker.Apply(List.nil(), printAccess,
                        List.of(treeMaker.Literal("!!!result is ")))));
                JCTree.JCFieldAccess printlnAccessLocal = treeMaker.Select(systemAccess, names.fromString("println"));
                statements = statements.append(treeMaker.Exec(treeMaker.Apply(List.nil(), printlnAccessLocal,
                        List.of(treeMaker.Ident(names.fromString(actualReturnVar))))));
                statements = statements.append(treeMaker.Return(treeMaker.Ident(names.fromString(actualReturnVar))));
            }
        }
        return treeMaker.Block(0L, statements);
    }

    private Symbol findSymbol(String className) {
        Symbol classSymbol = elements.getTypeElement(className);
        if (classSymbol == null) throw new IllegalStateException("findSymbol: couldn't find symbol " + className);
        return classSymbol;
    }

    private Symbol findSymbol(String className, String symbolToString) {
        Symbol classSymbol = findSymbol(className);

        for (Symbol symbol : classSymbol.getEnclosedElements()) {
            if (symbolToString.equals(symbol.toString())) return symbol;
        }

        throw new IllegalStateException("findSymbol: couldn't find symbol " + className + "." + symbolToString);
    }

    private Symbol.MethodSymbol lookupMethod(Symbol.TypeSymbol tsym, Name name, List<Type> argtypes) {
        for (Symbol s : tsym.members().getSymbolsByName(name, s -> s.kind == MTH)) {
            if (types.isSameTypes(s.type.getParameterTypes(), argtypes)) {
                return (Symbol.MethodSymbol) s;
            }
        }
        return null;
    }
}