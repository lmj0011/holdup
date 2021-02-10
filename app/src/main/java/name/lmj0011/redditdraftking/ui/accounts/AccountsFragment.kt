package name.lmj0011.redditdraftking.ui.accounts

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import name.lmj0011.redditdraftking.R
import name.lmj0011.redditdraftking.database.AppDatabase
import name.lmj0011.redditdraftking.databinding.FragmentAccountsBinding
import name.lmj0011.redditdraftking.helpers.adapters.AccountListAdapter
import name.lmj0011.redditdraftking.helpers.factories.ViewModelFactory
import name.lmj0011.redditdraftking.helpers.util.launchIO

class AccountsFragment: Fragment(R.layout.fragment_accounts)  {
    private lateinit var binding: FragmentAccountsBinding
    private val  accountsViewModel by viewModels<AccountsViewModel> {
        ViewModelFactory(
            AppDatabase.getInstance(requireActivity().application).sharedDao,
            requireActivity().application)
    }
    private lateinit var listAdapter: AccountListAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        setHasOptionsMenu(true)
        return super.onCreateView(inflater, container, savedInstanceState)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupBinding(view)
        setupRecyclerView()
        setupObservers()
        setupSwipeToRefresh()
        refreshRecyclerView()
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)
        menu.findItem(R.id.action_manage_accounts).isVisible = false
    }

    private fun setupBinding(view: View) {
        binding = FragmentAccountsBinding.bind(view)
        binding.lifecycleOwner = this
        binding.accountsViewModel = accountsViewModel
    }

    private fun setupRecyclerView() {
        listAdapter = AccountListAdapter(
            AccountListAdapter.LogOutClickListener { acct ->
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Logout ${acct.name}?")
                    .setPositiveButton("Logout") { _, _ ->
                        launchIO {
                            accountsViewModel.deleteAccount(acct)
                            withContext(Dispatchers.Main) {
                                findNavController().navigate(R.id.accountsFragment)
                            }
                        }
                    }
                    .setNegativeButton("Cancel") {_, _ -> }
                    .show()
            },
            AccountListAdapter.AccountNameClickListener {_ -> }
        )

        val decor = DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL)
        binding.accountList.addItemDecoration(decor)
        binding.accountList.adapter = listAdapter
    }

    private fun setupSwipeToRefresh() {

    }

    private fun refreshRecyclerView() {
        launchIO {
            val accounts = accountsViewModel.getAccounts()

            withContext(Dispatchers.Main) {
                listAdapter.submitList(accounts)
                listAdapter.notifyDataSetChanged()
            }
        }
    }

    private fun setupObservers() {

    }

}
