package com.crimson.mvvm_frame

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.MenuItem
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.RecyclerView
import com.crimson.mvvm.base.BaseActivity
import com.crimson.mvvm.base.BaseViewModel
import com.crimson.mvvm.binding.adapter.ViewPager2FragmentAdapter
import com.crimson.mvvm.binding.bindAdapter
import com.crimson.mvvm.binding.bindTabLayout
import com.crimson.mvvm.binding.bindViewPager2
import com.crimson.mvvm.binding.consumer.bindConsumer
import com.crimson.mvvm.binding.consumer.bindTiConsumer
import com.crimson.mvvm.coroutines.callRemoteLiveDataAsync
import com.crimson.mvvm.coroutines.ioCoroutineGlobal
import com.crimson.mvvm.ext.logw
import com.crimson.mvvm.ext.view.toast
import com.crimson.mvvm.livedata.SingleLiveData
import com.crimson.mvvm.net.errorResponseCode
import com.crimson.mvvm.net.handle
import com.crimson.mvvm.rx.bus.RxCode
import com.crimson.mvvm.rx.bus.RxDisposable
import com.crimson.mvvm_frame.databinding.ActivityTabBinding
import com.crimson.mvvm_frame.model.AuthorModel
import com.crimson.mvvm_frame.model.kdo.TabListEntity
import com.trello.rxlifecycle3.android.lifecycle.kotlin.bindToLifecycle
import io.reactivex.disposables.Disposable
import kotlinx.android.synthetic.main.activity_tab.*
import org.koin.androidx.viewmodel.ext.android.getViewModel
import org.koin.core.inject

/**
 * @author crimson
 * @date   2019-12-22
 *  use viewpager2 with tablayout
 *  see: https://developer.android.com/guide/navigation/navigation-swipe-view-2
 */
class TabActivity : BaseActivity<ActivityTabBinding, TabViewModel>() {


    override fun initContentView(savedInstanceState: Bundle?): Int {
        return R.layout.activity_tab
    }

    override fun initViewModelId(): Int {
        return BR.viewModel
    }

    override fun initViewModel(): TabViewModel? {
        return getViewModel()
    }

    override fun initTitleText(): CharSequence? {
        return "欧拉欧拉欧拉"
    }

    override fun isTitleTextCenter(): Boolean {
        return true
    }

    override fun initMenuRes(): Int? {
        return R.menu.tab_menu
    }


    override fun onMenuItemSelected(item: MenuItem) {
        when (item.itemId) {
            R.id.page_refresh -> {
                logw("refresh page")
                toast("refresh page")


            }

        }

    }

    override fun initView() {

        vm?.getData()


    }


    override fun initViewObservable() {


        vm?.tabDataCompleteLD?.observe(this, Observer {

            tab_layout.bindViewPager2(view_pager, this, vm?.fragments, it)

            (view_pager.getChildAt(0) as? RecyclerView)?.setItemViewCacheSize(
                vm?.fragments?.size ?: 4
            )

        })


    }


}

/**
 * data from coroutine
 */
class TabViewModel : BaseViewModel() {


    //koin inject
    val model by inject<AuthorModel>()

    //live data
    val tabDataCompleteLD by inject<SingleLiveData<MutableList<String>>>()

    val fragments = arrayListOf<Fragment>()


    val tabSelectChanged = bindConsumer<Int> {
        logw("tabSelectChanged -> $this")

    }


    val vp2SelectedConsumer =
        bindConsumer<Int> {

            logw("vp2page -> $this")
        }

    val vp2ScrolledConsumer =
        bindTiConsumer<Int, Float, Int> { t1, t2, t3 ->
            logw("vp2pos -> $t1 positionOffset->$t2 positionOffsetPixels -> $t3")
        }


    /**
     * run with 协程
     */
    fun getData() {

//        viewModelScope.launch {
//
//        }


        callRemoteLiveDataAsync {
            model.getData()
        }
            //观察livedata
            ?.observe(lifecycleOwner, Observer {

                //LiveData.handle() 扩展
                it.handle({
                    //when loading
                    onLoadingViewInjectToRoot()

                }, {
                    //result empty
                    onLoadingViewResult()

                }, {
                    //result error 可做错误处理
                    toast("网络错误")
                    onLoadingError()

                }, { _, responseCode ->

                    //result remote error,可根据responseCode做错误提示
                    errorResponseCode(responseCode)
                    onLoadingError()

                }, {
                    //result success
                    onLoadingViewResult()
                    ioCoroutineGlobal {
                        handleData(this)
                    }
                })
            })


    }


    private fun handleData(tabData: TabListEntity) {
        val titles = arrayListOf<String>()

        tabData.data.forEach {
            titles.add(it.name)
            val fragment = AuthorFragment()

            fragment.arguments = Bundle().apply {
                putInt("id", it.id)
            }
            fragments.add(fragment)
        }

        tabDataCompleteLD.postValue(titles)
    }

    @SuppressLint("CheckResult")
    override fun registerRxBus() {

        rxbus.toObservable(RxCode.POST_CODE, Integer::class.java)
            .bindToLifecycle(lifecycleOwner)
            .subscribe {
                if (it.toInt() == RxCode.ERROR_LAYOUT_CLICK_CODE) {
                    getData()
                }
            }


    }


}


