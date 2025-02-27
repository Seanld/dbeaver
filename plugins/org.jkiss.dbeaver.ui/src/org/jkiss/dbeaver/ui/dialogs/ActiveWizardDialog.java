/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2022 DBeaver Corp and others
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jkiss.dbeaver.ui.dialogs;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.ISelectionService;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWizard;
import org.jkiss.dbeaver.ui.UIUtils;

import java.util.HashSet;
import java.util.Set;

/**
 * ActiveWizardDialog
 */
public class ActiveWizardDialog extends WizardDialog
{

    private Set<String> resizedShells = new HashSet<>();
    private boolean adaptContainerSizeToPages = false;

    private String finishButtonLabel = IDialogConstants.FINISH_LABEL;
    private String cancelButtonLabel = IDialogConstants.CANCEL_LABEL;

    public ActiveWizardDialog(IWorkbenchWindow window, IWizard wizard)
    {
        this(window, wizard, null);
    }

    public ActiveWizardDialog(IWorkbenchWindow window, IWizard wizard, IStructuredSelection selection)
    {
        super(window.getShell(), wizard);

        // Initialize wizard
        if (wizard instanceof IWorkbenchWizard) {
            if (selection == null) {
                final ISelectionService selectionService = window.getSelectionService();
                if (selectionService != null && selectionService.getSelection() instanceof IStructuredSelection) {
                    selection = (IStructuredSelection) selectionService.getSelection();
                }
            }
            ((IWorkbenchWizard)wizard).init(window.getWorkbench(), selection);
        }
        addPageChangingListener(event -> {
            if (event.getCurrentPage() instanceof ActiveWizardPage) {
                ((ActiveWizardPage) event.getCurrentPage()).deactivatePage();
            }
//                if (event.getTargetPage() instanceof ActiveWizardPage) {
//                    ((ActiveWizardPage) event.getTargetPage()).activatePage();
//                }
        });
    }

    @Override
    public int getShellStyle() {
        if (isModalWizard() || UIUtils.isInDialog()) {
            return super.getShellStyle();
        }
        return SWT.CLOSE | SWT.MAX | SWT.MIN | SWT.TITLE | SWT.BORDER | SWT.RESIZE | getDefaultOrientation();
    }

    protected boolean isModalWizard() {
        return true;
    }

    @Override
    protected IDialogSettings getDialogBoundsSettings()
    {
        return UIUtils.getDialogSettings("DBeaver.ActiveWizardDialog." + getWizard().getClass().getSimpleName());
    }

    public void setAdaptContainerSizeToPages(boolean adaptContainerSizeToPages) {
        this.adaptContainerSizeToPages = adaptContainerSizeToPages;
    }

    @Override
    public void showPage(IWizardPage page) {
        super.showPage(page);
        if (adaptContainerSizeToPages && !resizedShells.contains(page.getName())) {
            UIUtils.resizeShell(getWizard().getContainer().getShell());
            resizedShells.add(page.getName());
        }
    }

    @Override
    protected void createButtonsForButtonBar(Composite parent) {
        super.createButtonsForButtonBar(parent);

        Button cancelButton = getButton(IDialogConstants.CANCEL_ID);
        cancelButton.setText(cancelButtonLabel);
        Button finishButton = getButton(IDialogConstants.FINISH_ID);
        finishButton.setText(finishButtonLabel);
    }

    public void setFinishButtonLabel(String finishButtonLabel) {
        this.finishButtonLabel = finishButtonLabel;
    }

    public void setCancelButtonLabel(String cancelButtonLabel) {
        this.cancelButtonLabel = cancelButtonLabel;
    }
}
