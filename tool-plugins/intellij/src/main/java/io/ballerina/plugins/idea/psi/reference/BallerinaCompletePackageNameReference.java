/*
 * Copyright (c) 2018, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.ballerina.plugins.idea.psi.reference;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiFileSystemItem;
import com.intellij.psi.ResolveResult;
import com.intellij.psi.impl.source.resolve.reference.impl.providers.FileReference;
import com.intellij.psi.impl.source.resolve.reference.impl.providers.FileReferenceSet;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.ArrayUtil;
import com.intellij.util.IncorrectOperationException;
import com.intellij.util.containers.ContainerUtil;
import io.ballerina.plugins.idea.completion.BallerinaCompletionUtils;
import io.ballerina.plugins.idea.psi.BallerinaImportDeclaration;
import io.ballerina.plugins.idea.sdk.BallerinaPathModificationTracker;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * Responsible for resolving packages.
 */
public class BallerinaCompletePackageNameReference extends FileReference {

    public BallerinaCompletePackageNameReference(@NotNull FileReferenceSet fileReferenceSet, TextRange range, int index,
                                                 String text) {
        super(fileReferenceSet, range, index, text);
    }

    @NotNull
    @Override
    protected ResolveResult[] innerResolve(boolean caseSensitive, @NotNull PsiFile containingFile) {
        String referenceText = getText();
        Set<ResolveResult> result = ContainerUtil.newLinkedHashSet();
        Set<ResolveResult> innerResult = ContainerUtil.newLinkedHashSet();
        for (PsiFileSystemItem context : getContexts()) {
            innerResolveInContext(referenceText, context, innerResult, caseSensitive);
            for (ResolveResult resolveResult : innerResult) {
                PsiElement element = resolveResult.getElement();
                if (element instanceof PsiDirectory) {
                    if (isLast()) {
                        return new ResolveResult[]{resolveResult};
                    }
                    result.add(resolveResult);
                }
            }
            innerResult.clear();
        }
        return result.isEmpty() ? ResolveResult.EMPTY_ARRAY : result.toArray(new ResolveResult[result.size()]);
    }

    @Override
    public PsiElement bindToElement(@NotNull PsiElement element, boolean absolute) throws IncorrectOperationException {
        //        if (!absolute) {
        //            FileReference firstReference = ArrayUtil.getFirstElement(getFileReferenceSet().getAllReferences
        // ());
        //            if (firstReference != null) {
        //                Collection<PsiFileSystemItem> contexts = getFileReferenceSet().getDefaultContexts();
        //                for (ResolveResult resolveResult : firstReference.multiResolve(false)) {
        //                    PsiElement resolveResultElement = resolveResult.getElement();
        //                    if (resolveResultElement instanceof PsiFileSystemItem) {
        //                        PsiFileSystemItem parentDirectory = ((PsiFileSystemItem)resolveResultElement)
        // .getParent();
        //                        if (parentDirectory != null && contexts.contains(parentDirectory)) {
        //                            return getElement();
        //                        }
        //                    }
        //                }
        //            }
        //        }
        return super.bindToElement(element, true);
    }

    @Override
    public PsiElement handleElementRename(String newElementName) throws IncorrectOperationException {
        return super.handleElementRename(newElementName);
    }

    @NotNull
    @Override
    public Object[] getVariants() {
        BallerinaImportDeclaration importDeclaration = PsiTreeUtil.getParentOfType(getElement(),
                BallerinaImportDeclaration.class);
        if (importDeclaration != null) {
            if (importDeclaration.getOrgName() == null && importDeclaration.getCompletePackageName() != null) {
                List<LookupElement> organizationList = new LinkedList<>();
                organizationList.add(BallerinaCompletionUtils.createOrganizationLookup("ballerina"));
                List<VirtualFile> organizations = BallerinaPathModificationTracker.getAllOrganizationsInUserRepo();
                for (VirtualFile organization : organizations) {
                    organizationList.add(BallerinaCompletionUtils.createOrganizationLookup(organization.getName()));
                }
                // super.getVariants will return the packages in the current project.
                return ArrayUtil.mergeArrays(super.getVariants(), organizationList.toArray(new LookupElement[0]));
            }
        }
        // If we return an empty array, package completion in the same project will not be available.
        return super.getVariants();
    }
}
