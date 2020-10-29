package junitmigration;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.codeStyle.JavaCodeStyleManager;
import com.intellij.psi.search.GlobalSearchScope;

import java.util.Arrays;
import java.util.Optional;

import static com.intellij.openapi.actionSystem.CommonDataKeys.PSI_ELEMENT;

public class MigrateToJunit5 extends AnAction {

    public static final String JUNIT4_CATEGORY_FULL_NAME = "org.junit.experimental.categories.Category";
    public static final String JUNIT4_TEST_FULL_NAME = "org.junit.Test";
    public static final String JUNIT5_TEST_FULL_NAME = "org.junit.jupiter.api.Test";
    public static final String JUNIT5_TAG_FULL_NAME = "org.junit.jupiter.api.Tag";

    private Project project;

    @Override
    public void actionPerformed(AnActionEvent e) {
        project = e.getProject();
        PsiElement psiElement = e.getData(PSI_ELEMENT);

        if (psiElement instanceof PsiClass) {
            replaceTestAnnotation((PsiClass) psiElement);
            replaceCategoryAnnotationWithTag((PsiClass) psiElement);
            removeRedundantImportsAndShortRef(psiElement);
        }
    }

    private void removeRedundantImportsAndShortRef(PsiElement psiElement) {
        CommandProcessor.getInstance().executeCommand(project, () ->
                ApplicationManager.getApplication().runWriteAction(() -> {
                    JavaCodeStyleManager.getInstance(project).removeRedundantImports((PsiJavaFile) psiElement.getContainingFile());
                    JavaCodeStyleManager.getInstance(project).shortenClassReferences(psiElement.getContainingFile());
                }), null, null);
    }

    private void replaceCategoryAnnotationWithTag(PsiClass psiClass) {
        Arrays.stream(psiClass.getMethods())
                .filter(m -> hasAnnotation(m, JUNIT4_CATEGORY_FULL_NAME))
                .forEach(this::replaceCategoryWithTag);
    }

    private void replaceTestAnnotation(PsiClass psiClass) {
        Arrays.stream(psiClass.getMethods())
                .filter(m -> hasAnnotation(m, JUNIT4_TEST_FULL_NAME))
                .forEach(this::replaceTestAnnotation);
    }

    private void replaceTestAnnotation(PsiMethod psiMethod) {
        PsiAnnotation junit4TestAnn = psiMethod.getModifierList().findAnnotation(JUNIT4_TEST_FULL_NAME);
        PsiAnnotation junit5Ann = createAnnotation("@" + JUNIT5_TEST_FULL_NAME, psiMethod);

        CommandProcessor.getInstance().executeCommand(project, () ->
                ApplicationManager.getApplication().runWriteAction(() -> {

                    psiMethod.getModifierList().addAfter(junit5Ann, junit4TestAnn);
                    junit4TestAnn.delete();
                    JavaCodeStyleManager.getInstance(project).removeRedundantImports((PsiJavaFile) psiMethod.getContainingFile());
                    addImport(psiMethod.getContainingFile(), JUNIT5_TEST_FULL_NAME);
                }), null, null);
    }

    private void replaceCategoryWithTag(PsiMethod psiMethod) {
        PsiAnnotation categoryAnn = psiMethod.getModifierList().findAnnotation(JUNIT4_CATEGORY_FULL_NAME);
        String objValue = categoryAnn.findDeclaredAttributeValue("value").getText();
        String strValue = objValue.substring(0, objValue.indexOf('.'));
        PsiAnnotation tagAnnotation = createAnnotation(String.format("@Tag(\"%s\")", strValue), psiMethod);

        CommandProcessor.getInstance().executeCommand(project, () ->
                ApplicationManager.getApplication().runWriteAction(() -> {
                    psiMethod.getModifierList().addAfter(tagAnnotation, categoryAnn);
                    categoryAnn.delete();
                    addImport(psiMethod.getContainingFile(), JUNIT5_TAG_FULL_NAME);
                    JavaCodeStyleManager.getInstance(project).shortenClassReferences(tagAnnotation);
                }), null, null);
    }

    private PsiAnnotation createAnnotation(String text, PsiMethod psiMethod) {
        return JavaPsiFacade.getInstance(project).getElementFactory().createAnnotationFromText(text, psiMethod);
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
