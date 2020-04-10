package com.bootcamp.bootcampmagic.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.GridLayoutManager
import com.bootcamp.bootcampmagic.R
import com.bootcamp.bootcampmagic.adapter.EndlessScrollListener
import com.bootcamp.bootcampmagic.adapter.GridSpacingItemDecoration
import com.bootcamp.bootcampmagic.adapter.SetsAdapter
import com.bootcamp.bootcampmagic.models.Card
import com.bootcamp.bootcampmagic.repositories.MtgRepository
import com.bootcamp.bootcampmagic.utils.App
import com.bootcamp.bootcampmagic.viewmodels.SetsViewModel
import com.bootcamp.bootcampmagic.viewmodels.SetsViewModelFactory
import com.bootcamp.bootcampmagic.viewmodels.SetsViewModelState
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.fragment_set.*

class SetsFragment() : Fragment() {


    private lateinit var adapter: SetsAdapter
    private lateinit var scrollListener: EndlessScrollListener
    private val viewModel: SetsViewModel by viewModels{
        App().let {
            SetsViewModelFactory(MtgRepository(it.getCardsDao(), it.getMtgDataSource()))
        }
    }


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_set, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        setupObservables()
        setupRecyclerView()
    }


    private fun setupObservables(){
        viewModel.getViewState().observe(viewLifecycleOwner, Observer { state ->
            if(state == null) return@Observer
            when(state){

                is SetsViewModelState.Error ->
                    when(state.message){
                        R.string.generic_network_error -> showNetworkError(state.message)
                    }


                is SetsViewModelState.BackgroundImage ->
                    (activity as ImmersiveActivity).setBackgroundImage(state.url)


                is SetsViewModelState.CacheLoaded ->
                    viewModel.refreshData()


                is SetsViewModelState.AddData ->{
                    adapter.addItems(state.items)
                    scrollListener.resume()
                }

            }
            viewModel.clearViewState()
        })


        viewModel.getData().observe(viewLifecycleOwner, Observer { items ->
            if(items.isNotEmpty()){
                adapter.setItems(items)
            }
        })


        viewModel.getSearchData().observe(viewLifecycleOwner, Observer { items ->
            if(items.isNotEmpty()){
                adapter.setItems(items)
            }
        })
    }


    private fun setupRecyclerView(){
        val listColumns = 3
        val layoutManager = GridLayoutManager(context, listColumns)
        layoutManager.spanSizeLookup = object: GridLayoutManager.SpanSizeLookup(){
            override fun getSpanSize(position: Int): Int {
                return if (adapter.isHeader(position)) layoutManager.spanCount else 1
            }
        }
        recycler_cards.layoutManager = layoutManager
        recycler_cards.addItemDecoration(GridSpacingItemDecoration(resources.getDimensionPixelSize(R.dimen.grid_item_margin)))
        adapter = SetsAdapter(clickListener)
        recycler_cards.adapter = adapter

        scrollListener = object: EndlessScrollListener(recycler_cards){
            override fun onFirstItem() {
            }
            override fun onScroll() {
            }
            override fun onLoadMore() {
                viewModel.loadMore()
            }
        }

        swipeRefresh.setOnRefreshListener {
            swipeRefresh.isRefreshing = false
            viewModel.refreshData()
        }
    }


    private val clickListener = object: SetsAdapter.OnItemClickListener{
        override fun onItemClicked(card: Card, position: Int) {
        }
    }


    private fun showNetworkError(errorMessage: Int){
        activity?.findViewById<View>(R.id.tab_set_favorites)?.let {
            Snackbar.make(it, errorMessage, Snackbar.LENGTH_LONG)
                .setAnchorView(it)
                .setAction(R.string.try_again) {
                    viewModel.loadMore()
                }
                .show()
        }
    }
}