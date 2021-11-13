/*
 * MD4J, a parser library for MDict format.
 * Copyright (C) 2021 Hiroshi Miura.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package io.github.eb4j.mdict.data;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;


@JacksonXmlRootElement(localName = "Dictionary")
public final class DictionaryInfo {
    @JacksonXmlProperty(localName = "GeneratedByEngineVersion", isAttribute = true)
    private String generatedByEngineVersion;
    @JacksonXmlProperty(localName = "RequiredEngineVersion", isAttribute = true)
    private String requiredEngineVersion;
    @JacksonXmlProperty(localName = "Format", isAttribute = true)
    private String format;
    @JacksonXmlProperty(localName = "KeyCaseSensitive", isAttribute = true)
    private String keyCaseSensitive;
    @JacksonXmlProperty(localName = "StripKey", isAttribute = true)
    private String stripKey;
    @JacksonXmlProperty(localName = "Encrypted", isAttribute = true)
    private String encrypted;
    @JacksonXmlProperty(localName = "RegisterBy", isAttribute = true)
    private String registerBy;
    @JacksonXmlProperty(localName = "Description", isAttribute = true)
    private String description;
    @JacksonXmlProperty(localName = "Title", isAttribute = true)
    private String title;
    @JacksonXmlProperty(localName = "Encoding", isAttribute = true)
    private String encoding;
    @JacksonXmlProperty(localName = "CreationDate", isAttribute = true)
    private String creationDate;
    @JacksonXmlProperty(localName = "Compact", isAttribute = true)
    private String compact;
    @JacksonXmlProperty(localName = "Compat", isAttribute = true)
    private String compat;
    @JacksonXmlProperty(localName = "Left2Right", isAttribute = true)
    private String left2Right;
    @JacksonXmlProperty(localName = "DataSourceFormat", isAttribute = true)
    private String dataSourceFormat;
    @JacksonXmlProperty(localName = "StyleSheet", isAttribute = true)
    private String styleSheet;
    @JacksonXmlProperty(localName = "RegCode", isAttribute = true)
    private String regCode;

    @JsonIgnore
    private long keyBlockPosition;


    public String getGeneratedByEngineVersion() {
        return generatedByEngineVersion;
    }

    public void setGeneratedByEngineVersion(final String generatedByEngineVersion) {
        this.generatedByEngineVersion = generatedByEngineVersion;
    }

    public String getRequiredEngineVersion() {
        return requiredEngineVersion;
    }

    public void setRequiredEngineVersion(final String requiredEngineVersion) {
        this.requiredEngineVersion = requiredEngineVersion;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(final String format) {
        this.format = format;
    }

    public String getKeyCaseSensitive() {
        return keyCaseSensitive;
    }

    public void setKeyCaseSensitive(final String keyCaseSensitive) {
        this.keyCaseSensitive = keyCaseSensitive;
    }

    public String getStripKey() {
        return stripKey;
    }

    public void setStripKey(final String stripKey) {
        this.stripKey = stripKey;
    }

    public String getEncrypted() {
        return encrypted;
    }

    public void setEncrypted(final String encrypted) {
        this.encrypted = encrypted;
    }

    public String getRegisterBy() {
        return registerBy;
    }

    public void setRegisterBy(final String registerBy) {
        this.registerBy = registerBy;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(final String description) {
        this.description = description;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(final String title) {
        this.title = title;
    }

    public String getEncoding() {
        return encoding;
    }

    public void setEncoding(final String encoding) {
        this.encoding = encoding;
    }

    public String getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(final String creationDate) {
        this.creationDate = creationDate;
    }

    public String getCompact() {
        return compact;
    }

    public void setCompact(final String compact) {
        this.compact = compact;
    }

    public String getCompat() {
        return compat;
    }

    public void setCompat(final String compat) {
        this.compat = compat;
    }

    public String getLeft2Right() {
        return left2Right;
    }

    public void setLeft2Right(final String left2Right) {
        this.left2Right = left2Right;
    }

    public String getDataSourceFormat() {
        return dataSourceFormat;
    }

    public void setDataSourceFormat(final String dataSourceFormat) {
        this.dataSourceFormat = dataSourceFormat;
    }

    public String getStyleSheet() {
        return styleSheet;
    }

    public void setStyleSheet(final String styleSheet) {
        this.styleSheet = styleSheet;
    }

    public String getRegCode() {
        return regCode;
    }

    public void setRegCode(final String regCode) {
        this.regCode = regCode;
    }

    public long getKeyBlockPosition() {
        return keyBlockPosition;
    }

    public void setKeyBlockPosition(final long keyBlockPosition) {
        this.keyBlockPosition = keyBlockPosition;
    }

    @Override
    public String toString() {
        return "Dictionary{" +
                "generatedByEngineVersion='" + generatedByEngineVersion + '\'' +
                ", requiredEngineVersion='" + requiredEngineVersion + '\'' +
                ", format='" + format + '\'' +
                ", keyCaseSensitive='" + keyCaseSensitive + '\'' +
                ", stripKey='" + stripKey + '\'' +
                ", encrypted='" + encrypted + '\'' +
                ", registerBy='" + registerBy + '\'' +
                ", description='" + description + '\'' +
                ", title='" + title + '\'' +
                ", encoding='" + encoding + '\'' +
                ", creationDate='" + creationDate + '\'' +
                ", compact='" + compact + '\'' +
                ", compat='" + compat + '\'' +
                ", left2Right='" + left2Right + '\'' +
                ", dataSourceFormat='" + dataSourceFormat + '\'' +
                ", styleSheet='" + styleSheet + '\'' +
                '}';
    }
}
