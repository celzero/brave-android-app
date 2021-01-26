/*
Copyright 2020 RethinkDNS and its authors

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

https://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/
package com.celzero.bravedns.adapter

import android.content.Context
import android.content.DialogInterface
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.content.res.AppCompatResources
import androidx.paging.PagedListAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.celzero.bravedns.R
import com.celzero.bravedns.database.AppInfo
import com.celzero.bravedns.database.AppInfoRepository
import com.celzero.bravedns.database.CategoryInfoRepository
import com.celzero.bravedns.databinding.ExcludedAppListItemBinding
import com.celzero.bravedns.ui.HomeScreenActivity.GlobalVariable.DEBUG
import com.celzero.bravedns.util.Constants.Companion.LOG_TAG
import com.celzero.bravedns.util.ThrowingHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class ExcludedAppListAdapter(private val context: Context, private val appInfoRepository: AppInfoRepository, private val categoryInfoRepository: CategoryInfoRepository) : PagedListAdapter<AppInfo, ExcludedAppListAdapter.ExcludedAppInfoViewHolder>(DIFF_CALLBACK) {

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<AppInfo>() {
            // Concert details may have changed if reloaded from the database,
            // but ID is fixed.
            override fun areItemsTheSame(oldConnection: AppInfo, newConnection: AppInfo) = oldConnection.packageInfo == newConnection.packageInfo

            override fun areContentsTheSame(oldConnection: AppInfo, newConnection: AppInfo) = oldConnection == newConnection
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExcludedAppInfoViewHolder {
        val itemBinding = ExcludedAppListItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ExcludedAppInfoViewHolder(itemBinding)
    }

    override fun onBindViewHolder(holder: ExcludedAppInfoViewHolder, position: Int) {
        val appInfo: AppInfo = getItem(position) ?: return
        holder.update(appInfo)
    }


    inner class ExcludedAppInfoViewHolder(private val b: ExcludedAppListItemBinding) : RecyclerView.ViewHolder(b.root) {

        fun update(appInfo: AppInfo) {
            b.excludedAppListApkLabelTv.text = appInfo.appName
            b.excludedAppListCheckbox.isChecked = appInfo.isExcluded
            try {
                //val icon = context.packageManager.getApplicationIcon(appInfo.packageInfo)
                //appIcon.setImageDrawable(icon)
                Glide.with(context).load(context.packageManager.getApplicationIcon(appInfo.packageInfo)).into(b.excludedAppListApkIconIv)
            } catch (e: Exception) {
                //appIcon.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.default_app_icon))
                Glide.with(context).load(AppCompatResources.getDrawable(context, R.drawable.default_app_icon)).into(b.excludedAppListApkIconIv)
                Log.e(LOG_TAG, "Application Icon not available for package: ${appInfo.packageInfo}" + e.message, e)
            }

            b.excludedAppListContainer.setOnClickListener {
                if (DEBUG) Log.d(LOG_TAG, "parentView- whitelist - ${appInfo.appName},${appInfo.isExcluded}")
                appInfo.isExcluded = !appInfo.isExcluded
                excludeAppsFromVPN(appInfo, appInfo.isExcluded)
            }

            b.excludedAppListCheckbox.setOnCheckedChangeListener(null)
            b.excludedAppListCheckbox.setOnClickListener {
                if (DEBUG) Log.d(LOG_TAG, "CheckBox- whitelist - ${appInfo.appName},${appInfo.isExcluded}")
                appInfo.isExcluded = !appInfo.isExcluded
                excludeAppsFromVPN(appInfo, appInfo.isExcluded)
            }
        }

        private fun excludeAppsFromVPN(appInfo: AppInfo, status: Boolean) {
            val appUIDList = appInfoRepository.getAppListForUID(appInfo.uid)
            var blockAllApps = false
            blockAllApps = if (appUIDList.size > 1) {
                showDialog(appUIDList, appInfo.appName, status)
            } else {
                true
            }
            if (blockAllApps) {
                b.excludedAppListCheckbox.isChecked = status
                GlobalScope.launch(Dispatchers.IO) {
                    appInfoRepository.updateExcludedList(appInfo.uid, status)
                    val count = appInfoRepository.getBlockedCountForCategory(appInfo.appCategory)
                    val excludedCount = appInfoRepository.getExcludedAppCountForCategory(appInfo.appCategory)
                    val whitelistCount = appInfoRepository.getBlockedCountForCategory(appInfo.appCategory)
                    categoryInfoRepository.updateBlockedCount(appInfo.appCategory, count)
                    categoryInfoRepository.updateExcludedCount(appInfo.appCategory, excludedCount)
                    categoryInfoRepository.updateWhitelistCount(appInfo.appCategory, whitelistCount)
                }
                //excludedAppsFromVPN[appInfo.packageInfo] = status
                if (DEBUG) Log.d(LOG_TAG, "Apps excluded - ${appInfo.appName}, $status")
            } else {
                b.excludedAppListCheckbox.isChecked = !status
                appInfo.isExcluded = !status
                if (DEBUG) Log.d(LOG_TAG, "App not excluded - ${appInfo.appName}, $status")
            }
        }

        private fun showDialog(packageList: List<AppInfo>, appName: String, isInternet: Boolean): Boolean {
            //Change the handler logic into some other
            val handler: Handler = ThrowingHandler()
            var positiveTxt = ""
            val packageNameList: List<String> = packageList.map { it.appName }
            var proceedBlocking: Boolean = false

            val builderSingle: AlertDialog.Builder = AlertDialog.Builder(context)

            builderSingle.setIcon(R.drawable.ic_exclude_app)
            if (isInternet) {
                builderSingle.setTitle("Excluding \"$appName\" will also exclude these ${packageList.size} apps")
                positiveTxt = "Exclude ${packageList.size} apps"
            } else {
                builderSingle.setTitle("Unexcluding \"$appName\" will also unexclude these ${packageList.size} apps")
                positiveTxt = "Unexclude ${packageList.size} apps"
            }
            val arrayAdapter = ArrayAdapter<String>(context, android.R.layout.simple_list_item_activated_1)
            arrayAdapter.addAll(packageNameList)
            builderSingle.setCancelable(false)
            //builderSingle.setSingleChoiceItems(arrayAdapter,-1,({dialogInterface: DialogInterface, which : Int ->}))
            builderSingle.setItems(packageNameList.toTypedArray(), null)


            /* builderSingle.setAdapter(arrayAdapter) { dialogInterface, which ->
                  if(DEBUG) Log.d(LOG_TAG,"OnClick")
                 //dialogInterface.cancel()
                 //builderSingle.setCancelable(false)
             }*/
            /*val alertDialog : AlertDialog = builderSingle.create()
            alertDialog.getListView().setOnItemClickListener({ adapterView, subview, i, l -> })*/
            builderSingle.setPositiveButton(positiveTxt) { dialogInterface: DialogInterface, i: Int ->
                proceedBlocking = true
                handler.sendMessage(handler.obtainMessage())
            }.setNeutralButton("Go Back") { dialogInterface: DialogInterface, i: Int ->
                handler.sendMessage(handler.obtainMessage())
                proceedBlocking = false
            }

            val alertDialog: AlertDialog = builderSingle.show()
            alertDialog.listView.setOnItemClickListener { adapterView, subview, i, l -> }
            alertDialog.setCancelable(false)
            try {
                Looper.loop()
            } catch (e2: java.lang.RuntimeException) {
            }

            return proceedBlocking
        }

    }

}