import com.intellij.codeInsight.completion.AllClassesGetter;
import com.intellij.codeInsight.completion.PlainPrefixMatcher;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.codeStyle.JavaCodeStyleManager;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.util.Processor;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class ReplaceAllCategoryAction extends AnAction {

    @Override
    public void actionPerformed(AnActionEvent e) {
        project = e.getProject();

        AllClassesGetter.processJavaClasses(
                new PlainPrefixMatcher(""),
                project,
                GlobalSearchScope.allScope(project),
                processor);
    }

    Processor<PsiClass> processor = psiClass -> {
        replaceCategory(psiClass);
        return true;
    };

    private Project project;

    private void replaceCategory(PsiClass psiClass) {
        Arrays.stream(psiClass.getMethods())
                .filter(m -> hasAnnotation(m, "org.junit.Test"))
                .filter(m -> hasAnnotation(m, "org.junit.experimental.categories.Category"))
                .forEach(this::replaceCategory);
    }

    private void replaceCategory(PsiMethod psiMethod) {
        PsiAnnotation name =
                psiMethod.getModifierList()
                        .findAnnotation("org.junit.experimental.categories.Category");

        String value = name.
                findDeclaredAttributeValue("value").getText();

        List<String> techCategories = getTechCategories(value);
        String productCategory = getProductCategory(value);

        addFeature(psiMethod, productCategory);
        addTypes(psiMethod, techCategories);
        deleteCategory(psiMethod);

    }

    private void deleteCategory(PsiMethod psiMethod) {
        PsiAnnotation oldFeature =
                psiMethod.getModifierList()
                        .findAnnotation("org.junit.experimental.categories.Category");

        CommandProcessor.getInstance().executeCommand(project, () ->
                ApplicationManager.getApplication().runWriteAction(() -> {
                    oldFeature.delete();
                }), null, null);
    }

    private void addTypes(PsiMethod psiMethod, List<String> techCategories) {

        String text = getTypesAnnotation(techCategories);

        CommandProcessor.getInstance().executeCommand(project, () ->
                ApplicationManager.getApplication().runWriteAction(() -> {
                    PsiAnnotation ann = createAnnotation(text, psiMethod);
                    addAfterTest(ann, psiMethod);
                    addImport(psiMethod.getContainingFile(), "com.wrike.annotation.Types");
                    addImport(psiMethod.getContainingFile(), "com.wrike.annotation.Type");
                    JavaCodeStyleManager.getInstance(project).shortenClassReferences(ann);
                }), null, null);
    }

    private void addAfterTest(PsiAnnotation psiAnnotation, PsiMethod psiMethod) {
        PsiAnnotation test = psiMethod.getModifierList().findAnnotation("org.junit.Test");
        psiMethod.getModifierList().addAfter(psiAnnotation, test);
    }

    private PsiAnnotation createAnnotation(String text, PsiMethod psiMethod) {
        return JavaPsiFacade.getInstance(project).getElementFactory().createAnnotationFromText(text, psiMethod);
    }

    private String getInnerType(List<String> feature) {
        return "{" + feature.stream().map(s -> "@Type(\"" + s + "\")").collect(Collectors.joining(",")) + "}";
    }

    private String getTypesAnnotation(List<String> feature) {
        String inner = getInnerType(feature);
        return "@Types(" + inner + ")";
    }

    private void addFeature(PsiMethod psiMethod, String productCategory) {
        String text = "io.qameta.allure.Feature(\"" + productCategory + "\")";

        CommandProcessor.getInstance().executeCommand(project, () ->
                ApplicationManager.getApplication().runWriteAction(() -> {
            PsiAnnotation ann = psiMethod.getModifierList().addAnnotation(text);
            JavaCodeStyleManager.getInstance(project).shortenClassReferences(ann);
        }), null, null);
    }

    private List<String> getTechCategories(String category) {
        List<String> tech = Arrays.asList("Smoke", "Firefox", "Safari");
        return Arrays.stream(category.substring(1, category.length() - 1)
                .split(", ")).map(s -> s.substring(0, s.indexOf('.')))
                .filter(tech::contains).collect(Collectors.toList());
    }

    private String getProductCategory(String category) {
        List<String> prod = Arrays.asList("Login", "Dashboard", "Inbox");
        return Arrays.stream(category.substring(1, category.length() - 1)
                .split(", "))
                .map(s -> s.substring(0, s.indexOf('.')))
                .filter(prod::contains).findFirst().get();
    }

    private boolean hasAnnotation(PsiMethod psiMethod, String annotationQualifiedName) {
        return Arrays.stream(psiMethod.getAnnotations())
                .anyMatch(psiAnnotation -> psiAnnotation.getQualifiedName().contains(annotationQualifiedName));
    }

    private void addImport(final PsiFile file, final String qualifiedName) {
        if (file instanceof PsiJavaFile) {
            addImport((PsiJavaFile) file, qualifiedName);
        }
    }

    private void addImport(final PsiJavaFile file, final String qualifiedName) {
        final Project project = file.getProject();
        Optional<PsiClass> possibleClass = Optional.ofNullable(JavaPsiFacade.getInstance(project)
                .findClass(qualifiedName, GlobalSearchScope.everythingScope(project)));
        possibleClass.ifPresent(psiClass -> JavaCodeStyleManager.getInstance(project).addImport(file, psiClass));
    }
}
