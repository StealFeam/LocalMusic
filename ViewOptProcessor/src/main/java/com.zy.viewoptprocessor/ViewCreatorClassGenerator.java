package com.zy.viewoptprocessor;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Writer;
import java.util.HashSet;

import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;

public class ViewCreatorClassGenerator {
    private final TypeElement mTypeElement;

    private final String mPackageName;
    private final String mClassName;
    private final ProcessingEnvironment mProcessingEnv;
    private final Messager mMessager;

    private static final String sProxyInterfaceName = "IViewCreator";

    public ViewCreatorClassGenerator(ProcessingEnvironment processingEnv, TypeElement classElement, Messager messager) {
        mProcessingEnv = processingEnv;
        mTypeElement = classElement;
        mMessager = messager;

        PackageElement packageElement = processingEnv.getElementUtils().getPackageOf(classElement);
        String packageName = packageElement.getQualifiedName().toString();

        String className = ClassValidator.getClassName(classElement, packageName);

        mPackageName = packageName;
        mClassName = className + "__ViewCreator__Proxy";
    }

    public void getJavaClassFile() {
        Writer writer= null;
        try {
            JavaFileObject jfo = mProcessingEnv.getFiler().createSourceFile(
                    mClassName,
                    mTypeElement
            );

            String classPath = jfo.toUri().getPath();

            String buildDirStr = "/app/build/";
            String buildDirFullPath = classPath.substring(0, classPath.indexOf(buildDirStr) + buildDirStr.length());
            File customViewFile = new File(buildDirFullPath + "tmp_custom_views/custom_view_final.txt");

            HashSet<String> customViewClassNameSet = new HashSet<>();
            putClassListData(customViewClassNameSet, customViewFile);

            String generateClassInfoStr = generateClassInfoStr(customViewClassNameSet);

            writer = jfo.openWriter();
            writer.write(generateClassInfoStr);
            writer.flush();

            mMessager.printMessage(Diagnostic.Kind.NOTE, "generate file path : " + classPath);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private String generateClassInfoStr(HashSet<String> customViewClassNameSet) {
        StringBuilder builder = new StringBuilder();
        builder.append("// Generated code. Do not modify!\n");
        builder.append("package ").append(mPackageName).append(";\n");
        builder.append("import com.zy.ppmusic.widget.*;\n");
        builder.append("import android.content.Context;\n");
        builder.append("import android.util.AttributeSet;\n");
        builder.append("import com.zy.viewopt.IViewCreator;\n");
        builder.append("import android.view.*;\n");
        builder.append("import android.widget.*;\n");
        builder.append("import android.webkit.*;\n");
        builder.append("import android.app.*;\n");

        builder.append('\n');

        builder.append("public class ").append(mClassName).append(" implements " + sProxyInterfaceName);
        builder.append(" { \n");

        generateMethodStr(builder, customViewClassNameSet);
        builder.append("\n");

        builder.append("}\n");

        return builder.toString();
    }

    private void generateMethodStr(StringBuilder builder, HashSet<String> customViewClassNameSet) {
        builder.append("\t@Override\n");
        builder.append("\tpublic View createView(String name, Context context, AttributeSet attrs) {\n");

        builder.append("\t\tswitch(name) {\n");

        for (String className : customViewClassNameSet) {
            if (className == null || className.trim().length() == 0) {
                continue;
            }
            builder.append("\t\t\tcase \"").append(className).append("\" : \n");
            builder.append("\t\t\treturn new ").append(className).append("(context, attrs);\n");
        }

        builder.append("\t\t}\n"); // switch end

        builder.append("\t\treturn null;\n");
        builder.append("\t}"); // method end
    }

    private void putClassListData(HashSet<String> customViewClassNameSet, File customViewFile) {
        if (customViewFile.exists()) {
            FileReader fr = null;
            BufferedReader br = null;
            try {
                fr = new FileReader(customViewFile);
                br = new BufferedReader(fr);
                String line;
                while ((line = br.readLine()) != null) {
                    if (line.trim().length() == 0) {
                        continue;
                    }
                    customViewClassNameSet.add(line.trim());
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (br != null) {
                    try {
                        br.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                if (fr != null) {
                    try {
                        fr.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        } else {
            System.err.println("----> file not found > " + customViewFile.getAbsolutePath());
        }
    }


}
