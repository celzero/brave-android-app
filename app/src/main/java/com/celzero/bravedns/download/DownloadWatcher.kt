/*
 * Copyright 2021 RethinkDNS and its authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.celzero.bravedns.download

import android.app.DownloadManager
import android.content.Context
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.celzero.bravedns.receiver.ReceiverHelper
import com.celzero.bravedns.ui.HomeScreenActivity
import com.celzero.bravedns.util.Constants

/**
 * The download watcher  - Worker initiated from AppDownloadManager class.
 * The worker will be listening for the status of the download for the download ID's
 * stored in shared preference.
 * Once the download is completed, the Worker will send a Result.success().
 * Else, the Result.retry() will be triggered to check again. 
 */
class DownloadWatcher(val context: Context, workerParameters: WorkerParameters)
        : Worker(context, workerParameters) {
    override fun doWork(): Result {

        if(ReceiverHelper.persistentState.downloadIDs.isEmpty()){
            return Result.retry()
        }

        when (checkForDownload(context)) {
            0 -> {
                return Result.retry()
            }
            -1 -> {
                return Result.failure()
            }
            1 -> {
                return Result.success()
            }
        }

        return Result.failure()
    }

    private fun checkForDownload(context: Context): Int {
        //Check for the download success from the receiver
        ReceiverHelper.persistentState.downloadIDs.forEach { downloadID ->
            val query = DownloadManager.Query()
            val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            val cursor = downloadManager.query(query)
            cursor?.let {
                if (cursor.moveToFirst()) {
                    val status = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS))
                    if (HomeScreenActivity.GlobalVariable.DEBUG) Log.d(Constants.LOG_TAG, "AppDownloadManager onReceive ACTION_DOWNLOAD_COMPLETE 1 $downloadID")
                    query.setFilterById(downloadID.toLong())
                    if (status == DownloadManager.STATUS_RUNNING) {
                        Log.i(Constants.LOG_TAG, "AppDownloadManager status is $downloadID running")
                    } else if (status == DownloadManager.STATUS_SUCCESSFUL) {
                        if (ReceiverHelper.persistentState.downloadIDs.contains(downloadID)) {
                            ReceiverHelper.persistentState.downloadIDs = ReceiverHelper.persistentState.downloadIDs - downloadID.toString()
                        }
                        Log.i(Constants.LOG_TAG, "AppDownloadManager onReceive STATUS_SUCCESSFUL - ${downloadID}")
                        if (ReceiverHelper.persistentState.downloadIDs.isEmpty()) {
                            cursor.close()
                            return 1
                        }
                    } else if (status == DownloadManager.STATUS_FAILED) {
                        if (HomeScreenActivity.GlobalVariable.DEBUG) Log.d(Constants.LOG_TAG, "AppDownloadManager STATUS_FAILED")
                        cursor.close()
                        return -1
                    }
                }
            }
            cursor.close()
        }
        return 0
    }


}