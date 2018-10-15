package com.github.jasync.sql.db.pool

import com.github.jasync.sql.db.util.complete
import com.github.jasync.sql.db.util.failure
import com.github.jasync.sql.db.util.flatMap
import com.github.jasync.sql.db.util.onComplete
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executor

/**
 *
 * Defines the common interface for sql object pools. These are pools that do not block clients trying to acquire
 * a resource from it. Different than the usual synchronous pool, you **must** return objects back to it manually
 * since it's impossible for the pool to know when the object is ready to be given back.
 *
 * @tparam T
 */

interface AsyncObjectPool2<T> {

  /**
   *
   * Returns an object from the pool to the callee , the returned future. If the pool can not create or enqueue
   * requests it will fill the returned <<scala.concurrent.Future>> , an
   * <<com.github.jasync.sql.db.pool.PoolExhaustedException>>.
   *
   * @return future that will eventually return a usable pool object.
   */

  fun take(): CompletableFuture<T>

  /**
   *
   * Returns an object taken from the pool back to it. This object will become available for another client to use.
   * If the object is invalid or can not be reused for some reason the <<scala.concurrent.Future>> returned will contain
   * the error that prevented this object of being added back to the pool. The object is then discarded from the pool.
   *
   * @param item
   * @return
   */

  fun giveBack(item: T): CompletableFuture<Unit>

  /**
   *
   * Closes this pool and future calls to **take** will cause the <<scala.concurrent.Future>> to raise an
   * <<com.github.jasync.sql.db.pool.PoolAlreadyTerminatedException>>.
   *
   * @return
   */

  fun close(): CompletableFuture<Unit>

  /**
   *
   * Retrieve and use an object from the pool for a single computation, returning it when the operation completes.
   *
   * @param function function that uses the object
   * @return function wrapped , take and giveBack
   */

  fun <A> use(executor: Executor, f: (T) -> CompletableFuture<A>): CompletableFuture<A> =
      take().flatMap(executor) { item ->
        val p = CompletableFuture<A>()
        try {
          f(item).onComplete(executor) { r ->
            giveBack(item).onComplete(executor) { _ ->
              p.complete(r)
            }
          }
        } catch (t: Throwable) {
          // calling f might throw exception.
          // in that case the item will be removed from the pool if identified as invalid by the factory.
          // the error returned to the user is the original error thrown by f.
          giveBack(item).onComplete(executor) { _ ->
            p.failure(t)
          }
        }

        p
      }

}