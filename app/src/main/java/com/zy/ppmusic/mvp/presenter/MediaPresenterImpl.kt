package com.zy.ppmusic.mvp.presenter

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.os.ResultReceiver
import android.provider.DocumentsContract
import android.support.v4.media.session.MediaControllerCompat
import com.zy.ppmusic.App
import com.zy.ppmusic.mvp.contract.IMediaActivityContract
import com.zy.ppmusic.mvp.model.MediaActivityModelImpl
import com.zy.ppmusic.utils.DataProvider
import com.zy.ppmusic.utils.FileUtils
import com.zy.ppmusic.utils.PrintLog
import com.zy.ppmusic.utils.toast
import kotlinx.coroutines.*

/**
 * @author stealfeam
 */
class MediaPresenterImpl(view: IMediaActivityContract.IMediaActivityView) : IMediaActivityContract.AbstractMediaActivityPresenter(view) {
    private val scanningScope = MainScope()

    override fun getChildrenUri(): String {
        return mModel.getGrantedChildrenUri()
    }

    override fun removeQueueItem(position: Int) {
        mModel?.removeQueueItem(position)
    }

    override fun getGrantedRootUri(): String {
        return mModel.getGrantedRootUri()
    }

    override fun setGrantedRootUri(uri: String, child: String) {
        mModel.saveGrantedUri(uri, child)
    }

    override fun createModel(): IMediaActivityContract.IMediaActivityModel {
        return MediaActivityModelImpl()
    }

    override fun attachModelController(controller: MediaControllerCompat?) {
        mModel.attachController(controller)
    }

    override fun refreshQueue(isRefresh: Boolean) {
        if (mView.get() == null) {
            PrintLog.i("view is null")
            return
        }
        mView.get()?.showLoading()
        scanningScope.launch {
            withContext(Dispatchers.IO) { DataProvider.get().loadData(isRefresh) }
            mView.get()?.hideLoading()
            mView.get()?.loadFinished(isRefresh)
        }
    }

    override fun getLocalMode(c: Context) {
        mModel.getLocalMode(c, e = {
            mView?.get()?.setRepeatMode(it)
        })
    }

    override fun deleteFile(path: String?): Boolean {
        return FileUtils.deleteFile(path)
    }

    override fun deleteFile(includeFile: Boolean, position: Int): Boolean? {
        return if (includeFile) {
            val path = DataProvider.get().getPath(position)
            val result = deleteFile(path)
            if (result) {
                scanningScope.launch {
                    removeQueueItem(position)
                    withContext(Dispatchers.IO) { DataProvider.get().removeItemIncludeFile(position) }
                }
                true
            } else {
                val supportResult = doSupportDelAction(position)
                if (supportResult == true) {
                    scanningScope.launch {
                        removeQueueItem(position)
                        withContext(Dispatchers.IO) { DataProvider.get().removeItemIncludeFile(position) }
                    }
                }
                supportResult
            }
        } else {
            removeQueueItem(position)
            true
        }
    }

    /**
     * 修复5.0后无法删除外部存储文件
     */
    private fun doSupportDelAction(position: Int): Boolean? {
        val contentResolver = App.instance!!.contentResolver
        return if (getGrantedRootUri().isNotEmpty()) {
            val path = DataProvider.get().getPath(position) ?: return false
            try {
                val cursor = contentResolver.query(Uri.parse(getChildrenUri()), arrayOf(DocumentsContract.Document.COLUMN_DOCUMENT_ID), null, null, null)
                return cursor?.use {
                    val uriList = mutableListOf<Uri>()
                    val name = path.splitToSequence("/").last()
                    while (!it.isLast && it.moveToNext()) {
                        val documentID = it.getString(0)
                        println("ID => $documentID")
                        if (documentID.contains(name)) {
                            uriList.add(DocumentsContract.buildDocumentUriUsingTree(Uri.parse(getGrantedRootUri()), documentID))
                        }
                    }
                    println("uriList===>$uriList")
                    return if (uriList.asSequence().map { delUri -> DocumentsContract.deleteDocument(contentResolver, delUri) }.none { result -> !result }) {
                        toast("删除成功")
                        removeQueueItem(position)
                        true
                    } else {
                        println("执行删除错误")
                        toast("删除失败")
                        false
                    }
                } ?: false
            } catch (e: SecurityException) {
                setGrantedRootUri("", "")
                return null
            } catch (e: Exception) {
                e.printStackTrace()
                toast("删除失败, 请联系开发者")
                return false
            }
                //                //0000-0000
                //                val rootId = getRootId(mPresenter!!.getGrantedRootUri())
                //                println("rootId...$rootId,和rootId相比的值--$this")
                //                try {
                //                    val delUri = DocumentsContract.buildDocumentUriUsingTree(Uri.parse(mPresenter.getChildrenUri()),
                //                            "$rootId:${substringAfter(rootId)}")
                //                    println("删除的uri----$delUri")
                //                    if (DocumentsContract.deleteDocument(contentResolver, delUri)) {
                //                        toast("删除成功")
                //                        notifyDelItem(position)
                //                    } else {
                //                        println("执行删除错误")
                //                        toast("删除失败")
                //                    }
                //                } catch (e: SecurityException) {
                //                    mPresenter?.setGrantedRootUri("", "")
                //                    doSupportDelAction(position)
                //                } catch (e: Exception) {
                //
                //                }
        } else {
            null
        }
    }

    override fun sendCommand(method: String, params: Bundle, resultReceiver: ResultReceiver) {
        mModel.postSendCommand(method, params, resultReceiver)
    }

    override fun playWithId(mediaId: String, extra: Bundle) {
        mModel.postPlayWithId(mediaId, extra)
    }

    override fun sendCustomAction(action: String, extra: Bundle) {
        mModel.postSendCustomAction(action, extra)
    }

    override fun setRepeatMode(context: Context, mode: Int) {
        mModel.postSetRepeatMode(context, mode)
    }

    override fun skipNext() {
        mModel.postSkipNext()
    }

    override fun skipToPosition(id: Long) {
        mModel.postSkipToPosition(id)
    }

    override fun skipPrevious() {
        mModel.postSkipPrevious()
    }

    override fun detachViewAndModel() {
        super.detachViewAndModel()
        mModel.shutdown()
    }
}
