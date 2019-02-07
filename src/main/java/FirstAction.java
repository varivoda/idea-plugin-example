import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;

import static com.intellij.openapi.actionSystem.CommonDataKeys.PSI_ELEMENT;

public class FirstAction extends AnAction {

    @Override
    public void actionPerformed(AnActionEvent e) {
        PsiElement psiElement = e.getData(PSI_ELEMENT);

        if (psiElement instanceof PsiClass) {
            String cName = ((PsiClass) psiElement).getQualifiedName();
            showDialog(cName);
        }

    }

    private void showDialog(String text) {
        new Dialog(text);
    }
}
