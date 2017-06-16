package com.zy.ppmusic

import android.os.Bundle
import android.support.design.widget.NavigationView
import android.support.v4.app.ActivityCompat
import android.support.v4.view.GravityCompat
import android.support.v4.widget.DrawerLayout
import android.support.v7.app.ActionBarDrawerToggle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.Toolbar
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import com.zy.ppmusic.adapter.MainMenuAdapter
import com.zy.ppmusic.entity.MainMenuEntity
import com.zy.ppmusic.utils.FileUtils
import com.zy.ppmusic.utils.PermissionUtil
import com.zy.ppmusic.utils.ScanMusicFile

class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {
    var TAG = "MainActivity"

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

        val list = PermissionUtil.checkPermission(this,"android.permission.READ_EXTERNAL_STORAGE")
                    as ArrayList
        if(list.size > 0){
            ActivityCompat.requestPermissions(this, list.toTypedArray(), 0x001)
        }
        ScanMusicFile.getInstance().scanMusicFile(this)

        val entities = java.util.ArrayList<MainMenuEntity>()
        entities.add(MainMenuEntity("打开蓝牙", R.mipmap.ic_launcher_round))
        entities.add(MainMenuEntity("本地乐库", R.mipmap.ic_launcher_round))
        entities.add(MainMenuEntity("热点分享", R.mipmap.ic_launcher_round))

        val mainRecycle = findViewById(R.id.recycle_main_menu) as RecyclerView
        mainRecycle.adapter = MainMenuAdapter(entities)
        mainRecycle.layoutManager = GridLayoutManager(this,3)

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

