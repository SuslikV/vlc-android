/*
 * ************************************************************************
 *  MainBrowserFragment.kt
 * *************************************************************************
 * Copyright © 2020 VLC authors and VideoLAN
 * Author: Nicolas POMEPUY
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston MA 02110-1301, USA.
 * **************************************************************************
 *
 *
 */

package org.videolan.vlc.gui.browser

import android.content.Intent
import android.os.Bundle
import android.view.*
import android.widget.TextView
import androidx.appcompat.view.ActionMode
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.*
import org.videolan.medialibrary.interfaces.media.MediaWrapper
import org.videolan.medialibrary.media.MediaLibraryItem
import org.videolan.resources.*
import org.videolan.tools.isStarted
import org.videolan.vlc.R
import org.videolan.vlc.gui.BaseFragment
import org.videolan.vlc.gui.SecondaryActivity
import org.videolan.vlc.gui.dialogs.CtxActionReceiver
import org.videolan.vlc.gui.dialogs.NetworkServerDialog
import org.videolan.vlc.gui.dialogs.showContext
import org.videolan.vlc.gui.helpers.UiTools.addToPlaylist
import org.videolan.vlc.gui.helpers.UiTools.addToPlaylistAsync
import org.videolan.vlc.gui.helpers.UiTools.showMediaInfo
import org.videolan.vlc.gui.helpers.hf.OTG_SCHEME
import org.videolan.vlc.media.MediaUtils
import org.videolan.vlc.repository.BrowserFavRepository
import org.videolan.vlc.viewmodels.browser.*

@ObsoleteCoroutinesApi
@ExperimentalCoroutinesApi
class MainBrowserFragment : BaseFragment(), View.OnClickListener, CtxActionReceiver {

    private var currentCtx: MainBrowserContainer? = null
    private lateinit var browserFavRepository: BrowserFavRepository
    private lateinit var localList: RecyclerView
    private lateinit var localEntry: ConstraintLayout
    private lateinit var localViewModel: BrowserModel

    private lateinit var favoritesList: RecyclerView
    private lateinit var favoritesEntry: ConstraintLayout
    private lateinit var favoritesViewModel: BrowserFavoritesModel

    private lateinit var networkList: RecyclerView
    private lateinit var networkEntry: ConstraintLayout
    private lateinit var networkViewModel: BrowserModel

    private var currentAdapterActionMode: BaseBrowserAdapter? = null

    private val containerAdapterAssociation = HashMap<MainBrowserContainer, Pair<BaseBrowserAdapter, ViewModel>>()

    override fun hasFAB() = true

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.main_browser_fragment, container, false)
    }

    override fun onActionItemClicked(mode: ActionMode?, item: MenuItem?): Boolean {
        if (!isStarted()) return false
        val list = currentAdapterActionMode?.multiSelectHelper?.getSelection() as? List<MediaWrapper>
                ?: return false
        if (list.isNotEmpty()) {
            when (item?.itemId) {
                R.id.action_mode_file_play -> MediaUtils.openList(activity, list, 0)
                R.id.action_mode_file_append -> MediaUtils.appendMedia(activity, list)
                R.id.action_mode_file_add_playlist -> requireActivity().addToPlaylist(list)
                R.id.action_mode_file_info -> requireActivity().showMediaInfo(list[0])
                else -> {
                    stopActionMode()
                    return false
                }
            }
        }
        stopActionMode()
        return true
    }

    override fun onCreateActionMode(mode: ActionMode?, menu: Menu?): Boolean {
        mode?.menuInflater?.inflate(R.menu.action_mode_browser_file, menu)
        return true
    }

    override fun onDestroyActionMode(mode: ActionMode?) {
        actionMode = null
        currentAdapterActionMode?.multiSelectHelper?.clearSelection()
        currentAdapterActionMode = null
    }

    override fun getTitle() = getString(R.string.browse)

    override fun onCreate(savedInstanceState: Bundle?) {
        browserFavRepository = BrowserFavRepository.getInstance(requireContext())
        super.onCreate(savedInstanceState)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        //local
        localEntry = view.findViewById(R.id.local_browser_entry)
        localEntry.findViewById<TextView>(R.id.title).text = getString(R.string.browser_storages)
        localList = localEntry.findViewById(R.id.list)
        localList.layoutManager = LinearLayoutManager(requireActivity(), RecyclerView.HORIZONTAL, false)
        val storageBbrowserContainer = MainBrowserContainer(isNetwork = false, isFile = true)
        val storageBrowserAdapter = BaseBrowserAdapter(storageBbrowserContainer)
        localList.adapter = storageBrowserAdapter
        localViewModel = getBrowserModel(category = TYPE_FILE, url = null, showHiddenFiles = false)
        containerAdapterAssociation[storageBbrowserContainer] = Pair(storageBrowserAdapter, localViewModel)
        localViewModel.dataset.observe(viewLifecycleOwner, Observer<List<MediaLibraryItem>> { list ->
            list?.let {
                storageBrowserAdapter.update(it)
//                updateEmptyView()
//                if (::cleanMenuItem.isInitialized) {
//                    cleanMenuItem.isVisible = list.isNotEmpty()
//                }
            }
        })
        localViewModel.browseRoot()
        localViewModel.getDescriptionUpdate().observe(viewLifecycleOwner, Observer { pair ->
            if (pair != null) storageBrowserAdapter.notifyItemChanged(pair.first, pair.second)
        })



        favoritesEntry = view.findViewById(R.id.fav_browser_entry)
        favoritesEntry.findViewById<TextView>(R.id.title).text = getString(R.string.favorite)
        favoritesList = favoritesEntry.findViewById(R.id.list)
        favoritesList.layoutManager = LinearLayoutManager(requireActivity(), RecyclerView.HORIZONTAL, false)
        val favoritesBrowserContainer = MainBrowserContainer(isNetwork = false, isFile = true)
        val favoritesAdapter = BaseBrowserAdapter(favoritesBrowserContainer)
        favoritesList.adapter = favoritesAdapter
        favoritesViewModel = BrowserFavoritesModel(requireContext())
        containerAdapterAssociation[favoritesBrowserContainer] = Pair(favoritesAdapter, favoritesViewModel)
        favoritesViewModel.updatedFavoriteList.observe(viewLifecycleOwner, Observer<List<MediaWrapper>> { list ->
            list?.let {
                favoritesAdapter.update(it)
//                updateEmptyView()
//                if (::cleanMenuItem.isInitialized) {
//                    cleanMenuItem.isVisible = list.isNotEmpty()
//                }
            }
        })
        localViewModel.browseRoot()

        networkEntry = view.findViewById(R.id.network_browser_entry)
        networkEntry.findViewById<TextView>(R.id.title).text = getString(R.string.network_browsing)
        networkList = networkEntry.findViewById(R.id.list)
        networkList.layoutManager = LinearLayoutManager(requireActivity(), RecyclerView.HORIZONTAL, false)
        val networkBrowserContainer = MainBrowserContainer(isNetwork = true, isFile = false)
        val networkAdapter = BaseBrowserAdapter(networkBrowserContainer)
        networkList.adapter = networkAdapter
        networkViewModel = getBrowserModel(category = TYPE_NETWORK, url = null, showHiddenFiles = false)
        containerAdapterAssociation[networkBrowserContainer] = Pair(networkAdapter, networkViewModel)
        networkViewModel.dataset.observe(viewLifecycleOwner, Observer<List<MediaLibraryItem>> { list ->
            list?.let {
                networkAdapter.update(it)
//                updateEmptyView()
//                if (::cleanMenuItem.isInitialized) {
//                    cleanMenuItem.isVisible = list.isNotEmpty()
//                }
            }
        })
        networkViewModel.browseRoot()
    }

    override fun onStart() {
        super.onStart()
        fabPlay?.setImageResource(R.drawable.ic_fab_add)
        fabPlay?.setOnClickListener(this)
    }

    override fun onClick(v: View) {
        if (v.id == R.id.fab) showAddServerDialog(null)
    }

    private fun showAddServerDialog(mw: MediaWrapper?) {
        val fm = try {
            parentFragmentManager
        } catch (e: IllegalStateException) {
            return
        }
        val dialog = NetworkServerDialog()
        mw?.let { dialog.setServer(it) }
        dialog.show(fm, "fragment_add_server")
    }

    inner class MainBrowserContainer(
            override val scannedDirectory: Boolean = false,
            override val mrl: String? = null,
            override val isRootDirectory: Boolean = true,
            override val isNetwork: Boolean,
            override val isFile: Boolean,
            override val inCards: Boolean = true
    ) : BrowserContainer<MediaLibraryItem> by BrowserContainerImpl(scannedDirectory, mrl, isRootDirectory, isNetwork, isFile, inCards) {
        override fun containerActivity() = requireActivity()

        fun requireAdapter() = containerAdapterAssociation[this]?.first
                ?: throw IllegalStateException("Adapter not stored on the association map")

        private fun requireViewModel() = containerAdapterAssociation[this]?.second
                ?: throw IllegalStateException("ViewModel not stored on the association map")

        private fun checkAdapterForActionMode(): Boolean {
            val adapter = requireAdapter()
            if (currentAdapterActionMode == null) {
                currentAdapterActionMode = adapter
            } else if (currentAdapterActionMode != adapter) return false
            return true
        }

        override fun onClick(v: View, position: Int, item: MediaLibraryItem) {
            val mediaWrapper = item as MediaWrapper
            if (actionMode != null) {
                if (!checkAdapterForActionMode()) return
                val adapter = requireAdapter()
                if (mediaWrapper.type == MediaWrapper.TYPE_AUDIO ||
                        mediaWrapper.type == MediaWrapper.TYPE_VIDEO ||
                        mediaWrapper.type == MediaWrapper.TYPE_DIR) {
                    adapter.multiSelectHelper.toggleSelection(position)
                    if (adapter.multiSelectHelper.getSelection().isEmpty()) stopActionMode()
                    invalidateActionMode()
                }
            } else {
                val intent = Intent(requireActivity().applicationContext, SecondaryActivity::class.java)
                intent.putExtra(KEY_MEDIA, item)
                intent.putExtra("fragment", SecondaryActivity.FILE_BROWSER)
                startActivity(intent)
            }
        }

        override fun onLongClick(v: View, position: Int, item: MediaLibraryItem): Boolean {
            if (item.itemType != MediaLibraryItem.TYPE_MEDIA) return false
            val mediaWrapper = item as MediaWrapper
            if (mediaWrapper.type == MediaWrapper.TYPE_AUDIO ||
                    mediaWrapper.type == MediaWrapper.TYPE_VIDEO ||
                    mediaWrapper.type == MediaWrapper.TYPE_DIR) {
                if (!checkAdapterForActionMode()) return false
                val adapter = requireAdapter()
                adapter.multiSelectHelper.toggleSelection(position)
                if (actionMode == null) startActionMode()
            } else onCtxClick(v, position, item)
            return true
        }

        override fun onImageClick(v: View, position: Int, item: MediaLibraryItem) {
            if (actionMode != null) {
                onClick(v, position, item)
                return
            }
            onLongClick(v, position, item)
        }

        override fun onCtxClick(v: View, position: Int, item: MediaLibraryItem) {

            if (actionMode == null && item.itemType == MediaLibraryItem.TYPE_MEDIA) lifecycleScope.launch {
                val viewModel = requireViewModel()

                val mw = item as MediaWrapper
                if (mw.uri.scheme == "content" || mw.uri.scheme == OTG_SCHEME) return@launch
                var flags = 0
                val isEmpty = (viewModel as? BrowserModel)?.isFolderEmpty(mw) ?: true
                if (!isEmpty) flags = flags or CTX_PLAY
                val isFileBrowser = isFile && item.uri.scheme == "file"
                val favExists = withContext(Dispatchers.IO) { browserFavRepository.browserFavExists(mw.uri) }
                flags = if (favExists) {
                    if (withContext(Dispatchers.IO) { browserFavRepository.isFavNetwork(mw.uri) }) flags or CTX_FAV_EDIT or CTX_FAV_REMOVE
                    else flags or CTX_FAV_REMOVE
                } else flags or CTX_FAV_ADD
                if (isFileBrowser) {
                    if (localViewModel.provider.hasMedias(mw)) flags = flags or CTX_ADD_FOLDER_PLAYLIST
                    if (localViewModel.provider.hasSubfolders(mw)) flags = flags or CTX_ADD_FOLDER_AND_SUB_PLAYLIST
                }
                if (flags != 0) {
                    showContext(requireActivity(), this@MainBrowserFragment, position, item.getTitle(), flags)
                    currentCtx = this@MainBrowserContainer
                }
            }
        }
    }

    override fun onCtxAction(position: Int, option: Int) {
        val adapter = currentCtx?.requireAdapter() ?: return
        val mw = adapter.getItem(position) as? MediaWrapper
                ?: return
        when (option) {
            CTX_PLAY -> MediaUtils.openMedia(activity, mw)
            CTX_FAV_REMOVE -> lifecycleScope.launch(Dispatchers.IO) { browserFavRepository.deleteBrowserFav(mw.uri) }
            CTX_ADD_FOLDER_PLAYLIST -> requireActivity().addToPlaylistAsync(mw.uri.toString(), false)
            CTX_ADD_FOLDER_AND_SUB_PLAYLIST -> requireActivity().addToPlaylistAsync(mw.uri.toString(), true)
            CTX_FAV_EDIT -> showAddServerDialog(mw)
        }
    }
}