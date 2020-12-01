package name.lmj0011.redditdraftking.helpers.interfaces

import android.view.View

interface FragmentBaseInit {

    /**
     *  These functions should be called in Fragment.onViewCreated
     *  in the following order
     */

    fun setupBinding(view: View)

    fun setupObservers()

    fun setupRecyclerView()
}