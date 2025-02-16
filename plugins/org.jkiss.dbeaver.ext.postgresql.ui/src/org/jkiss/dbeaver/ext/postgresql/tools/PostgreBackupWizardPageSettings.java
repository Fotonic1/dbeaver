/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.postgresql.tools;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.jkiss.dbeaver.ext.postgresql.PostgreMessages;
import org.jkiss.dbeaver.ext.postgresql.tasks.PostgreBackupRestoreSettings;
import org.jkiss.dbeaver.ext.postgresql.tasks.PostgreDatabaseBackupSettings;
import org.jkiss.dbeaver.tasks.nativetool.NativeToolUtils;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.contentassist.ContentAssistUtils;
import org.jkiss.dbeaver.ui.contentassist.SmartTextContentAdapter;
import org.jkiss.dbeaver.ui.contentassist.StringContentProposalProvider;
import org.jkiss.dbeaver.ui.dialogs.DialogUtils;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.utils.CommonUtils;

import java.io.File;
import java.util.Objects;


class PostgreBackupWizardPageSettings extends PostgreToolWizardPageSettings<PostgreBackupWizard> {

    private Text outputFolderText;
    private Text outputFileText;
    private Combo formatCombo;
    private Combo compressCombo;
    private Combo encodingCombo;
    private Button useInsertsCheck;
    private Button noPrivilegesCheck;
    private Button noOwnerCheck;

    PostgreBackupWizardPageSettings(PostgreBackupWizard wizard)
    {
        super(wizard, PostgreMessages.wizard_backup_page_setting_title_setting);
        setTitle(PostgreMessages.wizard_backup_page_setting_title);
        setDescription(PostgreMessages.wizard_backup_page_setting_description);
    }

    @Override
    protected boolean determinePageCompletion() {
        if (wizard.getSettings().getOutputFolder() == null) {
            setErrorMessage("Output folder not specified");
            return false;
        }
        return super.determinePageCompletion();
    }

    @Override
    public void createControl(Composite parent)
    {
        Composite composite = UIUtils.createPlaceholder(parent, 1);

        SelectionListener changeListener = new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                updateState();
            }
        };

        Group formatGroup = UIUtils.createControlGroup(composite, PostgreMessages.wizard_backup_page_setting_group_setting, 2, GridData.FILL_HORIZONTAL, 0);
        formatCombo = UIUtils.createLabelCombo(formatGroup, PostgreMessages.wizard_backup_page_setting_label_format, SWT.DROP_DOWN | SWT.READ_ONLY);
        formatCombo.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING));
        for (PostgreDatabaseBackupSettings.ExportFormat format : PostgreDatabaseBackupSettings.ExportFormat.values()) {
            formatCombo.add(format.getTitle());
        }
        formatCombo.select(wizard.getSettings().getFormat().ordinal());
        formatCombo.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                fixOutputFileExtension();
                updateState();
            }
        });

        compressCombo = UIUtils.createLabelCombo(formatGroup, PostgreMessages.wizard_backup_page_setting_label_compression, SWT.DROP_DOWN | SWT.READ_ONLY);
        compressCombo.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING));
        compressCombo.add("");
        for (int i = 0; i <= 9; i++) {
            String compStr = String.valueOf(i);
            compressCombo.add(compStr);
            if (compStr.equals(wizard.getSettings().getCompression())) {
                compressCombo.select(i);
            }
        }
        if (compressCombo.getSelectionIndex() < 0) {
            compressCombo.select(0);
        }
        compressCombo.addSelectionListener(changeListener);

        UIUtils.createControlLabel(formatGroup, PostgreMessages.wizard_backup_page_setting_label_encoding);
        encodingCombo = UIUtils.createEncodingCombo(formatGroup, null);
        encodingCombo.addSelectionListener(changeListener);
        encodingCombo.setText(wizard.getSettings().getEncoding());

        useInsertsCheck = UIUtils.createCheckbox(formatGroup,
        	PostgreMessages.wizard_backup_page_setting_checkbox_use_insert,
            null,
            wizard.getSettings().isUseInserts(),
            2
        );
        useInsertsCheck.addSelectionListener(changeListener);

        noPrivilegesCheck = UIUtils.createCheckbox(formatGroup,
            PostgreMessages.wizard_backup_page_setting_checkbox_no_privileges,
            null,
            wizard.getSettings().isNoPrivileges(),
            2
        );
        noPrivilegesCheck.addSelectionListener(changeListener);

        noOwnerCheck = UIUtils.createCheckbox(formatGroup,
            PostgreMessages.wizard_backup_page_setting_checkbox_no_owner,
            null,
            wizard.getSettings().isNoOwner(),
            2
        );
        noOwnerCheck.addSelectionListener(changeListener);

        Group outputGroup = UIUtils.createControlGroup(composite, PostgreMessages.wizard_backup_page_setting_group_output, 2, GridData.FILL_HORIZONTAL, 0);
        outputFolderText = DialogUtils.createOutputFolderChooser(
            outputGroup,
            PostgreMessages.wizard_backup_page_setting_label_output_folder,
            wizard.getSettings().getOutputFolder() != null ? wizard.getSettings().getOutputFolder().getAbsolutePath() : null,
            e -> updateState());
        outputFileText = UIUtils.createLabelText(
            outputGroup,
            PostgreMessages.wizard_backup_page_setting_label_file_name_pattern,
            wizard.getSettings().getOutputFilePattern());
        UIUtils.setContentProposalToolTip(outputFileText, PostgreMessages.wizard_backup_page_setting_label_file_name_pattern_output,
            NativeToolUtils.VARIABLE_HOST,
            NativeToolUtils.VARIABLE_DATABASE,
            NativeToolUtils.VARIABLE_TABLE,
            NativeToolUtils.VARIABLE_DATE,
            NativeToolUtils.VARIABLE_TIMESTAMP,
            NativeToolUtils.VARIABLE_CONN_TYPE);
        ContentAssistUtils.installContentProposal(
            outputFileText,
            new SmartTextContentAdapter(),
            new StringContentProposalProvider(
                GeneralUtils.variablePattern(NativeToolUtils.VARIABLE_HOST),
                GeneralUtils.variablePattern(NativeToolUtils.VARIABLE_DATABASE),
                GeneralUtils.variablePattern(NativeToolUtils.VARIABLE_TABLE),
                GeneralUtils.variablePattern(NativeToolUtils.VARIABLE_DATE),
                GeneralUtils.variablePattern(NativeToolUtils.VARIABLE_TIMESTAMP),
                GeneralUtils.variablePattern(NativeToolUtils.VARIABLE_CONN_TYPE)));
        outputFileText.addModifyListener(e -> wizard.getSettings().setOutputFilePattern(outputFileText.getText()));
        fixOutputFileExtension();

        createExtraArgsInput(outputGroup);

        Composite extraGroup = UIUtils.createComposite(composite, 2);
        createSecurityGroup(extraGroup);

        setControl(composite);
    }

    private void fixOutputFileExtension() {
        String text = outputFileText.getText();
        String name;
        String ext;
        int idxOfExtStart = text.lastIndexOf('.');
        if (idxOfExtStart > -1 && idxOfExtStart <= text.length()) {
            name = text.substring(0, idxOfExtStart);
            ext = text.substring(idxOfExtStart + 1);
        } else {
            name = text;
            ext = "";
        }
        String newExt = getChosenExportFormat().getExt();
        boolean isDotWithEmptyExt = ext.isEmpty() && idxOfExtStart > -1; // {file_name}.
        if (Objects.equals(ext, newExt) && !isDotWithEmptyExt) {
            return;
        }
        if (!newExt.isEmpty()) {
            newExt = "." + newExt;
        }
        text = name + newExt;
        outputFileText.setText(text);
    }

    @Override
    protected void updateState()
    {
        saveState();
        updatePageCompletion();
        getContainer().updateButtons();
    }

    @Override
    public void saveState() {
        super.saveState();

        PostgreDatabaseBackupSettings settings = wizard.getSettings();

        String fileName = outputFolderText.getText();
        settings.setOutputFolder(CommonUtils.isEmpty(fileName) ? null : new File(fileName));
        settings.setOutputFilePattern(outputFileText.getText());

        settings.setFormat(getChosenExportFormat());
        settings.setCompression(compressCombo.getText());
        settings.setEncoding(encodingCombo.getText());
        settings.setUseInserts(useInsertsCheck.getSelection());
        settings.setNoPrivileges(noPrivilegesCheck.getSelection());
        settings.setNoOwner(noOwnerCheck.getSelection());
    }

    private PostgreBackupRestoreSettings.ExportFormat getChosenExportFormat() {
        return PostgreDatabaseBackupSettings.ExportFormat.values()[formatCombo.getSelectionIndex()];
    }
}
