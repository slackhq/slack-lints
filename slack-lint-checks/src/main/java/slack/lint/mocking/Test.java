package slack.lint.mocking;

import com.intellij.psi.*;
import org.jetbrains.kotlin.psi.*;

public class Test {

    public boolean resolveClassIsEnum(KtDotQualifiedExpression expression) {
        KtExpression receiverExpression = expression.getReceiverExpression();
        if (receiverExpression instanceof KtNameReferenceExpression) {
            PsiElement resolved = ((KtNameReferenceExpression) receiverExpression).getReference().resolve();
            if (resolved instanceof KtClass) {
                return ((KtClass) resolved).isEnum();
            }
        }
        return false;
    }
}

