package com.example.demo.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.demo.App
import com.example.demo.model.datasource.ApiSource

class MovieViewModelFactory : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MovieViewModel::class.java)) {
            return MovieViewModel(
                client = ApiSource.client,
                movieDao = App.database.movieDao()
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}