/*
 * Copyright (C) 2010-2018 Stichting Akvo (Akvo Foundation)
 *
 * This file is part of Akvo FLOW.
 *
 * Akvo FLOW is free software: you can redistribute it and modify it under the terms of
 * the GNU Affero General Public License (AGPL) as published by the Free Software Foundation,
 * either version 3 of the License or any later version.
 *
 * Akvo FLOW is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Affero General Public License included below for more details.
 *
 * The full license text can also be seen at <http://www.gnu.org/licenses/agpl.html>.
 *
 */

package org.akvo.flow.data.entity;

import com.google.gson.annotations.SerializedName;

public class ApiApkData {

    @SerializedName("version")
    private final String version;

    @SerializedName("fileName")
    private final String fileUrl;

    @SerializedName("md5Checksum")
    private final String md5Checksum;

    public ApiApkData(String version, String fileUrl, String md5Checksum) {
        this.version = version;
        this.fileUrl = fileUrl;
        this.md5Checksum = md5Checksum;
    }

    public String getVersion() {
        return version;
    }

    public String getFileUrl() {
        return fileUrl;
    }

    public String getMd5Checksum() {
        return md5Checksum;
    }
}
