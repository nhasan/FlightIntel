/*
 * FlightIntel for Pilots
 *
 * Copyright 2011-2022 Nadeem Hasan <nhasan@nadmm.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.nadmm.airports

import android.annotation.SuppressLint
import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteQueryBuilder
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.*
import android.view.animation.AnimationUtils
import android.widget.*
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.core.view.MenuProvider
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.preference.PreferenceManager
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.navigation.NavigationView
import com.nadmm.airports.aeronav.ChartsDownloadActivity
import com.nadmm.airports.afd.AfdMainActivity
import com.nadmm.airports.clocks.ClocksActivity
import com.nadmm.airports.data.DatabaseManager
import com.nadmm.airports.data.DatabaseManager.*
import com.nadmm.airports.data.DownloadActivity
import com.nadmm.airports.dof.NearbyObstaclesActivity
import com.nadmm.airports.e6b.E6bActivity
import com.nadmm.airports.library.LibraryActivity
import com.nadmm.airports.scratchpad.ScratchPadActivity
import com.nadmm.airports.tfr.TfrListActivity
import com.nadmm.airports.utils.*
import com.nadmm.airports.views.MultiSwipeRefreshLayout
import com.nadmm.airports.wx.WxMainActivity
import java.util.*

abstract class ActivityBase : AppCompatActivity(), MultiSwipeRefreshLayout.CanChildScrollUpCallback {

    lateinit var dbManager: DatabaseManager
        private set

    private var mActionBarToolbar: Toolbar? = null
    private var mSwipeRefreshLayout: SwipeRefreshLayout? = null
    private var mAppBar: AppBarLayout? = null
    private var mDrawerLayout: DrawerLayout? = null
    private var mDrawerToggle: ActionBarDrawerToggle? = null
    private var mNavigationView: NavigationView? = null

    private lateinit var mInflater: LayoutInflater
    private lateinit var mHandler: Handler
    private lateinit var mPreferences: SharedPreferences

    private val mBackStackChangedListener =
            FragmentManager.OnBackStackChangedListener { this.updateDrawerToggle() }

    protected val actionBarToolbar: Toolbar?
        get() {
            if (mActionBarToolbar == null) {
                mAppBar = findViewById(R.id.appbar)
                mActionBarToolbar = findViewById(R.id.toolbar_actionbar)
                if (mActionBarToolbar != null) {
                    setSupportActionBar(mActionBarToolbar)
                    val actionBar = supportActionBar
                    if (actionBar != null) {
                        actionBar.setHomeButtonEnabled(true)
                        actionBar.setDisplayHomeAsUpEnabled(true)
                    }
                }
            }
            return mActionBarToolbar
        }

    protected open val selfNavDrawerItem: Int = NAVDRAWER_ITEM_INVALID

    var isRefreshing: Boolean
        get() = mSwipeRefreshLayout?.isRefreshing ?: false
        set(refreshing) {
                mSwipeRefreshLayout?.isRefreshing = refreshing
        }

    val prefHomeAirport: String
        get() = mPreferences.getString(PreferencesActivity.KEY_HOME_AIRPORT, "") ?: ""

    val prefUseGps: Boolean
        get() = mPreferences.getBoolean(PreferencesActivity.KEY_LOCATION_USE_GPS, false)

    val prefNearbyRadius: Int
        get() =  mPreferences.getString(PreferencesActivity.KEY_LOCATION_NEARBY_RADIUS, null)
                ?.toIntOrNull() ?: 30

    val prefShowExtraRunwayData: Boolean
        get() = mPreferences.getBoolean(PreferencesActivity.KEY_SHOW_EXTRA_RUNWAY_DATA, false)

    val prefShowGpsNotam: Boolean
        get() = mPreferences.getBoolean(PreferencesActivity.KEY_SHOW_GPS_NOTAMS, false)

    val prefAutoDownoadOnMeteredNetwork: Boolean
        get() = mPreferences.getBoolean(PreferencesActivity.KEY_AUTO_DOWNLOAD_ON_3G, false)

    val prefDisclaimerAgreed: Boolean
        get() = mPreferences.getBoolean(PreferencesActivity.KEY_DISCLAIMER_AGREED, false)

    val prefShowLocalTime: Boolean
        get() = mPreferences.getBoolean(PreferencesActivity.KEY_SHOW_LOCAL_TIME, false)

    val prefHomeScreen: String
        get() = mPreferences.getString(PreferencesActivity.KEY_HOME_SCREEN, "A/FD") ?: ""

    val prefAlwaysShowNearby: Boolean
        get() = mPreferences.getBoolean(PreferencesActivity.KEY_ALWAYS_SHOW_NEARBY, false)

    val prefAlertsEnabled: Boolean
        get() = mPreferences.getBoolean(PreferencesActivity.KEY_FCM_ENABLE, false)

    override fun onCreate(savedInstanceState: Bundle?) {
        mPreferences = PreferenceManager.getDefaultSharedPreferences(this)

        val theme = mPreferences.getString(PreferencesActivity.KEY_THEME, null)
            ?: resources.getString(R.string.theme_default)
        val mode = PreferencesActivity.getNighMode(theme)
        AppCompatDelegate.setDefaultNightMode(mode)

        super.onCreate(savedInstanceState)

        dbManager = instance(this)
        mInflater = layoutInflater
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out)

        mHandler = Handler(Looper.getMainLooper())

        val intent = intent
        if (intent.hasExtra(EXTRA_MSG)) {
            val msg = intent.getStringExtra(EXTRA_MSG)
            Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
        }

        addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                // Add menu items here
                menuInflater.inflate(R.menu.mainmenu, menu)

                val searchManager = getSystemService(Context.SEARCH_SERVICE) as SearchManager
                (menu.findItem(R.id.menu_search).actionView as SearchView).apply {
                    setSearchableInfo(searchManager.getSearchableInfo(componentName))
                    setIconifiedByDefault(false)
                }
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                if (mDrawerToggle?.onOptionsItemSelected(menuItem) == true) {
                    return true
                }

                return when (menuItem.itemId) {
                    android.R.id.home -> {
                        onBackPressed()
                        true
                    }
                    R.id.menu_search -> true
                    else -> true
                }
            }
        })
    }

    override fun onPause() {
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
        supportFragmentManager.removeOnBackStackChangedListener(mBackStackChangedListener)
        super.onPause()
    }

    override fun onResume() {
        super.onResume()

        mNavigationView?.setCheckedItem(selfNavDrawerItem)

        // Whenever the fragment back stack changes, we may need to update the
        // action bar toggle: only top level screens show the hamburger-like icon, inner
        // screens - either Activities or fragments - show the "Up" icon instead.
        supportFragmentManager.addOnBackStackChangedListener(mBackStackChangedListener)
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)

        setupNavDrawer()
        trySetupSwipeRefresh()
        enableDisableSwipeRefresh(false)

        // Sync the toggle state after onRestoreInstanceState has occurred.
        mDrawerToggle?.syncState()
    }

    override fun setContentView(layoutResID: Int) {
        super.setContentView(layoutResID)
        actionBarToolbar
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (mDrawerToggle?.onOptionsItemSelected(item) == true) {
            return true
        }

        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                true
            }
            R.id.menu_search -> true
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onBackPressed() {
        // If the drawer is open, back will close it
        if (mDrawerLayout?.isDrawerOpen(GravityCompat.START) == true) {
            mDrawerLayout?.closeDrawers()
            return
        }
        // Otherwise, it may return to the previous fragment stack
        val fragmentManager = supportFragmentManager
        if (fragmentManager.backStackEntryCount > 0) {
            fragmentManager.popBackStack()
        } else {
            // Lastly, it will rely on the system behavior for back
            super.onBackPressed()
        }
    }

    private fun setupNavDrawer() {
        val selfItem = selfNavDrawerItem

        mDrawerLayout = findViewById(R.id.drawer_layout) ?: return

        mNavigationView = mDrawerLayout?.findViewById(R.id.navdrawer)
        if (selfItem == NAVDRAWER_ITEM_INVALID) {
            // do not show a nav drawer
            mNavigationView?.apply {
                (parent as ViewGroup).removeView(this)
            }
            mDrawerLayout = null
            return
        }

        mDrawerToggle = object : ActionBarDrawerToggle(this, mDrawerLayout, actionBarToolbar,
                R.string.drawer_open, R.string.drawer_close) {
            override fun onDrawerClosed(view: View) {
                invalidateOptionsMenu()
                onNavDrawerStateChanged()
            }

            override fun onDrawerOpened(drawerView: View) {
                onNavDrawerStateChanged()
                invalidateOptionsMenu() // creates call to onPrepareOptionsMenu()
            }
        }

        mDrawerToggle?.apply {
            setToolbarNavigationClickListener { onBackPressed() }
            isDrawerSlideAnimationEnabled = false
            mDrawerLayout?.addDrawerListener(this)
        }
        updateDrawerToggle()

        // Initialize navigation drawer
        mNavigationView!!.setNavigationItemSelectedListener { item ->
            item.isChecked = true
            val id = item.itemId

            if (id != selfNavDrawerItem) {
                // Launch the target Activity after a short delay to allow the drawer close
                // animation to finish without stutter
                mHandler.postDelayed({ goToNavDrawerItem(id) }, NAVDRAWER_LAUNCH_DELAY.toLong())
            }

            mDrawerLayout?.closeDrawer(GravityCompat.START)
            false
        }
    }

    private fun updateDrawerToggle() {
        mDrawerToggle?.apply {
            val isRoot = supportFragmentManager.backStackEntryCount == 0
            isDrawerIndicatorEnabled = isRoot
            mDrawerLayout?.setDrawerLockMode(
                if (isRoot) DrawerLayout.LOCK_MODE_UNLOCKED
                else DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
            supportActionBar?.apply {
                setDisplayShowHomeEnabled(!isRoot)
                setDisplayHomeAsUpEnabled(!isRoot)
                setHomeButtonEnabled(!isRoot)
            }
            if (isRoot) {
                syncState()
            }
        }
    }

    private fun goToNavDrawerItem(id: Int) {
        val intent: Intent
        when (id) {
            R.id.navdrawer_afd -> {
                intent = Intent(this, AfdMainActivity::class.java)
                startActivity(intent)
                finish()
            }
            R.id.navdrawer_wx -> {
                intent = Intent(this, WxMainActivity::class.java)
                startActivity(intent)
                finish()
            }
            R.id.navdrawer_dof -> {
                intent = Intent(this, NearbyObstaclesActivity::class.java)
                startActivity(intent)
                finish()
            }
            R.id.navdrawer_tfr -> {
                intent = Intent(this, TfrListActivity::class.java)
                startActivity(intent)
                finish()
            }
            R.id.navdrawer_library -> {
                intent = Intent(this, LibraryActivity::class.java)
                startActivity(intent)
                finish()
            }
            R.id.navdrawer_scratchpad -> {
                intent = Intent(this, ScratchPadActivity::class.java)
                startActivity(intent)
                finish()
            }
            R.id.navdrawer_clocks -> {
                intent = Intent(this, ClocksActivity::class.java)
                startActivity(intent)
                finish()
            }
            R.id.navdrawer_e6b -> {
                intent = Intent(this, E6bActivity::class.java)
                startActivity(intent)
                finish()
            }
            R.id.navdrawer_charts -> {
                intent = Intent(this, ChartsDownloadActivity::class.java)
                startActivity(intent)
                finish()
            }
            R.id.navdrawer_download -> {
                val download = Intent(this, DownloadActivity::class.java)
                startActivity(download)
            }
            R.id.navdrawer_about -> {
                val about = Intent(this, AboutActivity::class.java)
                startActivity(about)
            }
            R.id.navdrawer_settings -> {
                intent = Intent(this, PreferencesActivity::class.java)
                startActivity(intent)
            }
        }
    }

    fun setDrawerIndicatorEnabled(enable: Boolean) {
        mDrawerToggle?.isDrawerIndicatorEnabled = enable
    }

    private fun trySetupSwipeRefresh() {
        mSwipeRefreshLayout = findViewById(R.id.swipe_refresh_layout) ?: return
        mSwipeRefreshLayout?.setColorSchemeResources(
                R.color.refresh_progress_1,
                R.color.refresh_progress_2,
                R.color.refresh_progress_3)
        mSwipeRefreshLayout?.setOnRefreshListener { this.requestDataRefresh() }

        if (mSwipeRefreshLayout is MultiSwipeRefreshLayout) {
            val mswrl = mSwipeRefreshLayout as MultiSwipeRefreshLayout?
            mswrl!!.setCanChildScrollUpCallback(this)
        }
    }

    override fun canSwipeRefreshChildScrollUp(): Boolean {
        return false
    }

    fun enableDisableSwipeRefresh(enable: Boolean) {
        mSwipeRefreshLayout?.isEnabled = enable
    }

    protected open fun requestDataRefresh() {}

    protected fun showAppBar(show: Boolean) {
        mAppBar?.setExpanded(show, true)
    }

    open fun onFragmentStarted(fragment: FragmentBase) {
        showAppBar(true)
    }

    // Subclasses can override this for custom behavior
    protected fun onNavDrawerStateChanged() {}

    protected open fun externalStorageStatusChanged() {
        if (!SystemUtils.isExternalStorageAvailable()) {
            val intent = Intent(this, ExternalStorageActivity::class.java)
            startActivity(intent)
        }
    }

    fun createContentView(id: Int): View? {
        return createContentView(inflate(id))
    }

    fun createContentView(view: View?): View? {
        view ?: return null
        val root = FrameLayout(this)
        root.layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)

        val pframe = LinearLayout(this)
        pframe.id = R.id.INTERNAL_PROGRESS_CONTAINER_ID
        pframe.gravity = Gravity.CENTER

        val progress = ProgressBar(this, null, android.R.attr.progressBarStyleLarge)
        pframe.addView(progress, FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        root.addView(pframe, FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))

        val lframe = FrameLayout(this)
        lframe.id = R.id.INTERNAL_FRAGMENT_CONTAINER_ID
        lframe.visibility = View.GONE

        lframe.addView(view, FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        root.addView(lframe, FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))

        return root
    }

    fun setContentShown(shown: Boolean) {
        val root = findViewById<View>(android.R.id.content)
        setContentShown(root, shown, true)
    }

    fun setContentShown(view: View?, shown: Boolean) {
        setContentShown(view, shown, true)
    }

    fun setContentShownNoAnimation(view: View?, shown: Boolean) {
        setContentShown(view, shown, false)
    }

    private fun setContentShown(view: View?, shown: Boolean, animation: Boolean) {
        view ?: return
        val progress = view.findViewById<View>(R.id.INTERNAL_PROGRESS_CONTAINER_ID)
        progress ?: return
        val content = view.findViewById<View>(R.id.INTERNAL_FRAGMENT_CONTAINER_ID)
        content ?: return

        if (shown) {
            if (animation) {
                progress.startAnimation(AnimationUtils.loadAnimation(this, R.anim.fade_out))
                content.startAnimation(AnimationUtils.loadAnimation(this, R.anim.fade_in))
            }
            progress.visibility = View.GONE
            content.visibility = View.VISIBLE
        } else {
            if (animation) {
                progress.startAnimation(AnimationUtils.loadAnimation(this, R.anim.fade_in))
                content.startAnimation(AnimationUtils.loadAnimation(this, R.anim.fade_out))
            }
            progress.visibility = View.VISIBLE
            content.visibility = View.GONE
        }
    }

    fun setContentMsg(msg: String) {
        val tv = TextView(this)
        tv.gravity = Gravity.CENTER_HORIZONTAL or Gravity.CENTER_VERTICAL
        tv.setPadding(dpToPx(12f), dpToPx(8f), dpToPx(12f), dpToPx(8f))
        tv.text = msg
        setContentView(createContentView(tv))
    }

    fun replaceFragment(clss: Class<*>, args: Bundle?, addToStack: Boolean): Fragment {
        return replaceFragment(clss, args, R.id.fragment_container, addToStack)
    }

    @JvmOverloads
    fun replaceFragment(clss: Class<*>, args: Bundle?, id: Int = R.id.fragment_container,
                        addToStack: Boolean = true): Fragment {
        var tag = clss.simpleName
        if (args != null && args.containsKey(FRAGMENT_TAG_EXTRA)) {
            val extra = args.getString(FRAGMENT_TAG_EXTRA)
            if (extra != null) {
                tag += extra
            }
        }
        val fm = supportFragmentManager
        var f = fm.findFragmentByTag(tag)
        if (f == null) {
            f = fm.fragmentFactory.instantiate(classLoader, clss.name)
            f.arguments = args
        }
        val ft = fm.beginTransaction()
        ft.replace(id, f, tag)
        if (addToStack) {
            ft.addToBackStack(tag)
        }
        ft.commit()
        return f
    }

    @Suppress("DEPRECATION")
    @JvmOverloads
    protected fun addFragment(clss: Class<*>, args: Bundle?,
                              id: Int = R.id.fragment_container): Fragment {
        val tag = clss.simpleName
        val fm = supportFragmentManager
        var f = fm.findFragmentByTag(tag)
        if (f == null) {
            f = Fragment.instantiate(this, clss.name, args)
            val ft = fm.beginTransaction()
            ft.add(id, f, tag)
            ft.commit()
        }
        return f
    }

    fun <T : View> inflate(resId: Int): T {
        return inflate(resId, null)
    }

    @Suppress("UNCHECKED_CAST")
    fun <T : View> inflate(resId: Int, root: ViewGroup?): T {
        return mInflater.inflate(resId, root, false) as T
    }

    fun getAirportDetails(siteNumber: String): Cursor? {
        val db = getDatabase(DB_FADDS)
        val builder = SQLiteQueryBuilder()
        builder.tables = "${Airports.TABLE_NAME} a LEFT OUTER JOIN ${States.TABLE_NAME} s " +
                "ON a.${Airports.ASSOC_STATE} = s.${States.STATE_CODE}"
        var c = builder.query(db, arrayOf("*"), "${Airports.SITE_NUMBER} = ?",
                arrayOf(siteNumber), null, null, null, null)
        if (!c.moveToFirst()) {
            c.close()
            c = null
        }

        return c
    }

    fun getDatabase(type: String): SQLiteDatabase {
        val db = dbManager.getDatabase(type)
        if (db == null) {
            val intent = Intent(this, DownloadActivity::class.java)
            intent.putExtra("MSG", "Database is corrupted. Please delete and re-install")
            startActivity(intent)
            finish()
        }
        return db
    }

    @SuppressLint("SetTextI18n")
    fun showAirportTitle(c: Cursor) {
        val root = findViewById<View>(R.id.airport_title_layout)
        var tv = root.findViewById<TextView>(R.id.facility_name)
        var code: String? = c.getString(c.getColumnIndex(Airports.ICAO_CODE))
        if (code.isNullOrBlank()) {
            code = c.getString(c.getColumnIndex(Airports.FAA_CODE))
        }
        val tower = c.getString(c.getColumnIndex(Airports.TOWER_ON_SITE))
        val color = if (tower == "Y") Color.rgb(64, 128, 192) else Color.rgb(160, 48, 92)
        tv.setTextColor(color)
        val name = c.getString(c.getColumnIndex(Airports.FACILITY_NAME))
        val siteNumber = c.getString(c.getColumnIndex(Airports.SITE_NUMBER))
        val type = DataUtils.decodeLandingFaclityType(siteNumber)
        tv.text = "$name $type"
        tv = root.findViewById(R.id.facility_id)
        tv.setTextColor(color)
        tv.text = code
        tv = root.findViewById(R.id.facility_info)
        val city = c.getString(c.getColumnIndex(Airports.ASSOC_CITY))
        var state: String? = c.getString(c.getColumnIndex(States.STATE_NAME))
        if (state.isNullOrBlank()) {
            state = c.getString(c.getColumnIndex(Airports.ASSOC_COUNTY))
        }
        tv.text = "$city, $state"
        tv = root.findViewById(R.id.facility_info2)
        val distance = c.getInt(c.getColumnIndex(Airports.DISTANCE_FROM_CITY_NM))
        val dir = c.getString(c.getColumnIndex(Airports.DIRECTION_FROM_CITY))
        val status = c.getString(c.getColumnIndex(Airports.STATUS_CODE))
        tv.text = "${DataUtils.decodeStatus(status)}, $distance miles $dir of city center"
        tv = root.findViewById(R.id.facility_info3)
        val elevMsl = c.getFloat(c.getColumnIndex(Airports.ELEVATION_MSL))
        var tpaAgl = c.getInt(c.getColumnIndex(Airports.PATTERN_ALTITUDE_AGL))
        var est = ""
        if (tpaAgl == 0) {
            tpaAgl = 1000
            est = " (est.)"
        }
        tv.text = "${FormatUtils.formatFeet(elevMsl)} MSL elev. - " +
                "${FormatUtils.formatFeet(elevMsl + tpaAgl)} MSL TPA $est"

        val s = c.getString(c.getColumnIndex(Airports.EFFECTIVE_DATE))
        val endDate = GregorianCalendar(TimeZone.getTimeZone("UTC"))
        val year = s.substring(6).toInt()
        val month = s.substring(0, 2).toInt() - 1
        val day = s.substring(3, 5).toInt()
        endDate.set(year, month, day, 9, 1, 0)
        // Calculate end date of the 28-day cycle
        endDate.add(GregorianCalendar.DAY_OF_MONTH, 28)
        val now = Calendar.getInstance()
        if (!now.before(endDate)) {
            // Show the expired warning
            tv = root.findViewById(R.id.expired_label)
            tv.visibility = View.VISIBLE
        }

        val cb = root.findViewById<CheckBox>(R.id.airport_star)
        cb.isChecked = dbManager.isFavoriteAirport(siteNumber)!!
        cb.tag = siteNumber
        cb.setOnClickListener { v ->
            val cb1 = v as CheckBox
            val siteNumber1 = cb1.tag as String
            if (cb1.isChecked) {
                dbManager.addToFavoriteAirports(siteNumber1)
            } else {
                dbManager.removeFromFavoriteAirports(siteNumber1)
            }
        }

        val iv = root.findViewById<ImageView>(R.id.airport_map)
        val lat = c.getString(c.getColumnIndex(Airports.REF_LATTITUDE_DEGREES))
        val lon = c.getString(c.getColumnIndex(Airports.REF_LONGITUDE_DEGREES))
        if (lat.isNotBlank() && lon.isNotBlank()) {
            iv.tag = "geo:$lat,$lon?z=16"
            iv.setOnClickListener { v ->
                val tag = v.tag as String
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(tag))
                startActivity(intent)
            }
        } else {
            iv.visibility = View.GONE
        }
    }

    fun showNavaidTitle(c: Cursor) {
        val root = findViewById<View>(R.id.navaid_title_layout)
        val id = c.getString(c.getColumnIndex(Nav1.NAVAID_ID))
        val name = c.getString(c.getColumnIndex(Nav1.NAVAID_NAME))
        val type = c.getString(c.getColumnIndex(Nav1.NAVAID_TYPE))
        var tv = root.findViewById<TextView>(R.id.navaid_name)
        tv.text = String.format(Locale.US, "%s - %s %s", id, name, type)
        val city = c.getString(c.getColumnIndex(Nav1.ASSOC_CITY))
        val state = c.getString(c.getColumnIndex(States.STATE_NAME))
        tv = root.findViewById(R.id.navaid_info)
        tv.text = String.format(Locale.US, "%s, %s", city, state)
        val use = c.getString(c.getColumnIndex(Nav1.PUBLIC_USE))
        val elevMsl = c.getFloat(c.getColumnIndex(Nav1.ELEVATION_MSL))
        tv = root.findViewById(R.id.navaid_info2)
        tv.text = String.format(Locale.US, "%s, %s elevation",
                if (use == "Y") "Public use" else "Private use",
                FormatUtils.formatFeetMsl(elevMsl))
        tv = root.findViewById(R.id.navaid_morse1)
        tv.text = DataUtils.getMorseCode(id.substring(0, 1))
        if (id.length > 1) {
            tv = root.findViewById(R.id.navaid_morse2)
            tv.text = DataUtils.getMorseCode(id.substring(1, 2))
        }
        if (id.length > 2) {
            tv = root.findViewById(R.id.navaid_morse3)
            tv.text = DataUtils.getMorseCode(id.substring(2, 3))
        }
    }

    fun postRunnable(r: Runnable, delayMillis: Long) {
        mHandler.postDelayed(r, delayMillis)
    }

    private fun dpToPx(dp: Float): Int {
        return UiUtils.convertDpToPx(this, dp)
    }

    fun setActionBarTitle(c: Cursor) {
        val siteNumber = c.getString(c.getColumnIndex(Airports.SITE_NUMBER))
        val type = DataUtils.decodeLandingFaclityType(siteNumber)
        val name = c.getString(c.getColumnIndex(Airports.FACILITY_NAME))
        var code: String? = c.getString(c.getColumnIndex(Airports.ICAO_CODE))
        if (code.isNullOrBlank()) {
            code = c.getString(c.getColumnIndex(Airports.FAA_CODE))
        }
        supportActionBar?.title = "$code - $name $type"
    }

    fun setActionBarTitle(c: Cursor, subtitle: String) {
        var code: String? = c.getString(c.getColumnIndex(Airports.ICAO_CODE))
        if (code.isNullOrBlank()) {
            code = c.getString(c.getColumnIndex(Airports.FAA_CODE)) ?: ""
        }
        var title = code
        val isScreenWide = resources.getBoolean(R.bool.IsScreenWide)
        if (isScreenWide) {
            val siteNumber = c.getString(c.getColumnIndex(Airports.SITE_NUMBER))
            val type = DataUtils.decodeLandingFaclityType(siteNumber)
            val name = c.getString(c.getColumnIndex(Airports.FACILITY_NAME))
            title = "$code - $name $type"
        }

        setActionBarTitle(title, subtitle)
    }

    fun setActionBarTitle(title: String) {
        supportActionBar?.apply {
            this.subtitle = getTitle()
            this.title = title
        }
    }

    fun setActionBarTitle(title: String, subtitle: String?) {
        supportActionBar?.apply {
            this.title = title
            this.subtitle = subtitle
        }
    }

    fun setActionBarSubtitle(subtitle: String) {
        supportActionBar?.subtitle = subtitle
    }

    fun showFaddsEffectiveDate(c: Cursor) {
        val tv = findViewById<TextView>(R.id.effective_date) ?: return
        var s = c.getString(c.getColumnIndex(Airports.EFFECTIVE_DATE))
        val date = TimeUtils.parseFaaDate(s) ?: return
        val start = Calendar.getInstance()
        start.timeZone = TimeZone.getTimeZone("UTC")
        start.time = date
        start.add(Calendar.MINUTE, 9 * 60 + 1)
        val end = Calendar.getInstance()
        end.timeZone = TimeZone.getTimeZone("UTC")
        end.time = date
        end.add(Calendar.DATE, 28)
        end.add(Calendar.MINUTE, 9 * 60 + 1)
        s = TimeUtils.formatDateRange(this, start, end)
        tv.text = s
    }

    companion object {

        private const val NAVDRAWER_LAUNCH_DELAY = 250
        protected const val NAVDRAWER_ITEM_INVALID = -1
        protected const val EXTRA_MSG = "MSG"

        const val FRAGMENT_TAG_EXTRA = "FRAGMENT_TAG_EXTRA"
    }

}
