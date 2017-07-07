package com.zy.ppmusic

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.*
import android.support.design.widget.NavigationView
import android.support.v4.app.ActivityCompat
import android.support.v4.media.session.PlaybackStateCompat
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
import com.zy.ppmusic.entity.MusicInfoEntity
import com.zy.ppmusic.service.PlayService
import com.zy.ppmusic.utils.PermissionUtil
import com.zy.ppmusic.utils.ScanMusicFile
import java.lang.ref.WeakReference

class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {
    var TAG = "MainActivity"
    var ivNextMusic: ImageView? = null
    var ivPlayOrPause: ImageView? = null
    var musicName: TextView? = null
    var musicManager:IMusicInterface?=null

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
            if (musicManager != null) {
                musicManager!!.next()
            }
        }

        musicName = findViewById(R.id.play_music_name) as TextView

        ivPlayOrPause = findViewById(R.id.play_or_pause) as ImageView?
        ivPlayOrPause!!.setOnClickListener {
            if (musicManager != null) {
                musicManager!!.playOrPause()
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
        mainRecycle.layoutManager = GridLayoutManager(this, 3)

        val it = Intent(this@MainActivity, PlayService::class.java)
        startService(it)

        ScanMusicFile.getInstance().setOnScanComplete(object : ScanMusicFile.OnScanComplete() {
            override fun onComplete(paths: ArrayList<String>) {
                val it = Intent(this@MainActivity, PlayService::class.java)
                it.putExtra("paths", paths)
                bindService(it, conn, Context.BIND_EXTERNAL_SERVICE)
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

    fun changeDisName(name: String) {
        musicName!!.text = name
        musicName!!.isSelected = true
    }

    /**
     * 与服务链连接的状态回调
     */
    var conn = object : ServiceConnection {
        override fun onServiceDisconnected(name: ComponentName?) {
            musicManager = null
        }

        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            println("服务已连接")
            musicManager = IMusicInterface.Stub.asInterface(service)
            musicManager!!.initPlayer(0,0)
            musicManager!!.registerMusicChange(musicChangeListener)
            musicManager!!.registerListener(stateChangeListener)
        }
    }
    /**
     * 监听播放文件
     */
    var musicChangeListener = object : IOnMusicChangeListener.Stub() {
        override fun onMusicChange(entity: com.zy.ppmusic.MusicInfoEntity?) {
            runOnUiThread {
                changeDisName(entity!!.name)
            }
        }
    }
    /**
     * 监听播放器状态
     */
    var stateChangeListener = object : IPlayerStateChangeListener.Stub() {
        override fun onPlayerStateChange(state: Int) {
            runOnUiThread {
                when(state){
                    PlaybackStateCompat.STATE_PLAYING->{
                        isPauseOrStart(false)
                    }
                    PlaybackStateCompat.STATE_PAUSED->{
                        isPauseOrStart(true)
                    }
                    else->{

                    }
                }
                println("state="+state)
            }
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

    override fun onDestroy() {
        super.onDestroy()
        if(musicManager != null && musicManager!!.asBinder().isBinderAlive){
            musicManager!!.unregisterListener(stateChangeListener)
            musicManager!!.unregisterMusicChange(musicChangeListener)
        }
        unbindService(conn)
    }
}

