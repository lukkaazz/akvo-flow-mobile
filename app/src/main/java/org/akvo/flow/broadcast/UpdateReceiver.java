/*
 * Copyright (C) 2019 Stichting Akvo (Akvo Foundation)
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
 */

package org.akvo.flow.broadcast;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import org.akvo.flow.service.DataFixWorker;
import org.akvo.flow.service.DataPointUploadWorker;

import java.util.concurrent.TimeUnit;

import androidx.work.Constraints;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

public class UpdateReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_MY_PACKAGE_REPLACED.equals(intent.getAction())) {
            Constraints constraints = new Constraints.Builder()
                    .setRequiresStorageNotLow(true)
                    .build();
            OneTimeWorkRequest dataFixRequest = new OneTimeWorkRequest
                    .Builder(DataFixWorker.class)
                    .setInitialDelay(0, TimeUnit.SECONDS)
                    .setConstraints(constraints)
                    .addTag(DataFixWorker.TAG)
                    .build();
            Constraints uploadConstraints = new Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.UNMETERED)
                    .build();
            OneTimeWorkRequest uploadWorkRequest = new OneTimeWorkRequest.Builder(
                    DataPointUploadWorker.class)
                    .setInitialDelay(0, TimeUnit.SECONDS)
                    .setConstraints(uploadConstraints)
                    .addTag(DataPointUploadWorker.TAG)
                    .build();
            WorkManager.getInstance(context.getApplicationContext())
                    .beginUniqueWork(DataFixWorker.TAG, ExistingWorkPolicy.REPLACE, dataFixRequest)
                    .then(uploadWorkRequest)
                    .enqueue();
        }
    }
}
