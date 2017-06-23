package com.zy.ppmusic

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.*
import android.support.design.widget.NavigationView
import android.support.v4.app.ActivityCompat
import android.support.v4.view.GravityCompat
import android.support.v4.widget.DrawerLayout
import android.support.v7.app.ActionBarDrawerToggle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.Toolbar
import android.view.Menu
import android.view.MenuItem
import android.widget.ImageView
import android.widget.TextView
import com.zy.ppmusic.adapter.MainMenuAdapter
import com.zy.ppmusic.bl.BlScanActivity
import com.zy.ppmusic.entity.MainMenuEntity
import com.zy.ppmusic.service.PlayService
import com.zy.ppmusic.utils.PermissionUtil
import com.zy.ppmusic.utils.ScanMusicFile
import java.lang.ref.WeakReference

class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {
    var TAG = "MainActivity"
    var ivNextMusic: ImageView? = null
    var ivPlayOrPause: ImageView? = null
    var messenger: Messenger? = null//playerService的消息传递
    var clientMessenger = Messenger(MsgHandler(this))//消息接受
    var isPause: Boolean = true
    var musicName: TextView? = null

    /**
     * 服务消息回传处理
     */
    private class MsgHandler(activity: MainActivity) : Handler() {
        private val weakReference: WeakReference<MainActivity> = WeakReference(activity)

        override fun handleMessage(msg: Message) {
            if (weakReference.get() != null) {
                when (msg.what) {
                    PlayService.START_OR_PAUSE -> {
                        println("操作修改成功")
                        weakReference.get()!!.isPause = !weakReference.get()!!.isPause
                        weakReference.get()!!.isPauseOrStart(weakReference.get()!!.isPause)
                    }
                    PlayService.PLAY_NEXT -> {
                        println("请求已收到")
                    }
                    PlayService.INIT_PLAYER -> {

                    }
                    else -> {
                        println("收到其他类型消息--what=" + msg.what)
                    }
                }
                val entity = msg.data
                if (entity != null) {
                    val musicPath = entity.getString("path")
                    val musicDuration = entity.getInt("duration")
                    weakReference.get()!!.changeDisName(musicPath)
                    println("收到服务的消息--" + musicPath + "\n" + musicDuration)
                }
            }
            super.handleMessage(msg)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val toolbar = findViewById(R.id.toolbar) as Toolbar
        setSupportActionBar(toolbar)

        val drawer = findViewById(R.id.drawer_layout) as DrawerLayout
        val toggle = ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close)
        drawer.addDrawerListener(toggle)
        toggle.syncState()

        val navigationView = findViewById(R.id.nav_view) as NavigationView
        navigationView.setNavigationItemSelectedListener(this)

        ivNextMusic = findViewById(R.id.next) as ImageView?
        ivNextMusic!!.setOnClickListener {
            if (messenger != null) {
                sendMsgToService(PlayService.PLAY_NEXT)
            }
        }

        musicName = findViewById(R.id.play_music_name) as TextView

        ivPlayOrPause = findViewById(R.id.play_or_pause) as ImageView?
        ivPlayOrPause!!.setOnClickListener {
            if (messenger != null) {
                sendMsgToService(PlayService.START_OR_PAUSE)
            }
        }

        val list = PermissionUtil.checkPermission(this, "android.permission.READ_EXTERNAL_STORAGE")
                as ArrayList
        if (list.size > 0) {
            ActivityCompat.requestPermissions(this, list.toTypedArray(), 0x001)
        }

        val entities = java.util.ArrayList<MainMenuEntity>()
        entities.add(MainMenuEntity("打开蓝牙", R.mipmap.ic_launcher_round))
        entities.add(MainMenuEntity("本地乐库", R.mipmap.ic_launcher_round))
        entities.add(MainMenuEntity("热点分享", R.mipmap.ic_launcher_round))

        val mainRecycle = findViewById(R.id.recycle_main_menu) as RecyclerView
        val mainAdapter = MainMenuAdapter(entities)
        mainAdapter.setListener { _, position ->
            when (position) {
                0 -> {
                    val it = Intent(this@MainActivity, BlScanActivity::class.java)
                    startActivity(it)
                }
            }
        }
        mainRecycle.adapter = mainAdapter
        mainRecycle.layoutManager = GridLayoutManager(this, 3) as RecyclerView.LayoutManager?

        ScanMusicFile.getInstance().setOnScanComplete(object : ScanMusicFile.OnScanComplete() {
            override fun onComplete(paths: ArrayList<String>) {
                val it = Intent(this@MainActivity, PlayService::class.java)
                it.putExtra("paths", paths)
                bindService(it, conn, Context.BIND_AUTO_CREATE)
                println("启动service")
            }
        }).scanMusicFile(applicationContext)
    }

    /**
     * 设置播放按钮的图片
     */
    fun isPauseOrStart(isPause: Boolean) {
        if (isPause) {
            ivPlayOrPause!!.setImageResource(R.mipmap.play)
        } else {
            ivPlayOrPause!!.setImageResource(R.mipmap.pause)
        }
    }

    fun changeDisName(path: String) {
        val arr = path.substring((path.lastIndexOf("/") + 1), path.lastIndexOf("."))
        musicName!!.text = arr
        musicName!!.isSelected = true
    }

    fun sendMsgToService(type: Int) {
        val msg = Message.obtain()
        msg.what = type
        msg.replyTo = clientMessenger
        messenger!!.send(msg)
    }

    /**
     * 与服务链连接的状态回调
     */
    var conn = object : ServiceConnection {
        override fun onServiceDisconnected(name: ComponentName?) {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            println("服务已连接")
            messenger = Messenger(service)
            sendMsgToService(PlayService.INIT_PLAYER)
        }
    }

    override fun onBackPressed() {
        val drawer = findViewById(R.id.drawer_layout) as DrawerLayout
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        when (item.itemId) {
            R.id.action_settings -> return true
            else -> return super.onOptionsItemSelected(item)
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        // Handle navigation view item clicks here.
        when (item.itemId) {
            R.id.nav_share -> {

            }
        }

        val drawer = findViewById(R.id.drawer_layout) as DrawerLayout
        drawer.closeDrawer(GravityCompat.START)
        return true
    }
}

