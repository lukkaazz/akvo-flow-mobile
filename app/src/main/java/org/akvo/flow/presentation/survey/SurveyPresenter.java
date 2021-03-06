/*
 * Copyright (C) 2018-2019 Stichting Akvo (Akvo Foundation)
 *
 * This file is part of Akvo Flow.
 *
 * Akvo Flow is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Akvo Flow is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Akvo Flow.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package org.akvo.flow.presentation.survey;

import androidx.core.util.Pair;

import org.akvo.flow.BuildConfig;
import org.akvo.flow.domain.entity.ApkData;
import org.akvo.flow.domain.interactor.DefaultObserver;
import org.akvo.flow.domain.interactor.UseCase;
import org.akvo.flow.domain.util.VersionHelper;
import org.akvo.flow.presentation.Presenter;
import org.akvo.flow.presentation.entity.ViewApkMapper;
import org.akvo.flow.util.ConstantUtil;

import javax.inject.Inject;
import javax.inject.Named;

import io.reactivex.annotations.NonNull;
import timber.log.Timber;

public class SurveyPresenter implements Presenter {

    private static final long NOT_NOTIFIED = -1;

    private final UseCase getApkDataPreferences;
    private final UseCase saveApkUpdateNotified;
    private final VersionHelper versionHelper;
    private final ViewApkMapper viewApkMapper;

    private SurveyView view;

    @Inject
    public SurveyPresenter(@Named("GetApkDataPreferences") UseCase getApkDataPreferences,
            @Named("SaveApkUpdateNotified") UseCase saveApkUpdateNotified,
            VersionHelper versionHelper, ViewApkMapper viewApkMapper) {
        this.getApkDataPreferences = getApkDataPreferences;
        this.saveApkUpdateNotified = saveApkUpdateNotified;
        this.versionHelper = versionHelper;
        this.viewApkMapper = viewApkMapper;
    }

    @Override
    public void destroy() {
        getApkDataPreferences.dispose();
        saveApkUpdateNotified.dispose();
    }

    public void setView(SurveyView view) {
        this.view = view;
    }

    public void verifyApkUpdate() {
        getApkDataPreferences.execute(new DefaultObserver<Pair<ApkData, Long>>() {
            @Override
            public void onError(Throwable e) {
                Timber.e(e);
            }

            @Override
            public void onNext(@NonNull Pair<ApkData, Long> apkDataLongPair) {
                showApkUpdateIfNeeded(apkDataLongPair.first, apkDataLongPair.second);
            }
        }, null);
    }

    private void showApkUpdateIfNeeded(ApkData apkData, long lastNotified) {
        if (!ApkData.NOT_SET_VALUE.equals(apkData) && shouldNotifyNewVersion(lastNotified)
                && versionHelper.isNewerVersion(BuildConfig.VERSION_NAME, apkData.getVersion())) {
            notifyNewVersionAvailable(apkData);
        }
    }

    private void notifyNewVersionAvailable(ApkData apkData) {
        saveApkUpdateNotified.execute(new DefaultObserver<Boolean>(){
            @Override
            public void onError(Throwable e) {
                Timber.e(e);
            }
        }, null);
        view.showNewVersionAvailable(viewApkMapper.transform(apkData));
    }

    private boolean shouldNotifyNewVersion(long lastNotified) {
        if (lastNotified == NOT_NOTIFIED) {
            return true;
        }
        return System.currentTimeMillis() - lastNotified
                >= ConstantUtil.UPDATE_NOTIFICATION_DELAY_IN_MS;
    }
}
