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
package org.jkiss.dbeaver.ui.actions.datasource;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.osgi.util.NLS;
import org.eclipse.ui.IViewPart;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.navigator.DBNBrowseSettings;
import org.jkiss.dbeaver.registry.DataSourceDescriptor;
import org.jkiss.dbeaver.registry.DataSourceNavigatorSettings;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.actions.AbstractDataSourceHandler;
import org.jkiss.dbeaver.ui.dialogs.connection.EditConnectionNavigatorSettingsDialog;
import org.jkiss.dbeaver.ui.internal.UINavigatorMessages;
import org.jkiss.dbeaver.ui.navigator.INavigatorModelView;
import org.jkiss.dbeaver.ui.navigator.NavigatorPreferences;
import org.jkiss.dbeaver.ui.navigator.database.DatabaseNavigatorView;

import java.util.List;

public class ConnectionViewSettingsContributor extends DataSourceMenuContributor {
    private static final Log log = Log.getLog(ConnectionViewSettingsContributor.class);

    @Override
    protected void fillContributionItems(final List<IContributionItem> menuItems) {
        DBPDataSourceContainer dsContainer = AbstractDataSourceHandler.getDataSourceContainerFromPart(UIUtils.getActiveWorkbenchWindow().getActivePage().getActivePart());
        if (dsContainer == null) {
            return;
        }
        boolean presetChecked = false;
        for (DataSourceNavigatorSettings.Preset preset : DataSourceNavigatorSettings.PRESETS.values()) {
            if (preset == DataSourceNavigatorSettings.PRESET_CUSTOM) {
                continue;
            }
            boolean checked = preset.getSettings().equals(dsContainer.getNavigatorSettings());
            if (checked) {
                presetChecked = checked;
            }
            menuItems.add(new ActionContributionItem(new UseSettingsPresetAction(dsContainer, preset, checked)));
        }
        menuItems.add(new ActionContributionItem(new UseSettingsCustomAction(dsContainer, !presetChecked)));
        menuItems.add(new Separator());
        menuItems.add(new ActionContributionItem(new ShowSystemObjectsAction(dsContainer)));
        menuItems.add(new Separator());
        menuItems.add(new ActionContributionItem(new ShowHostNameAction(dsContainer)));
        menuItems.add(new ActionContributionItem(new ShowStatisticsAction(dsContainer)));
        menuItems.add(new ActionContributionItem(new ShowStatusIconsAction(dsContainer)));
    }

    private abstract static class SettingsAction extends Action {
        final DBPDataSourceContainer dsContainer;

        SettingsAction(DBPDataSourceContainer dsContainer, String name, int style) {
            super(name, style);
            this.dsContainer = dsContainer;
        }

        void updateSettings(DBNBrowseSettings settings) {
            ((DataSourceDescriptor)this.dsContainer).setNavigatorSettings(settings);
            dsContainer.persistConfiguration();

            if (dsContainer.isConnected()) {
                if (UIUtils.confirmAction(
                    UIUtils.getActiveWorkbenchShell(),
                    CoreMessages.dialog_connection_edit_wizard_conn_change_title,
                    NLS.bind(CoreMessages.dialog_connection_edit_wizard_conn_change_question, dsContainer.getName()) ))
                {
                    DataSourceHandler.reconnectDataSource(null, dsContainer);
                }
            }
        }

        void refreshNavigator() {
            IViewPart view = UIUtils.getActiveWorkbenchWindow().getActivePage().findView(DatabaseNavigatorView.VIEW_ID);
            if (view instanceof INavigatorModelView) {
                Viewer navigatorViewer = ((INavigatorModelView) view).getNavigatorViewer();
                if (navigatorViewer != null) {
                    navigatorViewer.getControl().redraw();
                }
            }
        }
    }

    private static class UseSettingsPresetAction extends SettingsAction {
        private final DataSourceNavigatorSettings.Preset preset;

        UseSettingsPresetAction(DBPDataSourceContainer dsContainer, DataSourceNavigatorSettings.Preset preset, boolean checked) {
            super(dsContainer, preset.getName(), AS_RADIO_BUTTON);
            this.preset = preset;
            setToolTipText(preset.getDescription());
            setChecked(checked);
        }

        @Override
        public void run() {
            if (isChecked()) {
                updateSettings(preset.getSettings());
            }
        }
    }

    private static class UseSettingsCustomAction extends SettingsAction {
        UseSettingsCustomAction(DBPDataSourceContainer dsContainer, boolean checked) {
            super(dsContainer, DataSourceNavigatorSettings.PRESET_CUSTOM.getName() + " ...", AS_RADIO_BUTTON);
            setToolTipText(DataSourceNavigatorSettings.PRESET_CUSTOM.getDescription());
            setChecked(checked);
        }

        @Override
        public void run() {
            if (!isChecked()) {
                return;
            }
            EditConnectionNavigatorSettingsDialog dialog = new EditConnectionNavigatorSettingsDialog(
                UIUtils.getActiveWorkbenchShell(),
                dsContainer.getNavigatorSettings(),
                dsContainer);
            if (dialog.open() == IDialogConstants.OK_ID) {
                updateSettings(dialog.getNavigatorSettings());
            }
        }
    }

    private static class ShowSystemObjectsAction extends SettingsAction {
        ShowSystemObjectsAction(DBPDataSourceContainer container) {
            super(container, CoreMessages.dialog_connection_wizard_final_checkbox_show_system_objects, AS_CHECK_BOX);
            setToolTipText(CoreMessages.dialog_connection_wizard_final_checkbox_show_system_objects_tip);
            setChecked(container.getNavigatorSettings().isShowSystemObjects());
        }

        @Override
        public void run() {
            DataSourceNavigatorSettings newSettings = new DataSourceNavigatorSettings(dsContainer.getNavigatorSettings());
            newSettings.setShowSystemObjects(isChecked());
            updateSettings(newSettings);
        }
    }

    private static class ShowHostNameAction extends SettingsAction {
        ShowHostNameAction(DBPDataSourceContainer container) {
            super(container, UINavigatorMessages.pref_page_database_general_label_show_host_name, AS_CHECK_BOX);
            setToolTipText(UINavigatorMessages.pref_page_database_general_label_show_host_name_tip);
            setChecked(DBWorkbench.getPlatform().getPreferenceStore().getBoolean(NavigatorPreferences.NAVIGATOR_SHOW_CONNECTION_HOST_NAME));
        }

        @Override
        public void run() {
            DBWorkbench.getPlatform().getPreferenceStore().setValue(NavigatorPreferences.NAVIGATOR_SHOW_CONNECTION_HOST_NAME, isChecked());
            refreshNavigator();
        }
    }

    private static class ShowStatisticsAction extends SettingsAction {
        ShowStatisticsAction(DBPDataSourceContainer container) {
            super(container, UINavigatorMessages.pref_page_database_general_label_show_statistics, AS_CHECK_BOX);
            setToolTipText(UINavigatorMessages.pref_page_database_general_label_show_statistics_tip);
            setChecked(DBWorkbench.getPlatform().getPreferenceStore().getBoolean(NavigatorPreferences.NAVIGATOR_SHOW_STATISTICS_INFO));
        }

        @Override
        public void run() {
            DBWorkbench.getPlatform().getPreferenceStore().setValue(NavigatorPreferences.NAVIGATOR_SHOW_STATISTICS_INFO, isChecked());
            refreshNavigator();
        }
    }

    private static class ShowStatusIconsAction extends SettingsAction {
        ShowStatusIconsAction(DBPDataSourceContainer container) {
            super(container, UINavigatorMessages.pref_page_database_general_label_show_node_actions, AS_CHECK_BOX);
            setToolTipText(UINavigatorMessages.pref_page_database_general_label_show_node_actions_tip);
            setChecked(DBWorkbench.getPlatform().getPreferenceStore().getBoolean(NavigatorPreferences.NAVIGATOR_SHOW_NODE_ACTIONS));
        }

        @Override
        public void run() {
            DBWorkbench.getPlatform().getPreferenceStore().setValue(NavigatorPreferences.NAVIGATOR_SHOW_NODE_ACTIONS, isChecked());
            refreshNavigator();
        }
    }

}