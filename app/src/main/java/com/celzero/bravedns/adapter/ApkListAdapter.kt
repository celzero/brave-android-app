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
import android.content.pm.PackageInfo
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.RecyclerView
import com.celzero.bravedns.databinding.LayoutApkItemBinding
import com.celzero.bravedns.ui.BottomSheetFragment
import com.celzero.bravedns.util.Utilities
import java.util.*
import kotlin.collections.ArrayList


class ApkListAdapter(var apkList: ArrayList<Apk>, private val context: Context) : RecyclerView.Adapter<ApkListAdapter.ApkListViewHolder>() {


    var apkListFiltered: ArrayList<Apk> = ArrayList()

    init {

        apkListFiltered.addAll(apkList)
    }

    fun updateApkList() {
        notifyDataSetChanged()
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ApkListViewHolder {
        val itemBinding = LayoutApkItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ApkListViewHolder(itemBinding, context, apkList)
    }

    override fun onBindViewHolder(holder: ApkListViewHolder, position: Int) {
        //print("onBindViewHolder: "+apkList[position].appInfo)
        holder.mIconImageView.setImageDrawable(context.packageManager.getApplicationIcon(apkList[position].packageName))
        holder.mLabelTextView.text = apkList[position].appName
        //holder.mLabelTextView.text =
        //  context.packageManager.getApplicationLabel(apkList.get(position).appInfo).toString()
        //holder.mPackageTextView.text = "Tap for more Info"
        /*if(position < 3 ) {
            holder.mIconIndicator.setBackgroundResource(R.color.colorRed_900)
            val anim: Animation = AlphaAnimation(0.0f, 1.0f)
            anim.setDuration(1000) //You can manage the blinking time with this parameter

            anim.setStartOffset(20)
            anim.setRepeatMode(Animation.REVERSE)
            anim.setRepeatCount(Animation.INFINITE)
            holder.mIconIndicator.startAnimation(anim)
        }*/

    }

    override fun getItemCount(): Int {
        return apkList.size
    }

    // Filter Class
    fun filter(c: String) {

        var charText = c
        println("apkList  : " + apkList.size)
        println("1 apkList Filtered : " + apkListFiltered.size)
        charText = charText.toLowerCase(Locale.getDefault())
        apkList.clear()
        if (charText.isEmpty()) {
            println("apkList Filtered : " + apkListFiltered.size)
            apkList.addAll(apkListFiltered)
        } else {
            for (wp in apkListFiltered) {
                if (wp.appName.toLowerCase(Locale.getDefault()).contains(charText)) {
                    apkList.add(wp)
                }
            }
        }
        notifyDataSetChanged()
    }


    inner class ApkListViewHolder(b: LayoutApkItemBinding, context: Context, apkList: ArrayList<Apk>) : RecyclerView.ViewHolder(b.root) {

        val mIconImageView: ImageView = b.apkIconIv
        val mLabelTextView: TextView = b.apkLabelTv

        init {
            //var permissionList : String = ""
            b.root.setOnClickListener {
                val permissionDetails = Utilities.getPermissionDetails(context, apkList[adapterPosition].packageName)
                println("One")

                var pos = 0
                if (permissionDetails.requestedPermissionsFlags != null) permissionDetails.requestedPermissionsFlags.forEach {

                    if ((it and PackageInfo.REQUESTED_PERMISSION_GRANTED) == PackageInfo.REQUESTED_PERMISSION_GRANTED) {
                        println("Granted: " + permissionDetails.requestedPermissions[pos])
                    }
                    pos++
                }

                val bottomSheetFragment = BottomSheetFragment(apkList[adapterPosition])
                val frag = context as FragmentActivity
                bottomSheetFragment.show(frag.supportFragmentManager, bottomSheetFragment.tag)

                /*val viewDialog = LayoutInflater.from(context).inflate(R.layout.bottom_sheet_permission_manager, null)
                val dialog = BottomSheetDialog(context)
                dialog.setContentView(viewDialog)*/

                /*viewDialog.findViewById<TextView>(R.id.textView).setOnClickListener{
                    print(apkList.get(adapterPosition).packageName)
                    dialog.dismiss()
                }

                viewDialog.findViewById<TextView>(R.id.textView2).setOnClickListener{
                    print(apkList.get(adapterPosition).packageName)
                    dialog.dismiss()
                }

                viewDialog.findViewById<TextView>(R.id.textView3).setOnClickListener{
                    print(apkList.get(adapterPosition).packageName)
                    dialog.dismiss()
                }*/

            }


        }
    }


}