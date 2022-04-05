package com.example.ffmpegvideorange2

import android.content.Intent
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.io.File
import java.util.*

class FileListActivity : AppCompatActivity() {
    var inputPath = Environment.getExternalStorageDirectory().toString() + "/testVideo"

    private var recyclerView: RecyclerView? = null
    private var adapter: FileListAdapter? = null
    private var dataList: List<String>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_file_list)

        recyclerView = findViewById<View>(R.id.recycler_view) as RecyclerView?
        adapter = FileListAdapter(this)
        recyclerView?.setLayoutManager(LinearLayoutManager(this))
        recyclerView?.setAdapter(adapter)

        dataList = getFileListData()
        adapter?.setData(dataList)
        adapter?.setOnItemClickListener(FileListAdapter.OnItemClickListener { position ->
            val intent = Intent(
                this@FileListActivity,
                VideoRangeActivity::class.java
            )
            intent.putExtra("filePath", dataList!![position])
            startActivity(intent)
        })
    }


    private fun getFileListData(): List<String>? {
        val file = File(inputPath)
        val files = file.listFiles()
        if (files == null) {
            Log.e("error", "空目录")
            return null
        }
        val s: MutableList<String> = ArrayList()
        for (i in files.indices) {
            s.add(files[i].absolutePath)
        }
        return s
    }


    fun goToVideoRange(view: View?) {
        val intent = Intent(
            this@FileListActivity,
            PreviewFrameActivity::class.java
        )
        startActivity(intent)
    }
}