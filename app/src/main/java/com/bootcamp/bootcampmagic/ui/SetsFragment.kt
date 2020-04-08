package com.bootcamp.bootcampmagic.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.GridLayoutManager
import com.bootcamp.bootcampmagic.R
import com.bootcamp.bootcampmagic.adapter.AdapterCards
import com.bootcamp.bootcampmagic.adapter.EndlessScrollListener
import com.bootcamp.bootcampmagic.adapter.GridSpacingItemDecoration
import com.bootcamp.bootcampmagic.models.Card
import com.bootcamp.bootcampmagic.repositories.CardsRepository
import com.bootcamp.bootcampmagic.utils.App
import com.bootcamp.bootcampmagic.viewmodels.SetsViewModel
import com.bootcamp.bootcampmagic.viewmodels.SetsViewModelFactory
import com.bootcamp.bootcampmagic.viewmodels.SetsViewModelState
import kotlinx.android.synthetic.main.fragment_set.*


class SetsFragment() : Fragment() {

    private lateinit var adapterCards: AdapterCards
    private var isRefreshing = true
    private val viewModel: SetsViewModel by viewModels{
        App().let {
            SetsViewModelFactory(CardsRepository(it.getCardsDataSource(), it.getCardsDao()))
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
        //(activity as AppCompatActivity).setSupportActionBar(toolbar)

        //recycler_cards.visibility = View.GONE
        setupRecyclerView()
        setupObservables()
    }


    private fun setupRecyclerView(){

        adapterCards = AdapterCards(clickListener)
        val spanCount = 3
        val layoutManager = GridLayoutManager(context, spanCount)
        layoutManager.spanSizeLookup = object: GridLayoutManager.SpanSizeLookup(){
            override fun getSpanSize(position: Int): Int {
                return if (adapterCards.isHeader(position)) layoutManager.spanCount else 1
            }
        }
        recycler_cards.layoutManager = layoutManager
        recycler_cards.addItemDecoration(GridSpacingItemDecoration(
            spanCount,
            resources.getDimensionPixelSize(R.dimen.grid_item_margin),
            false))
        recycler_cards.adapter = adapterCards

        val endlessScrollListener = object : EndlessScrollListener(recycler_cards){
            override fun onFirstItem() {
            }
            override fun onScroll() {
            }
            override fun onLoadMore() {
                viewModel.loadCards()
            }
        }

    }

    private val clickListener = object: AdapterCards.OnItemClickListener{
        override fun onItemClicked(card: Card, position: Int) {
        }
    }

    private fun setupObservables(){
        viewModel.getViewState().observe(viewLifecycleOwner, Observer {
            when(it){

                is SetsViewModelState.Error ->
                    showErrorMessage(it.toString())

                is SetsViewModelState.CacheLoaded -> {
                    refresh()
                }

            }
        })
        viewModel.getBackgroundImage().observe(viewLifecycleOwner, Observer {
            (activity as MainActivity).setBackgroundImage(it)
        })
        viewModel.getData().observe(viewLifecycleOwner, Observer {
            if(it.isNotEmpty()){
                when(isRefreshing){
                    true -> {
                        adapterCards.setItems(it)
                        //progress_circular.visibility = View.GONE
                    }
                    else -> adapterCards.addItems(it)
                }
                isRefreshing = false
            }
        })
    }

    private fun refresh(){
        isRefreshing = true
        viewModel.refresh()
    }

    private fun showErrorMessage(errorMessage: String){
        //progress_circular.visibility = View.GONE
        Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
    }
}