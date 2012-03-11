/*
 * Copyright 2012 CodeSlap
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.codeslap.persistence.pref;

/**
 * @author cristian
 */
class PrefMetadata {
    private int title, summary, order, dialogTitle, dialogMessage, dialogIcon, entries, entryValues;
    private Class<?> type;
    private String defaultValue;
    private String key, dependency;

    public PrefMetadata setTitle(int title) {
        this.title = title;
        return this;
    }

    public PrefMetadata setSummary(int summary) {
        this.summary = summary;
        return this;
    }

    public PrefMetadata setOrder(int order) {
        this.order = order;
        return this;
    }

    public PrefMetadata setType(Class<?> type) {
        this.type = type;
        return this;
    }

    public PrefMetadata setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
        return this;
    }

    public PrefMetadata setKey(String key) {
        this.key = key;
        return this;
    }

    public PrefMetadata setDialogIcon(int dialogIcon) {
        this.dialogIcon = dialogIcon;
        return this;
    }

    public PrefMetadata setDialogMessage(int dialogMessage) {
        this.dialogMessage = dialogMessage;
        return this;
    }

    public PrefMetadata setDialogTitle(int dialogTitle) {
        this.dialogTitle = dialogTitle;
        return this;
    }

    public PrefMetadata setEntries(int entries) {
        this.entries = entries;
        return this;
    }

    public PrefMetadata setEntryValues(int entryValues) {
        this.entryValues = entryValues;
        return this;
    }

    public PrefMetadata setDependency(String dependency) {
        this.dependency = dependency;
        return this;
    }

    public String getDependency() {
        return dependency;
    }

    public int getTitle() {
        return title;
    }

    public int getSummary() {
        return summary;
    }

    public int getOrder() {
        return order;
    }

    public int getDialogTitle() {
        return dialogTitle;
    }

    public int getDialogMessage() {
        return dialogMessage;
    }

    public int getDialogIcon() {
        return dialogIcon;
    }

    public Class<?> getType() {
        return type;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    public String getKey() {
        return key;
    }

    public int getEntries() {
        return entries;
    }

    public int getEntryValues() {
        return entryValues;
    }
}
