package com.zy.viewoptprocessor;

import com.google.auto.service.AutoService;
import com.zy.viewopt.ViewOptHost;

import java.util.LinkedHashSet;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;

@AutoService(Processor.class)
public class ViewCreatorProcessor extends AbstractProcessor {
    private Messager mMessager;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        mMessager = processingEnv.getMessager();
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        Set<? extends Element> classElements = roundEnv.getElementsAnnotatedWith(ViewOptHost.class);
        for (Element element : classElements) {
            TypeElement classElement = (TypeElement) element;
            ViewCreatorClassGenerator viewCreatorClassGenerator = new ViewCreatorClassGenerator(processingEnv, classElement, mMessager);
            viewCreatorClassGenerator.getJavaClassFile();
            break;
        }
        return true;
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        Set<String> types = new LinkedHashSet<>();
        types.add(ViewOptHost.class.getCanonicalName());
        return types;
    }
}
