package com.example.demo.viewmodel

import androidx.annotation.StringRes
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.demo.model.api.MovieApi
import com.example.demo.model.dao.MovieDao
import com.example.demo.model.entity.Movie
import com.example.demo.model.entity.MovieEntity
import com.example.demo.model.entity.movieEntityMapper
import com.example.demo.model.entity.movieMapper
import kotlinx.coroutines.*
import java.util.concurrent.TimeoutException

class MovieViewModel(
    private val client: MovieApi,
    private val movieDao: MovieDao
) : ViewModel() {

    private val _movieListUI = MutableLiveData<MovieListUI>()
    val movieListUI: LiveData<MovieListUI> = _movieListUI

    fun changeFavouriteState(movie: Movie, isFavourite: Boolean) {
        viewModelScope.launch {
            try {
                if (isFavourite) {
                    movieDao.insert(MovieEntity.from(movie))
                    _movieListUI.value = MovieListUI.MovieInserted(movie.copy(isFavourite = true))
                }
            } catch (e: Exception) {
                println("MovieInsertException: $e")
                _movieListUI.value = MovieListUI.MovieIsAlreadyFavourite
            }
        }
    }
    fun fetchPopularMovieList() {
        _movieListUI.value = MovieListUI.Loading(true)

        viewModelScope.launch {
            try {
                val cachedMovieList = withTimeoutOrNull(3000) {
                    movieDao.getAll().map(movieEntityMapper)
                }

                if (cachedMovieList != null) {
                    _movieListUI.value = MovieListUI.Success(cachedMovieList)
                } else {
                    val movieList = withContext(Dispatchers.IO) {
                        val movieListDeferred = async { client.fetchMovieList() }
                        val movieList = movieListDeferred.await()
                        movieList.results.map(movieMapper)
                    }
                    _movieListUI.value = MovieListUI.Success(movieList)

                    movieList.forEach { movie ->
                        launch {
                            try {
                                movieDao.insert(MovieEntity.from(movie))
                            }
                            catch (e: Exception) {
                                println("MovieInsertException: $e")
                            }
                        }
                    }
                }

            } catch (e: TimeoutException) {
                _movieListUI.value = MovieListUI.Error(errorMessage = com.example.demo.R.string.timeout_error) // Replace with your actual error string resource
            } catch (e: Exception) {
                println("MovieRetrieveError: $e")
                _movieListUI.value = MovieListUI.Error(errorMessage = com.example.demo.R.string.network_error) // Replace with your actual error string resource
            } finally {
                _movieListUI.value = MovieListUI.Loading(false)
            }
        }
    }
}

sealed interface MovieListUI {
    data class Loading(val isLoading: Boolean) : MovieListUI
    data class Error(@StringRes val errorMessage: Int) : MovieListUI
    data class Success(val movieList: List<Movie>) : MovieListUI
    data object Empty : MovieListUI
    data class MovieInserted(val movie: Movie) : MovieListUI
    data object MovieIsAlreadyFavourite : MovieListUI
}