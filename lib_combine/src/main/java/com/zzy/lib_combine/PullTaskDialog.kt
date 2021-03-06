package com.zzy.lib_combine

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Bundle
import android.view.*
import android.widget.Button
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.zzy.common.bean.NetResult
import com.zzy.common.bean.RSSITaskBean
import com.zzy.common.net.HttpUtil
import com.zzy.common.util.ioSync
import com.zzy.common.util.postUIThreadDelay
import com.zzy.common.util.runUIThread
import com.zzy.common.util.toastShort
import kotlinx.android.synthetic.main.dialog_pull_data.*

/**
 * create by zuyuan on 2021/3/4
 */
class PullTaskDialog : DialogFragment() {

    private lateinit var rvTasks: RecyclerView

    private var data: List<RSSITaskBean> = emptyList()

    private val adapter by lazy {
        Adapter()
    }

    init {
        isCancelable = false
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dialog_pull_data, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        rvTasks = view.findViewById(R.id.rvTasks)
        rvTasks.layoutManager = LinearLayoutManager(context)
        ivClose.setOnClickListener {
            dismissAndFinish()
        }
        dialog!!.setOnKeyListener { _, code, _ ->
            if (code == KeyEvent.KEYCODE_BACK) {
                dismissAndFinish()
                true
            } else {
                false
            }
        }
    }

    override fun onResume() {
        super.onResume()
        ioSync {
            val result: NetResult<List<RSSITaskBean>> = HttpUtil.getAllTaskData()
            if (result.code != NetResult.SUCCESS_CODE) {
                runUIThread {
                    llLoading.visibility = View.GONE
                    tvNetError.text = result.msg
                }
            } else {
                postUIThreadDelay(300L) {
                    data = result.data!!
                    adapter.data = data
                    rvTasks.adapter = adapter
                    llLoading.visibility = View.GONE
                }
            }
        }
    }

    private fun dismissAndFinish() {
        dialog?.let { dialog ->
            dialog.setOnDismissListener {
                postUIThreadDelay(50L) {
                    dialog.ownerActivity?.finish()
                }
            }
            dialog.dismiss()
        }
    }

    class Holder(val view: Button) : RecyclerView.ViewHolder(view)

    class Adapter : RecyclerView.Adapter<Holder>() {

        var data: List<RSSITaskBean> = emptyList()
            set(value) {
                field = value
                notifyDataSetChanged()
            }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
            val tv = Button(parent.context).apply {
                layoutParams = ViewGroup.MarginLayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT)
                textSize = 14f
                gravity = Gravity.CENTER or Gravity.START
                setTextColor(Color.GRAY)
                isAllCaps = false
            }
            return Holder(tv)
        }

        override fun getItemCount(): Int = data.size

        @SuppressLint("SetTextI18n")
        override fun onBindViewHolder(holder: Holder, position: Int) {
            val taskBean = data[position]
            holder.view.text = "任务名称:\n${taskBean.task_name}\n需要的WiFi:\n${taskBean.wifi_tags.map { it.ssid }.toList()}"
        }
    }
}