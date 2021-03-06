package concept.omdb.data.repo

import concept.omdb.data.dao.Movie
import concept.omdb.data.dao.MovieInfo
import concept.omdb.data.dao.Search
import io.reactivex.Observable
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Provides movies' data from local db or remote API
 */
@Singleton
class MovieDataRepository @Inject constructor() : MovieRepository {

    @Inject lateinit var localData: LocalDataSource
    @Inject lateinit var remoteData: RemoteDataSource

    /**
     * Get movies' list from local cache or from remote API
     * If local cache is expired (see [LocalDataSource.expirationTime]) - reload data from API
     */
    override fun getMovies(query: String): Observable<List<Movie>> =
        localMovies(query).switchIfEmpty(Observable.defer { remoteMovies(query) })

    /**
     * Get movie info from local cache or from remote API
     * If local data exists - always return it, never reload from API
     */
    override fun getMovieInfo(imdbID: String): Observable<MovieInfo> =
        localMovieInfo(imdbID).switchIfEmpty(Observable.defer { remoteMovieInfo(imdbID) })

    /**
     * Load last search from local DB
     * If search does not exists yet (on first app start), returns empty object (query is null)
     */
    override fun getLastSearch(): Observable<Search> =
        Observable.fromCallable { localData.getLastSearch() }

    /**
     * Get all saved queries from local DB
     */
    override fun getAllQueries(): Observable<List<String>> = Observable
        .fromCallable { localData.getAllSearches() }
        .map { search -> search.map { it.query } }

    /**
     * Private functions to keep public functions' code shorter, readable and testable
     */
    private fun localMovies(query: String): Observable<List<Movie>> = Observable
        .fromCallable { localData.getMovies(query) }
        .filter { it.isNotEmpty() }

    private fun remoteMovies(query: String): Observable<List<Movie>> = remoteData
        .getMovies(title = query)
        .doOnNext { localData.saveMovies(query, it) }
        .map { localData.getMovies(query) }

    private fun localMovieInfo(imdbID: String): Observable<MovieInfo> = Observable
        .fromCallable { localData.getMovieInfo(imdbID) }
        .filter { !it.imdbID.isNullOrEmpty() }

    private fun remoteMovieInfo(imdbID: String): Observable<MovieInfo> = remoteData
        .getMovieInfo(imdbID = imdbID)
        .doOnNext { localData.saveMovieInfo(it) }
        .map { localData.getMovieInfo(imdbID) }
}